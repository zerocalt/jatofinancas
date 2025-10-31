package com.example.app1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.app1.data.CartaoDAO;
import com.example.app1.utils.MenuBottomUtils;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialog;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.zip.Inflater;

public class TelaPrincipal extends AppCompatActivity {
    private TextView txtMes;
    private TextView txtAno;

    private TextView saldoContas, limitesCartoes, receitasMes, despesasMes;

    private int idUsuarioLogado = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_principal);

        final View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right, systemBarsInsets.bottom);
            return insets;
        });

        idUsuarioLogado = getIdUsuarioLogado();

        txtMes = findViewById(R.id.txtMes);
        txtAno = findViewById(R.id.txtAno);
        saldoContas = findViewById(R.id.saldoContas);
        limitesCartoes = findViewById(R.id.limitesCartoes);
        receitasMes = findViewById(R.id.receitasMes);
        despesasMes = findViewById(R.id.despesasMes);

        Calendar agora = Calendar.getInstance();
        String[] nomesMes = {
                "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        };
        txtMes.setText(nomesMes[agora.get(Calendar.MONTH)]);
        txtAno.setText(String.valueOf(agora.get(Calendar.YEAR)));

        LinearLayout btnMesAno = findViewById(R.id.btnMesAno);
        btnMesAno.setOnClickListener(v -> showMonthYearPicker());

        Bundle args = new Bundle();
        args.putInt("botaoInativo", BottomMenuFragment.PRINCIPAL);
        BottomMenuFragment fragment = new BottomMenuFragment();
        fragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.menu_container, fragment)
                .commit();

        carregarResumos();
    }

    private void carregarResumos() {
        saldoContas.setText(formatarBR(getSaldoTotalContas()));
        limitesCartoes.setText(formatarBR(getLimiteDisponivelTotalCartoes()));
        receitasMes.setText(formatarBR(getReceitasMes()));
        despesasMes.setText(formatarBR(getDespesasMes()));

        receitasMes.setTextColor(Color.parseColor("#006400"));
        despesasMes.setTextColor(Color.parseColor("#E45757"));
    }

    private double getSaldoTotalContas() {
        double saldoTotal = 0;
        try (MeuDbHelper dbHelper = new MeuDbHelper(this);
             SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT SUM(saldo) FROM contas WHERE id_usuario = ?", new String[]{String.valueOf(idUsuarioLogado)})) {
            if (cursor.moveToFirst()) {
                saldoTotal = cursor.getDouble(0);
            }
        }
        return saldoTotal;
    }

    private double getLimiteDisponivelTotalCartoes() {
        double limiteDisponivelTotal = 0;
        try (MeuDbHelper dbHelper = new MeuDbHelper(this);
             SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT id, limite FROM cartoes WHERE id_usuario = ? AND ativo = 1", new String[]{String.valueOf(idUsuarioLogado)})) {

            while (cursor.moveToNext()) {
                int idCartao = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                double limiteCartao = cursor.getDouble(cursor.getColumnIndexOrThrow("limite"));
                double limiteUtilizado = CartaoDAO.calcularLimiteUtilizado(this, idCartao);
                limiteDisponivelTotal += (limiteCartao - limiteUtilizado);
            }
        }
        return limiteDisponivelTotal;
    }

    private double getReceitasMes() {
        double receitasContas = getSomaTransacoesPorTipo(1);
        double receitasRecorrentes = getSomaTransacoesRecorrentesMes(1);
        return receitasContas + receitasRecorrentes;
    }

    private double getDespesasMes() {
        double despesasContas = getSomaTransacoesPorTipo(2);
        double despesasRecorrentes = getSomaTransacoesRecorrentesMes(2);
        double faturasCartao = getSomaFaturasCartao();
        return despesasContas + despesasRecorrentes + faturasCartao;
    }

    private double getSomaTransacoesPorTipo(int tipo) {
        double total = 0;
        String mesStr = txtMes.getText().toString();
        int ano = Integer.parseInt(txtAno.getText().toString());
        int mes = 0;
        final String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        for (int i = 0; i < nomesMes.length; i++) {
            if (nomesMes[i].equalsIgnoreCase(mesStr)) {
                mes = i;
                break;
            }
        }

        Calendar inicio = Calendar.getInstance();
        inicio.set(ano, mes, 1);
        Calendar fim = Calendar.getInstance();
        fim.set(ano, mes, inicio.getActualMaximum(Calendar.DAY_OF_MONTH));

        String dataInicio = String.format(Locale.ROOT, "%04d-%02d-%02d", inicio.get(Calendar.YEAR), inicio.get(Calendar.MONTH) + 1, inicio.get(Calendar.DAY_OF_MONTH));
        String dataFim = String.format(Locale.ROOT, "%04d-%02d-%02d", fim.get(Calendar.YEAR), fim.get(Calendar.MONTH) + 1, fim.get(Calendar.DAY_OF_MONTH));

        try (MeuDbHelper dbHelper = new MeuDbHelper(this);
             SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT SUM(valor) FROM transacoes WHERE id_usuario = ? AND tipo = ? AND data_movimentacao BETWEEN ? AND ?", new String[]{String.valueOf(idUsuarioLogado), String.valueOf(tipo), dataInicio, dataFim})) {
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        }
        return total;
    }

    private double getSomaTransacoesRecorrentesMes(int tipo) {
        double total = 0;
        String mesStr = txtMes.getText().toString();
        int ano = Integer.parseInt(txtAno.getText().toString());
        int mes = 0;
        final String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        for (int i = 0; i < nomesMes.length; i++) {
            if (nomesMes[i].equalsIgnoreCase(mesStr)) {
                mes = i;
                break;
            }
        }

        // Formato 'YYYY-MM' para o mês selecionado
        String mesAnoSelecionado = String.format(Locale.ROOT, "%04d-%02d", ano, mes + 1);

        try (MeuDbHelper dbHelper = new MeuDbHelper(this);
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {

            // 1. Buscar transações mestre recorrentes ativas que começaram antes ou no mês selecionado
            String query = "SELECT id, valor, data_movimentacao, repetir_periodo FROM transacoes " +
                           "WHERE id_usuario = ? AND recorrente_ativo = 1 AND tipo = ? AND id_mestre IS NULL " +
                           "AND strftime('%Y-%m', data_movimentacao) <= ?";
            
            try (Cursor cursorMestre = db.rawQuery(query, new String[]{String.valueOf(idUsuarioLogado), String.valueOf(tipo), mesAnoSelecionado})) {
                while (cursorMestre.moveToNext()) {
                    int idMestre = cursorMestre.getInt(cursorMestre.getColumnIndexOrThrow("id"));
                    double valor = cursorMestre.getDouble(cursorMestre.getColumnIndexOrThrow("valor"));
                    String dataInicioStr = cursorMestre.getString(cursorMestre.getColumnIndexOrThrow("data_movimentacao"));
                    int repetirPeriodo = cursorMestre.getInt(cursorMestre.getColumnIndexOrThrow("repetir_periodo"));

                    // 2. Verificar se já existe uma parcela para esta transação mestre no mês selecionado
                    String queryParcela = "SELECT id FROM transacoes WHERE id_mestre = ? AND strftime('%Y-%m', data_movimentacao) = ?";
                    boolean parcelaExiste = false;
                    try (Cursor cursorParcela = db.rawQuery(queryParcela, new String[]{String.valueOf(idMestre), mesAnoSelecionado})) {
                        if (cursorParcela.moveToFirst()) {
                            parcelaExiste = true;
                        }
                    }
                    
                    // 3. Se não houver parcela e a transação for aplicável ao mês, adicionar o valor
                    if (!parcelaExiste) {
                         // Lógica para verificar se a transação recorrente deve ocorrer no mês selecionado
                        if (shouldOccurInMonth(dataInicioStr, repetirPeriodo, ano, mes)) {
                            total += valor;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Tratar exceção
        }

        return total;
    }

    private boolean shouldOccurInMonth(String dataInicioStr, int repetirPeriodo, int anoSelecionado, int mesSelecionado) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            Calendar calInicio = Calendar.getInstance();
            calInicio.setTime(sdf.parse(dataInicioStr));

            int anoInicio = calInicio.get(Calendar.YEAR);
            int mesInicio = calInicio.get(Calendar.MONTH);

            if (anoSelecionado < anoInicio || (anoSelecionado == anoInicio && mesSelecionado < mesInicio)) {
                return false; // Mês selecionado é anterior ao início da recorrência
            }

            int diffEmMeses = (anoSelecionado - anoInicio) * 12 + (mesSelecionado - mesInicio);

            switch (repetirPeriodo) {
                case 1: // Semanal - Complicado, por simplicidade, vamos considerar mensal por enquanto.
                     // Para uma lógica semanal correta, precisaríamos de mais detalhes
                    return true; // Simplificação: se for semanal, aparece todo mês.
                case 2: // Mensal
                    return true;
                case 3: // Bimestral
                    return diffEmMeses % 2 == 0;
                case 4: // Trimestral
                    return diffEmMeses % 3 == 0;
                case 5: // Semestral
                    return diffEmMeses % 6 == 0;
                case 6: // Anual
                    return diffEmMeses % 12 == 0;
                default:
                    return false;
            }
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private double getSomaFaturasCartao() {
        double totalFaturas = 0;
        String mesStr = txtMes.getText().toString();
        int ano = Integer.parseInt(txtAno.getText().toString());
        int mes = 0;
        final String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        for (int i = 0; i < nomesMes.length; i++) {
            if (nomesMes[i].equalsIgnoreCase(mesStr)) {
                mes = i;
                break;
            }
        }

        try (MeuDbHelper dbHelper = new MeuDbHelper(this);
             SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT SUM(f.valor_total) FROM faturas f JOIN cartoes c ON f.id_cartao = c.id WHERE c.id_usuario = ? AND strftime('%m', f.data_vencimento) = ? AND strftime('%Y', f.data_vencimento) = ?", new String[]{String.valueOf(idUsuarioLogado), String.format(Locale.ROOT, "%02d", mes + 1), String.valueOf(ano)})) {
            if (cursor.moveToFirst()) {
                totalFaturas = cursor.getDouble(0);
            }
        }
        return totalFaturas;
    }

    private int getIdUsuarioLogado() {
        try {
            MasterKey masterKey = new MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            SharedPreferences prefs = EncryptedSharedPreferences.create(this, "secure_login_prefs", masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            return prefs.getInt("saved_user_id", -1);
        } catch (Exception e) {
            return -1;
        }
    }

    private String formatarBR(double valor) {
        NumberFormat formatoBR = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return formatoBR.format(valor);
    }

    private void showMonthYearPicker() {
        String mesSelecionado = txtMes.getText().toString();
        int anoSelecionado = Integer.parseInt(txtAno.getText().toString());

        String[] nomesMes = {
                "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        };

        int mesIndex = 0;
        for (int i = 0; i < nomesMes.length; i++) {
            if (nomesMes[i].equalsIgnoreCase(mesSelecionado)) {
                mesIndex = i;
                break;
            }
        }

        MonthYearPickerDialogFragment dialogFragment =
                MonthYearPickerDialogFragment.getInstance(mesIndex, anoSelecionado);

        dialogFragment.setOnDateSetListener((year, monthOfYear) -> {
            txtMes.setText(nomesMes[monthOfYear]);
            txtAno.setText(String.valueOf(year));
            carregarResumos();
        });

        dialogFragment.show(getSupportFragmentManager(), "MonthYearPickerDialog");
    }
}
