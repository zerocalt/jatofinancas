package com.example.app1.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.app1.Cartao;
import com.example.app1.CartaoFatura;
import com.example.app1.GerenciadorDeFatura;
import com.example.app1.MeuDbHelper;
import com.example.app1.TelaCadCartao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

                    Calendar hoje = Calendar.getInstance();
                    int mesAtual = hoje.get(Calendar.MONTH) + 1;  // Janeiro é 0, por isso +1
                    int anoAtual = hoje.get(Calendar.YEAR);
                    cartao.valorFaturaParcial = calcularFatura(context, cartao.id, mesAtual, anoAtual);
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
            String query = "SELECT SUM(p.valor) as total " +
                    "FROM parcelas_cartao p " +
                    "INNER JOIN transacoes_cartao t ON p.id_transacao = t.id " +
                    "WHERE t.id_cartao = ? AND p.paga = 0";

            try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idCartao)})) {
                if (cursor.moveToFirst()) {
                    totalUtilizado = cursor.isNull(0) ? 0 : cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
                }
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao calcular limite utilizado", e);
        }

        return totalUtilizado;
    }

    public static double calcularFatura(Context context, int idCartao, int mes, int ano) {
        double total = 0;
        MeuDbHelper dbHelper = new MeuDbHelper(context);

        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.rawQuery(
                     "SELECT SUM(p.valor) " +
                             "FROM parcelas_cartao p " +
                             "INNER JOIN faturas f ON p.id_fatura = f.id " +
                             "WHERE f.id_cartao = ? AND f.mes = ? AND f.ano = ?",
                     new String[]{String.valueOf(idCartao), String.valueOf(mes), String.valueOf(ano)})) {

            if (c.moveToFirst()) {
                total = c.isNull(0) ? 0 : c.getDouble(0);
            }
        } catch (Exception e) {
            Log.e("FaturasDAO", "Erro ao calcular total da fatura completa", e);
        }

        return total;
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

    public static List<CartaoFatura> getCartoesComFatura(Context context, int idUsuario, int ano, int mes) {
        List<CartaoFatura> listaCartaoFatura = new ArrayList<>();
        MeuDbHelper dbHelper = new MeuDbHelper(context);

        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            // Pega os cartões ativos do usuário
            String queryCartoes = "SELECT id, nome, bandeira, cor, data_vencimento_fatura, data_fechamento_fatura " +
                    "FROM cartoes WHERE id_usuario = ? AND ativo = 1 ORDER BY nome ASC";

            try (Cursor cursorCartoes = db.rawQuery(queryCartoes, new String[]{String.valueOf(idUsuario)})) {
                while (cursorCartoes.moveToNext()) {
                    int idCartao = cursorCartoes.getInt(cursorCartoes.getColumnIndexOrThrow("id"));
                    String nome = cursorCartoes.getString(cursorCartoes.getColumnIndexOrThrow("nome"));
                    String bandeira = cursorCartoes.getString(cursorCartoes.getColumnIndexOrThrow("bandeira"));
                    String cor = cursorCartoes.getString(cursorCartoes.getColumnIndexOrThrow("cor"));
                    int diaVencimento = cursorCartoes.getInt(cursorCartoes.getColumnIndexOrThrow("data_vencimento_fatura"));
                    int diaFechamento = cursorCartoes.getInt(cursorCartoes.getColumnIndexOrThrow("data_fechamento_fatura"));

                    Cartao cartao = new Cartao(idCartao, nome);
                    cartao.setBandeira(bandeira);
                    cartao.setCor(cor);
                    cartao.setDataVencimentoFatura(diaVencimento);

                    double valorFatura = 0;

                    // Calcula o mês e o ano da fatura (mês seguinte ao selecionado)
                    int mesFatura = mes + 1;
                    int anoFatura = ano;

                    // Se passou de dezembro, volta para janeiro e incrementa o ano
                    if (mesFatura > 12) {
                        mesFatura = 1;
                        anoFatura++;
                    }

                    // Agora chama o cálculo com o mês e ano corretos
                    valorFatura = calcularFatura(context, idCartao, mesFatura, anoFatura);

                    listaCartaoFatura.add(new CartaoFatura(cartao, valorFatura));
                }
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao buscar cartões com fatura", e);
        }

        return listaCartaoFatura;
    }

}