package com.example.app1.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.app1.Categoria;
import com.example.app1.MeuDbHelper;

import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    /**
     * Retorna uma lista de categorias do usuário, ordenadas por nome.
     */
    public static List<Categoria> carregarCategorias(Context ctx, int idUsuario) {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT id, nome, cor FROM categorias WHERE id_usuario = ? ORDER BY nome COLLATE NOCASE ASC";

        try (SQLiteDatabase db = new MeuDbHelper(ctx).getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idUsuario)})) {

            if (cursor != null && cursor.moveToFirst()) {
                ArrayList<Integer> idsUnicos = new ArrayList<>();
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                    String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                    if (!idsUnicos.contains(id)) {
                        idsUnicos.add(id);
                        lista.add(new Categoria(id, nome, cor != null ? cor : "#888888"));
                    }
                } while (cursor.moveToNext());
            }
        }

        return lista;
    }
}