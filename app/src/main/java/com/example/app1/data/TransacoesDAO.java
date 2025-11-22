package com.example.app1.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.example.app1.MeuDbHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * DAO de transações revisado para suportar parcelas (numero_parcela / total_parcelas)
 * e transacao mestre (id_mestre).
 */
public class TransacoesDAO {

    private static final String TAG = "TransacoesDAO";
    private static final SimpleDateFormat SDF_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final SimpleDateFormat SDF_DATE = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    // ------------------- Transacao POJO -------------------
    public static class Transacao {
        public int id;
        public int idUsuario;
        public int idConta;
        public double valor;
        public int tipo; // 1 receita, 2 despesa
        public int pago;
        public int recebido;
        public String dataMovimentacao;
        public String dataPagamento;
        public String descricao;
        public Integer idCategoria;
        public String observacao;
        public int recorrente;
        public int numeroParcela;
        public int totalParcelas;
        public int repetirPeriodo;
        public Integer idMestre;

        public Transacao() {}
    }

    // ------------------- CRUD / Util -------------------

    /**
     * Exclui uma transação. Se for um mestre (recorrente=1 e id_mestre IS NULL) apaga também suas parcelas.
     */
    public static boolean excluirTransacao(Context context, int id, String tipo) {
        try (SQLiteDatabase db = new MeuDbHelper(context).getWritableDatabase()) {
            try (Cursor c = db.rawQuery("SELECT recorrente, id_mestre FROM transacoes WHERE id = ?", new String[]{String.valueOf(id)})) {
                if (c.moveToFirst()) {
                    int recorrente = 0;
                    int idxRec = c.getColumnIndex("recorrente");
                    if (idxRec != -1 && !c.isNull(idxRec)) recorrente = c.getInt(idxRec);

                    int idxIdMestre = c.getColumnIndex("id_mestre");
                    boolean idMestreIsNull = idxIdMestre == -1 || c.isNull(idxIdMestre);

                    if (recorrente == 1 && idMestreIsNull) {
                        // é mestre -> desativar a recorrência, não apagar registros
                        ContentValues values = new ContentValues();
                        values.put("recorrente_ativo", 0);
                        int updated = db.update("transacoes", values, "id = ?", new String[]{String.valueOf(id)});
                        return updated > 0;
                    }
                }
            }
            // caso normal: apagar somente o registro
            return db.delete("transacoes", "id = ?", new String[]{String.valueOf(id)}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "excluirTransacao", e);
            return false;
        }
    }

    /**
     * Marca uma transação existente como paga/recebida e atualiza saldo da conta.
     * data_pagamento recebe timestamp atual.
     */
    public static boolean pagarDespesaReceberReceita(Context context, int transacaoId, int contaId, boolean isReceita, String dataPagamento) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                double valor = 0;
                try (Cursor cursor = db.rawQuery("SELECT valor FROM transacoes WHERE id = ?", new String[]{String.valueOf(transacaoId)})) {
                    if (cursor.moveToFirst()) valor = cursor.getDouble(0);
                }

                if (valor == 0) {
                    db.setTransactionSuccessful();
                    return true;
                }

                ContentValues values = new ContentValues();
                values.put(isReceita ? "recebido" : "pago", 1);
                values.put("data_pagamento", dataPagamento);

                int updated = db.update("transacoes", values, "id = ?", new String[]{String.valueOf(transacaoId)});
                if (updated == 0) {
                    db.endTransaction();
                    return false;
                }

