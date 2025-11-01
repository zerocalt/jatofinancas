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
                    int diaFechamento = cursorCartoes.getInt(cursorCartoes.getColumnIndexOrThrow("data_fechamento_fatura"));
                    Log.d(TAG, "Processando Cartão ID: " + idCartao);

                    Map<String, List<DespesaOrfa>> faturasParaProcessar = new HashMap<>();

                    // 1. Buscar transações órfãs (à vista)
                    String queryTransacoes = "SELECT id, valor, data_compra FROM transacoes_cartao WHERE id_cartao = ? AND id_fatura IS NULL AND (parcelas <= 1 OR parcelas IS NULL)";
                    try (Cursor cTrans = db.rawQuery(queryTransacoes, new String[]{String.valueOf(idCartao)})) {
                        while (cTrans.moveToNext()) {
                            int id = cTrans.getInt(0);
                            double valor = cTrans.getDouble(1);
                            String data = cTrans.getString(2);
                            String chaveFatura = getChaveFatura(data, diaFechamento);
                            if (chaveFatura.isEmpty()) continue;
                            faturasParaProcessar.computeIfAbsent(chaveFatura, k -> new ArrayList<>()).add(new DespesaOrfa(id, valor, "transacao"));
                        }
                    }

                    // 2. Buscar parcelas órfãs
                    String queryParcelas = "SELECT p.id, p.valor, p.data_vencimento FROM parcelas_cartao p JOIN transacoes_cartao tc ON p.id_transacao_cartao = tc.id WHERE tc.id_cartao = ? AND p.id_fatura IS NULL";
                    try (Cursor cParc = db.rawQuery(queryParcelas, new String[]{String.valueOf(idCartao)})) {
                        while (cParc.moveToNext()) {
                            int id = cParc.getInt(0);
                            double valor = cParc.getDouble(1);
                            String data = cParc.getString(2);
                            String chaveFatura = getChaveFatura(data, diaFechamento);
                            if (chaveFatura.isEmpty()) continue;
                            faturasParaProcessar.computeIfAbsent(chaveFatura, k -> new ArrayList<>()).add(new DespesaOrfa(id, valor, "parcela"));
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

                            if (statusFatura == 1) {
                                Log.w(TAG, "Fatura " + mes + "/" + ano + " está paga. Marcando " + despesas.size() + " despesas como não alocáveis.");
                                for (DespesaOrfa despesa : despesas) {
                                    ContentValues cv = new ContentValues();
                                    cv.put("id_fatura", -1);
                                    String tabela = despesa.tipo.equals("parcela") ? "parcelas_cartao" : "transacoes_cartao";
                                    db.update(tabela, cv, "id = ?", new String[]{String.valueOf(despesa.id)});
                                }
                            } else {
                                double valorTotalOrfas = 0;
                                for (DespesaOrfa d : despesas) valorTotalOrfas += d.valor;

                                if (idFatura != -1) { // Fatura existe e está aberta
                                    Log.d(TAG, "Fatura " + mes + "/" + ano + " (ID:" + idFatura + ") aberta. Adicionando R$" + valorTotalOrfas);
                                    db.execSQL("UPDATE faturas SET valor_total = valor_total + ? WHERE id = ?", new Object[]{valorTotalOrfas, idFatura});
                                } else { // Fatura não existe, CRIAR!
                                    Log.d(TAG, "Fatura " + mes + "/" + ano + " não existe. Criando com valor R$" + valorTotalOrfas);
                                    ContentValues fv = new ContentValues();
                                    fv.put("id_cartao", idCartao);
                                    fv.put("mes", mes);
                                    fv.put("ano", ano);
                                    fv.put("valor_total", valorTotalOrfas);
                                    fv.put("status", 0);

                                    Calendar calVenc = Calendar.getInstance();
                                    calVenc.set(ano, mes - 1, diaVencimento > 28 ? 28 : diaVencimento); // Evitar dias inválidos
                                    fv.put("data_vencimento", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(calVenc.getTime()));

                                    idFatura = db.insert("faturas", null, fv);
                                }

                                if (idFatura != -1) {
                                    for (DespesaOrfa despesa : despesas) {
                                        ContentValues cv = new ContentValues();
                                        cv.put("id_fatura", idFatura);
                                        String tabela = despesa.tipo.equals("parcela") ? "parcelas_cartao" : "transacoes_cartao";
                                        db.update(tabela, cv, "id = ?", new String[]{String.valueOf(despesa.id)});
                                    }
                                    Log.d(TAG, despesas.size() + " despesas vinculadas à fatura ID: " + idFatura);
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

    private static String getChaveFatura(String dataDespesa, int diaFechamento) {
        Calendar calDespesa = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
            calDespesa.setTime(sdf.parse(dataDespesa));
        } catch (ParseException e) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
                calDespesa.setTime(sdf.parse(dataDespesa));
            } catch (ParseException ex) {
                Log.e(TAG, "Data inválida: " + dataDespesa);
                return "";
            }
        }

        Calendar calFatura = (Calendar) calDespesa.clone();

        if (calDespesa.get(Calendar.DAY_OF_MONTH) > diaFechamento) {
            calFatura.add(Calendar.MONTH, 1);
        }

        return calFatura.get(Calendar.YEAR) + "-" + String.format(Locale.ROOT, "%02d", calFatura.get(Calendar.MONTH) + 1);
    }
}
