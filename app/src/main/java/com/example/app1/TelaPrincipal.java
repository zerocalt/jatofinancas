package com.example.app1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.app1.data.CartaoDAO;
import com.example.app1.data.ContaDAO;
import com.example.app1.data.TransacoesDAO;
import com.example.app1.utils.MascaraMonetaria;
import com.example.app1.utils.PopupSaldoUtil;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelaPrincipal extends AppCompatActivity implements CartaoPrincipalAdapter.OnCartaoClickListener {
    private TextView txtMes, txtAno;
    private TextView saldoContas, receitasMes, despesasMes, valorReceitasPendentes, valorDespesasPendentes, txtTotalContas, txtTotalCartao;
    private LinearLayout btnResumoReceitas, btnResumoDespesas, receitasPendentes, despesasPendentes, alertasPendentes, totalContasLayout, layoutContas, layoutCartoes, totalCartaoLayout;
    private ProgressBar progressBar;
    private Group groupContent;
    private RecyclerView recyclerContas, recyclerCartao;
    private ContasAdapter contasAdapter;
    private CartaoPrincipalAdapter cartaoAdapter;
    private View dividerContas, dividerCartao;
    private int idUsuarioLogado = -1;

    private static class ResumoFinanceiro {
        double saldoTotalContas;
        double totalReceitas;
        double totalDespesas;
        double receitasPendentes;
        double despesasPendentes;
        List<Conta> contas;
        List<CartaoFatura> cartoes;
    }

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

        bindViews();
        setupRecyclerViews();
        setupClickListeners();

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

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarDadosAsync();
    }

    private void bindViews() {
        txtMes = findViewById(R.id.txtMes);
        txtAno = findViewById(R.id.txtAno);
        saldoContas = findViewById(R.id.saldoContas);
        receitasMes = findViewById(R.id.receitasMes);
        despesasMes = findViewById(R.id.despesasMes);
        valorReceitasPendentes = findViewById(R.id.valorReceitasPendentes);
        valorDespesasPendentes = findViewById(R.id.valorDespesasPendentes);
        btnResumoReceitas = findViewById(R.id.btnResumoReceitas);
        btnResumoDespesas = findViewById(R.id.btnResumoDespesas);
        receitasPendentes = findViewById(R.id.receitasPendentes);
        despesasPendentes = findViewById(R.id.despesasPendentes);
        alertasPendentes = findViewById(R.id.alertasPendentes);
        progressBar = findViewById(R.id.progressBar);
        groupContent = findViewById(R.id.groupContent);
        layoutContas = findViewById(R.id.layoutContas);
        recyclerContas = findViewById(R.id.recyclerContas);
        txtTotalContas = findViewById(R.id.txtTotalContas);
        dividerContas = findViewById(R.id.dividerContas);
        totalContasLayout = findViewById(R.id.totalContasLayout);
        layoutCartoes = findViewById(R.id.layoutCartoes);
        recyclerCartao = findViewById(R.id.recyclerCartao);
        txtTotalCartao = findViewById(R.id.txtTotalCartao);
        dividerCartao = findViewById(R.id.dividerCartao);
        totalCartaoLayout = findViewById(R.id.totalCartaoLayout);
    }

    private void setupRecyclerViews() {
        recyclerContas.setLayoutManager(new LinearLayoutManager(this));
        contasAdapter = new ContasAdapter(this, new ArrayList<>(), new ContasAdapter.OnContaClickListener() {
            @Override
            public void onEditarSaldo(Conta conta) {
                PopupSaldoUtil.mostrarPopupEditarSaldo(TelaPrincipal.this, conta, new PopupSaldoUtil.OnSaldoAtualizadoListener() {
                    @Override
                    public void onSaldoAtualizado() {
                        carregarDadosAsync(); // ou o método que atualiza a lista na TelaConta
                    }
                });
            }

            @Override
            public void onExibirTransacoes(Conta conta) {
                Intent intent = new Intent(this, TelaTransacoes.class);
                intent.putExtra("id_conta", conta.getId()); // ou outro identificador
                this.startActivity(intent);
            }

        });
        recyclerContas.setAdapter(contasAdapter);

        recyclerCartao.setLayoutManager(new LinearLayoutManager(this));
        cartaoAdapter = new CartaoPrincipalAdapter(this, new ArrayList<>(), this);
        recyclerCartao.setAdapter(cartaoAdapter);
    }
    
    private void setupClickListeners() {
        btnResumoReceitas.setOnClickListener(v -> abrirTransacoesComFiltro("receita", null));
        btnResumoDespesas.setOnClickListener(v -> abrirTransacoesComFiltro("despesa", null));
        receitasPendentes.setOnClickListener(v -> abrirTransacoesComFiltro("receita", "pendente"));
        despesasPendentes.setOnClickListener(v -> abrirTransacoesComFiltro("despesa", "pendente"));
    }

    @Override
    public void onCartaoClick(int idCartao) {
        Intent intent = new Intent(this, TelaFaturaCartao.class);
        intent.putExtra("id_cartao", idCartao);
        intent.putExtra("id_usuario", idUsuarioLogado);
        startActivity(intent);
    }

    private void abrirTransacoesComFiltro(String tipo, String status) {
        Intent intent = new Intent(this, TelaTransacoes.class);
        intent.putExtra("id_usuario", idUsuarioLogado);
        if (tipo != null) {
            intent.putExtra("filtro_tipo", tipo);
        }
        if (status != null) {
            intent.putExtra("filtro_status", status);
        }
        startActivity(intent);
    }

    public void carregarDadosAsync() {
        progressBar.setVisibility(View.VISIBLE);
        groupContent.setVisibility(View.GONE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            ResumoFinanceiro resumo = calcularResumoFinanceiro();
            handler.post(() -> {
                atualizarUIComResumo(resumo);
                progressBar.setVisibility(View.GONE);
                groupContent.setVisibility(View.VISIBLE);
            });
        });
    }

    private ResumoFinanceiro calcularResumoFinanceiro() {
        ResumoFinanceiro resumo = new ResumoFinanceiro();

        int ano = Integer.parseInt(txtAno.getText().toString());
        int mes = getMesIndex(txtMes.getText().toString()) + 1;

        // Contas e cartões
        resumo.contas = ContaDAO.getContasTelaInicial(this, idUsuarioLogado);

        // Calcula saldo previsto em cada conta!
        for (Conta conta : resumo.contas) {
            double saldoPrevisto = ContaDAO.getSaldoPrevistoConta(this, idUsuarioLogado, conta.getId());
            conta.setSaldoPrevisto(saldoPrevisto);
        }

        resumo.cartoes = CartaoDAO.getCartoesComFatura(this, idUsuarioLogado, ano, mes - 1);

        // Saldo total das contas
        resumo.saldoTotalContas = ContaDAO.getSaldoTotalContas(this, idUsuarioLogado);

        // 1️⃣ Receitas
        resumo.totalReceitas = TransacoesDAO.getTotalReceitasMes(this, idUsuarioLogado, ano, mes);
        resumo.receitasPendentes = TransacoesDAO.getReceitasPendentes(this, idUsuarioLogado, ano, mes);

        // 2️⃣ Despesas
        resumo.totalDespesas = TransacoesDAO.getTotalDespesasMes(this, idUsuarioLogado, ano, mes);
        resumo.despesasPendentes = TransacoesDAO.getDespesasPendentes(this, idUsuarioLogado, ano, mes);

        return resumo;
    }

    private void atualizarUIComResumo(ResumoFinanceiro resumo) {
        saldoContas.setText(formatarBR(resumo.saldoTotalContas));
        receitasMes.setText(formatarBR(resumo.totalReceitas));
        despesasMes.setText(formatarBR(resumo.totalDespesas));
        receitasMes.setTextColor(Color.parseColor("#006400"));
        despesasMes.setTextColor(Color.parseColor("#E45757"));

        valorReceitasPendentes.setText(formatarBR(resumo.receitasPendentes));
        valorDespesasPendentes.setText(formatarBR(resumo.despesasPendentes));

        boolean mostrarReceitasPendentes = resumo.receitasPendentes > 0;
        boolean mostrarDespesasPendentes = resumo.despesasPendentes > 0;

        receitasPendentes.setVisibility(mostrarReceitasPendentes ? View.VISIBLE : View.GONE);
        despesasPendentes.setVisibility(mostrarDespesasPendentes ? View.VISIBLE : View.GONE);
        alertasPendentes.setVisibility(mostrarReceitasPendentes || mostrarDespesasPendentes ? View.VISIBLE : View.GONE);

        if (resumo.contas != null && !resumo.contas.isEmpty()) {
            layoutContas.setVisibility(View.VISIBLE);
            contasAdapter.setContas(resumo.contas);

            double total = 0;
            for (Conta conta : resumo.contas) {
                total += conta.getSaldo();
            }
            txtTotalContas.setText(formatarBR(total));
            txtTotalContas.setTextColor(Color.WHITE);

            dividerContas.setVisibility(View.VISIBLE);
            totalContasLayout.setVisibility(View.VISIBLE);
        } else {
            layoutContas.setVisibility(View.GONE);
        }

        if (resumo.cartoes != null && !resumo.cartoes.isEmpty()) {
            layoutCartoes.setVisibility(View.VISIBLE);
            cartaoAdapter.setCartoes(resumo.cartoes);

            double total = 0;
            for (CartaoFatura cf : resumo.cartoes) {
                total += cf.getValorFatura();
            }
            txtTotalCartao.setText(formatarBR(total));

            dividerCartao.setVisibility(View.VISIBLE);
            totalCartaoLayout.setVisibility(View.VISIBLE);
        } else {
            layoutCartoes.setVisibility(View.GONE);
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
            carregarDadosAsync();
        });
        dialogFragment.show(getSupportFragmentManager(), "MonthYearPickerDialog");
    }

}
