package com.example.app1.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.app1.MeuDbHelper;

public class TransacoesCartaoDAO {

    private static final String TAG = "TransacoesCartaoDAO";

    /**
     * Exclui uma transação de cartão de crédito e suas parcelas, com verificação de segurança.
     *
     * @param context Contexto da aplicação.
     * @param idTransacaoCartao ID da transação de cartão a ser excluída.
     * @return 0 se a exclusão foi bem-sucedida.
     *         1 se a exclusão foi bloqueada porque a fatura já está paga.
     *         -1 se ocorreu um erro.
     */
    public static int excluirTransacao(Context context, int idTransacaoCartao) {
        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getWritableDatabase()) {

            Integer idFatura = null;
            double valorTransacao = 0;

            // 1. Obter id_fatura e valor da transação que será excluída
            try (Cursor c = db.rawQuery("SELECT id_fatura, valor FROM transacoes_cartao WHERE id = ?", new String[]{String.valueOf(idTransacaoCartao)})) {
                if (c.moveToFirst()) {
                    if (!c.isNull(0)) {
                        idFatura = c.getInt(0);
                    }
                    valorTransacao = c.getDouble(1);
                } else {
                    return -1; // Transação não encontrada
                }
            }

            // Se a transação não está associada a nenhuma fatura ou foi marcada como não alocável.
            if (idFatura == null || idFatura == -1) {
                db.beginTransaction();
                try {
                    db.delete("parcelas_cartao", "id_transacao_cartao = ?", new String[]{String.valueOf(idTransacaoCartao)});
                    db.delete("transacoes_cartao", "id = ?", new String[]{String.valueOf(idTransacaoCartao)});
                    db.setTransactionSuccessful();
                    Log.i(TAG, "Transação (ID: " + idTransacaoCartao + ") não vinculada a uma fatura foi excluída com sucesso.");
                    return 0;
                } finally {
                    db.endTransaction();
                }
            }

            // 2. Verificar o status da fatura associada
            try (Cursor c = db.rawQuery("SELECT status FROM faturas WHERE id = ?", new String[]{String.valueOf(idFatura)})) {
                if (c.moveToFirst()) {
                    int statusFatura = c.getInt(0);
                    if (statusFatura == 1) {
                        // CASO 1: FATURA PAGA - Bloquear exclusão
                        Log.w(TAG, "Tentativa de exclusão bloqueada. A transação (ID: " + idTransacaoCartao + ") pertence à fatura paga (ID: " + idFatura + ").");
                        return 1; // Código de erro para "Fatura Paga"
                    }
                }
            }

            // CASO 2: FATURA ABERTA - Proceder com a exclusão e atualização
            db.beginTransaction();
            try {
                // a. Subtrai o valor da transação do total da fatura
                db.execSQL("UPDATE faturas SET valor_total = valor_total - ? WHERE id = ?", new Object[]{valorTransacao, idFatura});

                // b. Exclui as parcelas associadas
                db.delete("parcelas_cartao", "id_transacao_cartao = ?", new String[]{String.valueOf(idTransacaoCartao)});

                // c. Exclui a transação principal
                db.delete("transacoes_cartao", "id = ?", new String[]{String.valueOf(idTransacaoCartao)});

                db.setTransactionSuccessful();
                Log.i(TAG, "Transação (ID: " + idTransacaoCartao + ") foi excluída com sucesso da fatura aberta (ID: " + idFatura + ").");
                return 0;

            } finally {
                db.endTransaction();
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao excluir transação de cartão.", e);
            return -1;
        }
    }
}
