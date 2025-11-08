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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

                    // Map para armazenar faturas afetadas
                    Map<String, List<DespesaOrfa>> faturasParaProcessar = new HashMap<>();

                    // --- Buscar transações únicas órfãs ---
                    String queryTransacoes = "SELECT id, valor, data_compra " +
                            "FROM transacoes_cartao " +
                            "WHERE id_cartao = ? AND id_fatura IS NULL AND parcelas = 1";
                    try (Cursor cTrans = db.rawQuery(queryTransacoes, new String[]{String.valueOf(idCartao)})) {
                        while (cTrans.moveToNext()) {
                            int idTrans = cTrans.getInt(0);
                            double valorTrans = cTrans.getDouble(1);
                            String data = cTrans.getString(2);

                            String chaveFatura = getChaveFatura(data, diaFechamento);
                            if (!chaveFatura.isEmpty()) {
                                faturasParaProcessar
                                        .computeIfAbsent(chaveFatura, k -> new ArrayList<>())
                                        .add(new DespesaOrfa(idTrans, valorTrans, "transacao"));
                            }
                        }
                    }

                    if (faturasParaProcessar.isEmpty()) {
                        Log.d(TAG, "Nenhuma transação órfã encontrada para o cartão ID " + idCartao);
                        continue;
                    }

                    db.beginTransaction();
                    try {
                        Set<Long> faturasAfetadas = new HashSet<>();

                        for (Map.Entry<String, List<DespesaOrfa>> entry : faturasParaProcessar.entrySet()) {
                            String chave = entry.getKey();
                            String[] parts = chave.split("-");
                            int ano = Integer.parseInt(parts[0]);
                            int mes = Integer.parseInt(parts[1]);
                            List<DespesaOrfa> transacoes = entry.getValue();

                            long idFatura = -1;
                            int statusFatura = -1;

                            // Verifica se fatura já existe
                            String queryFatura = "SELECT id, status FROM faturas WHERE id_cartao = ? AND mes = ? AND ano = ?";
                            try (Cursor cf = db.rawQuery(queryFatura, new String[]{String.valueOf(idCartao), String.valueOf(mes), String.valueOf(ano)})) {
                                if (cf.moveToNext()) {
                                    idFatura = cf.getLong(0);
                                    statusFatura = cf.getInt(1);
                                }
                            }

                            if (statusFatura == 1) {
                                Log.w(TAG, "⚠ Fatura " + chave + " já está paga. Ignorando " + transacoes.size() + " transações.");
                                continue;
                            }

                            // Cria fatura se não existir
                            if (idFatura == -1) {
                                ContentValues fv = new ContentValues();
                                fv.put("id_cartao", idCartao);
                                fv.put("mes", mes);
                                fv.put("ano", ano);
                                fv.put("valor_total", 0);
                                fv.put("status", 0);

                                Calendar calVenc = Calendar.getInstance();
                                calVenc.set(ano, mes - 1, diaVencimento);
                                fv.put("data_vencimento", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(calVenc.getTime()));

                                idFatura = db.insert("faturas", null, fv);
                            }

                            if (idFatura != -1) {
                                // Vincula as transações órfãs à fatura
                                for (DespesaOrfa despesa : transacoes) {
                                    ContentValues cv = new ContentValues();
                                    cv.put("id_fatura", idFatura);
                                    db.update("transacoes_cartao", cv, "id = ?", new String[]{String.valueOf(despesa.id)});
                                }
                                faturasAfetadas.add(idFatura);
                            }
                        }

                        // Recalcula o total de todas as faturas afetadas
                        for (Long idFatura : faturasAfetadas) {
                            recalcularValorTotalFatura(db, idFatura);
                        }

                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Erro ao processar faturas pendentes", e);
        }

        long duracao = System.currentTimeMillis() - inicio;
        Log.i(TAG, "=== Processamento concluído em " + duracao + " ms ===");
    }

    public static void recalcularValorTotalFatura(SQLiteDatabase db, long idFatura) {
        try {
            double totalParcelas = 0;
            try (Cursor c = db.rawQuery("SELECT SUM(valor) FROM parcelas_cartao WHERE id_fatura = ?", new String[]{String.valueOf(idFatura)})) {
                if (c.moveToFirst()) {
                    totalParcelas = c.getDouble(0);
                }
            }

            double totalTransacoesUnicas = 0;
            try (Cursor c = db.rawQuery("SELECT SUM(valor) FROM transacoes_cartao WHERE id_fatura = ? AND parcelas = 1", new String[]{String.valueOf(idFatura)})) {
                if (c.moveToFirst()) {
                    totalTransacoesUnicas = c.getDouble(0);
                }
            }

            double total = totalParcelas + totalTransacoesUnicas;
            db.execSQL("UPDATE faturas SET valor_total = ? WHERE id = ?", new Object[]{total, idFatura});
            Log.d(TAG, "✔ Valor total recalculado da fatura ID " + idFatura + ": R$" + total);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao recalcular valor da fatura ID " + idFatura, e);
        }
    }

    public static String getChaveFatura(String dataDespesa, int diaFechamento) {
        Calendar calDespesa = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = dataDespesa.length() > 10 ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT) : new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            calDespesa.setTime(sdf.parse(dataDespesa));
        } catch (ParseException ex) {
            Log.e(TAG, "Formato de data inválido: " + dataDespesa, ex);
            return "";
        }

        Calendar calVencimentoFatura = (Calendar) calDespesa.clone();

        // A fatura vence no mês seguinte ao fechamento.
        // Se a compra for depois do fechamento, o ciclo de faturamento também é do mês seguinte.
        if (calDespesa.get(Calendar.DAY_OF_MONTH) > diaFechamento) {
            calVencimentoFatura.add(Calendar.MONTH, 1); // Joga para o ciclo de faturamento seguinte
        }

        calVencimentoFatura.add(Calendar.MONTH, 1); // Adiciona 1 mês para obter o mês de vencimento

        int anoFatura = calVencimentoFatura.get(Calendar.YEAR);
        int mesFatura = calVencimentoFatura.get(Calendar.MONTH) + 1; // Calendar.MONTH é 0-based

        return anoFatura + "-" + String.format(Locale.ROOT, "%02d", mesFatura);
    }
}
