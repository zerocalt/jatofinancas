package com.example.app1.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.app1.Conta;
import com.example.app1.MeuDbHelper;

import java.util.ArrayList;
import java.util.List;

public class ContaDAO {

    /**
     * Carrega todas as contas do usuário e retorna uma lista de objetos Conta.
     *
     * @param ctx        Contexto da aplicação.
     * @param idUsuario  ID do usuário logado.
     * @return Lista de contas pertencentes ao usuário.
     */
    public static List<Conta> carregarListaContas(Context ctx, int idUsuario) {
        List<Conta> contas = new ArrayList<>();

        if (idUsuario != -1) {
            MeuDbHelper dbHelper = new MeuDbHelper(ctx);

            try (SQLiteDatabase dbRead = dbHelper.getReadableDatabase();
                 Cursor cursor = dbRead.rawQuery(
                         "SELECT id, nome, saldo, tipo_conta, cor, mostrar_na_tela_inicial " +
                                 "FROM contas WHERE id_usuario = ? ORDER BY nome COLLATE NOCASE ASC",
                         new String[]{String.valueOf(idUsuario)})) {

                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                    double saldo = cursor.getDouble(cursor.getColumnIndexOrThrow("saldo"));
                    int tipoConta = cursor.getInt(cursor.getColumnIndexOrThrow("tipo_conta"));
                    String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                    int mostrarNaTelaInicial = cursor.getInt(cursor.getColumnIndexOrThrow("mostrar_na_tela_inicial"));

                    Conta conta = new Conta(id, nome, saldo, tipoConta, cor, mostrarNaTelaInicial);
                    contas.add(conta);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return contas;
    }
}