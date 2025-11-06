package com.example.app1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Color;

import com.example.app1.data.ContaDAO;
import com.example.app1.data.TransacoesCartaoDAO;
import com.example.app1.data.TransacoesDAO;
import com.example.app1.utils.MenuHelper;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;

import java.text.NumberFormat;
import java.text.ParseException;
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

public class TelaTransacoes extends AppCompatActivity implements TransacaoAdapter.OnTransacaoListener {

    private TextView txtMes, txtAno, txtNenhumaTransacao;
    private int idUsuarioLogado = -1;
    private String filtroTipo = null; // "receita" ou "despesa"
    private String filtroStatus = null; // "pago" ou "pendente"

    private TextView totalReceitas, totalPendente, totalPago;
    private RecyclerView recyclerTransacoes;
    private TransacaoAdapter adapter;
    private final List<Object> displayList = new ArrayList<>();

    public static class TransacaoItem implements Comparable<TransacaoItem> {
        int id, recorrente, parcelas, numeroParcela, pago, recebido, idMestre, idCartao, idConta;
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
            this.idCartao = cur.getColumnIndex("id_cartao") != -1 ? cur.getInt(cur.getColumnIndexOrThrow("id_cartao")) : -1;
            this.idConta = cur.getColumnIndex("id_conta") != -1 ? cur.getInt(cur.getColumnIndexOrThrow("id_conta")) : -1;
        }

