package com.example.app1.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.app1.MeuDbHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TransacoesDAO {

    /**
     * Salva uma despesa ou receita única.
     * Atualiza saldo da conta caso esteja marcada como paga ou recebida.
     */
    public static boolean salvarTransacaoUnica(Context context, int idUsuario, int idConta, double valor,
                                               int tipo, int pago, int recebido, String dataMovimentacao,
                                               String descricao, int idCategoria, String observacao) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);

        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put("id_usuario", idUsuario);
            cv.put("id_conta", idConta);
            cv.put("valor", valor);
            cv.put("tipo", tipo);
            cv.put("pago", pago);
            cv.put("recebido", recebido);
            cv.put("data_movimentacao", dataMovimentacao);
            cv.put("descricao", descricao);
            cv.put("id_categoria", idCategoria);
            cv.put("observacao", observacao);

            long resultado = db.insert("transacoes", null, cv);

            // 🟩 Atualiza saldo se for pago (despesa) ou recebido (receita)
            if ((tipo == 2 && pago == 1) || (tipo == 1 && recebido == 1)) {
                atualizarSaldoConta(db, idConta, valor, tipo);
            }

            return resultado != -1;
        } catch (Exception e) {
            Log.e("TransacoesDAO", "Erro ao salvar transação única", e);
            return false;
        }
    }

    /**
     * Salva uma transação recorrente (fixa ou parcelada).
     * A primeira parcela é marcada como paga/recebida se o usuário tiver selecionado.
     * Atualiza saldo da conta apenas na primeira parcela confirmada.
     */
    public static boolean salvarTransacaoRecorrente(
            Context context,
            int idUsuario,
            int idConta,
            double valor,
            int tipo,
            int pago,
            int recebido,
            String dataMovimentacao,
            String descricao,
            int idCategoria,
            String observacao,
            int repetirQtd,
            int repetirPeriodo,
            int recorrenteAtivo
    ) {
        MeuDbHelper dbHelper = new MeuDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 🟩 Transação mestre
            ContentValues mestreCv = new ContentValues();
            mestreCv.put("id_usuario", idUsuario);
            mestreCv.put("id_conta", idConta);
            mestreCv.put("valor", valor);
            mestreCv.put("tipo", tipo);
            mestreCv.put("pago", 0);
            mestreCv.put("recebido", recebido);
            mestreCv.put("data_movimentacao", dataMovimentacao);
            mestreCv.put("descricao", descricao);
            mestreCv.put("id_categoria", idCategoria);
            mestreCv.put("observacao", observacao);
            mestreCv.put("recorrente", 1);
            mestreCv.put("repetir_qtd", repetirQtd);
            mestreCv.put("repetir_periodo", repetirPeriodo);
            mestreCv.put("recorrente_ativo", recorrenteAtivo);

            long idMestre = db.insert("transacoes", null, mestreCv);
            if (idMestre == -1) return false;

            // 🟩 Parcela(s)
            for (int i = 0; i < repetirQtd; i++) {
                ContentValues parcelaCv = new ContentValues();
                parcelaCv.put("id_usuario", idUsuario);
                parcelaCv.put("id_conta", idConta);
                parcelaCv.put("valor", valor);
                parcelaCv.put("tipo", tipo);
                parcelaCv.put("pago", (i == 0) ? pago : 0);
                parcelaCv.put("recebido", (i == 0) ? recebido : 0);

                String dataParcela = calcularDataRecorrente(dataMovimentacao, repetirPeriodo, i);
                parcelaCv.put("data_movimentacao", dataParcela);

                parcelaCv.put("descricao", descricao);
                parcelaCv.put("id_categoria", idCategoria);
                parcelaCv.put("observacao", observacao);
                parcelaCv.put("id_mestre", idMestre);

                long idParcela = db.insert("transacoes", null, parcelaCv);
                if (idParcela == -1) return false;

                // 🟩 Atualiza saldo somente na 1ª parcela confirmada
                if (i == 0 && ((tipo == 2 && pago == 1) || (tipo == 1 && recebido == 1))) {
                    atualizarSaldoConta(db, idConta, valor, tipo);
                }
            }

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e("TransacoesDAO", "Erro ao salvar transação recorrente", e);
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Atualiza saldo da conta:
     * - Subtrai valor se for despesa (tipo=2)
     * - Soma valor se for receita (tipo=1)
     */
    private static void atualizarSaldoConta(SQLiteDatabase db, int idConta, double valor, int tipo) {
        Cursor cursor = db.rawQuery("SELECT saldo FROM contas WHERE id = ?", new String[]{String.valueOf(idConta)});
        if (cursor.moveToFirst()) {
            double saldoAtual = cursor.getDouble(cursor.getColumnIndexOrThrow("saldo"));
            double novoSaldo = (tipo == 1) ? (saldoAtual + valor) : (saldoAtual - valor);

            ContentValues cv = new ContentValues();
            cv.put("saldo", novoSaldo);
            db.update("contas", cv, "id = ?", new String[]{String.valueOf(idConta)});
        }
        cursor.close();
    }

    private static String calcularDataRecorrente(String dataInicial, int periodo, int indiceParcela) {
        Calendar cal = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date data = sdf.parse(dataInicial);
            cal.setTime(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        switch (periodo) {
            case 1: cal.add(Calendar.WEEK_OF_YEAR, indiceParcela); break;
            case 2: cal.add(Calendar.MONTH, indiceParcela); break;
            case 3: cal.add(Calendar.MONTH, indiceParcela * 2); break;
            case 4: cal.add(Calendar.MONTH, indiceParcela * 3); break;
            case 5: cal.add(Calendar.MONTH, indiceParcela * 6); break;
            case 6: cal.add(Calendar.YEAR, indiceParcela); break;
        }

        SimpleDateFormat sdfFinal = new SimpleDateFormat("yyyy-MM-dd");
        return sdfFinal.format(cal.getTime());
    }
}