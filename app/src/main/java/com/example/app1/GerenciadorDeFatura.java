package com.example.app1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GerenciadorDeFatura {

    private static final String TAG = "GerenciadorDeFatura";

    public static void verificarELancarDespesasFixas(Context context) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            Map<Integer, int[]> cacheCartoes = new HashMap<>();

            // 1. Descobrir a data da última fatura existente no sistema
            Calendar calLimite = Calendar.getInstance();
            try (Cursor c = db.rawQuery("SELECT MAX(ano * 100 + mes) as max_data FROM faturas", null)) {
                if (c.moveToFirst() && !c.isNull(0)) {
                    int maxData = c.getInt(0);
                    int anoMax = maxData / 100;
                    int mesMax = maxData % 100;
                    
                    Calendar calMaxFatura = Calendar.getInstance();
                    calMaxFatura.set(anoMax, mesMax - 1, 1);

                    if (calMaxFatura.after(calLimite)) {
                        calLimite = calMaxFatura;
                    }
                }
            }
            calLimite.add(Calendar.MONTH, 1);

            // 2. Buscar todas as despesas fixas ativas
            String query = "SELECT id, id_usuario, id_cartao, descricao, valor_total, id_categoria, data_compra, observacao FROM transacoes_cartao WHERE fixa = 1 AND status = 1";

            try (Cursor cursorTransacoes = db.rawQuery(query, null)) {

                while (cursorTransacoes.moveToNext()) {
                    int idTransacao = cursorTransacoes.getInt(cursorTransacoes.getColumnIndexOrThrow("id"));
                    int idUsuario = cursorTransacoes.getInt(cursorTransacoes.getColumnIndexOrThrow("id_usuario"));
                    int idCartao = cursorTransacoes.getInt(cursorTransacoes.getColumnIndexOrThrow("id_cartao"));
                    String descricao = cursorTransacoes.getString(cursorTransacoes.getColumnIndexOrThrow("descricao"));
                    double valor = cursorTransacoes.getDouble(cursorTransacoes.getColumnIndexOrThrow("valor_total"));
                    int idCategoria = cursorTransacoes.getInt(cursorTransacoes.getColumnIndexOrThrow("id_categoria"));
                    String dataCompraStr = cursorTransacoes.getString(cursorTransacoes.getColumnIndexOrThrow("data_compra"));
                    String observacao = cursorTransacoes.getString(cursorTransacoes.getColumnIndexOrThrow("observacao"));

                    Date dataInicio = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dataCompraStr);

                    int[] diasCartao = cacheCartoes.get(idCartao);
                    if (diasCartao == null) {
                        try (Cursor cCartao = db.rawQuery("SELECT data_fechamento_fatura, data_vencimento_fatura FROM cartoes WHERE id = ?", new String[]{String.valueOf(idCartao)})) {
                            if (cCartao.moveToFirst()) {
                                diasCartao = new int[]{cCartao.getInt(0), cCartao.getInt(1)};
                                cacheCartoes.put(idCartao, diasCartao);
                            } else {
                                continue; 
                            }
                        }
                    }
                    int diaFechamento = diasCartao[0];
                    int diaVencimento = diasCartao[1];

                    Calendar calLoop = Calendar.getInstance();
                    calLoop.setTime(dataInicio);

                    // 3. Loop até o limite (data atual ou última fatura futura)
                    while (calLoop.before(calLimite)) {
                        Calendar calLancamento = Calendar.getInstance();
                        calLancamento.setTime(dataInicio);
                        calLancamento.set(Calendar.YEAR, calLoop.get(Calendar.YEAR));
                        calLancamento.set(Calendar.MONTH, calLoop.get(Calendar.MONTH));

                        Calendar calFaturaRef = (Calendar) calLancamento.clone();
                        if (calLancamento.get(Calendar.DAY_OF_MONTH) > diaFechamento) {
                            calFaturaRef.add(Calendar.MONTH, 1);
                        }
                        int mesFatura = calFaturaRef.get(Calendar.MONTH) + 1;
                        int anoFatura = calFaturaRef.get(Calendar.YEAR);

                        boolean parcelaExiste = false;
                        String checkParcelaSql = "SELECT 1 FROM parcelas_cartao pc " +
                                "JOIN faturas f ON pc.id_fatura = f.id " +
                                "WHERE pc.id_transacao = ? AND f.mes = ? AND f.ano = ?";
                        try (Cursor checkCursor = db.rawQuery(checkParcelaSql, new String[]{String.valueOf(idTransacao), String.valueOf(mesFatura), String.valueOf(anoFatura)})) {
                            if (checkCursor.moveToFirst()) {
                                parcelaExiste = true;
                            }
                        }

                        if (!parcelaExiste) {
                            Log.d(TAG, "Lançando despesa fixa '" + descricao + "' para a fatura " + mesFatura + "/" + anoFatura);

                            long idFatura = localizarOuCriarFatura(db, idCartao, calLancamento.getTime(), diaFechamento, diaVencimento);

                            // --- CORREÇÃO DEFINITIVA PARA O ERRO 'NOT NULL constraint failed' ---
                            String dataVencimentoFatura = null;
                            try (Cursor cFatura = db.rawQuery("SELECT data_vencimento FROM faturas WHERE id = ?", new String[]{String.valueOf(idFatura)})) {
                                if (cFatura.moveToFirst()) {
                                    dataVencimentoFatura = cFatura.getString(cFatura.getColumnIndexOrThrow("data_vencimento"));
                                }
                            }
                            
                            if (dataVencimentoFatura == null || dataVencimentoFatura.isEmpty()) {
                                Log.e(TAG, "CRÍTICO: Não foi possível encontrar a data de vencimento para a fatura ID: " + idFatura + ". Pulando lançamento.");
                                continue; // Evita o crash
                            }

                            ContentValues pVal = new ContentValues();
                            pVal.put("id_transacao", idTransacao);
                            pVal.put("id_usuario", idUsuario);
                            pVal.put("id_cartao", idCartao);
                            pVal.put("descricao", descricao);
                            pVal.put("valor", valor);
                            pVal.put("num_parcela", 1);
                            pVal.put("total_parcelas", 1);
                            pVal.put("data_compra", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calLancamento.getTime()));
                            pVal.put("id_fatura", idFatura);
                            pVal.put("fixa", 1);
                            pVal.put("id_categoria", idCategoria);
                            pVal.put("observacao", observacao);
                            pVal.put("data_vencimento", dataVencimentoFatura);

                            db.insertOrThrow("parcelas_cartao", null, pVal);

                            db.execSQL("UPDATE faturas SET valor_total = valor_total + ? WHERE id = ?", new Object[]{valor, idFatura});
                        }
                        calLoop.add(Calendar.MONTH, 1);
                    }
                }
            }

            db.setTransactionSuccessful();
            Log.i(TAG, "Verificação de despesas fixas concluída.");

        } catch (Exception e) {
            Log.e(TAG, "Erro ao verificar e lançar despesas fixas.", e);
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public static void corrigirFaturasAnteriores(Context context) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            Map<Integer, int[]> cacheCartoes = new HashMap<>();

            String sql = "SELECT id, id_fatura, id_cartao, data_compra, valor FROM parcelas_cartao";
            try (Cursor cursor = db.rawQuery(sql, null)) {

                while (cursor.moveToNext()) {
                    int idParcela = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    int idFaturaAtual = cursor.getInt(cursor.getColumnIndexOrThrow("id_fatura"));
                    int idCartao = cursor.getInt(cursor.getColumnIndexOrThrow("id_cartao"));
                    String dataCompraStr = cursor.getString(cursor.getColumnIndexOrThrow("data_compra"));
                    double valorParcela = cursor.getDouble(cursor.getColumnIndexOrThrow("valor"));

                    Date dataCompra = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dataCompraStr);

                    int[] diasCartao = cacheCartoes.get(idCartao);
                    if (diasCartao == null) {
                        try (Cursor cCartao = db.rawQuery("SELECT data_fechamento_fatura, data_vencimento_fatura FROM cartoes WHERE id = ?", new String[]{String.valueOf(idCartao)})) {
                            if (cCartao.moveToFirst()) {
                                diasCartao = new int[]{cCartao.getInt(0), cCartao.getInt(1)};
                                cacheCartoes.put(idCartao, diasCartao);
                            }
                        }
                    }

                    if (diasCartao == null) continue;

                    long idFaturaCorreta = localizarOuCriarFatura(db, idCartao, dataCompra, diasCartao[0], diasCartao[1]);

                    if (idFaturaAtual != idFaturaCorreta) {
                        ContentValues cv = new ContentValues();
                        cv.put("id_fatura", idFaturaCorreta);
                        db.update("parcelas_cartao", cv, "id = ?", new String[]{String.valueOf(idParcela)});

                        db.execSQL("UPDATE faturas SET valor_total = valor_total - ? WHERE id = ?",
                                new Object[]{valorParcela, idFaturaAtual});

                        db.execSQL("UPDATE faturas SET valor_total = valor_total + ? WHERE id = ?",
                                new Object[]{valorParcela, idFaturaCorreta});

                        Log.d(TAG, "Parcela " + idParcela + " movida da fatura " + idFaturaAtual + " para " + idFaturaCorreta);
                    }
                }
            }

            db.setTransactionSuccessful();
            Log.i(TAG, "Correção de faturas concluída com sucesso.");

        } catch (Exception e) {
            Log.e(TAG, "Erro ao corrigir faturas anteriores.", e);
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    private static long localizarOuCriarFatura(SQLiteDatabase db, int idCartao, Date dataCompra,
                                               int diaFechamento, int diaVencimento) {
        Calendar calCompra = Calendar.getInstance();
        calCompra.setTime(dataCompra);

        Calendar calFatura = (Calendar) calCompra.clone();

        if (calCompra.get(Calendar.DAY_OF_MONTH) > diaFechamento) {
            calFatura.add(Calendar.MONTH, 1);
        }

        int mesFatura = calFatura.get(Calendar.MONTH) + 1;
        int anoFatura = calFatura.get(Calendar.YEAR);

        try (Cursor c = db.rawQuery(
                "SELECT id FROM faturas WHERE id_cartao = ? AND mes = ? AND ano = ?",
                new String[]{String.valueOf(idCartao), String.valueOf(mesFatura), String.valueOf(anoFatura)})) {
            if (c.moveToFirst()) {
                return c.getLong(c.getColumnIndexOrThrow("id"));
            }
        }

        Calendar calVencimento = Calendar.getInstance();
        calVencimento.set(Calendar.YEAR, anoFatura);
        calVencimento.set(Calendar.MONTH, mesFatura - 1);
        calVencimento.set(Calendar.DAY_OF_MONTH, diaVencimento);

        if (diaVencimento < diaFechamento) {
            calVencimento.add(Calendar.MONTH, 1);
        }

        ContentValues fVals = new ContentValues();
        fVals.put("id_cartao", idCartao);
        fVals.put("mes", mesFatura);
        fVals.put("ano", anoFatura);
        fVals.put("data_vencimento", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calVencimento.getTime()));
        fVals.put("valor_total", 0);
        fVals.put("status", 0);

        return db.insert("faturas", null, fVals);
    }
}
