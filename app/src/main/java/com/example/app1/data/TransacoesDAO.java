package com.example.app1.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.app1.MeuDbHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TransacoesDAO {

    public static boolean salvarTransacaoUnica(Context context, int idUsuario, int idConta, double valor,
                                               int tipo, int pago, int recebido, String dataMovimentacao,
                                               String descricao, int idCategoria, String observacao) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);

        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put("id_usuario", idUsuario);
            cv.put("id_conta", idConta);
            cv.put("valor", valor);
            cv.put("tipo", tipo);
            cv.put("pago", pago);
            cv.put("recebido", recebido);
            cv.put("data_movimentacao", dataMovimentacao);
            cv.put("descricao", descricao);
            cv.put("id_categoria", idCategoria);
            cv.put("observacao", observacao);

            long resultado = db.insert("transacoes", null, cv);

            if ((tipo == 2 && pago == 1) || (tipo == 1 && recebido == 1)) {
                atualizarSaldoConta(db, idConta, valor, tipo);
            }

            return resultado != -1;
        } catch (Exception e) {
            Log.e("TransacoesDAO", "Erro ao salvar transação única", e);
            return false;
        }
    }

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
            int repetirQtd,
            int repetirPeriodo,
            int recorrenteAtivo
    ) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues mestreCv = new ContentValues();
                mestreCv.put("id_usuario", idUsuario);
                mestreCv.put("id_conta", idConta);
                mestreCv.put("valor", valor);
                mestreCv.put("tipo", tipo);
                mestreCv.put("pago", 0);
                mestreCv.put("recebido", 0);
                mestreCv.put("data_movimentacao", dataMovimentacao);
                mestreCv.put("descricao", descricao);
                mestreCv.put("id_categoria", idCategoria);
                mestreCv.put("observacao", observacao);
                mestreCv.put("recorrente", 1);
                mestreCv.put("repetir_qtd", repetirQtd);
                mestreCv.put("repetir_periodo", repetirPeriodo);
                mestreCv.put("recorrente_ativo", recorrenteAtivo);

                long idMestre = db.insert("transacoes", null, mestreCv);
                if (idMestre == -1) return false;

                for (int i = 0; i < repetirQtd; i++) {
                    ContentValues parcelaCv = new ContentValues();
                    parcelaCv.put("id_usuario", idUsuario);
                    parcelaCv.put("id_conta", idConta);
                    parcelaCv.put("valor", valor);
                    parcelaCv.put("tipo", tipo);
                    parcelaCv.put("pago", (i == 0) ? pago : 0);
                    parcelaCv.put("recebido", (i == 0) ? recebido : 0);

                    String dataParcela = calcularDataRecorrente(dataMovimentacao, repetirPeriodo, i);
                    parcelaCv.put("data_movimentacao", dataParcela);

                    parcelaCv.put("descricao", descricao);
                    parcelaCv.put("id_categoria", idCategoria);
                    parcelaCv.put("observacao", observacao);
                    parcelaCv.put("id_mestre", idMestre);

                    long idParcela = db.insert("transacoes", null, parcelaCv);
                    if (idParcela == -1) return false;

                    if (i == 0 && ((tipo == 2 && pago == 1) || (tipo == 1 && recebido == 1))) {
                        atualizarSaldoConta(db, idConta, valor, tipo);
                    }
                }

                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e("TransacoesDAO", "Erro ao salvar transação recorrente", e);
            return false;
        }
    }
    
    public static boolean salvarTransacaoFixa(Context context, int idUsuario, int idConta, double valor, int tipo, int pago, int recebido, String dataMovimentacao, String descricao, int idCategoria, String observacao, int repetirPeriodo) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues mestreCv = new ContentValues();
                mestreCv.put("id_usuario", idUsuario);
                mestreCv.put("id_conta", idConta);
                mestreCv.put("valor", valor);
                mestreCv.put("tipo", tipo);
                mestreCv.put("pago", 0);
                mestreCv.put("recebido", 0);
                mestreCv.put("data_movimentacao", dataMovimentacao);
                mestreCv.put("descricao", descricao);
                mestreCv.put("id_categoria", idCategoria);
                mestreCv.put("observacao", observacao);
                mestreCv.put("recorrente", 1);
                mestreCv.put("repetir_qtd", 0);
                mestreCv.put("repetir_periodo", repetirPeriodo);
                mestreCv.put("recorrente_ativo", 1);

                long idMestre = db.insert("transacoes", null, mestreCv);
                if (idMestre == -1) return false;

                if ((tipo == 2 && pago == 1) || (tipo == 1 && recebido == 1)) {
                    ContentValues parcelaCv = new ContentValues();
                    parcelaCv.put("id_usuario", idUsuario);
                    parcelaCv.put("id_conta", idConta);
                    parcelaCv.put("valor", valor);
                    parcelaCv.put("tipo", tipo);
                    parcelaCv.put("pago", pago);
                    parcelaCv.put("recebido", recebido);
                    parcelaCv.put("data_movimentacao", dataMovimentacao);
                    parcelaCv.put("descricao", descricao);
                    parcelaCv.put("id_categoria", idCategoria);
                    parcelaCv.put("observacao", observacao);
                    parcelaCv.put("id_mestre", idMestre);

                    long idParcela = db.insert("transacoes", null, parcelaCv);
                    if (idParcela == -1) return false;

                    atualizarSaldoConta(db, idConta, valor, tipo);
                }

                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e("TransacoesDAO", "Erro ao salvar transação fixa", e);
            return false;
        }
    }

    public static boolean excluirTransacao(Context context, int idTransacao, String tipo) {
        if ("transacao_cartao".equals(tipo)) {
            return excluirTransacaoCartao(context, idTransacao);
        } else {
            return excluirTransacaoConta(context, idTransacao);
        }
    }

    private static boolean excluirTransacaoConta(Context context, int idTransacao) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                int idConta = -1;
                double valor = 0;
                int tipo = 0;
                int pago = 0;
                int recebido = 0;
                int idMestre = -1;
                String dataMovimentacao = "";

                try (Cursor cursor = db.rawQuery("SELECT * FROM transacoes WHERE id = ?", new String[]{String.valueOf(idTransacao)})) {
                    if (cursor.moveToFirst()) {
                        idConta = cursor.getInt(cursor.getColumnIndexOrThrow("id_conta"));
                        valor = cursor.getDouble(cursor.getColumnIndexOrThrow("valor"));
                        tipo = cursor.getInt(cursor.getColumnIndexOrThrow("tipo"));
                        pago = cursor.getInt(cursor.getColumnIndexOrThrow("pago"));
                        recebido = cursor.getInt(cursor.getColumnIndexOrThrow("recebido"));
                        idMestre = cursor.getInt(cursor.getColumnIndexOrThrow("id_mestre"));
                        dataMovimentacao = cursor.getString(cursor.getColumnIndexOrThrow("data_movimentacao"));
                    } else {
                        return false; 
                    }
                }

                if ((tipo == 2 && pago == 1) || (tipo == 1 && recebido == 1)) {
                    reverterSaldoConta(db, idConta, valor, tipo);
                }

                int deletedRows;
                if (idMestre > 0) { 
                    deletedRows = db.delete("transacoes", "id_mestre = ? AND data_movimentacao >= ? AND pago = 0 AND recebido = 0", new String[]{String.valueOf(idMestre), dataMovimentacao});
                    ContentValues cvMestre = new ContentValues();
                    cvMestre.put("recorrente_ativo", 0);
                    db.update("transacoes", cvMestre, "id = ?", new String[]{String.valueOf(idMestre)});

                } else { 
                    deletedRows = db.delete("transacoes", "id = ?", new String[]{String.valueOf(idTransacao)});
                    db.delete("transacoes", "id_mestre = ?", new String[]{String.valueOf(idTransacao)});
                }

                db.setTransactionSuccessful();
                return deletedRows > 0;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e("TransacoesDAO", "Erro ao excluir transação da conta", e);
            return false;
        }
    }

    private static boolean excluirTransacaoCartao(Context context, int idTransacaoCartao) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                db.delete("parcelas_cartao", "id_transacao_cartao = ?", new String[]{String.valueOf(idTransacaoCartao)});
                int deletedRows = db.delete("transacoes_cartao", "id = ?", new String[]{String.valueOf(idTransacaoCartao)});
                db.delete("despesas_recorrentes_cartao", "id_transacao_cartao = ?", new String[]{String.valueOf(idTransacaoCartao)});

                db.setTransactionSuccessful();
                return deletedRows > 0;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e("TransacoesDAO", "Erro ao excluir transação do cartão", e);
            return false;
        }
    }

    private static void reverterSaldoConta(SQLiteDatabase db, int idConta, double valor, int tipo) {
        try (Cursor cursor = db.rawQuery("SELECT saldo FROM contas WHERE id = ?", new String[]{String.valueOf(idConta)})) {
            if (cursor.moveToFirst()) {
                double saldoAtual = cursor.getDouble(cursor.getColumnIndexOrThrow("saldo"));
                double novoSaldo = (tipo == 1) ? (saldoAtual - valor) : (saldoAtual + valor);

                ContentValues cv = new ContentValues();
                cv.put("saldo", novoSaldo);
                db.update("contas", cv, "id = ?", new String[]{String.valueOf(idConta)});
            }
        }
    }

    private static void atualizarSaldoConta(SQLiteDatabase db, int idConta, double valor, int tipo) {
        try (Cursor cursor = db.rawQuery("SELECT saldo FROM contas WHERE id = ?", new String[]{String.valueOf(idConta)})) {
            if (cursor.moveToFirst()) {
                double saldoAtual = cursor.getDouble(cursor.getColumnIndexOrThrow("saldo"));
                double novoSaldo = (tipo == 1) ? (saldoAtual + valor) : (saldoAtual - valor);

                ContentValues cv = new ContentValues();
                cv.put("saldo", novoSaldo);
                db.update("contas", cv, "id = ?", new String[]{String.valueOf(idConta)});
            }
        }
    }

    private static String calcularDataRecorrente(String dataInicial, int periodo, int indiceParcela) {
        Calendar cal = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            cal.setTime(sdf.parse(dataInicial));
        } catch (Exception e) {
            e.printStackTrace();
        }

        switch (periodo) {
            case 1: cal.add(Calendar.WEEK_OF_YEAR, indiceParcela); break;
            case 2: cal.add(Calendar.MONTH, indiceParcela); break;
            case 3: cal.add(Calendar.MONTH, indiceParcela * 2); break;
            case 4: cal.add(Calendar.MONTH, indiceParcela * 3); break;
            case 5: cal.add(Calendar.MONTH, indiceParcela * 6); break;
            case 6: cal.add(Calendar.YEAR, indiceParcela); break;
        }

        SimpleDateFormat sdfFinal = new SimpleDateFormat("yyyy-MM-dd");
        return sdfFinal.format(cal.getTime());
    }
}
