package com.example.app1;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Color;

import com.example.app1.data.TransacoesCartaoDAO;
import com.example.app1.data.TransacoesDAO;
import com.example.app1.utils.MenuHelper;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class TelaTransacoes extends AppCompatActivity implements MenuCadDespesaCartaoFragment.OnDespesaSalvaListener, TransacaoAdapter.OnTransacaoListener {

    private TextView txtMes, txtAno, txtNenhumaTransacao;
    private int idUsuarioLogado = -1;

    private TextView totalReceitas, totalPendente, totalPago;
    private RecyclerView recyclerTransacoes;
    private TransacaoAdapter adapter;
    private final List<Object> displayList = new ArrayList<>();

    public static class TransacaoItem implements Comparable<TransacaoItem> {
        int id, recorrente, parcelas, numeroParcela, pago, recebido, idMestre;
        String descricao, data, categoriaNome, categoriaCor, tipoTransacao;
        double valor;
        boolean isProjecao;

        public TransacaoItem(@NonNull Cursor cur, String tipo, boolean isProjecao, String dataOverride) {
            this.id = cur.getInt(cur.getColumnIndexOrThrow("id"));
            this.descricao = cur.getString(cur.getColumnIndexOrThrow("descricao"));
            this.valor = cur.getDouble(cur.getColumnIndexOrThrow("valor"));
            this.data = (dataOverride != null) ? dataOverride : cur.getString(cur.getColumnIndexOrThrow("data"));
            this.categoriaNome = cur.getString(cur.getColumnIndexOrThrow("categoria_nome"));
            this.categoriaCor = cur.getString(cur.getColumnIndexOrThrow("categoria_cor"));
            this.tipoTransacao = tipo;
            this.recorrente = cur.getInt(cur.getColumnIndexOrThrow("recorrente"));
            this.parcelas = cur.getInt(cur.getColumnIndexOrThrow("parcelas"));
            this.numeroParcela = cur.getInt(cur.getColumnIndexOrThrow("numero_parcela"));
            this.idMestre = cur.getInt(cur.getColumnIndexOrThrow("id_mestre"));
            this.isProjecao = isProjecao;
            this.pago = isProjecao ? 0 : cur.getInt(cur.getColumnIndexOrThrow("pago"));
            this.recebido = isProjecao ? 0 : cur.getInt(cur.getColumnIndexOrThrow("recebido"));
        }

        @Override
        public int compareTo(TransacaoItem other) {
            return other.data.compareTo(this.data);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_transacoes);

        idUsuarioLogado = getIntent().getIntExtra("id_usuario", -1);

        bindViews();
        setupRecyclerView();
        setupListeners();

        Calendar agora = Calendar.getInstance();
        String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        txtMes.setText(nomesMes[agora.get(Calendar.MONTH)]);
        txtAno.setText(String.valueOf(agora.get(Calendar.YEAR)));

        Bundle args = new Bundle();
        args.putInt("botaoInativo", BottomMenuFragment.TRANSACOES);
        getSupportFragmentManager().beginTransaction().replace(R.id.menu_container, BottomMenuFragment.class, args).commit();

        carregarTransacoesAsync();
    }
    
    private void bindViews() {
        txtMes = findViewById(R.id.txtMes);
        txtAno = findViewById(R.id.txtAno);
        txtNenhumaTransacao = findViewById(R.id.txt_nenhuma_transacao);
        totalReceitas = findViewById(R.id.totalReceitas);
        totalPendente = findViewById(R.id.totalPendente);
        totalPago = findViewById(R.id.totalPago);
        recyclerTransacoes = findViewById(R.id.recycler_transacoes);
    }

    private void setupRecyclerView() {
        recyclerTransacoes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransacaoAdapter(this, displayList, this);
        recyclerTransacoes.setAdapter(adapter);
    }

    private void setupListeners() {
        findViewById(R.id.btnMesAno).setOnClickListener(v -> showMonthYearPicker());
    }

    private void showMonthYearPicker() {
        final String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        int mesIndex = getMesIndex(txtMes.getText().toString());
        int anoSelecionado = Integer.parseInt(txtAno.getText().toString());

        MonthYearPickerDialogFragment dialogFragment = MonthYearPickerDialogFragment.getInstance(mesIndex, anoSelecionado);
        dialogFragment.setOnDateSetListener((year, monthOfYear) -> {
            txtMes.setText(nomesMes[monthOfYear]);
            txtAno.setText(String.valueOf(year));
            carregarTransacoesAsync();
        });
        dialogFragment.show(getSupportFragmentManager(), "MonthYearPickerDialog");
    }

    public void carregarTransacoesAsync() {
        txtNenhumaTransacao.setVisibility(View.GONE);
        recyclerTransacoes.setVisibility(View.GONE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<TransacaoItem> transacoes = buscarTransacoesDoMes();
            handler.post(() -> processarEExibirTransacoes(transacoes));
        });
    }

    private List<TransacaoItem> buscarTransacoesDoMes() {
        List<TransacaoItem> itens = new ArrayList<>();
        String mesAno = String.format(Locale.ROOT, "%04d-%02d", Integer.parseInt(txtAno.getText().toString()), getMesIndex(txtMes.getText().toString()) + 1);

        try (SQLiteDatabase db = new MeuDbHelper(this).getReadableDatabase()) {
            String queryContas = "SELECT t.id, t.descricao, t.valor, t.data_movimentacao AS data, c.nome AS categoria_nome, c.cor AS categoria_cor, (CASE t.tipo WHEN 1 THEN 'receita' ELSE 'despesa' END) as tipo, t.recorrente, t.repetir_qtd AS parcelas, 1 AS numero_parcela, t.pago, t.recebido, t.id_mestre, t.repetir_periodo FROM transacoes t LEFT JOIN categorias c ON t.id_categoria = c.id WHERE t.id_usuario = ? AND substr(t.data_movimentacao,1,7) = ?";
            try(Cursor cur = db.rawQuery(queryContas, new String[]{String.valueOf(idUsuarioLogado), mesAno})) {
                while (cur.moveToNext()) {
                    String tipo = cur.getString(cur.getColumnIndexOrThrow("tipo"));
                    itens.add(new TransacaoItem(cur, tipo, false, null));
                }
            }
            
            String queryCartao = "SELECT tc.id, tc.descricao, p.valor, p.data_vencimento as data, c.nome as categoria_nome, c.cor as categoria_cor, 'cartao' as tipo, tc.recorrente, tc.parcelas, p.numero_parcela, p.paga as pago, 0 as recebido, tc.id as id_mestre, 0 as repetir_periodo FROM parcelas_cartao p JOIN transacoes_cartao tc ON p.id_transacao_cartao = tc.id LEFT JOIN categorias c ON tc.id_categoria = c.id WHERE tc.id_usuario = ? AND substr(p.data_vencimento,1,7) = ?";
            try(Cursor cur = db.rawQuery(queryCartao, new String[]{String.valueOf(idUsuarioLogado), mesAno})) {
                while (cur.moveToNext()) {
                    itens.add(new TransacaoItem(cur, "cartao", false, null));
                }
            }
        } catch (Exception e) {
            Log.e("TelaTransacoes", "Erro ao buscar transacoes", e);
        }

        Collections.sort(itens);
        return itens;
    }

    private void processarEExibirTransacoes(List<TransacaoItem> transacoes) {
        displayList.clear();
        if (transacoes.isEmpty()) {
            txtNenhumaTransacao.setVisibility(View.VISIBLE);
            return;
        }

        List<TransacaoItem> receitas = new ArrayList<>();
        List<TransacaoItem> despesas = new ArrayList<>();
        List<TransacaoItem> cartao = new ArrayList<>();
        double totalReceitasValor = 0, totalPendenteValor = 0, totalPagoValor = 0;

        for(TransacaoItem item : transacoes) {
            if("receita".equals(item.tipoTransacao)) {
                receitas.add(item);
                totalReceitasValor += item.valor;
            } else if ("despesa".equals(item.tipoTransacao)) {
                despesas.add(item);
                if(item.pago == 1) totalPagoValor += item.valor; else totalPendenteValor += item.valor;
            } else {
                cartao.add(item);
                if(item.pago == 1) totalPagoValor += item.valor; else totalPendenteValor += item.valor;
            }
        }

        double totalDespesasValor = 0;
        for(TransacaoItem item : despesas) totalDespesasValor += item.valor;
        double totalCartaoValor = 0;
        for(TransacaoItem item : cartao) totalCartaoValor += item.valor;

        addSection(displayList, new TransacaoAdapter.HeaderData("Receitas", totalReceitasValor), receitas);
        addSection(displayList, new TransacaoAdapter.HeaderData("Despesas", totalDespesasValor), despesas);
        addSection(displayList, new TransacaoAdapter.HeaderData("Cartão de Crédito", totalCartaoValor), cartao);

        totalReceitas.setText(formatarBR(totalReceitasValor));
        totalPendente.setText(formatarBR(totalPendenteValor));
        totalPago.setText(formatarBR(totalPagoValor));

        totalReceitas.setTextColor(Color.parseColor("#006400"));
        totalPendente.setTextColor(Color.parseColor("#E45757"));
        totalPago.setTextColor(Color.parseColor("#E45757"));
        
        recyclerTransacoes.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    private void addSection(List<Object> list, TransacaoAdapter.HeaderData header, List<TransacaoItem> items) {
        if (!items.isEmpty()) {
            list.add(header);
            String ultimoDia = "";
            for (TransacaoItem item : items) {
                String dataFormatada = formatarDataBR(item.data, true);
                if (!dataFormatada.equals(ultimoDia)) {
                    list.add(new TransacaoAdapter.HeaderData(dataFormatada));
                    ultimoDia = dataFormatada;
                }
                list.add(item);
            }
        }
    }

    @Override
    public void onTransacaoClick(TransacaoItem item) {
        int idParaOperacao = item.idMestre > 0 ? item.idMestre : item.id;
        mostrarMenuTransacao(findViewById(android.R.id.content), idParaOperacao, item.tipoTransacao);
    }

    private void mostrarMenuTransacao(View view, int idTransacao, String tipo) {
        MenuHelper.MenuItemData[] menuItems = {
                new MenuHelper.MenuItemData("Editar", R.drawable.ic_edit, () -> editarTransacao(idTransacao, tipo)),
                new MenuHelper.MenuItemData("Excluir", R.drawable.ic_delete, () -> confirmarExclusao(idTransacao, tipo))
        };
        MenuHelper.showMenu(this, view, menuItems);
    }
    
    private void editarTransacao(int idTransacao, String tipo) {
         if ("cartao".equals(tipo)) {
            MenuCadDespesaCartaoFragment fragment = MenuCadDespesaCartaoFragment.newInstance(idUsuarioLogado, -1);
            fragment.setOnDespesaSalvaListener(this::carregarTransacoesAsync);
            fragment.editarTransacao(idTransacao);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerFragment, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            // Lógica para editar receita/despesa normal
        }
    }

    private void confirmarExclusao(int id, String tipo) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Excluir Transação")
                .setMessage("Tem certeza que deseja excluir esta transação?")
                .setPositiveButton("Sim", (dialog, which) -> excluirTransacao(id, tipo))
                .setNegativeButton("Não", null)
                .show();
    }

    private void excluirTransacao(int id, String tipo) {
        boolean sucesso;
        if ("cartao".equals(tipo)) {
            sucesso = TransacoesCartaoDAO.excluirTransacao(this, id) == 0;
        } else {
            sucesso = TransacoesDAO.excluirTransacao(this, id, tipo);
        }

        if (sucesso) {
            Toast.makeText(this, "Transação removida com sucesso!", Toast.LENGTH_SHORT).show();
            carregarTransacoesAsync();
        } else {
            Toast.makeText(this, "Erro ao remover transação.", Toast.LENGTH_SHORT).show();
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

    private String formatarDataBR(String dataISO, boolean comDiaDaSemana) {
        if (dataISO == null || dataISO.isEmpty()) return "Data inválida";
        String formatoSaida = comDiaDaSemana ? "EEEE, dd 'de' MMMM" : "dd/MM/yyyy";
        try {
            SimpleDateFormat sdfEntrada = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdfEntrada.parse(dataISO);
            return new SimpleDateFormat(formatoSaida, new Locale("pt", "BR")).format(date);
        } catch (Exception e) {
            try {
                 SimpleDateFormat sdfEntrada = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                 Date date = sdfEntrada.parse(dataISO);
                 return new SimpleDateFormat(formatoSaida, new Locale("pt", "BR")).format(date);
            } catch (Exception e2) {
                 return dataISO.length() > 10 ? dataISO.substring(0, 10) : dataISO;
            }
        }
    }

    @Override
    public void onDespesaSalva() {
        carregarTransacoesAsync();
    }
}
