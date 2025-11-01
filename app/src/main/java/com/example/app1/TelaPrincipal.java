package com.example.app1;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TelaPrincipal extends AppCompatActivity {
    private TextView txtMes, txtAno;
    private TextView saldoContas, receitasMes, despesasMes, valorReceitasPendentes, valorDespesasPendentes;
    private LinearLayout receitasPendentes, despesasPendentes, alertasPendentes;
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
        receitasMes = findViewById(R.id.receitasMes);
        despesasMes = findViewById(R.id.despesasMes);
        valorReceitasPendentes = findViewById(R.id.valorReceitasPendentes);
        valorDespesasPendentes = findViewById(R.id.valorDespesasPendentes);
        receitasPendentes = findViewById(R.id.receitasPendentes);
        despesasPendentes = findViewById(R.id.despesasPendentes);
        alertasPendentes = findViewById(R.id.alertasPendentes);

        Calendar agora = Calendar.getInstance();
        String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        txtMes.setText(nomesMes[agora.get(Calendar.MONTH)]);
        txtAno.setText(String.valueOf(agora.get(Calendar.YEAR)));

        findViewById(R.id.btnMesAno).setOnClickListener(v -> showMonthYearPicker());

        Bundle args = new Bundle();
        args.putInt("botaoInativo", BottomMenuFragment.PRINCIPAL);
        BottomMenuFragment fragment = new BottomMenuFragment();
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction().replace(R.id.menu_container, fragment).commit();

        atualizarValoresTela();
    }

    private void atualizarValoresTela() {
        // 1. Saldo em contas
        saldoContas.setText(formatarBR(getSaldoTotalContas()));

        // 2. Receitas, Despesas e Pendências
        String mesStr = txtMes.getText().toString();
        int ano = Integer.parseInt(txtAno.getText().toString());
        int mes = getMesIndex(mesStr);
        String mesAnoSelecionado = String.format(Locale.ROOT, "%04d-%02d", ano, mes + 1);

        double totalReceitas = 0;
        double totalDespesas = 0;
        double receitasPendentesValor = 0;
        double despesasPendentesValor = 0;

        try (MeuDbHelper dbHelper = new MeuDbHelper(this);
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {

            // Parte 1: Processar transações existentes no mês (avulsas e filhas de recorrentes)
            String queryExistentes = "SELECT valor, tipo, pago, recebido FROM transacoes WHERE id_usuario = ? AND substr(data_movimentacao, 1, 7) = ?";
            try (Cursor cur = db.rawQuery(queryExistentes, new String[]{String.valueOf(idUsuarioLogado), mesAnoSelecionado})) {
                while (cur.moveToNext()) {
                    double valor = cur.getDouble(0);
                    int tipo = cur.getInt(1);
                    int pago = cur.getInt(2);
                    int recebido = cur.getInt(3);

                    if (tipo == 1) { // Receita
                        totalReceitas += valor;
                        if (recebido == 0) receitasPendentesValor += valor;
                    } else { // Despesa
                        totalDespesas += valor;
                        if (pago == 0) despesasPendentesValor += valor;
                    }
                }
            }

            // Parte 2: Processar projeções de transações recorrentes (fixas)
            String queryProjecao = "SELECT mestre.valor, mestre.tipo, mestre.data_movimentacao, mestre.repetir_periodo " +
                                   "FROM transacoes AS mestre " +
                                   "WHERE mestre.id_usuario = ? AND mestre.recorrente = 1 AND mestre.recorrente_ativo = 1 AND mestre.id_mestre IS NULL " +
                                   "AND substr(mestre.data_movimentacao, 1, 7) <= ? " +
                                   "AND NOT EXISTS (SELECT 1 FROM transacoes AS filha WHERE filha.id_mestre = mestre.id AND substr(filha.data_movimentacao, 1, 7) = ?)";
            try (Cursor curMestre = db.rawQuery(queryProjecao, new String[]{String.valueOf(idUsuarioLogado), mesAnoSelecionado, mesAnoSelecionado})) {
                while (curMestre.moveToNext()) {
                    double valor = curMestre.getDouble(0);
                    int tipo = curMestre.getInt(1);
                    String dataInicioStr = curMestre.getString(2);
                    int repetirPeriodo = curMestre.getInt(3);

                    if (shouldOccurInMonth(dataInicioStr, repetirPeriodo, ano, mes)) {
                        if (tipo == 1) {
                            totalReceitas += valor;
                            receitasPendentesValor += valor;
                        } else {
                            totalDespesas += valor;
                            despesasPendentesValor += valor;
                        }
                    }
                }
            }

            // Parte 3: Processar faturas de cartão
            String queryFaturas = "SELECT f.valor_total, f.status FROM faturas f " +
                                  "JOIN cartoes c ON f.id_cartao = c.id " +
                                  "WHERE c.id_usuario = ? AND substr(f.data_vencimento, 1, 7) = ?";
            try (Cursor curFaturas = db.rawQuery(queryFaturas, new String[]{String.valueOf(idUsuarioLogado), mesAnoSelecionado})) {
                while (curFaturas.moveToNext()) {
                    double valorFatura = curFaturas.getDouble(0);
                    int statusFatura = curFaturas.getInt(1);

                    totalDespesas += valorFatura;
                    if (statusFatura == 0) { // Fatura não paga (aberta)
                        despesasPendentesValor += valorFatura;
                    }
                }
            }

        } catch (Exception e) {
            Log.e("TelaPrincipal", "Erro ao calcular valores", e);
        }

        // Parte 4: Atualizar a UI
        receitasMes.setText(formatarBR(totalReceitas));
        despesasMes.setText(formatarBR(totalDespesas));
        receitasMes.setTextColor(Color.parseColor("#006400"));
        despesasMes.setTextColor(Color.parseColor("#E45757"));

        valorReceitasPendentes.setText(formatarBR(receitasPendentesValor));
        valorDespesasPendentes.setText(formatarBR(despesasPendentesValor));

        boolean mostrarReceitasPendentes = receitasPendentesValor > 0;
        boolean mostrarDespesasPendentes = despesasPendentesValor > 0;

        receitasPendentes.setVisibility(mostrarReceitasPendentes ? View.VISIBLE : View.GONE);
        despesasPendentes.setVisibility(mostrarDespesasPendentes ? View.VISIBLE : View.GONE);
        alertasPendentes.setVisibility(mostrarReceitasPendentes || mostrarDespesasPendentes ? View.VISIBLE : View.GONE);
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

    private boolean shouldOccurInMonth(String dataInicioStr, int repetirPeriodo, int anoSelecionado, int mesSelecionado) {
        if (dataInicioStr == null || dataInicioStr.length() < 7) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            Date dataInicio = sdf.parse(dataInicioStr.substring(0, 10));
            Calendar calInicio = Calendar.getInstance();
            calInicio.setTime(dataInicio);

            int anosDiff = anoSelecionado - calInicio.get(Calendar.YEAR);
            int mesesDiff = anosDiff * 12 + (mesSelecionado - calInicio.get(Calendar.MONTH));

            if (mesesDiff < 0) return false;
            if (repetirPeriodo < 1) repetirPeriodo = 1; // Prevenção de divisão por zero

            return mesesDiff % repetirPeriodo == 0;
        } catch (ParseException e) {
            Log.e("TelaPrincipal", "Erro em shouldOccurInMonth", e);
            return false;
        }
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

    private int getMesIndex(String nomeMes) {
        String[] nomes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        for (int i = 0; i < nomes.length; i++) {
            if (nomes[i].equalsIgnoreCase(nomeMes)) return i;
        }
        return 0;
    }

    private String formatarBR(double valor) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(valor);
    }

    private void showMonthYearPicker() {
        String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        int anoSelecionado = Integer.parseInt(txtAno.getText().toString());
        int mesIndex = getMesIndex(txtMes.getText().toString());

        MonthYearPickerDialogFragment dialogFragment = MonthYearPickerDialogFragment.getInstance(mesIndex, anoSelecionado);
        dialogFragment.setOnDateSetListener((year, monthOfYear) -> {
            txtMes.setText(nomesMes[monthOfYear]);
            txtAno.setText(String.valueOf(year));
            atualizarValoresTela();
        });
        dialogFragment.show(getSupportFragmentManager(), "MonthYearPickerDialog");
    }
}
