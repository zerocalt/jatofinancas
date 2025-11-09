package com.example.app1.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.app1.MeuDbHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransacoesCartaoDAO {

    private static final String TAG = "TransacoesCartaoDAO";

    // ---------------------- MÉTODOS PRINCIPAIS ----------------------

    public static long inserirTransacao(Context context,
                                        int idUsuario,
                                        int idCartao,
                                        String descricao,
                                        double valorTotal,
                                        int qtdParcelas,
                                        int fixa,
                                        int idCategoria,
                                        Date dataCompra,
                                        String observacao) {
        long idTransacao = -1;
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            // Inserir transação principal
            ContentValues cv = new ContentValues();
            cv.put("id_usuario", idUsuario);
            cv.put("id_cartao", idCartao);
            cv.put("descricao", descricao);
            cv.put("valor_total", valorTotal);
            cv.put("qtd_parcelas", qtdParcelas);
            cv.put("fixa", fixa);
            cv.put("id_categoria", idCategoria);
            cv.put("data_compra", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dataCompra));
            cv.put("observacao", observacao);
            cv.put("status", 1);
            idTransacao = db.insertOrThrow("transacoes_cartao", null, cv);

            // Obter dias do cartão
            int[] dias = obterDiasCartao(db, idCartao, dataCompra);
            int diaFechamento = dias[0];
            int diaVencimento = dias[1];

            // Gerar parcelas e faturas
            gerarParcelasEFaturas(db, idTransacao, idUsuario, idCartao, descricao, valorTotal,
                    qtdParcelas, fixa, idCategoria, dataCompra, observacao, diaFechamento, diaVencimento);

            db.setTransactionSuccessful();
            Log.i(TAG, "Transação inserida com sucesso. ID: " + idTransacao);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao inserir transação", e);
        } finally {
            db.endTransaction();
            db.close();
            dbHelper.close();
        }

        return idTransacao;
    }

    public static boolean atualizarTransacao(Context context,
                                             int idTransacao,
                                             String descricao,
                                             double valorTotal,
                                             int qtdParcelas,
                                             int fixa,
                                             int idCategoria,
                                             Date dataCompra,
                                             String observacao,
                                             int idCartao,
                                             int idUsuario) {
        boolean sucesso = false;
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            // Atualiza transação principal
            ContentValues cv = new ContentValues();
            cv.put("descricao", descricao);
            cv.put("valor_total", valorTotal);
            cv.put("qtd_parcelas", qtdParcelas);
            cv.put("fixa", fixa);
            cv.put("id_categoria", idCategoria);
            cv.put("data_compra", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dataCompra));
            cv.put("observacao", observacao);
            cv.put("id_cartao", idCartao);

            int rows = db.update("transacoes_cartao", cv, "id = ?", new String[]{String.valueOf(idTransacao)});

            // Remove parcelas antigas
            db.delete("parcelas_cartao", "id_transacao = ?", new String[]{String.valueOf(idTransacao)});

            // Obter dias do cartão
            int[] dias = obterDiasCartao(db, idCartao, dataCompra);
            int diaFechamento = dias[0];
            int diaVencimento = dias[1];

            // Recria parcelas e faturas
            gerarParcelasEFaturas(db, idTransacao, idUsuario, idCartao, descricao, valorTotal,
                    qtdParcelas, fixa, idCategoria, dataCompra, observacao, diaFechamento, diaVencimento);

            db.setTransactionSuccessful();
            sucesso = rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao atualizar transação", e);
        } finally {
            db.endTransaction();
            db.close();
            dbHelper.close();
        }

        return sucesso;
    }

    public static boolean excluirTransacao(Context context, int idTransacao) {
        boolean sucesso = false;
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            db.delete("parcelas_cartao", "id_transacao = ?", new String[]{String.valueOf(idTransacao)});
            int rows = db.delete("transacoes_cartao", "id = ?", new String[]{String.valueOf(idTransacao)});

            db.setTransactionSuccessful();
            sucesso = rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao excluir transação", e);
        } finally {
            db.endTransaction();
            db.close();
            dbHelper.close();
        }

        return sucesso;
    }

    public static List<ContentValues> buscarTransacoesPorUsuario(Context context, int idUsuario, int idCartao) {
        List<ContentValues> transacoes = new ArrayList<>();
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor c = db.rawQuery("SELECT * FROM transacoes_cartao WHERE id_usuario = ? AND id_cartao = ? AND status = 1 ORDER BY data_compra DESC",
                new String[]{String.valueOf(idUsuario), String.valueOf(idCartao)})) {

            while (c.moveToNext()) {
                ContentValues cv = new ContentValues();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    int type = c.getType(i);
                    switch (type) {
                        case Cursor.FIELD_TYPE_INTEGER:
                            cv.put(c.getColumnName(i), c.getInt(i));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            cv.put(c.getColumnName(i), c.getString(i));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            cv.put(c.getColumnName(i), c.getDouble(i));
                            break;
                        case Cursor.FIELD_TYPE_NULL:
                        default:
                            cv.putNull(c.getColumnName(i));
                    }
                }
                transacoes.add(cv);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar transações", e);
        } finally {
            db.close();
            dbHelper.close();
        }

        return transacoes;
    }

    // ---------------------- MÉTODOS AUXILIARES ----------------------

    private static void gerarParcelasEFaturas(SQLiteDatabase db,
                                              long idTransacao,
                                              int idUsuario,
                                              int idCartao,
                                              String descricao,
                                              double valorTotal,
                                              int qtdParcelas,
                                              int fixa,
                                              int idCategoria,
                                              Date dataCompra,
                                              String observacao,
                                              int diaFechamentoCartao,
                                              int diaVencimentoCartao) {
        try {
            double valorParcela = valorTotal / qtdParcelas;
            Calendar cal = Calendar.getInstance();
            cal.setTime(dataCompra);

            for (int i = 1; i <= qtdParcelas; i++) {

                // Determina a fatura correta para a data da parcela
                long idFatura = localizarOuCriarFatura(db, idCartao, cal.getTime(), diaFechamentoCartao, diaVencimentoCartao);

                // Cria parcela
                ContentValues pVal = new ContentValues();
                pVal.put("id_transacao", idTransacao);
                pVal.put("id_usuario", idUsuario);
                pVal.put("id_cartao", idCartao);
                pVal.put("descricao", descricao + (qtdParcelas > 1 ? " (" + i + "/" + qtdParcelas + ")" : ""));
                pVal.put("valor", valorParcela);
                pVal.put("num_parcela", i);
                pVal.put("total_parcelas", qtdParcelas);
                pVal.put("data_compra", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime()));

                // Data de vencimento da parcela = dia de vencimento da fatura
                Calendar venc = Calendar.getInstance();
                venc.setTime(cal.getTime());
                // Se dia de vencimento menor que dia da compra, passa para o próximo mês
                if (venc.get(Calendar.DAY_OF_MONTH) > diaVencimentoCartao) {
                    venc.add(Calendar.MONTH, 1);
                }
                venc.set(Calendar.DAY_OF_MONTH, diaVencimentoCartao);
                pVal.put("data_vencimento", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(venc.getTime()));

                pVal.put("id_fatura", idFatura);
                pVal.put("fixa", fixa);
                pVal.put("id_categoria", idCategoria);
                pVal.put("observacao", observacao);
                db.insert("parcelas_cartao", null, pVal);

                // Atualiza valor total da fatura
                db.execSQL("UPDATE faturas SET valor_total = valor_total + ? WHERE id = ?", new Object[]{valorParcela, idFatura});

                // Próximo mês
                cal.add(Calendar.MONTH, 1);
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao gerar parcelas", e);
        }
    }

    private static long localizarOuCriarFatura(SQLiteDatabase db, int idCartao, Date dataCompra,
                                               int diaFechamento, int diaVencimento) {
        Calendar compra = Calendar.getInstance();
        compra.setTime(dataCompra);

        // Compra após fechamento -> fatura do próximo mês
        if (compra.get(Calendar.DAY_OF_MONTH) > diaFechamento) {
            compra.add(Calendar.MONTH, 1);
        }

        int mes = compra.get(Calendar.MONTH) + 1; // Janeiro=0
        int ano = compra.get(Calendar.YEAR);

        // Verifica se fatura existe
        try (Cursor c = db.rawQuery(
                "SELECT id FROM faturas WHERE id_cartao = ? AND mes = ? AND ano = ?",
                new String[]{String.valueOf(idCartao), String.valueOf(mes), String.valueOf(ano)})) {
            if (c.moveToFirst()) return c.getLong(0);
        }

        // Cria nova fatura
        ContentValues fVals = new ContentValues();
        fVals.put("id_cartao", idCartao);
        fVals.put("mes", mes);
        fVals.put("ano", ano);

        // Calcula data de vencimento da fatura
        Calendar venc = Calendar.getInstance();
        venc.set(Calendar.YEAR, ano);
        venc.set(Calendar.MONTH, mes - 1);
        venc.set(Calendar.DAY_OF_MONTH, diaVencimento);
        fVals.put("data_vencimento", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(venc.getTime()));

        fVals.put("valor_total", 0);
        fVals.put("status", 0); // aberta

        return db.insert("faturas", null, fVals);
    }

    private static int[] obterDiasCartao(SQLiteDatabase db, int idCartao, Date dataCompra) {
        int diaFechamento = -1;
        int diaVencimento = 5; // default

        try (Cursor c = db.rawQuery(
                "SELECT data_fechamento_fatura, data_vencimento_fatura FROM cartoes WHERE id = ?",
                new String[]{String.valueOf(idCartao)})) {
            if (c.moveToFirst()) {
                diaFechamento = c.getInt(0);
                diaVencimento = c.getInt(1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao obter dias do cartão", e);
        }

        // Se dia de fechamento não definido ou zero, assume último dia do mês
        if (diaFechamento <= 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dataCompra);
            diaFechamento = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        }

        return new int[]{diaFechamento, diaVencimento};
    }

}