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
        Log.i(TAG, "Iniciando processamento de faturas pendentes...");
        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getWritableDatabase()) {

            String queryCartoes = "SELECT id, data_vencimento_fatura, data_fechamento_fatura FROM cartoes WHERE ativo = 1";
            try (Cursor cursorCartoes = db.rawQuery(queryCartoes, null)) {
                while (cursorCartoes.moveToNext()) {
                    int idCartao = cursorCartoes.getInt(cursorCartoes.getColumnIndexOrThrow("id"));
                    int diaVencimento = cursorCartoes.getInt(cursorCartoes.getColumnIndexOrThrow("data_vencimento_fatura"));
                    int diaFechamento = cursorCartoes.getInt(cursorCartoes.getColumnIndexOrThrow("data_fechamento_fatura")); // diaFechamento não é usado na nova lógica, mas mantido para contexto
                    Log.d(TAG, "Processando Cartão ID: " + idCartao);

                    Map<String, List<DespesaOrfa>> faturasParaProcessar = new HashMap<>();

                    String queryParcelas = "SELECT p.id, p.valor, p.data_vencimento FROM parcelas_cartao p " +
                                           "JOIN transacoes_cartao tc ON p.id_transacao_cartao = tc.id " +
                                           "WHERE tc.id_cartao = ? AND p.id_fatura IS NULL";

                    try (Cursor cParc = db.rawQuery(queryParcelas, new String[]{String.valueOf(idCartao)})) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
                        SimpleDateFormat chaveSdf = new SimpleDateFormat("yyyy-MM", Locale.ROOT);

                        while (cParc.moveToNext()) {
                            int idParcela = cParc.getInt(0);
                            double valorParcela = cParc.getDouble(1);
                            String dataVencimentoParcelaStr = cParc.getString(2);

                            try {
                                Date dataVencimento = sdf.parse(dataVencimentoParcelaStr);
                                String chaveFatura = chaveSdf.format(dataVencimento);

                                faturasParaProcessar.computeIfAbsent(chaveFatura, k -> new ArrayList<>()).add(new DespesaOrfa(idParcela, valorParcela, "parcela"));
                            } catch (ParseException e) {
                                Log.e(TAG, "Data de vencimento inválida para parcela ID " + idParcela, e);
                            }
                        }
                    }

                    if (faturasParaProcessar.isEmpty()) {
                        Log.d(TAG, "Nenhuma despesa órfã encontrada para o cartão " + idCartao);
                        continue;
                    }

                    db.beginTransaction();
                    try {
                        for (Map.Entry<String, List<DespesaOrfa>> entry : faturasParaProcessar.entrySet()) {
                            String[] parts = entry.getKey().split("-");
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

                            if (statusFatura == 1) { // Fatura já está paga
                                Log.w(TAG, "Fatura " + mes + "/" + ano + " está paga. Ignorando " + despesas.size() + " despesas.");
                            } else {
                                double valorTotalOrfas = despesas.stream().mapToDouble(d -> d.valor).sum();

                                if (idFatura != -1) { // Fatura existe e está aberta
                                    Log.d(TAG, "Fatura " + mes + "/" + ano + " (ID:" + idFatura + ") aberta. Adicionando R$" + valorTotalOrfas);
                                    db.execSQL("UPDATE faturas SET valor_total = valor_total + ? WHERE id = ?", new Object[]{valorTotalOrfas, idFatura});
                                } else { // Fatura não existe, precisa ser criada
                                    Log.d(TAG, "Fatura " + mes + "/" + ano + " não existe. Criando com valor R$" + valorTotalOrfas);
                                    ContentValues fv = new ContentValues();
                                    fv.put("id_cartao", idCartao);
                                    fv.put("mes", mes);
                                    fv.put("ano", ano);
                                    fv.put("valor_total", valorTotalOrfas);
                                    fv.put("status", 0); // Fatura aberta

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
                                }
                            }
                        }
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro catastrófico ao processar faturas pendentes.", e);
        }
        Log.i(TAG, "Processamento de faturas finalizado.");
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
            Log.e(TAG, "Formato de data inválido fornecido: " + dataDespesa, ex);
            return "";
        }

        Calendar calFatura = (Calendar) calDespesa.clone();
        if (calDespesa.get(Calendar.DAY_OF_MONTH) > diaFechamento) {
            calFatura.add(Calendar.MONTH, 1);
        }

        int anoFatura = calFatura.get(Calendar.YEAR);
        int mesFatura = calFatura.get(Calendar.MONTH) + 1;

        return anoFatura + "-" + String.format(Locale.ROOT, "%02d", mesFatura);
    }
}
