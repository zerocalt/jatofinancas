package com.example.app1.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.app1.Cartao;
import com.example.app1.MeuDbHelper;

import java.util.ArrayList;
import java.util.List;

public class CartaoDAO {

    /**
     * Busca todos os cartões ativos de um usuário
     */
    public static List<Cartao> buscarCartoesAtivosPorUsuario(Context ctx, int idUsuario) {
        List<Cartao> lista = new ArrayList<>();
        String sql = "SELECT id, id_usuario, id_conta, nome, limite, data_vencimento_fatura, " +
                "data_fechamento_fatura, cor, bandeira, ativo, data_hora_cadastro " +
                "FROM cartoes WHERE id_usuario = ? AND ativo = 1 ORDER BY nome COLLATE NOCASE ASC";

        try (SQLiteDatabase db = new MeuDbHelper(ctx).getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idUsuario)})) {

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    int idUsuarioCartao = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario"));
                    int idConta = cursor.getInt(cursor.getColumnIndexOrThrow("id_conta"));
                    String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                    double limite = cursor.getDouble(cursor.getColumnIndexOrThrow("limite"));
                    int vencimentoFatura = cursor.getInt(cursor.getColumnIndexOrThrow("data_vencimento_fatura"));
                    int fechamentoFatura = cursor.getInt(cursor.getColumnIndexOrThrow("data_fechamento_fatura"));
                    String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                    String bandeira = cursor.getString(cursor.getColumnIndexOrThrow("bandeira"));
                    int ativo = cursor.getInt(cursor.getColumnIndexOrThrow("ativo"));
                    String dataHoraCadastro = cursor.getString(cursor.getColumnIndexOrThrow("data_hora_cadastro"));

                    lista.add(new Cartao(id, idUsuarioCartao, idConta, nome, limite,
                            vencimentoFatura, fechamentoFatura,
                            cor != null ? cor : "#888888",
                            bandeira != null ? bandeira : "",
                            ativo,
                            dataHoraCadastro));

                } while (cursor.moveToNext());
            }
        }

        return lista;
    }

    public static double calcularLimiteUtilizado(Context ctx, int idCartao) {
        double total = 0.0;
        SQLiteDatabase db = new MeuDbHelper(ctx).getReadableDatabase();

        String sql =
                "SELECT SUM(p.valor) AS total FROM parcelas_cartao p " +
                        "INNER JOIN transacoes_cartao t ON t.id = p.id_transacao_cartao " +
                        "WHERE t.id_cartao = ? AND p.paga = 0";

        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(idCartao)})) {
            if (c.moveToFirst()) {
                total += c.getDouble(c.getColumnIndexOrThrow("total"));
            }
        }

        // Somar também recorrentes ativas (se houver)
        String sqlRec =
                "SELECT SUM(d.valor) AS total FROM despesas_recorrentes_cartao d " +
                        "INNER JOIN transacoes_cartao t ON t.id = d.id_transacao_cartao " +
                        "WHERE t.id_cartao = ? AND (d.data_final IS NULL OR d.data_final >= date('now'))";

        try (Cursor c2 = db.rawQuery(sqlRec, new String[]{String.valueOf(idCartao)})) {
            if (c2.moveToFirst()) {
                total += c2.getDouble(c2.getColumnIndexOrThrow("total"));
            }
        }

        db.close();
        return total;
    }

}
