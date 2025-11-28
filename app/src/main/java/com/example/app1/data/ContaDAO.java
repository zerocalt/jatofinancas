package com.example.app1.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.app1.Conta;
import com.example.app1.MeuDbHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ContaDAO {

    public static boolean inserirConta(Context ctx, Conta conta, int idUsuario) {
        MeuDbHelper dbHelper = new MeuDbHelper(ctx);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("nome", conta.getNome());
            values.put("saldo", conta.getSaldo());
            values.put("tipo_conta", conta.getTipoConta());
            values.put("cor", conta.getCor());
            values.put("mostrar_na_tela_inicial", conta.isMostrarNaTelaInicial());
            values.put("id_usuario", idUsuario);

            long result = db.insert("contas", null, values);
            return result != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Conta> getContasTelaInicial(Context ctx, int idUsuario) {
        List<Conta> contas = new ArrayList<>();
        if (idUsuario == -1) {
            return contas;
        }

        MeuDbHelper dbHelper = new MeuDbHelper(ctx);
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(
                     "SELECT id, nome, saldo, tipo_conta, cor, mostrar_na_tela_inicial " +
                             "FROM contas WHERE id_usuario = ? AND mostrar_na_tela_inicial = 1 ORDER BY nome COLLATE NOCASE ASC",
                     new String[]{String.valueOf(idUsuario)})) {

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                double saldo = cursor.getDouble(cursor.getColumnIndexOrThrow("saldo"));
                int tipoConta = cursor.getInt(cursor.getColumnIndexOrThrow("tipo_conta"));
                String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                boolean mostrarNaTelaInicial = cursor.getInt(cursor.getColumnIndexOrThrow("mostrar_na_tela_inicial")) > 0;
                contas.add(new Conta(id, nome, saldo, cor, tipoConta, mostrarNaTelaInicial));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contas;
    }

    public static List<Conta> carregarListaContas(Context ctx, int idUsuario) {
        List<Conta> contas = new ArrayList<>();

        if (idUsuario != -1) {
            MeuDbHelper dbHelper = new MeuDbHelper(ctx);

            String sql = "SELECT id, nome, saldo, tipo_conta, cor, mostrar_na_tela_inicial, " +
                    "(EXISTS (SELECT 1 FROM transacoes t WHERE t.id_conta = contas.id) OR " +
                    " EXISTS (SELECT 1 FROM cartoes c WHERE c.id_conta = contas.id)) as em_uso " +
                    "FROM contas WHERE id_usuario = ? ORDER BY nome COLLATE NOCASE ASC";

            try (SQLiteDatabase dbRead = dbHelper.getReadableDatabase();
                 Cursor cursor = dbRead.rawQuery(sql, new String[]{String.valueOf(idUsuario)})) {

                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                    double saldo = cursor.getDouble(cursor.getColumnIndexOrThrow("saldo"));
                    int tipoConta = cursor.getInt(cursor.getColumnIndexOrThrow("tipo_conta"));
                    String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                    boolean mostrarNaTelaInicial = cursor.getInt(cursor.getColumnIndexOrThrow("mostrar_na_tela_inicial")) > 0;
                    boolean emUso = cursor.getInt(cursor.getColumnIndexOrThrow("em_uso")) > 0;

                    Conta conta = new Conta(id, nome, saldo, cor, tipoConta, mostrarNaTelaInicial, emUso);
                    contas.add(conta);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return contas;
    }
    
    public static List<Conta> getContas(Context ctx, int idUsuario) {
        return carregarListaContas(ctx, idUsuario);
    }

    public static boolean excluirConta(Context ctx, int contaId) {
        MeuDbHelper dbHelper = new MeuDbHelper(ctx);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            int result = db.delete("contas", "id = ?", new String[]{String.valueOf(contaId)});
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Conta getContaById(Context ctx, int contaId) {
        MeuDbHelper dbHelper = new MeuDbHelper(ctx);
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(
                     "SELECT id, nome, saldo, tipo_conta, cor, mostrar_na_tela_inicial " +
                             "FROM contas WHERE id = ?",
                     new String[]{String.valueOf(contaId)})) {

            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                double saldo = cursor.getDouble(cursor.getColumnIndexOrThrow("saldo"));
                int tipoConta = cursor.getInt(cursor.getColumnIndexOrThrow("tipo_conta"));
                String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                boolean mostrarNaTelaInicial = cursor.getInt(cursor.getColumnIndexOrThrow("mostrar_na_tela_inicial")) > 0;
                return new Conta(id, nome, saldo, cor, tipoConta, mostrarNaTelaInicial);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean atualizarConta(Context ctx, Conta conta) {
        MeuDbHelper dbHelper = new MeuDbHelper(ctx);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("nome", conta.getNome());
            values.put("saldo", conta.getSaldo());
            values.put("tipo_conta", conta.getTipoConta());
            values.put("cor", conta.getCor());
            values.put("mostrar_na_tela_inicial", conta.isMostrarNaTelaInicial());

            int result = db.update("contas", values, "id = ?", new String[]{String.valueOf(conta.getId())});
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean atualizarSaldoConta(Context ctx, int contaId, double novoSaldo) {
        MeuDbHelper dbHelper = new MeuDbHelper(ctx);
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("saldo", novoSaldo);

            int result = db.update("contas", values, "id = ?", new String[]{String.valueOf(contaId)});
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Retorna o saldo total das contas que aparecem na tela inicial
    public static double getSaldoTotalContas(Context context, int idUsuario) {
        double saldoTotal = 0.0;

        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {

            String sql = "SELECT SUM(saldo) FROM contas WHERE id_usuario = ? AND mostrar_na_tela_inicial = 1";
            try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idUsuario)})) {
                if (cursor.moveToFirst()) {
                    saldoTotal = cursor.getDouble(0);
                }
            }

        } catch (Exception e) {
            Log.e("ContaDAO", "Erro ao calcular saldo total das contas", e);
        }

        return saldoTotal;
    }

    public static double getSaldoPrevistoConta(Context context, int idUsuario, int idConta, int ano, int mes) {
        double saldoAtual = 0.0;
        double receitasPendentes = 0.0;
        double despesasPendentes = 0.0;

        double recPendNormais = 0.0;
        double despPendNormais = 0.0;
        double despFixasInf = 0.0;
        double despMestresParc = 0.0;

        String mesAno = String.format(Locale.ROOT, "%04d-%02d", ano, mes);

        try (MeuDbHelper dbHelper = new MeuDbHelper(context);
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {

            // 1) Saldo atual da conta
            String sqlSaldo = "SELECT saldo FROM contas WHERE id = ? AND id_usuario = ?";
            try (Cursor c = db.rawQuery(sqlSaldo,
                    new String[]{String.valueOf(idConta), String.valueOf(idUsuario)})) {
                if (c.moveToFirst()) {
                    saldoAtual = c.getDouble(0);
                }
            }

            // 2) RECEITAS PENDENTES DA CONTA
            String qRecPend = "SELECT SUM(valor) FROM transacoes " +
                    "WHERE id_usuario = ? AND id_conta = ? " +
                    "AND tipo = 1 AND recebido = 0 " +
                    "AND substr(data_movimentacao,1,7) = ?";
            try (Cursor c = db.rawQuery(qRecPend,
                    new String[]{String.valueOf(idUsuario), String.valueOf(idConta), mesAno})) {
                if (c.moveToFirst() && !c.isNull(0)) {
                    recPendNormais = c.getDouble(0);
                }
            }
            receitasPendentes += recPendNormais;

            // 3) DESPESAS PENDENTES DA CONTA

            // 3.1 Despesas normais pendentes
            String qDespNormais = "SELECT SUM(valor) FROM transacoes " +
                    "WHERE id_usuario = ? AND id_conta = ? " +
                    "AND tipo = 2 AND pago = 0 " +
                    "AND (recorrente IS NULL OR recorrente = 0) " +
                    "AND substr(data_movimentacao,1,7) <= ?";
            try (Cursor c = db.rawQuery(qDespNormais,
                    new String[]{String.valueOf(idUsuario), String.valueOf(idConta), mesAno})) {
                if (c.moveToFirst() && !c.isNull(0)) {
                    despPendNormais = c.getDouble(0);
                }
            }
            despesasPendentes += despPendNormais;

            // 3.2 Mestres fixas infinitas SEM filhas no mês
            String qFixasInf = "SELECT SUM(valor) FROM transacoes t1 " +
                    "WHERE t1.id_usuario = ? AND t1.id_conta = ? " +
                    "AND t1.tipo = 2 " +
                    "AND t1.recorrente = 1 " +
                    "AND t1.recorrente_ativo = 1 " +
                    "AND (t1.total_parcelas = 0 OR t1.total_parcelas IS NULL) " +
                    "AND t1.pago = 0 " +
                    "AND t1.id NOT IN ( " +
                    "    SELECT DISTINCT t2.id_mestre " +
                    "    FROM transacoes t2 " +
                    "    WHERE t2.id_usuario = ? AND t2.id_conta = ? " +
                    "      AND t2.tipo = 2 " +
                    "      AND t2.id_mestre IS NOT NULL " +
                    "      AND substr(t2.data_movimentacao,1,7) = ? " +
                    ")";
            try (Cursor c = db.rawQuery(qFixasInf,
                    new String[]{String.valueOf(idUsuario), String.valueOf(idConta),
                            String.valueOf(idUsuario), String.valueOf(idConta), mesAno})) {
                if (c.moveToFirst() && !c.isNull(0)) {
                    despFixasInf = c.getDouble(0);
                }
            }
            despesasPendentes += despFixasInf;

            // 3.3 Mestres parceladas recorrentes SEM filhas no mês
            String qMestresParc = "SELECT SUM(valor) FROM transacoes t1 " +
                    "WHERE t1.id_usuario = ? AND t1.id_conta = ? " +
                    "AND t1.tipo = 2 " +
                    "AND t1.recorrente = 1 " +
                    "AND t1.recorrente_ativo = 1 " +
                    "AND t1.total_parcelas > 0 " +
                    "AND t1.pago = 0 " +
                    "AND t1.id NOT IN ( " +
                    "    SELECT DISTINCT t2.id_mestre " +
                    "    FROM transacoes t2 " +
                    "    WHERE t2.id_usuario = ? AND t2.id_conta = ? " +
                    "      AND t2.tipo = 2 " +
                    "      AND t2.id_mestre IS NOT NULL " +
                    "      AND substr(t2.data_movimentacao,1,7) = ? " +
                    ")";
            try (Cursor c = db.rawQuery(qMestresParc,
                    new String[]{String.valueOf(idUsuario), String.valueOf(idConta),
                            String.valueOf(idUsuario), String.valueOf(idConta), mesAno})) {
                if (c.moveToFirst() && !c.isNull(0)) {
                    despMestresParc = c.getDouble(0);
                }
            }
            despesasPendentes += despMestresParc;

            // 3.4 Faturas pendentes desta conta (via cartão -> conta)
            String qFaturasConta = "SELECT SUM(f.valor_total) FROM faturas f " +
                    "JOIN cartoes c ON f.id_cartao = c.id " +
                    "WHERE c.id_usuario = ? " +
                    "AND c.id_conta = ? " +
                    "AND f.status = 0 " +
                    "AND substr(f.data_vencimento,1,7) <= ?";
            try (Cursor c = db.rawQuery(qFaturasConta,
                    new String[]{String.valueOf(idUsuario), String.valueOf(idConta), mesAno})) {
                if (c.moveToFirst() && !c.isNull(0)) {
                    double despFaturas = c.getDouble(0);
                    despesasPendentes += despFaturas;
                }
            }

        } catch (Exception e) {
            Log.e("SaldoPrevistoConta", "Erro getSaldoPrevistoConta", e);
        }

        double saldoPrevisto = saldoAtual + receitasPendentes - despesasPendentes;
        return saldoPrevisto;
    }

}
