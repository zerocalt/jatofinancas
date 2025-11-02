package com.example.app1.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.app1.Cartao;
import com.example.app1.TelaCadCartao;
import com.example.app1.MeuDbHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CartaoDAO {

    public static List<TelaCadCartao.CartaoModel> buscarCartoes(Context context, int idUsuario) {
        List<TelaCadCartao.CartaoModel> cartoes = new ArrayList<>();
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            String query = "SELECT id, nome, bandeira, limite, data_vencimento_fatura, data_fechamento_fatura, cor, ativo, id_conta, id_usuario FROM cartoes WHERE id_usuario = ? ORDER BY ativo DESC, nome ASC";
            try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idUsuario)})) {
                while (cursor.moveToNext()) {
                    TelaCadCartao.CartaoModel cartao = new TelaCadCartao.CartaoModel();
                    cartao.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    cartao.nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                    cartao.bandeira = cursor.getString(cursor.getColumnIndexOrThrow("bandeira"));
                    cartao.limite = cursor.getDouble(cursor.getColumnIndexOrThrow("limite"));
                    cartao.diaVencimento = cursor.getInt(cursor.getColumnIndexOrThrow("data_vencimento_fatura"));
                    cartao.diaFechamento = cursor.getInt(cursor.getColumnIndexOrThrow("data_fechamento_fatura"));
                    cartao.cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                    cartao.ativo = cursor.getInt(cursor.getColumnIndexOrThrow("ativo"));
                    cartao.idConta = cursor.getInt(cursor.getColumnIndexOrThrow("id_conta"));
                    cartao.idUsuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario")); // Adicionado

                    cartao.valorFaturaParcial = calcularFaturaParcial(context, cartao.id, cartao.diaFechamento);
                    cartoes.add(cartao);
                }
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao buscar cartões", e);
        }
        return cartoes;
    }

    public static TelaCadCartao.CartaoModel buscarCartaoPorId(Context context, int idCartao) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            String query = "SELECT id, nome, bandeira, limite, data_vencimento_fatura, data_fechamento_fatura, cor, ativo, id_conta, id_usuario FROM cartoes WHERE id = ?";
            try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idCartao)})) {
                if (cursor.moveToFirst()) {
                    TelaCadCartao.CartaoModel cartao = new TelaCadCartao.CartaoModel();
                    cartao.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    cartao.nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                    cartao.bandeira = cursor.getString(cursor.getColumnIndexOrThrow("bandeira"));
                    cartao.limite = cursor.getDouble(cursor.getColumnIndexOrThrow("limite"));
                    cartao.diaVencimento = cursor.getInt(cursor.getColumnIndexOrThrow("data_vencimento_fatura"));
                    cartao.diaFechamento = cursor.getInt(cursor.getColumnIndexOrThrow("data_fechamento_fatura"));
                    cartao.cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                    cartao.ativo = cursor.getInt(cursor.getColumnIndexOrThrow("ativo"));
                    cartao.idConta = cursor.getInt(cursor.getColumnIndexOrThrow("id_conta"));
                    cartao.idUsuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario")); // Adicionado
                    return cartao;
                }
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao buscar cartão por ID", e);
        }
        return null;
    }


    public static boolean excluirCartao(Context context, int idCartao) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                db.delete("parcelas_cartao", "id_transacao_cartao IN (SELECT id FROM transacoes_cartao WHERE id_cartao = ?)", new String[]{String.valueOf(idCartao)});
                db.delete("transacoes_cartao", "id_cartao = ?", new String[]{String.valueOf(idCartao)});
                db.delete("cartoes", "id = ?", new String[]{String.valueOf(idCartao)});
                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao excluir cartão", e);
            return false;
        }
    }

    public static double calcularLimiteUtilizado(Context context, int idCartao) {
        double totalUtilizado = 0;
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            String query = "SELECT SUM(valor) as total FROM parcelas_cartao WHERE id_transacao_cartao IN (SELECT id FROM transacoes_cartao WHERE id_cartao = ?) AND paga = 0";
            try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idCartao)})) {
                if (cursor.moveToFirst()) {
                    totalUtilizado = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
                }
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao calcular limite utilizado", e);
        }
        return totalUtilizado;
    }

    public static double calcularFaturaParcial(Context context, int idCartao, int diaFechamento) {
        double totalFatura = 0;
        Calendar hoje = Calendar.getInstance();
        Calendar inicioFatura = Calendar.getInstance();

        if (hoje.get(Calendar.DAY_OF_MONTH) > diaFechamento) {
            inicioFatura.set(hoje.get(Calendar.YEAR), hoje.get(Calendar.MONTH), diaFechamento);
        } else {
            inicioFatura.add(Calendar.MONTH, -1);
            inicioFatura.set(Calendar.DAY_OF_MONTH, diaFechamento);
        }

        String dataInicioFormatada = new java.text.SimpleDateFormat("yyyy-MM-dd").format(inicioFatura.getTime());

        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            String query = "SELECT SUM(p.valor) as total FROM parcelas_cartao p JOIN transacoes_cartao t ON p.id_transacao_cartao = t.id WHERE t.id_cartao = ? AND p.data_vencimento > ? AND p.paga = 0";
            try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idCartao), dataInicioFormatada})) {
                if (cursor.moveToFirst()) {
                    totalFatura = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
                }
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao calcular fatura parcial", e);
        }

        return totalFatura;
    }

    public static List<Cartao> buscarCartoesAtivosPorUsuario(Context context, int idUsuario) {
        List<Cartao> cartoes = new ArrayList<>();
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            String query = "SELECT id, nome, bandeira, cor FROM cartoes WHERE id_usuario = ? AND ativo = 1 ORDER BY nome ASC";
            try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idUsuario)})) {
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                    Cartao cartao = new Cartao(id, nome);
                    cartao.setBandeira(cursor.getString(cursor.getColumnIndexOrThrow("bandeira")));
                    cartao.setCor(cursor.getString(cursor.getColumnIndexOrThrow("cor")));
                    cartoes.add(cartao);
                }
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao buscar cartões ativos", e);
        }
        return cartoes;
    }
}