        public TransacaoItem(int id, String descricao, double valor, String data, String categoriaNome, String categoriaCor, String tipo, int recorrente, int idMestre, int repetir_periodo, int idConta) {
            this.id = id;
            this.descricao = descricao;
            this.valor = valor;
            this.data = data;
            this.categoriaNome = categoriaNome;
            this.categoriaCor = categoriaCor;
            this.tipoTransacao = tipo;
            this.recorrente = recorrente;
            this.parcelas = 1;
            this.numeroParcela = 1;
            this.idMestre = idMestre;
            this.isProjecao = true;
            this.pago = 0;
            this.recebido = 0;
            this.idCartao = -1;
            this.idConta = idConta;
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        idUsuarioLogado = getIntent().getIntExtra("id_usuario", -1);
        filtroTipo = getIntent().getStringExtra("filtro_tipo");
        filtroStatus = getIntent().getStringExtra("filtro_status");

        getSupportFragmentManager().setFragmentResultListener("despesaSalvaRequest", this, (requestKey, bundle) -> {
            if (bundle.getBoolean("atualizar")) {
                carregarTransacoesAsync();
            }
        });

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
        int ano = Integer.parseInt(txtAno.getText().toString());
        int mes = getMesIndex(txtMes.getText().toString());
        String mesAno = String.format(Locale.ROOT, "%04d-%02d", ano, mes + 1);
        
        try (SQLiteDatabase db = new MeuDbHelper(this).getReadableDatabase()) {

            if (!"cartao".equals(filtroTipo)) {
                ArrayList<String> argsContas = new ArrayList<>();
                argsContas.add(String.valueOf(idUsuarioLogado));
                argsContas.add(mesAno);

                StringBuilder queryContasBuilder = new StringBuilder("SELECT t.id, t.descricao, t.valor, t.data_movimentacao AS data, c.nome AS categoria_nome, c.cor AS categoria_cor, (CASE t.tipo WHEN 1 THEN 'receita' ELSE 'despesa' END) as tipo, t.recorrente, t.repetir_qtd AS parcelas, 1 AS numero_parcela, t.pago, t.recebido, t.id_mestre, t.repetir_periodo, t.id_conta, -1 as id_cartao FROM transacoes t LEFT JOIN categorias c ON t.id_categoria = c.id WHERE t.id_usuario = ? AND substr(t.data_movimentacao,1,7) = ?");

                if (filtroTipo != null) {
                    if ("receita".equals(filtroTipo)) {
                        queryContasBuilder.append(" AND t.tipo = 1");
                    } else if ("despesa".equals(filtroTipo)) {
                        queryContasBuilder.append(" AND t.tipo = 2");
                    }
                }

                if (filtroStatus != null) {
                    if ("pendente".equals(filtroStatus)) {
                        queryContasBuilder.append(" AND t.pago = 0");
                    } else if ("pago".equals(filtroStatus)) {
                        queryContasBuilder.append(" AND t.pago = 1");
                    }
                }

                try(Cursor cur = db.rawQuery(queryContasBuilder.toString(), argsContas.toArray(new String[0]))) {
                    while (cur.moveToNext()) {
                        itens.add(new TransacaoItem(cur, cur.getString(cur.getColumnIndexOrThrow("tipo")), false, null));
                    }
                }
            }

            if (!"receita".equals(filtroTipo)) {
                 ArrayList<String> argsFaturas = new ArrayList<>();
                argsFaturas.add(String.valueOf(idUsuarioLogado));
                argsFaturas.add(mesAno);

                StringBuilder queryFaturasBuilder = new StringBuilder(
                    "SELECT f.id, 'Fatura ' || cr.nome AS descricao, f.valor_total AS valor, f.data_vencimento AS data, 'Fatura de Cartão' AS categoria_nome, cr.cor AS categoria_cor, 'fatura_despesa' as tipo, 0 as recorrente, 1 AS parcelas, 1 AS numero_parcela, f.status as pago, 0 as recebido, f.id as id_mestre, 0 as repetir_periodo, cr.id as id_cartao, cr.id_conta " +
                    "FROM faturas f JOIN cartoes cr ON f.id_cartao = cr.id " +
                    "WHERE cr.id_usuario = ? ");

                if ("pendente".equals(filtroStatus)) {
                    queryFaturasBuilder.append(" AND substr(f.data_vencimento, 1, 7) <= ? AND f.status = 0");
                } else {
                     queryFaturasBuilder.append(" AND substr(f.data_vencimento, 1, 7) = ?");
                     if("pago".equals(filtroStatus)){
                        queryFaturasBuilder.append(" AND f.status = 1");
                     }
                }

                try (Cursor cur = db.rawQuery(queryFaturasBuilder.toString(), argsFaturas.toArray(new String[0]))) {
                    while (cur.moveToNext()) {
                        itens.add(new TransacaoItem(cur, "fatura_despesa", false, null));
                    }
                }
            }

            String queryProjecao = "SELECT mestre.id, mestre.descricao, mestre.valor, mestre.data_movimentacao, c.nome as categoria_nome, c.cor as categoria_cor, (CASE mestre.tipo WHEN 1 THEN 'receita' ELSE 'despesa' END) as tipo, mestre.recorrente, mestre.id as id_mestre, mestre.repetir_periodo, mestre.id_conta FROM transacoes AS mestre LEFT JOIN categorias c ON mestre.id_categoria = c.id WHERE mestre.id_usuario = ? AND mestre.recorrente = 1 AND mestre.recorrente_ativo = 1 AND mestre.id_mestre IS NULL AND substr(mestre.data_movimentacao, 1, 7) <= ? AND NOT EXISTS (SELECT 1 FROM transacoes AS filha WHERE filha.id_mestre = mestre.id AND substr(filha.data_movimentacao, 1, 7) = ?)";
            try (Cursor curMestre = db.rawQuery(queryProjecao, new String[]{String.valueOf(idUsuarioLogado), mesAno, mesAno})) {
                while (curMestre.moveToNext()) {
                    if (shouldOccurInMonth(curMestre.getString(curMestre.getColumnIndexOrThrow("data_movimentacao")), curMestre.getInt(curMestre.getColumnIndexOrThrow("repetir_periodo")), ano, mes)) {
                        
                        String tipoTransacao = curMestre.getString(curMestre.getColumnIndexOrThrow("tipo"));
                        boolean tipoCorresponde = (filtroTipo == null) || (filtroTipo.equals(tipoTransacao));
                        boolean statusCorresponde = (filtroStatus == null) || "pendente".equals(filtroStatus);

                        if(tipoCorresponde && statusCorresponde){
                            itens.add(new TransacaoItem(
                                curMestre.getInt(curMestre.getColumnIndexOrThrow("id")),
                                curMestre.getString(curMestre.getColumnIndexOrThrow("descricao")),
                                curMestre.getDouble(curMestre.getColumnIndexOrThrow("valor")),
                                mesAno + "-01 00:00:00",
                                curMestre.getString(curMestre.getColumnIndexOrThrow("categoria_nome")),
                                curMestre.getString(curMestre.getColumnIndexOrThrow("categoria_cor")),
                                tipoTransacao,
                                1,
                                curMestre.getInt(curMestre.getColumnIndexOrThrow("id_mestre")),
                                curMestre.getInt(curMestre.getColumnIndexOrThrow("repetir_periodo")),
                                curMestre.getInt(curMestre.getColumnIndexOrThrow("id_conta"))
                            ));
                        }
                    }
                }
            }

        } catch (Exception e) {
            android.util.Log.e("TelaTransacoes", "Erro ao buscar transacoes", e);
        }

        Collections.sort(itens);
        return itens;
    }

