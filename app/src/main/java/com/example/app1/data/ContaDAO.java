package com.example.app1.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.app1.Conta;
import com.example.app1.MeuDbHelper;

import java.util.ArrayList;
import java.util.List;

public class ContaDAO {

    public static boolean inserirConta(Context ctx, Conta conta, int idUsuario) {
        MeuDbHelper dbHelper = new MeuDbHelper(ctx);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("nome", conta.getNome());
            values.put("saldo", conta.getSaldo());
            values.put("tipo_conta", conta.getTipoConta());
            values.put("cor", conta.getCor());
            values.put("mostrar_na_tela_inicial", conta.isMostrarNaTelaInicial());
            values.put("id_usuario", idUsuario);

            long result = db.insert("contas", null, values);
            return result != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Conta> getContasTelaInicial(Context ctx, int idUsuario) {
        List<Conta> contas = new ArrayList<>();
        if (idUsuario == -1) {
            return contas;
        }

        MeuDbHelper dbHelper = new MeuDbHelper(ctx);
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(
                     "SELECT id, nome, saldo, tipo_conta, cor, mostrar_na_tela_inicial " +
                             "FROM contas WHERE id_usuario = ? AND mostrar_na_tela_inicial = 1 ORDER BY nome COLLATE NOCASE ASC",
                     new String[]{String.valueOf(idUsuario)})) {

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                double saldo = cursor.getDouble(cursor.getColumnIndexOrThrow("saldo"));
                int tipoConta = cursor.getInt(cursor.getColumnIndexOrThrow("tipo_conta"));
                String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                boolean mostrarNaTelaInicial = cursor.getInt(cursor.getColumnIndexOrThrow("mostrar_na_tela_inicial")) > 0;
                contas.add(new Conta(id, nome, saldo, cor, tipoConta, mostrarNaTelaInicial));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contas;
    }

    public static List<Conta> carregarListaContas(Context ctx, int idUsuario) {
        List<Conta> contas = new ArrayList<>();

        if (idUsuario != -1) {
            MeuDbHelper dbHelper = new MeuDbHelper(ctx);

            String sql = "SELECT id, nome, saldo, tipo_conta, cor, mostrar_na_tela_inicial, " +
                    "(EXISTS (SELECT 1 FROM transacoes t WHERE t.id_conta = contas.id) OR " +
                    " EXISTS (SELECT 1 FROM cartoes c WHERE c.id_conta = contas.id)) as em_uso " +
                    "FROM contas WHERE id_usuario = ? ORDER BY nome COLLATE NOCASE ASC";

            try (SQLiteDatabase dbRead = dbHelper.getReadableDatabase();
                 Cursor cursor = dbRead.rawQuery(sql, new String[]{String.valueOf(idUsuario)})) {

                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                    double saldo = cursor.getDouble(cursor.getColumnIndexOrThrow("saldo"));
                    int tipoConta = cursor.getInt(cursor.getColumnIndexOrThrow("tipo_conta"));
                    String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                    boolean mostrarNaTelaInicial = cursor.getInt(cursor.getColumnIndexOrThrow("mostrar_na_tela_inicial")) > 0;
                    boolean emUso = cursor.getInt(cursor.getColumnIndexOrThrow("em_uso")) > 0;

                    Conta conta = new Conta(id, nome, saldo, cor, tipoConta, mostrarNaTelaInicial, emUso);
                    contas.add(conta);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return contas;
    }

    public static boolean excluirConta(Context ctx, int contaId) {
        MeuDbHelper dbHelper = new MeuDbHelper(ctx);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            int result = db.delete("contas", "id = ?", new String[]{String.valueOf(contaId)});
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Conta getContaById(Context ctx, int contaId) {
        MeuDbHelper dbHelper = new MeuDbHelper(ctx);
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(
                     "SELECT id, nome, saldo, tipo_conta, cor, mostrar_na_tela_inicial " +
                             "FROM contas WHERE id = ?",
                     new String[]{String.valueOf(contaId)})) {

            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                double saldo = cursor.getDouble(cursor.getColumnIndexOrThrow("saldo"));
                int tipoConta = cursor.getInt(cursor.getColumnIndexOrThrow("tipo_conta"));
                String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                boolean mostrarNaTelaInicial = cursor.getInt(cursor.getColumnIndexOrThrow("mostrar_na_tela_inicial")) > 0;
                return new Conta(id, nome, saldo, cor, tipoConta, mostrarNaTelaInicial);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean atualizarConta(Context ctx, Conta conta) {
        MeuDbHelper dbHelper = new MeuDbHelper(ctx);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("nome", conta.getNome());
            values.put("saldo", conta.getSaldo());
            values.put("tipo_conta", conta.getTipoConta());
            values.put("cor", conta.getCor());
            values.put("mostrar_na_tela_inicial", conta.isMostrarNaTelaInicial());

            int result = db.update("contas", values, "id = ?", new String[]{String.valueOf(conta.getId())});
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean atualizarSaldoConta(Context ctx, int contaId, double novoSaldo) {
        MeuDbHelper dbHelper = new MeuDbHelper(ctx);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("saldo", novoSaldo);

            int result = db.update("contas", values, "id = ?", new String[]{String.valueOf(contaId)});
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
