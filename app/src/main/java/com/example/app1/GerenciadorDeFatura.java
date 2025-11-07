package com.example.app1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GerenciadorDeFatura {

    private static final String TAG = "GerenciadorDeFatura";

    private static class DespesaOrfa {
        int id;
        double valor;
        String tipo; // "parcela" ou "transacao"

        DespesaOrfa(int id, double valor, String tipo) {
            this.id = id;
            this.valor = valor;
            this.tipo = tipo;
        }
    }

    public static void processarFaturasPendentes(Context context) {
        long inicio = System.currentTimeMillis();
        Log.i(TAG, "=== Iniciando processamento de faturas pendentes ===");

        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getWritableDatabase()) {

            String queryCartoes = "SELECT id, data_vencimento_fatura, data_fechamento_fatura FROM cartoes WHERE ativo = 1";
            try (Cursor cursorCartoes = db.rawQuery(queryCartoes, null)) {
                while (cursorCartoes.moveToNext()) {
                    int idCartao = cursorCartoes.getInt(cursorCartoes.getColumnIndexOrThrow("id"));
                    int diaVencimento = cursorCartoes.getInt(cursorCartoes.getColumnIndexOrThrow("data_vencimento_fatura"));
                    int diaFechamento = cursorCartoes.getInt(cursorCartoes.getColumnIndexOrThrow("data_fechamento_fatura"));
                    Log.i(TAG, "→ Processando cartão ID: " + idCartao + " (fechamento dia " + diaFechamento + ")");

                    Map<String, List<DespesaOrfa>> faturasParaProcessar = new HashMap<>();

                    String queryParcelas = "SELECT p.id, p.valor, tc.data_compra " +
                            "FROM parcelas_cartao p " +
                            "JOIN transacoes_cartao tc ON p.id_transacao_cartao = tc.id " +
                            "WHERE tc.id_cartao = ? AND p.id_fatura IS NULL";

                    try (Cursor cParc = db.rawQuery(queryParcelas, new String[]{String.valueOf(idCartao)})) {
                        while (cParc.moveToNext()) {
                            int idParcela = cParc.getInt(0);
                            double valorParcela = cParc.getDouble(1);
                            String dataTransacao = cParc.getString(2);

                            String chaveFatura = getChaveFatura(dataTransacao, diaFechamento);
                            if (!chaveFatura.isEmpty()) {
                                faturasParaProcessar.computeIfAbsent(chaveFatura, k -> new ArrayList<>())
                                        .add(new DespesaOrfa(idParcela, valorParcela, "parcela"));
                            }
                        }
                    }

                    if (faturasParaProcessar.isEmpty()) {
                        Log.d(TAG, "Nenhuma parcela órfã encontrada para o cartão ID " + idCartao);
                        continue;
                    }

                    db.beginTransaction();
                    try {
                        for (Map.Entry<String, List<DespesaOrfa>> entry : faturasParaProcessar.entrySet()) {
                            String chave = entry.getKey();
                            String[] parts = chave.split("-");
                            int ano = Integer.parseInt(parts[0]);
                            int mes = Integer.parseInt(parts[1]);
                            List<DespesaOrfa> despesas = entry.getValue();

                            long idFatura = -1;
                            int statusFatura = -1;

                            String queryFatura = "SELECT id, status FROM faturas WHERE id_cartao = ? AND mes = ? AND ano = ?";
                            try (Cursor cf = db.rawQuery(queryFatura, new String[]{String.valueOf(idCartao), String.valueOf(mes), String.valueOf(ano)})) {
                                if (cf.moveToNext()) {
                                    idFatura = cf.getLong(0);
                                    statusFatura = cf.getInt(1);
                                }
                            }

                            if (statusFatura == 1) {
                                Log.w(TAG, "⚠ Fatura " + chave + " já está paga. Ignorando " + despesas.size() + " despesas.");
                                continue;
                            }

                            double valorTotalOrfas = despesas.stream().mapToDouble(d -> d.valor).sum();

                            if (idFatura != -1) {
                                Log.d(TAG, "Fatura existente " + chave + " (ID:" + idFatura + ") → adicionando R$" + valorTotalOrfas);
                                db.execSQL("UPDATE faturas SET valor_total = valor_total + ? WHERE id = ?", new Object[]{valorTotalOrfas, idFatura});
                            } else {
                                Log.d(TAG, "Criando nova fatura " + chave + " com valor R$" + valorTotalOrfas);
                                ContentValues fv = new ContentValues();
                                fv.put("id_cartao", idCartao);
                                fv.put("mes", mes);
                                fv.put("ano", ano);
                                fv.put("valor_total", valorTotalOrfas);
                                fv.put("status", 0);

                                Calendar calVenc = Calendar.getInstance();
                                calVenc.set(ano, mes - 1, diaVencimento);
                                fv.put("data_vencimento", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(calVenc.getTime()));

                                idFatura = db.insert("faturas", null, fv);
                            }

                            if (idFatura != -1) {
                                for (DespesaOrfa despesa : despesas) {
                                    ContentValues cv = new ContentValues();
                                    cv.put("id_fatura", idFatura);
                                    db.update("parcelas_cartao", cv, "id = ?", new String[]{String.valueOf(despesa.id)});
                                }
                                Log.d(TAG, despesas.size() + " parcelas vinculadas à fatura ID: " + idFatura);

                                // Recalcular total final
                                recalcularValorTotalFatura(db, idFatura);
                            }
                        }
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Erro grave ao processar faturas pendentes", e);
        }

        long duracao = System.currentTimeMillis() - inicio;
        Log.i(TAG, "=== Processamento concluído em " + duracao + " ms ===");
    }

    public static void recalcularValorTotalFatura(SQLiteDatabase db, long idFatura) {
        try (Cursor c = db.rawQuery(
                "SELECT SUM(p.valor) FROM parcelas_cartao p WHERE p.id_fatura = ?",
                new String[]{String.valueOf(idFatura)})) {

            if (c.moveToFirst()) {
                double total = c.getDouble(0);
                db.execSQL("UPDATE faturas SET valor_total = ? WHERE id = ?", new Object[]{total, idFatura});
                Log.d(TAG, "✔ Valor total recalculado da fatura ID " + idFatura + ": R$" + total);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao recalcular valor da fatura ID " + idFatura, e);
        }
    }

    public static String getChaveFatura(String dataDespesa, int diaFechamento) {
        Calendar calDespesa = Calendar.getInstance();
        try {
            if (dataDespesa.length() > 10) {
                calDespesa.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).parse(dataDespesa));
            } else {
                calDespesa.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(dataDespesa));
            }
        } catch (ParseException ex) {
            Log.e(TAG, "Formato de data inválido: " + dataDespesa, ex);
            return "";
        }

        Calendar calFatura = (Calendar) calDespesa.clone();

        if (calDespesa.get(Calendar.DAY_OF_MONTH) > diaFechamento) {
            calFatura.add(Calendar.MONTH, 1);
        }

        calFatura.add(Calendar.MONTH, 1);

        int anoFatura = calFatura.get(Calendar.YEAR);
        int mesFatura = calFatura.get(Calendar.MONTH) + 1;

        return anoFatura + "-" + String.format(Locale.ROOT, "%02d", mesFatura);
    }
}