    private boolean shouldOccurInMonth(String dataInicioStr, int repetirPeriodo, int anoSelecionado, int mesSelecionado) {
        if (dataInicioStr == null || dataInicioStr.length() < 7) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            Date dataInicio = sdf.parse(dataInicioStr.substring(0, 10));
            Calendar calInicio = Calendar.getInstance();
            calInicio.setTime(dataInicio);

            int anosDiff = anoSelecionado - calInicio.get(Calendar.YEAR);
            int mesesDiff = anosDiff * 12 + (mesSelecionado - (calInicio.get(Calendar.MONTH)));

            if (mesesDiff < 0) return false;
            if (repetirPeriodo <= 0) return true;

            return mesesDiff % repetirPeriodo == 0;
        } catch (ParseException e) {
            android.util.Log.e("TelaTransacoes", "Erro em shouldOccurInMonth", e);
            return false;
        }
    }

    private void processarEExibirTransacoes(List<TransacaoItem> transacoes) {
        displayList.clear();
        if (transacoes.isEmpty()) {
            txtNenhumaTransacao.setVisibility(View.VISIBLE);
            return;
        }

        List<TransacaoItem> receitas = new ArrayList<>();
        List<TransacaoItem> despesas = new ArrayList<>();
        double totalReceitasValor = 0, totalPendenteValor = 0, totalPagoValor = 0;

        for(TransacaoItem item : transacoes) {
            if("receita".equals(item.tipoTransacao)) {
                receitas.add(item);
                totalReceitasValor += item.valor;
                 if(item.recebido == 1) totalPagoValor += item.valor; else totalPendenteValor += item.valor;
            } else { // despesa ou fatura
                despesas.add(item);
                if(item.pago == 1) totalPagoValor += item.valor; else totalPendenteValor += item.valor;
            }
        }

        double totalDespesasValor = 0;
        for(TransacaoItem item : despesas) totalDespesasValor += item.valor;

        addSection(displayList, new TransacaoAdapter.HeaderData("Receitas", totalReceitasValor), receitas);
        addSection(displayList, new TransacaoAdapter.HeaderData("Despesas", totalDespesasValor), despesas);
        
        totalReceitas.setText(formatarBR(totalReceitasValor));
        totalPendente.setText(formatarBR(totalPendenteValor));
        totalPago.setText(formatarBR(totalPagoValor));

        totalReceitas.setTextColor(Color.parseColor("#006400"));
        totalPendente.setTextColor(Color.parseColor("#E45757"));
        totalPago.setTextColor(Color.parseColor("#339933"));
        
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
    public void onTransacaoClick(TransacaoItem item, View anchorView) {
        mostrarMenuTransacao(anchorView, item);
    }

    private void mostrarMenuTransacao(View view, TransacaoItem item) {
        ArrayList<MenuHelper.MenuItemData> menuItems = new ArrayList<>();

        if ("receita".equals(item.tipoTransacao) && item.recebido == 0) {
            menuItems.add(new MenuHelper.MenuItemData("Receber Receita", R.drawable.ic_check, () -> mostrarPopupPagamento(item)));
        } else if (("despesa".equals(item.tipoTransacao) || "fatura_despesa".equals(item.tipoTransacao)) && item.pago == 0) {
            menuItems.add(new MenuHelper.MenuItemData("Pagar Despesa", R.drawable.ic_check, () -> mostrarPopupPagamento(item)));
        }

        if ("fatura_despesa".equals(item.tipoTransacao)) {
            menuItems.add(new MenuHelper.MenuItemData("Ver Fatura", R.drawable.ic_credit_card, () -> verFatura(item)));
        } else {
            menuItems.add(new MenuHelper.MenuItemData("Editar", R.drawable.ic_edit, () -> editarTransacao(item)));
        }

        menuItems.add(new MenuHelper.MenuItemData("Excluir", R.drawable.ic_delete, () -> confirmarExclusao(item)));

        MenuHelper.showMenu(this, view, menuItems.toArray(new MenuHelper.MenuItemData[0]));
    }

    private void mostrarPopupPagamento(TransacaoItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        boolean isReceita = "receita".equals(item.tipoTransacao);
        builder.setTitle(isReceita ? "Receber Receita" : "Pagar Despesa");

        View view = getLayoutInflater().inflate(R.layout.dialog_pagamento, null);
        Spinner spinnerContas = view.findViewById(R.id.spinner_contas);
        builder.setView(view);
        
        List<Conta> contas = ContaDAO.getContas(this, idUsuarioLogado);
        ArrayAdapter<Conta> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, contas);
        spinnerContas.setAdapter(adapter);

        int selection = -1;
        for (int i = 0; i < contas.size(); i++) {
            if (contas.get(i).getId() == item.idConta) {
                selection = i;
                break;
            }
        }
        if (selection != -1) {
            spinnerContas.setSelection(selection);
        }

        builder.setPositiveButton(isReceita ? "Receber" : "Pagar", (dialog, which) -> {
            Conta contaSelecionada = (Conta) spinnerContas.getSelectedItem();
            boolean sucesso;

            if (item.isProjecao) {
                String mesAno = String.format(Locale.ROOT, "%04d-%02d", Integer.parseInt(txtAno.getText().toString()), getMesIndex(txtMes.getText().toString()) + 1);
                sucesso = TransacoesDAO.pagarTransacaoRecorrente(this, item.id, contaSelecionada.getId(), mesAno);
            } else if ("fatura_despesa".equals(item.tipoTransacao)) {
                sucesso = TransacoesDAO.pagarFatura(this, item.id, contaSelecionada.getId());
            } else {
                sucesso = TransacoesDAO.pagarDespesaReceberReceita(this, item.id, contaSelecionada.getId(), isReceita);
            }

            if (sucesso) {
                Toast.makeText(this, "Transação atualizada!", Toast.LENGTH_SHORT).show();
                carregarTransacoesAsync();
            } else {
                Toast.makeText(this, "Erro ao processar pagamento.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.create().show();
    }

    private void verFatura(TransacaoItem item) {
        Intent intent = new Intent(this, TelaFaturaCartao.class);
        intent.putExtra("id_cartao", item.idCartao);
        intent.putExtra("id_fatura", item.id);
        intent.putExtra("id_usuario", idUsuarioLogado);
        startActivity(intent);
    }
    
    private void editarTransacao(TransacaoItem item) {
        if ("cartao".equals(item.tipoTransacao)) {
           MenuCadDespesaCartaoFragment fragment = MenuCadDespesaCartaoFragment.newInstance(idUsuarioLogado, item.idCartao);
           fragment.editarTransacao(item.id);
           getSupportFragmentManager().beginTransaction()
                   .replace(R.id.containerFragment, fragment)
                   .addToBackStack(null)
                   .commit();
       } else {
           Toast.makeText(this, "Edição de despesas normais a ser implementada.", Toast.LENGTH_SHORT).show();
       }
   }

    private void confirmarExclusao(TransacaoItem item) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Excluir Transação")
                .setMessage("Tem certeza que deseja excluir esta transação?")
                .setPositiveButton("Sim", (dialog, which) -> excluirTransacao(item))
                .setNegativeButton("Não", null)
                .show();
    }

    private void excluirTransacao(TransacaoItem item) {
        boolean sucesso = false;
        if ("fatura_despesa".equals(item.tipoTransacao)){
            Toast.makeText(this, "Exclusão de faturas ainda não suportada.", Toast.LENGTH_SHORT).show();
            return;
        }
        else if ("cartao".equals(item.tipoTransacao)) {
            sucesso = TransacoesCartaoDAO.excluirTransacao(this, item.id) == 0;
        } else {
            sucesso = TransacoesDAO.excluirTransacao(this, item.id, item.tipoTransacao);
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
}
