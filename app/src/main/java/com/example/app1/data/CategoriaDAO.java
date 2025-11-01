package com.example.app1.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.app1.Categoria;
import com.example.app1.MeuDbHelper;

import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    public static boolean inserirCategoria(Context ctx, Categoria categoria, int idUsuario) {
        try (SQLiteDatabase db = new MeuDbHelper(ctx).getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("nome", categoria.getNome());
            values.put("cor", categoria.getCor());
            values.put("id_usuario", idUsuario);
            long resultado = db.insert("categorias", null, values);
            return resultado != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Categoria> carregarCategorias(Context ctx, int idUsuario) {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT id, nome, cor FROM categorias WHERE id_usuario = ? ORDER BY nome COLLATE NOCASE ASC";
        try (SQLiteDatabase db = new MeuDbHelper(ctx).getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idUsuario)})) {
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                    String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                    lista.add(new Categoria(id, nome, cor != null ? cor : "#888888"));
                } while (cursor.moveToNext());
            }
        }
        return lista;
    }

    public static Categoria buscarCategoriaPorNome(Context ctx, int idUsuario, String nomeCategoria) {
        String sql = "SELECT id, nome, cor FROM categorias WHERE id_usuario = ? AND nome = ?";
        try (SQLiteDatabase db = new MeuDbHelper(ctx).getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idUsuario), nomeCategoria})) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                return new Categoria(id, nome, cor != null ? cor : "#888888");
            }
        }
        return null;
    }

    public static Categoria buscarCategoriaPorId(Context ctx, int idCategoria) {
        String sql = "SELECT id, nome, cor FROM categorias WHERE id = ?";
        try (SQLiteDatabase db = new MeuDbHelper(ctx).getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idCategoria)})) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                return new Categoria(id, nome, cor != null ? cor : "#888888");
            }
        }
        return null;
    }

    public static boolean atualizarCategoria(Context ctx, Categoria categoria) {
        try (SQLiteDatabase db = new MeuDbHelper(ctx).getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("nome", categoria.getNome());
            values.put("cor", categoria.getCor());
            int rows = db.update("categorias", values, "id = ?", new String[]{String.valueOf(categoria.getId())});
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean excluirCategoria(Context ctx, int idCategoria) {
        try (SQLiteDatabase db = new MeuDbHelper(ctx).getWritableDatabase()) {
            int rows = db.delete("categorias", "id = ?", new String[]{String.valueOf(idCategoria)});
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
