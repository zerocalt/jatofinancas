package com.example.app1.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.app1.MeuDbHelper;
import com.github.mikephil.charting.data.PieEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GraficosDAO {

    public static class CategoriaGasto {
        public String nome;
        public float total;
        public String cor;

        public CategoriaGasto(String nome, float total, String cor) {
            this.nome = nome;
            this.total = total;
            this.cor = cor;
        }
    }

    public static class BalancoMensal {
        public String mesAno;
        public float receitas;
        public float despesas;

        public BalancoMensal(String mesAno, float receitas, float despesas) {
            this.mesAno = mesAno;
            this.receitas = receitas;
            this.despesas = despesas;
        }
    }

    public static List<CategoriaGasto> getDespesasPorCategoria(Context context, int idUsuario, int ano, int mes) {
        List<CategoriaGasto> resultados = new ArrayList<>();
        String mesAno = String.format(Locale.ROOT, "%04d-%02d", ano, mes + 1);

        try (SQLiteDatabase db = new MeuDbHelper(context).getReadableDatabase()) {
            String sql = "SELECT categoria_nome, categoria_cor, SUM(total) as total_categoria FROM ( " +
                     "    SELECT c.nome as categoria_nome, c.cor as categoria_cor, t.valor as total " +
                     "    FROM transacoes t JOIN categorias c ON t.id_categoria = c.id " +
                     "    WHERE t.id_usuario = ? AND t.tipo = 2 AND substr(t.data_movimentacao, 1, 7) = ? " +
                     "    UNION ALL " +
                     "    SELECT 'Fatura de Cartão' as categoria_nome, cr.cor as categoria_cor, f.valor_total as total " +
                     "    FROM faturas f JOIN cartoes cr ON f.id_cartao = cr.id " +
                     "    WHERE cr.id_usuario = ? AND substr(f.data_vencimento, 1, 7) = ? " +
                     "    UNION ALL " +
                     "    SELECT c.nome as categoria_nome, c.cor as categoria_cor, mestre.valor as total " +
                     "    FROM transacoes AS mestre JOIN categorias c ON mestre.id_categoria = c.id " +
                     "    WHERE mestre.id_usuario = ? AND mestre.tipo = 2 AND mestre.recorrente = 1 AND mestre.recorrente_ativo = 1 AND mestre.id_mestre IS NULL " +
                     "    AND substr(mestre.data_movimentacao, 1, 7) <= ? AND NOT EXISTS (SELECT 1 FROM transacoes AS filha WHERE filha.id_mestre = mestre.id AND substr(filha.data_movimentacao, 1, 7) = ?) " +
                     ") GROUP BY categoria_nome, categoria_cor HAVING total_categoria > 0";

            try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idUsuario), mesAno, String.valueOf(idUsuario), mesAno, String.valueOf(idUsuario), mesAno, mesAno})) {
                while (cursor.moveToNext()) {
                    resultados.add(new CategoriaGasto(
                        cursor.getString(cursor.getColumnIndexOrThrow("categoria_nome")),
                        cursor.getFloat(cursor.getColumnIndexOrThrow("total_categoria")),
                        cursor.getString(cursor.getColumnIndexOrThrow("categoria_cor"))
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultados;
    }

    public static List<BalancoMensal> getFluxoDeCaixaMensal(Context context, int idUsuario) {
        return getBalanco(context, idUsuario, true);
    }

    public static List<BalancoMensal> getTendenciaMensal(Context context, int idUsuario) {
        return getBalanco(context, idUsuario, false);
    }

    private static List<BalancoMensal> getBalanco(Context context, int idUsuario, boolean apenasPago) {
        List<BalancoMensal> balanco = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdfLabel = new SimpleDateFormat("MMM/yy", new Locale("pt", "BR"));
        SimpleDateFormat sdfQuery = new SimpleDateFormat("yyyy-MM", Locale.ROOT);

        for (int i = 0; i < 6; i++) {
            String mesAnoLabel = sdfLabel.format(cal.getTime());
            String mesAnoQuery = sdfQuery.format(cal.getTime());

            float totalReceitas = 0;
            float totalDespesas = 0;

            try (SQLiteDatabase db = new MeuDbHelper(context).getReadableDatabase()) {
                String[] args = {String.valueOf(idUsuario), mesAnoQuery};
                String filtroPago = apenasPago ? " AND pago = 1" : "";
                String filtroRecebido = apenasPago ? " AND recebido = 1" : "";
                String filtroStatusFatura = apenasPago ? " AND f.status = 1" : "";

                try (Cursor c = db.rawQuery("SELECT SUM(valor) FROM transacoes WHERE id_usuario = ? AND tipo = 1" + filtroRecebido + " AND substr(data_movimentacao, 1, 7) = ? AND id_categoria != (SELECT id FROM categorias WHERE nome = 'Saldo Inicial')", args)) {
                    if (c.moveToFirst()) totalReceitas = c.getFloat(0);
                }
                try (Cursor c = db.rawQuery("SELECT SUM(valor) FROM transacoes WHERE id_usuario = ? AND tipo = 2" + filtroPago + " AND substr(data_movimentacao, 1, 7) = ?", args)) {
                    if (c.moveToFirst()) totalDespesas += c.getFloat(0);
                }
                try (Cursor c = db.rawQuery("SELECT SUM(f.valor_total) FROM faturas f JOIN cartoes cr ON f.id_cartao = cr.id WHERE cr.id_usuario = ?" + filtroStatusFatura + " AND substr(f.data_vencimento, 1, 7) = ?", args)) {
                    if (c.moveToFirst()) totalDespesas += c.getFloat(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            balanco.add(new BalancoMensal(mesAnoLabel, totalReceitas, totalDespesas));
            cal.add(Calendar.MONTH, -1);
        }
        Collections.reverse(balanco);
        return balanco;
    }
}