                String op = isReceita ? "+" : "-";
                db.execSQL("UPDATE contas SET saldo = saldo " + op + " ? WHERE id = ?", new Object[]{valor, contaId});

                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "pagarDespesaReceberReceita", e);
            return false;
        }
    }

    public static boolean pagarFatura(Context context, int faturaId, int contaId, String dataPagamento) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                double valor = 0;
                try (Cursor cursor = db.rawQuery("SELECT valor_total FROM faturas WHERE id = ?", new String[]{String.valueOf(faturaId)})) {
                    if (cursor.moveToFirst()) valor = cursor.getDouble(0);
                }

                if (valor == 0) {
                    db.setTransactionSuccessful();
                    return true;
                }

                // Atualiza a fatura com status pago e data de pagamento
                ContentValues values = new ContentValues();
                values.put("status", 1);
                values.put("data_pagamento", dataPagamento);

                if (db.update("faturas", values, "id = ?", new String[]{String.valueOf(faturaId)}) == 0) {
                    db.endTransaction();
                    return false;
                }

                // Atualiza as parcelas vinculadas a essa fatura como pagas, com data de pagamento
                ContentValues parcelaValues = new ContentValues();
                parcelaValues.put("paga", 1);
                parcelaValues.put("data_pagamento", dataPagamento);

                db.update("parcelas_cartao", parcelaValues, "id_fatura = ?", new String[]{String.valueOf(faturaId)});

                // Atualiza o saldo da conta
                db.execSQL("UPDATE contas SET saldo = saldo - ? WHERE id = ?", new Object[]{valor, contaId});

                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "pagarFatura", e);
            return false;
        }
    }

    // ------------------- Inserções -------------------

    /**
     * Salva uma transação única. Se pago/recebido == 1 na criação grava data_pagamento = data_movimentacao.
     */
    public static boolean salvarTransacaoUnica(
            Context context,
            int idUsuario,
            int idConta,
            double valor,
            int tipo,
            int pago,
            int recebido,
            String dataMovimentacao,
            String descricao,
            int idCategoria,
            String observacao
    ) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                values.put("id_usuario", idUsuario);
                values.put("id_conta", idConta);
                values.put("valor", valor);
                values.put("tipo", tipo);
                values.put("pago", pago);
                values.put("recebido", recebido);
                values.put("data_movimentacao", dataMovimentacao);
                values.put("descricao", descricao);
                values.put("id_categoria", idCategoria);
                values.put("observacao", observacao);

                if (pago == 1 || recebido == 1) {
                    values.put("data_pagamento", dataMovimentacao);
                } else {
                    values.putNull("data_pagamento");
                }

                long insertedId = db.insert("transacoes", null, values);
                if (insertedId == -1) {
                    db.endTransaction();
                    return false;
                }

                if (pago == 1 || recebido == 1) {
                    String op = (tipo == 1) ? "+" : "-";
                    db.execSQL("UPDATE contas SET saldo = saldo " + op + " ? WHERE id = ?", new Object[]{valor, idConta});
                }

                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "salvarTransacaoUnica", e);
            return false;
        }
    }

    /**
     * Nova assinatura: salvarTransacaoRecorrente(...) — compatível com a chamada do seu fragment.
     *
     * idMestreParam:
     *  - se >0: usa o mestre existente (não cria novo mestre), e gera parcelas vinculadas a esse mestre
     *  - se ==0: cria um novo mestre e gera parcelas normalmente
     */
    public static boolean salvarTransacaoRecorrente(
            Context context,
            int idUsuario,
            int idConta,
            double valor,
            int tipo,
            int pago,
            int recebido,
            String dataMovimentacao,
            String descricao,
            int idCategoria,
            String observacao,
            int quantidade,    // total_parcelas
            int periodo,       // repetir_periodo (1=semanal,2=mensal,...)
            int idMestreParam  // se >0, reaproveita mestre existente
    ) {
        if (quantidade <= 0) quantidade = 1;

        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                long idMestre;
                if (idMestreParam > 0) {
                    // usa mestre existente (assumimos que exista)
                    idMestre = idMestreParam;
                } else {
                    // inserir mestre
                    ContentValues mestre = new ContentValues();
                    mestre.put("id_usuario", idUsuario);
                    mestre.put("id_conta", idConta);
                    mestre.put("valor", valor);
                    mestre.put("tipo", tipo);
                    mestre.put("pago", 0);
                    mestre.put("recebido", 0);
                    mestre.put("data_movimentacao", dataMovimentacao);
                    mestre.putNull("data_pagamento");
                    mestre.put("descricao", descricao);
                    mestre.put("id_categoria", idCategoria);
                    mestre.put("observacao", observacao);
                    mestre.put("recorrente", 1);
                    mestre.put("repetir_periodo", periodo);
                    mestre.put("total_parcelas", quantidade);
                    mestre.put("numero_parcela", 0);
                    mestre.putNull("id_mestre");
                    mestre.put("recorrente_ativo", 1);

                    long inserted = db.insert("transacoes", null, mestre);
                    if (inserted == -1) {
                        db.endTransaction();
                        return false;
                    }
                    idMestre = inserted;
                }

                // processar data base
                Date dataBase;
                String hora = "00:00:00";
                try {
                    dataBase = SDF_DATETIME.parse(dataMovimentacao);
                    if (dataMovimentacao.length() >= 19) hora = dataMovimentacao.substring(11, 19);
                } catch (Exception e) {
                    try {
                        dataBase = SDF_DATE.parse(dataMovimentacao);
                    } catch (ParseException ex) {
                        dataBase = new Date();
                    }
                }

                Calendar baseCal = Calendar.getInstance();
                baseCal.setTime(dataBase);
                int diaFixo = baseCal.get(Calendar.DAY_OF_MONTH);

                // gerar parcelas 1..quantidade
                for (int numeroParcela = 1; numeroParcela <= quantidade; numeroParcela++) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(baseCal.getTime());
                    int i = numeroParcela - 1;

                    if (periodo == 1) {
                        c.add(Calendar.DAY_OF_MONTH, 7 * i);
                    } else {
                        int meses = 0;
                        switch (periodo) {
                            case 2: meses = 1 * i; break; // mensal
                            case 3: meses = 2 * i; break; // bimestral
                            case 4: meses = 3 * i; break; // trimestral
                            case 5: meses = 6 * i; break; // semestral
                            case 6: meses = 12 * i; break; // anual
                            default: meses = 1 * i; break;
                        }
                        if (meses > 0) {
                            c.set(Calendar.DAY_OF_MONTH, 1);
                            c.add(Calendar.MONTH, meses);
                            int ultimoDia = c.getActualMaximum(Calendar.DAY_OF_MONTH);
                            c.set(Calendar.DAY_OF_MONTH, Math.min(diaFixo, ultimoDia));
                        }
                    }

                    String dataParcela = formatCalendarWithTime(c, hora);

                    ContentValues parcela = new ContentValues();
                    parcela.put("id_usuario", idUsuario);
                    parcela.put("id_conta", idConta);
                    parcela.put("valor", valor);
                    parcela.put("tipo", tipo);
                    parcela.put("descricao", descricao);
                    parcela.put("id_categoria", idCategoria);
                    parcela.put("observacao", observacao);

                    parcela.put("recorrente", 0);
                    parcela.put("repetir_periodo", 0);

                    parcela.put("total_parcelas", quantidade);
                    parcela.put("numero_parcela", numeroParcela);

                    parcela.put("data_movimentacao", dataParcela);
                    parcela.put("id_mestre", idMestre);

                    if (numeroParcela == 1 && (pago == 1 || recebido == 1)) {
                        parcela.put("pago", pago);
                        parcela.put("recebido", recebido);
                        parcela.put("data_pagamento", dataParcela);
                        String op = (tipo == 1 ? "+" : "-");
                        db.execSQL("UPDATE contas SET saldo = saldo " + op + " ? WHERE id = ?", new Object[]{valor, idConta});
                    } else {
                        parcela.put("pago", 0);
                        parcela.put("recebido", 0);
                        parcela.putNull("data_pagamento");
                    }

                    long idParcela = db.insert("transacoes", null, parcela);
                    if (idParcela == -1) {
                        db.endTransaction();
                        return false;
                    }
                }

                db.setTransactionSuccessful();
                return true;

            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "salvarTransacaoRecorrente", e);
            return false;
        }
    }

    /**
     * Salva transacao fixa (mantém registro mestre recorrente ativo) — compatibilidade.
     */
    public static boolean salvarTransacaoFixa(
            Context context,
            int idUsuario,
            int idConta,
            double valor,
            int tipo,
            int pago,
            int recebido,
            String data,
            String descricao,
            int idCategoria,
            String observacao,
            int periodo
    ) {
        try (SQLiteDatabase db = new MeuDbHelper(context).getWritableDatabase()) {
            db.beginTransaction();
            try {
                // Cria transação mestre, SEM marcar paga
                ContentValues values = new ContentValues();
                values.put("id_usuario", idUsuario);
                values.put("id_conta", idConta);
                values.put("valor", valor);
                values.put("tipo", tipo);
                values.put("pago", 0); // mestre nunca marcado como pago
                values.put("recebido", 0);
                values.put("data_movimentacao", data);
                values.putNull("data_pagamento");
                values.put("descricao", descricao);
                values.put("id_categoria", idCategoria);
                values.put("observacao", observacao);
                values.put("recorrente", 1);
                values.put("recorrente_ativo", 1);
                values.put("repetir_periodo", periodo);

                long idMestre = db.insert("transacoes", null, values);
                if (idMestre == -1) {
                    db.endTransaction();
                    return false;
                }

                // Se marcado como pago, cria transação filha correspondente
                if (pago == 1 || recebido == 1) {
                    ContentValues filha = new ContentValues();
                    filha.put("id_usuario", idUsuario);
                    filha.put("id_conta", idConta);
                    filha.put("valor", valor);
                    filha.put("tipo", tipo);
                    filha.put("pago", pago);
                    filha.put("recebido", recebido);
                    filha.put("data_movimentacao", data);
                    filha.put("data_pagamento", data);
                    filha.put("descricao", descricao);
                    filha.put("id_categoria", idCategoria);
                    filha.put("observacao", observacao);
                    filha.put("recorrente", 0); // filha não é recorrente
                    filha.put("id_mestre", idMestre); // referência ao mestre
                    filha.put("total_parcelas", 0);
                    filha.put("numero_parcela", 0);
                    filha.put("repetir_periodo", 0);

                    long idFilha = db.insert("transacoes", null, filha);
                    if (idFilha == -1) {
                        db.endTransaction();
                        return false;
                    }

                    // Atualiza saldo das contas descontando o valor da filha paga
                    String op = (tipo == 1) ? "+" : "-";
                    db.execSQL("UPDATE contas SET saldo = saldo " + op + " ? WHERE id = ?", new Object[]{valor, idConta});
                }

                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "salvarTransacaoFixa", e);
            return false;
        }
    }

    /**
     * Paga transacao recorrente: usa parcela existente ou cria parcela do mes e marca paga.
     */
    public static boolean pagarTransacaoRecorrente(Context context, int idMestre, int contaPagamentoId, String mesAno, String dataPagamento) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                int transacaoExistenteId = -1;
                try (Cursor cursor = db.rawQuery("SELECT id FROM transacoes WHERE id_mestre = ? AND substr(data_movimentacao,1,7)=?", new String[]{String.valueOf(idMestre), mesAno})) {
                    if (cursor.moveToFirst()) transacaoExistenteId = cursor.getInt(0);
                }

                if (transacaoExistenteId != -1) {
                    boolean result = pagarDespesaReceberReceita(context, transacaoExistenteId, contaPagamentoId, false, dataPagamento);
                    if (result) db.setTransactionSuccessful();
                    return result;
                } else {
                    try (Cursor mestreCursor = db.rawQuery("SELECT * FROM transacoes WHERE id = ?", new String[]{String.valueOf(idMestre)})) {
                        if (!mestreCursor.moveToFirst()) {
                            db.endTransaction();
                            return false;
                        }

                        ContentValues values = new ContentValues();
                        values.put("id_usuario", mestreCursor.getInt(mestreCursor.getColumnIndexOrThrow("id_usuario")));
                        values.put("id_conta", mestreCursor.getInt(mestreCursor.getColumnIndexOrThrow("id_conta")));
                        values.put("valor", mestreCursor.getDouble(mestreCursor.getColumnIndexOrThrow("valor")));
                        values.put("tipo", mestreCursor.getInt(mestreCursor.getColumnIndexOrThrow("tipo")));
                        values.put("pago", 1);
                        values.put("recebido", 0);

                        String diaHora = new SimpleDateFormat("dd HH:mm:ss", Locale.ROOT).format(new Date());
                        values.put("data_movimentacao", mesAno + "-" + diaHora);
                        values.put("data_pagamento", dataPagamento);
                        values.put("descricao", mestreCursor.getString(mestreCursor.getColumnIndexOrThrow("descricao")));
                        values.put("id_categoria", mestreCursor.getInt(mestreCursor.getColumnIndexOrThrow("id_categoria")));
                        values.put("observacao", mestreCursor.getString(mestreCursor.getColumnIndexOrThrow("observacao")));
                        values.put("recorrente", 1);
                        values.put("id_mestre", idMestre);

                        long newId = db.insert("transacoes", null, values);
                        if (newId == -1) {
                            db.endTransaction();
                            return false;
                        }

                        double valor = values.getAsDouble("valor");
                        db.execSQL("UPDATE contas SET saldo = saldo - ? WHERE id = ?", new Object[]{valor, contaPagamentoId});

                        db.setTransactionSuccessful();
                        return true;
                    }
                }
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "pagarTransacaoRecorrente", e);
            return false;
        }
    }

    // ---------------- Utility ----------------

    private static String formatCalendarWithTime(Calendar cal, String timePart) {
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return String.format(Locale.ROOT, "%04d-%02d-%02d %s", year, month, day, timePart);
    }

    // -------------------- RECEITAS --------------------

    public static double getTotalReceitasMes(Context context, int idUsuario, int ano, int mes) {
        double total = 0;
        String mesAno = String.format(Locale.ROOT, "%04d-%02d", ano, mes);

        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {

            String query = "SELECT SUM(valor) FROM transacoes " +
                    "WHERE id_usuario = ? AND tipo = 1 AND substr(data_movimentacao,1,7) = ?";
            try (Cursor cur = db.rawQuery(query, new String[]{String.valueOf(idUsuario), mesAno})) {
                if (cur.moveToFirst()) total = cur.getDouble(0);
            }

        } catch (Exception e) {
            Log.e("TransacoesDAO", "Erro getTotalReceitasMes", e);
        }
        return total;
    }

    public static double getReceitasPendentes(Context context, int idUsuario, int ano, int mes) {
        double total = 0;
        String mesAno = String.format(Locale.ROOT, "%04d-%02d", ano, mes);

        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {

            String query = "SELECT SUM(valor) FROM transacoes " +
                    "WHERE id_usuario = ? AND tipo = 1 AND recebido = 0 " +
                    "AND substr(data_movimentacao,1,7) = ?";
            try (Cursor cur = db.rawQuery(query, new String[]{String.valueOf(idUsuario), mesAno})) {
                if (cur.moveToFirst()) total = cur.getDouble(0);
            }

        } catch (Exception e) {
            Log.e("TransacoesDAO", "Erro getReceitasPendentes", e);
        }
        return total;
    }

    // -------------------- DESPESAS --------------------

    public static double getTotalDespesasMes(Context context, int idUsuario, int ano, int mes) {
        double total = 0;
        String mesAno = String.format(Locale.ROOT, "%04d-%02d", ano, mes);

        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {

            // 1) Soma das filhas no mês
            String queryFilhas = "SELECT SUM(valor) FROM transacoes " +
                    "WHERE id_usuario = ? AND tipo = 2 AND id_mestre IS NOT NULL " +
                    "AND substr(data_movimentacao,1,7) = ?";
            try (Cursor cur = db.rawQuery(queryFilhas, new String[]{String.valueOf(idUsuario), mesAno})) {
                if (cur.moveToFirst()) total = cur.getDouble(0);
            }

            // 2) Soma das despesas mestre sem filhas no mês
            String queryMestres = "SELECT SUM(valor) FROM transacoes " +
                    "WHERE id_usuario = ? AND tipo = 2 AND recorrente = 1 AND recorrente_ativo = 1 AND (total_parcelas = 0 OR total_parcelas IS NULL) " +
                    "AND id NOT IN (SELECT DISTINCT id_mestre FROM transacoes WHERE substr(data_movimentacao,1,7) = ?)";
            try (Cursor cur = db.rawQuery(queryMestres, new String[]{String.valueOf(idUsuario), mesAno})) {
                if (cur.moveToFirst()) total += cur.getDouble(0);
            }

            // 3) Soma das transações únicas (não filhas nem mestres)
            String queryUnicas = "SELECT SUM(valor) FROM transacoes " +
                    "WHERE id_usuario = ? AND tipo = 2 " +
                    "AND (id_mestre IS NULL OR id_mestre = 0) AND (recorrente IS NULL OR recorrente = 0) " +
                    "AND substr(data_movimentacao,1,7) = ?";
            try (Cursor cur = db.rawQuery(queryUnicas, new String[]{String.valueOf(idUsuario), mesAno})) {
                if (cur.moveToFirst()) total += cur.getDouble(0);
            }

            // 4) Soma das faturas do mês
            String queryFaturas = "SELECT SUM(valor_total) FROM faturas f JOIN cartoes c ON f.id_cartao = c.id " +
                    "WHERE c.id_usuario = ? AND substr(f.data_vencimento,1,7) = ?";
            try (Cursor cur = db.rawQuery(queryFaturas, new String[]{String.valueOf(idUsuario), mesAno})) {
                if (cur.moveToFirst()) total += cur.getDouble(0);
            }

        } catch (Exception e) {
            Log.e("TransacoesDAO", "Erro getTotalDespesasMes", e);
        }
        return total;
    }

    public static double getDespesasPendentes(Context context, int idUsuario, int ano, int mes) {
        double total = 0;
        String mesAno = String.format(Locale.ROOT, "%04d-%02d", ano, mes);

        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {

            // 1️⃣ Despesas pendentes normais (não recorrentes ou filhas), excluindo mestres com filhas no período
            String queryDespesas = "SELECT SUM(valor) FROM transacoes " +
                    "WHERE id_usuario = ? AND tipo = 2 AND pago = 0 " +
                    "AND NOT (recorrente = 1 AND total_parcelas > 0 AND (id_mestre IS NULL OR id_mestre = 0)) " +
                    "AND id NOT IN ( " +
                    "    SELECT DISTINCT id_mestre FROM transacoes " +
                    "    WHERE substr(data_movimentacao,1,7) = ? " +
                    ") " +
                    "AND substr(data_movimentacao,1,7) <= ?";
            try (Cursor cur = db.rawQuery(queryDespesas, new String[]{String.valueOf(idUsuario), mesAno, mesAno})) {
                if (cur.moveToFirst() && !cur.isNull(0)) total += cur.getDouble(0);
            }

            // 2️⃣ Faturas pendentes
            String queryFaturasPendentes = "SELECT SUM(valor_total) FROM faturas f " +
                    "JOIN cartoes c ON f.id_cartao = c.id " +
                    "WHERE c.id_usuario = ? AND substr(f.data_vencimento,1,7) <= ? AND f.status = 0";
            try (Cursor cur = db.rawQuery(queryFaturasPendentes, new String[]{String.valueOf(idUsuario), mesAno})) {
                if (cur.moveToFirst() && !cur.isNull(0)) total += cur.getDouble(0);
            }

            // 3️⃣ Parcelas recorrentes ainda não geradas (somente despesas)
            String queryRecorrentes = "SELECT SUM(valor) FROM transacoes AS mestre " +
                    "WHERE id_usuario = ? AND tipo = 2 AND recorrente = 1 AND recorrente_ativo = 1 " +
                    "AND total_parcelas > 0 " +
                    "AND (id_mestre IS NULL OR id_mestre = 0) " +
                    "AND NOT EXISTS ( " +
                    "    SELECT 1 FROM transacoes AS filha " +
                    "    WHERE filha.id_mestre = mestre.id " +
                    "    AND substr(filha.data_movimentacao,1,7) = ? " +
                    ")";
            try (Cursor cur = db.rawQuery(queryRecorrentes, new String[]{String.valueOf(idUsuario), mesAno})) {
                if (cur.moveToFirst() && !cur.isNull(0)) total += cur.getDouble(0);
            }

        } catch (Exception e) {
            Log.e("TransacoesDAO", "Erro getDespesasPendentes", e);
        }

        return total;
    }

    public static void ajustarSaldoParaEdicao(Context context, int idTransacao, int idContaNova, double valorNovo) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                double valorAntigo = 0;
                int idContaAntiga = -1;

                try (Cursor cursor = db.rawQuery("SELECT valor, id_conta FROM transacoes WHERE id = ?", new String[]{String.valueOf(idTransacao)})) {
                    if (cursor.moveToFirst()) {
                        valorAntigo = cursor.getDouble(cursor.getColumnIndexOrThrow("valor"));
                        idContaAntiga = cursor.getInt(cursor.getColumnIndexOrThrow("id_conta"));
                    }
                }

                // Reembolsa valor antigo na conta antiga
                if (idContaAntiga != -1) {
                    db.execSQL("UPDATE contas SET saldo = saldo + ? WHERE id = ?", new Object[]{valorAntigo, idContaAntiga});
                }

                // Debita valor novo da conta nova
                if (idContaNova != -1) {
                    db.execSQL("UPDATE contas SET saldo = saldo - ? WHERE id = ?", new Object[]{valorNovo, idContaNova});
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e("TransacoesDAO", "ajustarSaldoParaEdicao", e);
        }
    }

    public static boolean atualizarFilhasNaoPagas(
            Context context,
            int idMestre,
            double novoValor,
            int idCategoria,
            String novaDescricao,
            String novaObservacao) {
        try (SQLiteDatabase db = new MeuDbHelper(context).getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("valor", novoValor);
            values.put("id_categoria", idCategoria);
            values.put("descricao", novaDescricao);
            values.put("observacao", novaObservacao);

            int updated = db.update("transacoes", values,
                    "id_mestre = ? AND pago = 0", new String[]{String.valueOf(idMestre)});
            return updated > 0;
        } catch (Exception e) {
            Log.e("TransacoesDAO", "atualizarFilhasNaoPagas", e);
            return false;
        }
    }

}