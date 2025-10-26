package com.example.app1.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.app1.MeuDbHelper;

import java.util.ArrayList;
import java.util.List;

public class ContaDAO {

    public static List<String> carregarListaContas(Context ctx, int idUsuario) {
        List<String> contas = new ArrayList<>();
        if (idUsuario != -1) {
            MeuDbHelper dbHelper = new MeuDbHelper(ctx);
            try (SQLiteDatabase dbRead = dbHelper.getReadableDatabase();
                 Cursor cursorConta = dbRead.rawQuery("SELECT nome FROM contas WHERE id_usuario = ? ORDER BY nome COLLATE NOCASE ASC", new String[]{String.valueOf(idUsuario)})) {
                while (cursorConta.moveToNext()) {
                    contas.add(cursorConta.getString(cursorConta.getColumnIndexOrThrow("nome")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return contas;
    }

}
