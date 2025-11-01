package com.example.app1.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.app1.Cartao;
import com.example.app1.MeuDbHelper;
import com.example.app1.TelaCadCartao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CartaoDAO {

    public static double calcularLimiteUtilizado(Context context, int idCartao) {
        double limiteUtilizado = 0;
        String query = "SELECT SUM(p.valor) FROM parcelas_cartao p " +
                       "JOIN transacoes_cartao tc ON p.id_transacao_cartao = tc.id " +
                       "WHERE tc.id_cartao = ? AND p.paga = 0";

        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idCartao)})) {
            if (cursor.moveToFirst()) {
                limiteUtilizado = cursor.getDouble(0);
            }
        }
        return limiteUtilizado;
    }

    public static double calcularFaturaParcial(Context context, int idCartao, int diaFechamento) {
        double totalFatura = 0;
        Calendar hoje = Calendar.getInstance();
        Calendar dataInicioFatura = Calendar.getInstance();
        dataInicioFatura.set(Calendar.DAY_OF_MONTH, diaFechamento + 1);
        dataInicioFatura.add(Calendar.MONTH, -1);

        Calendar dataFimFatura = Calendar.getInstance();
        dataFimFatura.set(Calendar.DAY_OF_MONTH, diaFechamento);

        if (hoje.get(Calendar.DAY_OF_MONTH) <= diaFechamento) {
            dataInicioFatura.add(Calendar.MONTH, -1);
            dataFimFatura.add(Calendar.MONTH, -1);
        }

        String dataInicioStr = String.format("%04d-%02d-%02d", dataInicioFatura.get(Calendar.YEAR), dataInicioFatura.get(Calendar.MONTH) + 1, dataInicioFatura.get(Calendar.DAY_OF_MONTH));
        String dataFimStr = String.format("%04d-%02d-%02d", dataFimFatura.get(Calendar.YEAR), dataFimFatura.get(Calendar.MONTH) + 1, dataFimFatura.get(Calendar.DAY_OF_MONTH));

        String query = "SELECT SUM(valor_parcela) AS total FROM (" +
                "SELECT p.valor AS valor_parcela FROM parcelas_cartao p " +
                "JOIN transacoes_cartao t ON t.id = p.id_transacao_cartao " +
                "WHERE t.id_cartao = ? AND p.data_vencimento BETWEEN ? AND ?" +
                " UNION ALL " +
                "SELECT t.valor AS valor_parcela FROM transacoes_cartao t " +
                "WHERE t.id_cartao = ? AND t.parcelas = 1 AND t.data_compra BETWEEN ? AND ?" +
                ")";

        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idCartao), dataInicioStr, dataFimStr, String.valueOf(idCartao), dataInicioStr, dataFimStr})) {
            if (cursor.moveToFirst()) {
                totalFatura = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao calcular fatura parcial", e);
        }
        return totalFatura;
    }

    public static List<TelaCadCartao.CartaoModel> buscarCartoes(Context context, int idUsuario) {
        List<TelaCadCartao.CartaoModel> cartoes = new ArrayList<>();
        String sql = "SELECT id, nome, bandeira, limite, data_vencimento_fatura, data_fechamento_fatura, cor, ativo, id_conta, id_usuario " +
                     "FROM cartoes WHERE id_usuario = ? ORDER BY ativo DESC, nome ASC";

        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idUsuario)})) {

            while (cursor.moveToNext()) {
                TelaCadCartao.CartaoModel c = new TelaCadCartao.CartaoModel();
                c.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                c.nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                c.bandeira = cursor.getString(cursor.getColumnIndexOrThrow("bandeira"));
                c.limite = cursor.getDouble(cursor.getColumnIndexOrThrow("limite"));
                c.diaVencimento = cursor.getInt(cursor.getColumnIndexOrThrow("data_vencimento_fatura"));
                c.diaFechamento = cursor.getInt(cursor.getColumnIndexOrThrow("data_fechamento_fatura"));
                c.cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                c.ativo = cursor.getInt(cursor.getColumnIndexOrThrow("ativo"));
                c.idConta = cursor.getInt(cursor.getColumnIndexOrThrow("id_conta"));
                c.idUsuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario"));
                c.valorFaturaParcial = calcularFaturaParcial(context, c.id, c.diaFechamento);
                cartoes.add(c);
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao buscar cartões", e);
        }
        return cartoes;
    }

    public static TelaCadCartao.CartaoModel buscarCartaoPorId(Context context, int idCartao) {
        String sql = "SELECT id, nome, bandeira, limite, data_vencimento_fatura, data_fechamento_fatura, cor, ativo, id_conta, id_usuario " +
                "FROM cartoes WHERE id = ?";

        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idCartao)})) {

            if (cursor.moveToFirst()) {
                TelaCadCartao.CartaoModel c = new TelaCadCartao.CartaoModel();
                c.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                c.nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                c.bandeira = cursor.getString(cursor.getColumnIndexOrThrow("bandeira"));
                c.limite = cursor.getDouble(cursor.getColumnIndexOrThrow("limite"));
                c.diaVencimento = cursor.getInt(cursor.getColumnIndexOrThrow("data_vencimento_fatura"));
                c.diaFechamento = cursor.getInt(cursor.getColumnIndexOrThrow("data_fechamento_fatura"));
                c.cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                c.ativo = cursor.getInt(cursor.getColumnIndexOrThrow("ativo"));
                c.idConta = cursor.getInt(cursor.getColumnIndexOrThrow("id_conta"));
                c.idUsuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario"));
                return c;
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao buscar cartão por ID", e);
        }
        return null;
    }

    public static List<Cartao> buscarCartoesAtivosPorUsuario(Context context, int idUsuario) {
        List<Cartao> cartoes = new ArrayList<>();
        String sql = "SELECT id, nome FROM cartoes WHERE id_usuario = ? AND ativo = 1 ORDER BY nome ASC";
        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idUsuario)})) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                cartoes.add(new Cartao(id, nome));
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao buscar cartões ativos", e);
        }
        return cartoes;
    }

    public static boolean excluirCartao(Context context, int idCartao) {
        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                List<Integer> idsTransacoes = new ArrayList<>();
                try (Cursor c = db.rawQuery("SELECT id FROM transacoes_cartao WHERE id_cartao = ?", new String[]{String.valueOf(idCartao)})) {
                    while(c.moveToNext()) {
                        idsTransacoes.add(c.getInt(0));
                    }
                }

                if (!idsTransacoes.isEmpty()) {
                    String ids = idsTransacoes.toString().replace("[", "(").replace("]", ")");
                    db.delete("parcelas_cartao", "id_transacao_cartao IN " + ids, null);
                    db.delete("transacoes_cartao", "id IN " + ids, null);
                }

                db.delete("faturas", "id_cartao = ?", new String[]{String.valueOf(idCartao)});
                db.delete("cartoes", "id = ?", new String[]{String.valueOf(idCartao)});

                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao excluir cartão e dependências", e);
            return false;
        }
    }
}
