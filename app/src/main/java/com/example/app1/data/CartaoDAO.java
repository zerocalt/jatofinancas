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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar agora = Calendar.getInstance();
        String hojeFormatado = sdf.format(agora.getTime());

        String chaveFatura = GerenciadorDeFatura.getChaveFatura(hojeFormatado, diaFechamento);
        if (chaveFatura.isEmpty()) return 0;

        String[] parts = chaveFatura.split("-");
        int ano = Integer.parseInt(parts[0]);
        int mes = Integer.parseInt(parts[1]);

        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {

            Calendar inicio = Calendar.getInstance();
            Calendar fim = Calendar.getInstance();

            if (diaFechamento > 0) {
                inicio.set(ano, mes - 1, diaFechamento + 1);
                inicio.add(Calendar.MONTH, -1);
                fim.set(ano, mes - 1, diaFechamento);
            } else {
                inicio.set(ano, mes - 1, 1);
                fim.set(ano, mes - 1, inicio.getActualMaximum(Calendar.DAY_OF_MONTH));
            }

            String dataInicio = sdf.format(inicio.getTime());
            String dataFim = sdf.format(fim.getTime());

            String query = "SELECT COALESCE(p.valor, t.valor) AS valor_parcela " +
                    "FROM transacoes_cartao t " +
                    "LEFT JOIN parcelas_cartao p ON t.id = p.id_transacao_cartao " +
                    "AND COALESCE(p.data_vencimento, t.data_compra) BETWEEN ? AND ? " +
                    "WHERE t.id_cartao = ?";

            try (Cursor cur = db.rawQuery(query, new String[]{dataInicio, dataFim, String.valueOf(idCartao)})) {
                while (cur.moveToNext()) {
                    totalFatura += cur.getDouble(cur.getColumnIndexOrThrow("valor_parcela"));
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

                    // Calcula intervalo de compras que compõem a fatura que vence no mês seguinte
                    Calendar inicio = Calendar.getInstance();
                    Calendar fim = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    // Ajusta mês anterior corretamente (cuidado com Janeiro)
                    int mesAnterior = mes - 1;
                    int anoAnterior = ano;
                    if (mesAnterior < 0) {
                        mesAnterior = 11;
                        anoAnterior -= 1;
                    }

                    inicio.set(anoAnterior, mesAnterior, diaFechamento + 1);
                    fim.set(ano, mes, diaFechamento > 0 ? diaFechamento : fim.getActualMaximum(Calendar.DAY_OF_MONTH));

                    String dataInicio = sdf.format(inicio.getTime());
                    String dataFim = sdf.format(fim.getTime());

                    // Soma o valor das parcelas/transações dentro do intervalo
                    String queryValor = "SELECT COALESCE(SUM(valor),0) FROM transacoes_cartao " +
                            "WHERE id_cartao = ? AND data_compra BETWEEN ? AND ?";
                    try (Cursor curValor = db.rawQuery(queryValor, new String[]{
                            String.valueOf(idCartao), dataInicio, dataFim})) {
                        if (curValor.moveToFirst()) {
                            valorFatura = curValor.getDouble(0);
                        }
                    }

                    listaCartaoFatura.add(new CartaoFatura(cartao, valorFatura));
                }
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao buscar cartões com fatura", e);
        }

        return listaCartaoFatura;
    }

}