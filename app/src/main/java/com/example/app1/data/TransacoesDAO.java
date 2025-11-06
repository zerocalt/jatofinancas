package com.example.app1.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.app1.MeuDbHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TransacoesDAO {

    public static boolean excluirTransacao(Context context, int id, String tipo) {
        try (SQLiteDatabase db = new MeuDbHelper(context).getWritableDatabase()) {
            return db.delete("transacoes", "id = ?", new String[]{String.valueOf(id)}) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean pagarDespesaReceberReceita(Context context, int transacaoId, int contaId, boolean isReceita) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                double valor = 0;
                try (Cursor cursor = db.rawQuery("SELECT valor FROM transacoes WHERE id = ?", new String[]{String.valueOf(transacaoId)})) {
                    if (cursor.moveToFirst()) valor = cursor.getDouble(0);
                }
                if (valor == 0) return true; 

                ContentValues values = new ContentValues();
                values.put(isReceita ? "recebido" : "pago", 1);
                values.put("data_pagamento", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()));
                if (db.update("transacoes", values, "id = ?", new String[]{String.valueOf(transacaoId)}) == 0) return false;

                String op = isReceita ? "+" : "-";
                db.execSQL("UPDATE contas SET saldo = saldo " + op + " ? WHERE id = ?", new Object[]{valor, contaId});

                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean pagarFatura(Context context, int faturaId, int contaId) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                double valor = 0;
                try (Cursor cursor = db.rawQuery("SELECT valor_total FROM faturas WHERE id = ?", new String[]{String.valueOf(faturaId)})) {
                    if (cursor.moveToFirst()) valor = cursor.getDouble(0);
                }
                if (valor == 0) return true;

                ContentValues values = new ContentValues();
                values.put("status", 1);
                values.put("data_pagamento", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()));
                if (db.update("faturas", values, "id = ?", new String[]{String.valueOf(faturaId)}) == 0) return false;

                db.execSQL("UPDATE contas SET saldo = saldo - ? WHERE id = ?", new Object[]{valor, contaId});

                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean salvarTransacaoUnica(Context context, int idUsuario, int idConta, double valor, int tipo, int pago, int recebido, String data, String descricao, int idCategoria, String observacao) {
        try (SQLiteDatabase db = new MeuDbHelper(context).getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                values.put("id_usuario", idUsuario);
                values.put("id_conta", idConta);
                values.put("valor", valor);
                values.put("tipo", tipo);
                values.put("pago", pago);
                values.put("recebido", recebido);
                values.put("data_movimentacao", data);
                values.put("descricao", descricao);
                values.put("id_categoria", idCategoria);
                values.put("observacao", observacao);

                if (db.insert("transacoes", null, values) == -1) return false;

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
            return false;
        }
    }

    public static boolean salvarTransacaoRecorrente(Context context, int idUsuario, int idConta, double valor, int tipo, int pago, int recebido, String data, String descricao, int idCategoria, String observacao, int quantidade, int periodo, int idMestre) {
        try (SQLiteDatabase db = new MeuDbHelper(context).getWritableDatabase()) {
            db.beginTransaction();
            try {
                // Esta função agora está correta, mas a lógica de parcelamento está no fragmento.
                // O ideal seria mover o loop para cá, mas mantemos por compatibilidade.
                 ContentValues values = new ContentValues();
                values.put("id_usuario", idUsuario);
                values.put("id_conta", idConta);
                values.put("valor", valor);
                values.put("tipo", tipo);
                values.put("pago", pago);
                values.put("recebido", recebido);
                values.put("data_movimentacao", data);
                values.put("descricao", descricao);
                values.put("id_categoria", idCategoria);
                values.put("observacao", observacao);
                values.put("recorrente", 1); 
                values.put("repetir_qtd", quantidade);
                values.put("repetir_periodo", periodo);
                values.put("id_mestre", idMestre > 0 ? idMestre : null);

                if (db.insert("transacoes", null, values) == -1) return false;

                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean salvarTransacaoFixa(Context context, int idUsuario, int idConta, double valor, int tipo, int pago, int recebido, String data, String descricao, int idCategoria, String observacao, int periodo) {
         try (SQLiteDatabase db = new MeuDbHelper(context).getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("id_usuario", idUsuario);
            values.put("id_conta", idConta);
            values.put("valor", valor);
            values.put("tipo", tipo);
            values.put("pago", 0);
            values.put("recebido", 0);
            values.put("data_movimentacao", data);
            values.put("descricao", descricao);
            values.put("id_categoria", idCategoria);
            values.put("observacao", observacao);
            values.put("recorrente", 1);
            values.put("recorrente_ativo", 1);
            values.put("repetir_periodo", 2); // 2 = Mensal para despesas fixas

            return db.insert("transacoes", null, values) != -1;
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean pagarTransacaoRecorrente(Context context, int idMestre, int contaPagamentoId, String mesAno) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                int transacaoExistenteId = -1;
                try (Cursor cursor = db.rawQuery("SELECT id FROM transacoes WHERE id_mestre = ? AND substr(data_movimentacao, 1, 7) = ?", new String[]{String.valueOf(idMestre), mesAno})) {
                    if (cursor.moveToFirst()) {
                        transacaoExistenteId = cursor.getInt(0);
                    }
                }

                if (transacaoExistenteId != -1) {
                    return pagarDespesaReceberReceita(context, transacaoExistenteId, contaPagamentoId, false);
                } else {
                    Cursor mestreCursor = db.rawQuery("SELECT * FROM transacoes WHERE id = ?", new String[]{String.valueOf(idMestre)});
                    if (!mestreCursor.moveToFirst()) {
                        mestreCursor.close();
                        return false;
                    }

                    ContentValues values = new ContentValues();
                    values.put("id_usuario", mestreCursor.getInt(mestreCursor.getColumnIndexOrThrow("id_usuario")));
                    values.put("id_conta", mestreCursor.getInt(mestreCursor.getColumnIndexOrThrow("id_conta")));
                    values.put("valor", mestreCursor.getDouble(mestreCursor.getColumnIndexOrThrow("valor")));
                    values.put("tipo", mestreCursor.getInt(mestreCursor.getColumnIndexOrThrow("tipo")));
                    values.put("pago", 1); 
                    values.put("recebido", 0);
                    values.put("data_movimentacao", mesAno + "-" + new SimpleDateFormat("dd HH:mm:ss", Locale.ROOT).format(new Date()));
                    values.put("data_pagamento", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()));
                    values.put("descricao", mestreCursor.getString(mestreCursor.getColumnIndexOrThrow("descricao")));
                    values.put("id_categoria", mestreCursor.getInt(mestreCursor.getColumnIndexOrThrow("id_categoria")));
                    values.put("observacao", mestreCursor.getString(mestreCursor.getColumnIndexOrThrow("observacao")));
                    values.put("recorrente", 1);
                    values.put("id_mestre", idMestre);
                    
                    long newId = db.insert("transacoes", null, values);
                    mestreCursor.close();

                    if (newId == -1) return false;

                    double valor = values.getAsDouble("valor");
                    db.execSQL("UPDATE contas SET saldo = saldo - ? WHERE id = ?", new Object[]{valor, contaPagamentoId});
                }

                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
