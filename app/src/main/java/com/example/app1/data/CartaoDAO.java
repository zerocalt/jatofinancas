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
        String hojeFormatado = sdf.format(new Date());
        String chaveFatura = GerenciadorDeFatura.getChaveFatura(hojeFormatado, diaFechamento);

        if (chaveFatura.isEmpty()) return 0;

        String[] parts = chaveFatura.split("-");
        int ano = Integer.parseInt(parts[0]);
        int mes = Integer.parseInt(parts[1]);

        MeuDbHelper dbHelper = new MeuDbHelper(context);
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            String query = "SELECT valor_total FROM faturas WHERE id_cartao = ? AND ano = ? AND mes = ? AND status = 0";
            try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idCartao), String.valueOf(ano), String.valueOf(mes)})) {
                if (cursor.moveToFirst()) {
                    return cursor.getDouble(0);
                } else {
                    // Se não encontrar fatura, calcula na mão (fallback)
                    Calendar inicio = Calendar.getInstance();
                    Calendar fim = Calendar.getInstance();

                    inicio.set(ano, mes -1, diaFechamento > 0 ? diaFechamento + 1 : 1);
                    if (diaFechamento > 0) inicio.add(Calendar.MONTH, -1);
                    fim.set(ano, mes-1, diaFechamento > 0 ? diaFechamento : inicio.getActualMaximum(Calendar.DAY_OF_MONTH));

                    String dataInicio = sdf.format(inicio.getTime());
                    String dataFim = sdf.format(fim.getTime());

                    String fallbackQuery = "SELECT SUM(p.valor) as total FROM parcelas_cartao p JOIN transacoes_cartao t ON p.id_transacao_cartao = t.id WHERE t.id_cartao = ? AND p.data_vencimento BETWEEN ? AND ? AND p.paga = 0";
                    try(Cursor fallbackCursor = db.rawQuery(fallbackQuery, new String[]{String.valueOf(idCartao), dataInicio, dataFim})){
                        if (fallbackCursor.moveToFirst()) {
                            return fallbackCursor.getDouble(0);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("CartaoDAO", "Erro ao buscar valor da fatura parcial", e);
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

                    // mes aqui é 0-based (0..11). Monta YYYY-MM para comparar substr(data_vencimento,1,7)
                    String mesAnoFatura = String.format(Locale.ROOT, "%04d-%02d", ano, mes + 1);

                    // 1) tenta encontrar fatura registrada (vencendo no mês selecionado)
                    String queryFatura = "SELECT COALESCE(SUM(valor_total),0) FROM faturas WHERE id_cartao = ? AND substr(data_vencimento, 1, 7) = ?";
                    try (Cursor cursorFatura = db.rawQuery(queryFatura, new String[]{String.valueOf(idCartao), mesAnoFatura})) {
                        if (cursorFatura.moveToFirst()) {
                            valorFatura = cursorFatura.getDouble(0);
                        }
                    }

                    // 2) se não encontrou, fallback: soma parcelas com data_vencimento dentro do intervalo que gera a fatura que vence no mês selecionado
                    if (valorFatura == 0) {
                        Calendar inicio = Calendar.getInstance();
                        Calendar fim = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                        // intervalo de compras que compõem a fatura que VENCE no mês selecionado:
                        // início = diaFechamento+1 do mês anterior, fim = diaVencimento do mês selecionado (ou diaFechamento do mês selecionado dependendo da sua regra)
                        // Aqui usamos a mesma lógica comum: compras entre (ano, mes-1, diaFechamento+1) e (ano, mes, diaFechamento)
                        inicio.set(ano, mes - 1, diaFechamento > 0 ? (diaFechamento + 1) : 1);
                        fim.set(ano, mes, diaFechamento > 0 ? diaFechamento : fim.getActualMaximum(Calendar.DAY_OF_MONTH));

                        String dataInicio = sdf.format(inicio.getTime());
                        String dataFim = sdf.format(fim.getTime());

                        String fallbackQuery = "SELECT COALESCE(SUM(p.valor),0) FROM parcelas_cartao p JOIN transacoes_cartao t ON p.id_transacao_cartao = t.id WHERE t.id_cartao = ? AND p.data_vencimento BETWEEN ? AND ?";
                        try (Cursor c2 = db.rawQuery(fallbackQuery, new String[]{String.valueOf(idCartao), dataInicio, dataFim})) {
                            if (c2.moveToFirst()) {
                                valorFatura = c2.getDouble(0);
                            }
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
