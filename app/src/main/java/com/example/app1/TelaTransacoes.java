package com.example.app1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.app1.data.TransacoesCartaoDAO;
import com.example.app1.data.TransacoesDAO;
import com.example.app1.interfaces.BottomMenuListener;
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

public class TelaTransacoes extends AppCompatActivity implements BottomMenuListener, MenuCadDespesaCartaoFragment.OnDespesaSalvaListener {

    private TextView txtMes, txtAno, txtNenhumaTransacao;
    private LinearLayout blocoReceitas, blocoDespesas, blocoCartao;
    private LinearLayout secaoReceitas, secaoDespesas, secaoCartao;
    private int idUsuarioLogado = -1;

    private double totalReceitasValor = 0;
    private double totalPendenteValor = 0;
    private double totalPagoValor = 0;
    private TextView totalReceitas, totalPendente, totalPago;

    static class TransacaoItem implements Comparable<TransacaoItem> {
        int id;
        String descricao;
        double valor;
        String data; // yyyy-MM-dd HH:mm:ss
        String categoriaNome;
        String categoriaCor;
        String tipoTransacao;
        int recorrente;
        int parcelas;
        int numeroParcela;
        int pago;
        int recebido;
        boolean isProjecao;
        int idMestre; // Adicionado para lógica

        public TransacaoItem(@NonNull Cursor cur, boolean isProjecao, String dataOverride) {
            this.id = cur.getInt(cur.getColumnIndexOrThrow("id"));
            this.descricao = cur.getString(cur.getColumnIndexOrThrow("descricao"));
            this.valor = cur.getDouble(cur.getColumnIndexOrThrow("valor"));
            this.data = (dataOverride != null) ? dataOverride : cur.getString(cur.getColumnIndexOrThrow("data"));
            this.categoriaNome = cur.getString(cur.getColumnIndexOrThrow("categoria_nome"));
            this.categoriaCor = cur.getString(cur.getColumnIndexOrThrow("categoria_cor"));
            this.tipoTransacao = cur.getString(cur.getColumnIndexOrThrow("tipo"));
            this.recorrente = cur.getInt(cur.getColumnIndexOrThrow("recorrente"));
            this.parcelas = cur.getInt(cur.getColumnIndexOrThrow("parcelas"));
            this.numeroParcela = cur.getInt(cur.getColumnIndexOrThrow("numero_parcela"));
            this.idMestre = cur.getInt(cur.getColumnIndexOrThrow("id_mestre"));
            this.isProjecao = isProjecao;

            if (isProjecao) {
                this.pago = 0;
                this.recebido = 0;
            } else {
                this.pago = cur.getInt(cur.getColumnIndexOrThrow("pago"));
                this.recebido = cur.getInt(cur.getColumnIndexOrThrow("recebido"));
            }
        }

        @Override
        public int compareTo(TransacaoItem other) {
            return other.data.compareTo(this.data); // Ordenação descendente
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tela_transacoes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        idUsuarioLogado = getIntent().getIntExtra("id_usuario", -1);

        txtMes = findViewById(R.id.txtMes);
        txtAno = findViewById(R.id.txtAno);
        blocoReceitas = findViewById(R.id.blocoReceitas);
        blocoDespesas = findViewById(R.id.blocoDespesas);
        blocoCartao = findViewById(R.id.blocoCartao);
        secaoReceitas = findViewById(R.id.secao_receitas);
        secaoDespesas = findViewById(R.id.secao_despesas);
        secaoCartao = findViewById(R.id.secao_cartao);
        txtNenhumaTransacao = findViewById(R.id.txt_nenhuma_transacao);

        totalReceitas = findViewById(R.id.totalReceitas);
        totalPendente = findViewById(R.id.totalPendente);
        totalPago = findViewById(R.id.totalPago);

        Calendar agora = Calendar.getInstance();
        String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        txtMes.setText(nomesMes[agora.get(Calendar.MONTH)]);
        txtAno.setText(String.valueOf(agora.get(Calendar.YEAR)));

        LinearLayout btnMesAno = findViewById(R.id.btnMesAno);
        btnMesAno.setOnClickListener(v -> showMonthYearPicker());

        Bundle args = new Bundle();
        args.putInt("botaoInativo", BottomMenuFragment.TRANSACOES);
        BottomMenuFragment fragment = new BottomMenuFragment();
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction().replace(R.id.menu_container, fragment).commit();

        carregarTransacoes();
    }

    private void showMonthYearPicker() {
        final String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        String mesSelecionado = txtMes.getText().toString();
        int anoSelecionado = Integer.parseInt(txtAno.getText().toString());
        int mesIndex = 0;
        for (int i = 0; i < nomesMes.length; i++) {
            if (nomesMes[i].equalsIgnoreCase(mesSelecionado)) {
                mesIndex = i;
                break;
            }
        }

        MonthYearPickerDialogFragment dialogFragment = MonthYearPickerDialogFragment.getInstance(mesIndex, anoSelecionado);
        dialogFragment.setOnDateSetListener((year, monthOfYear) -> {
            txtMes.setText(nomesMes[monthOfYear]);
            txtAno.setText(String.valueOf(year));
            carregarTransacoes();
        });
        dialogFragment.show(getSupportFragmentManager(), "MonthYearPickerDialog");
    }

    public void carregarTransacoes() {
        blocoReceitas.removeAllViews();
        blocoDespesas.removeAllViews();
        blocoCartao.removeAllViews();

        totalReceitasValor = 0;
        totalPendenteValor = 0;
        totalPagoValor = 0;

        boolean hasReceitas = carregarSecaoContas("receita");
        boolean hasDespesas = carregarSecaoContas("despesa");
        boolean hasCartao = carregarSecaoCartao();

        totalReceitas.setText(formatarBR(totalReceitasValor));
        totalPendente.setText(formatarBR(totalPendenteValor));
        totalPago.setText(formatarBR(totalPagoValor));

        totalReceitas.setTextColor(Color.parseColor("#006400"));
        totalPendente.setTextColor(Color.parseColor("#E45757"));
        totalPago.setTextColor(Color.parseColor("#E45757"));

        secaoReceitas.setVisibility(hasReceitas ? View.VISIBLE : View.GONE);
        secaoDespesas.setVisibility(hasDespesas ? View.VISIBLE : View.GONE);
        secaoCartao.setVisibility(hasCartao ? View.VISIBLE : View.GONE);

        if (!hasReceitas && !hasDespesas && !hasCartao) {
            txtNenhumaTransacao.setVisibility(View.VISIBLE);
        } else {
            txtNenhumaTransacao.setVisibility(View.GONE);
        }
    }

    private boolean carregarSecaoContas(String tipoSecao) {
        int tipo = tipoSecao.equals("receita") ? 1 : 2;
        List<TransacaoItem> itens = new ArrayList<>();

        try (MeuDbHelper dbHelper = new MeuDbHelper(this);
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {

            String mesStr = txtMes.getText().toString();
            int ano = Integer.parseInt(txtAno.getText().toString());
            int mes = getMesIndex(mesStr);
            String mesAnoSelecionado = String.format(Locale.ROOT, "%04d-%02d", ano, mes + 1);

            // ================================
            // 1️⃣ TRANSACOES REAIS (existentes)
            // ================================
            String queryExistentes =
                    "SELECT t.id, t.descricao, t.valor, t.data_movimentacao AS data, " +
                            "c.nome AS categoria_nome, c.cor AS categoria_cor, " +
                            "'transacao' AS tipo, t.recorrente, t.repetir_qtd AS parcelas, " +
                            "1 AS numero_parcela, t.pago, t.recebido, t.id_mestre, t.repetir_periodo " +
                            "FROM transacoes t " +
                            "LEFT JOIN categorias c ON t.id_categoria = c.id " +
                            "WHERE t.id_usuario = ? AND t.tipo = ? " +
                            "AND substr(t.data_movimentacao,1,7) = ? " +
                            "AND (t.id_mestre IS NOT NULL OR t.recorrente = 0)";
            try (Cursor cur = db.rawQuery(queryExistentes,
                    new String[]{String.valueOf(idUsuarioLogado), String.valueOf(tipo), mesAnoSelecionado})) {
                while (cur.moveToNext()) {
                    itens.add(new TransacaoItem(cur, false, null));
                }
            }

            // ========================================
            // 2️⃣ PROJECAO DE FIXAS (sem filha no mês)
            // ========================================
            String queryProjecao =
                    "SELECT mestre.id, mestre.descricao, mestre.valor, mestre.data_movimentacao AS data, " +
                            "c.nome AS categoria_nome, c.cor AS categoria_cor, 'transacao' AS tipo, " +
                            "mestre.recorrente, mestre.repetir_qtd AS parcelas, mestre.repetir_periodo, " +
                            "mestre.pago, mestre.recebido, 1 AS numero_parcela, mestre.id_mestre " +
                            "FROM transacoes AS mestre " +
                            "LEFT JOIN categorias c ON mestre.id_categoria = c.id " +
                            "WHERE mestre.id_usuario = ? AND mestre.tipo = ? " +
                            "AND mestre.recorrente = 1 AND mestre.recorrente_ativo = 1 " +
                            "AND mestre.id_mestre IS NULL " + // só os mestres
                            "AND substr(mestre.data_movimentacao,1,7) <= ? " +
                            "AND NOT EXISTS ( " +
                            "   SELECT 1 FROM transacoes AS filha " +
                            "   WHERE filha.id_mestre = mestre.id " +
                            "   AND substr(filha.data_movimentacao,1,7) = ? " +
                            ")";

            try (Cursor curMestre = db.rawQuery(queryProjecao,
                    new String[]{String.valueOf(idUsuarioLogado), String.valueOf(tipo), mesAnoSelecionado, mesAnoSelecionado})) {

                while (curMestre.moveToNext()) {
                    String dataInicioStr = curMestre.getString(curMestre.getColumnIndexOrThrow("data"));
                    int repetirPeriodo = curMestre.getInt(curMestre.getColumnIndexOrThrow("repetir_periodo"));

                    if (shouldOccurInMonth(dataInicioStr, repetirPeriodo, ano, mes)) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
                            Calendar calProjecao = Calendar.getInstance();
                            calProjecao.setTime(sdf.parse(dataInicioStr.substring(0, 10)));
                            calProjecao.set(Calendar.YEAR, ano);
                            calProjecao.set(Calendar.MONTH, mes);
                            String dataProjecao = sdf.format(calProjecao.getTime()) + " 00:00:00";

                            itens.add(new TransacaoItem(curMestre, true, dataProjecao));
                        } catch (ParseException e) {
                            Log.e("TelaTransacoes", "Erro ao projetar data", e);
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e("TelaTransacoes", "Erro ao carregar seção de contas", e);
        }

        // =============================
        // 3️⃣ Monta o layout da tela
        // =============================
        Collections.sort(itens);

        if (!itens.isEmpty()) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout layoutDestino = tipoSecao.equals("receita") ? blocoReceitas : blocoDespesas;
            String ultimoDia = "";

            for (TransacaoItem item : itens) {
                if (tipoSecao.equals("receita")) {
                    totalReceitasValor += item.valor;
                } else {
                    if (item.pago == 1)
                        totalPagoValor += item.valor;
                    else
                        totalPendenteValor += item.valor;
                }

                String dataFormatada = formatarDataBR(item.data, true);
                if (!dataFormatada.equals(ultimoDia)) {
                    TextView dataLabel = new TextView(this);
                    dataLabel.setText(dataFormatada);
                    dataLabel.setTypeface(Typeface.DEFAULT_BOLD);
                    dataLabel.setTextColor(Color.GRAY);
                    dataLabel.setTextSize(13f);
                    dataLabel.setPadding(4, 16, 4, 8);
                    layoutDestino.addView(dataLabel);
                    ultimoDia = dataFormatada;
                }

                View itemView = createTransacaoView(item, inflater, tipoSecao);
                layoutDestino.addView(itemView);
            }
        }

        return !itens.isEmpty();
    }

    private boolean carregarSecaoCartao() {
        SQLiteDatabase db = null;
        Cursor cur = null;
        boolean hasContent = false;
        try {
            String mesStr = txtMes.getText().toString();
            int ano = Integer.parseInt(txtAno.getText().toString());
            int mes = getMesIndex(mesStr);

            Calendar inicioCal = Calendar.getInstance();
            inicioCal.set(ano, mes, 1, 0, 0, 0);
            Calendar fimCal = Calendar.getInstance();
            fimCal.set(ano, mes, inicioCal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
            String dataInicio = isoFormat.format(inicioCal.getTime());
            String dataFim = isoFormat.format(fimCal.getTime());

            db = new MeuDbHelper(this).getReadableDatabase();
            // Esta query é complexa e pode ser otimizada no futuro. Por agora, restauramos a funcionalidade.
            String query = "SELECT * FROM ( " +
                           "SELECT tc.id, tc.descricao, p.valor, p.data_vencimento as data, c.nome as categoria_nome, c.cor as categoria_cor, 'transacao_cartao' as tipo, tc.recorrente, tc.parcelas, p.numero_parcela, p.paga as pago, 0 as recebido, tc.id as id_mestre, 0 as repetir_periodo " +
                           "FROM parcelas_cartao p JOIN transacoes_cartao tc ON p.id_transacao_cartao = tc.id LEFT JOIN categorias c ON tc.id_categoria = c.id " +
                           "WHERE tc.id_usuario = ? AND p.data_vencimento BETWEEN ? AND ? " +
                           "UNION ALL " +
                           "SELECT tc.id, tc.descricao, tc.valor, tc.data_compra as data, c.nome as categoria_nome, c.cor as categoria_cor, 'transacao_cartao' as tipo, tc.recorrente, tc.parcelas, 1 as numero_parcela, 0 as pago, 0 as recebido, 0 as id_mestre, 0 as repetir_periodo " +
                           "FROM transacoes_cartao tc LEFT JOIN categorias c ON tc.id_categoria = c.id " +
                           "WHERE tc.id_usuario = ? AND (tc.parcelas = 1 OR tc.parcelas IS NULL) AND tc.recorrente = 0 AND tc.data_compra BETWEEN ? AND ? " +
                           ") ORDER BY data DESC";
            String[] args = {String.valueOf(idUsuarioLogado), dataInicio, dataFim, String.valueOf(idUsuarioLogado), dataInicio, dataFim};

            cur = db.rawQuery(query, args);
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (cur != null && cur.getCount() > 0) {
                hasContent = true;
                String ultimoDia = "";
                while (cur.moveToNext()) {
                    if (cur.getInt(cur.getColumnIndexOrThrow("pago")) == 1) {
                        totalPagoValor += cur.getDouble(cur.getColumnIndexOrThrow("valor"));
                    } else {
                        totalPendenteValor += cur.getDouble(cur.getColumnIndexOrThrow("valor"));
                    }

                    String dataRegistro = cur.getString(cur.getColumnIndexOrThrow("data"));
                    String dataFormatada = formatarDataBR(dataRegistro, true);

                    if (!dataFormatada.equals(ultimoDia)) {
                        TextView dataLabel = new TextView(this);
                        dataLabel.setText(dataFormatada);
                        dataLabel.setTypeface(Typeface.DEFAULT_BOLD);
                        dataLabel.setTextColor(Color.GRAY);
                        dataLabel.setTextSize(13f);
                        dataLabel.setPadding(4, 16, 4, 8);
                        blocoCartao.addView(dataLabel);
                        ultimoDia = dataFormatada;
                    }
                    View item = createTransacaoView(new TransacaoItem(cur, false, null), inflater, "cartao");
                    blocoCartao.addView(item);
                }
            }
        } catch (Exception e) {
            Log.e("TelaTransacoes", "Erro ao carregar seção cartao", e);
        } finally {
            if (cur != null) cur.close();
            if (db != null) db.close();
        }
        return hasContent;
    }


    private boolean shouldOccurInMonth(String dataInicioStr, int repetirPeriodo, int anoSelecionado, int mesSelecionado) {
        if (dataInicioStr == null || dataInicioStr.length() < 7) return false;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            Date dataInicio = sdf.parse(dataInicioStr.substring(0, 10));

            Calendar calInicio = Calendar.getInstance();
            calInicio.setTime(dataInicio);

            Calendar calSelecionado = Calendar.getInstance();
            calSelecionado.set(Calendar.YEAR, anoSelecionado);
            calSelecionado.set(Calendar.MONTH, mesSelecionado);
            calSelecionado.set(Calendar.DAY_OF_MONTH, 1);

            // Se o mês selecionado é antes da data inicial, não ocorre ainda
            if (calSelecionado.before(calInicio)) return false;

            // Calcula a diferença total em meses entre início e mês atual
            int anosDiff = anoSelecionado - calInicio.get(Calendar.YEAR);
            int mesesDiff = anosDiff * 12 + (mesSelecionado - calInicio.get(Calendar.MONTH));

            // período = 1 (mensal), 2 (bimestral), 3 (trimestral)...
            if (repetirPeriodo < 1) repetirPeriodo = 1;

            // Se a diferença é múltiplo do período, então deve ocorrer neste mês
            return mesesDiff % repetirPeriodo == 0;

        } catch (ParseException e) {
            Log.e("TelaTransacoes", "Erro em shouldOccurInMonth", e);
            return false;
        }
    }

    private View createTransacaoView(TransacaoItem transacao, LayoutInflater inflater, String tipoSecao) {
        View item = inflater.inflate(R.layout.item_transacao, null, false);

        ((TextView) item.findViewById(R.id.tituloTransacao)).setText(transacao.descricao);
        ((TextView) item.findViewById(R.id.tituloCategoria)).setText(transacao.categoriaNome != null ? transacao.categoriaNome : "Outros");
        
        TextView valorView = item.findViewById(R.id.valorTransacao);
        valorView.setText(formatarBR(transacao.valor));

        TextView inicialCat = item.findViewById(R.id.txtIconCategoria);
        inicialCat.setText(transacao.categoriaNome != null && !transacao.categoriaNome.isEmpty() ? transacao.categoriaNome.substring(0, 1) : "?");

        Drawable background = inicialCat.getBackground();
        if (background instanceof GradientDrawable) {
            GradientDrawable circle = (GradientDrawable) background.mutate();
            if (transacao.categoriaCor != null && !transacao.categoriaCor.isEmpty()) {
                try {
                    circle.setColor(Color.parseColor(transacao.categoriaCor));
                } catch (IllegalArgumentException e) { /* Cor inválida */ }
            }
        }

        if (tipoSecao.equals("receita")) {
            valorView.setTextColor(Color.parseColor("#006400"));
        } else {
             valorView.setTextColor(Color.parseColor("#E45757"));
        }

        ImageView statusIndicator = item.findViewById(R.id.statusIndicator);
        boolean isPaga = (tipoSecao.equals("receita") && transacao.recebido == 1) || (!tipoSecao.equals("receita") && transacao.pago == 1);
        ((GradientDrawable) statusIndicator.getDrawable()).setColor(isPaga ? Color.parseColor("#006400") : Color.parseColor("#E45757"));
        
        TextView labelTipo = item.findViewById(R.id.labelTipoTransacao);
        String tipoLabel = "";
        if (transacao.isProjecao) {
            tipoLabel = "(fixa)";
        } else if (transacao.recorrente == 1 && transacao.idMestre > 0) { // Parcela de uma recorrencia
            tipoLabel = "";
        } else if (transacao.parcelas > 1) {
            tipoLabel = "(" + transacao.numeroParcela + "/" + transacao.parcelas + ")";
        }

        if (!tipoLabel.isEmpty()) {
            labelTipo.setText(tipoLabel);
            labelTipo.setVisibility(View.VISIBLE);
        } else {
            labelTipo.setVisibility(View.GONE);
        }

        item.setOnClickListener(v -> {
            int idParaOperacao = transacao.id;
            // Se for filha de uma transação recorrente, a operação deve ser no mestre.
            if (transacao.idMestre > 0) {
                idParaOperacao = transacao.idMestre;
            }
            mostrarMenuTransacao(v, idParaOperacao, transacao.tipoTransacao);
        });

        return item;
    }

    private void mostrarMenuTransacao(View itemView, int idTransacao, String tipo) {
        MenuHelper.MenuItemData[] menuItems = {
                new MenuHelper.MenuItemData("Editar", R.drawable.ic_edit, () -> {
                    if ("transacao_cartao".equals(tipo)) {
                        MenuCadDespesaCartaoFragment fragment = MenuCadDespesaCartaoFragment.newInstance(idUsuarioLogado, -1);
                        fragment.setOnDespesaSalvaListener(this);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.containerFragment, fragment)
                                .addToBackStack(null)
                                .commit();
                        fragment.editarTransacao(idTransacao);
                    } else {
                        String tipoMovimento = getTipoMovimento(idTransacao);
                        MenuCadDespesaFragment fragment = MenuCadDespesaFragment.newInstance(idUsuarioLogado, tipoMovimento);
                        fragment.setOnTransacaoSalvaListener(this::carregarTransacoes);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.containerFragment, fragment)
                                .addToBackStack(null)
                                .commit();
                        fragment.editarTransacao(idTransacao);
                    }
                }),
                new MenuHelper.MenuItemData("Excluir", R.drawable.ic_delete, () ->
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Excluir Transação")
                            .setMessage("Tem certeza que deseja excluir esta transação?")
                            .setPositiveButton("Sim", (dialog, which) -> excluirTransacao(idTransacao, tipo))
                            .setNegativeButton("Não", null)
                            .show()
                )
        };
        MenuHelper.showMenu(this, itemView, menuItems);
    }

    private void excluirTransacao(int id, String tipo) {
        if ("transacao_cartao".equals(tipo)) {
            int resultado = TransacoesCartaoDAO.excluirTransacao(this, id);
            switch (resultado) {
                case 0: // Sucesso
                    Toast.makeText(this, "Despesa do cartão removida com sucesso!", Toast.LENGTH_SHORT).show();
                    carregarTransacoes();
                    break;
                case 1: // Fatura Paga
                    Toast.makeText(this, "Não é possível remover. A despesa pertence a uma fatura já paga.", Toast.LENGTH_LONG).show();
                    break;
                case -1: // Erro
                default:
                    Toast.makeText(this, "Erro ao remover a despesa do cartão.", Toast.LENGTH_SHORT).show();
                    break;
            }
        } else {
            boolean sucesso = TransacoesDAO.excluirTransacao(this, id, tipo);
            if (sucesso) {
                Toast.makeText(this, "Transação removida com sucesso!", Toast.LENGTH_SHORT).show();
                carregarTransacoes();
            } else {
                Toast.makeText(this, "Erro ao remover transação.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int getMesIndex(String nomeMes) {
        String[] nomes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        for (int i = 0; i < nomes.length; i++) {
            if (nomes[i].equalsIgnoreCase(nomeMes)) return i;
        }
        return 0;
    }

    private String getTipoMovimento(int idTransacao) {
        try (SQLiteDatabase db = new MeuDbHelper(this).getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT tipo FROM transacoes WHERE id = ?", new String[]{String.valueOf(idTransacao)})) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndexOrThrow("tipo")) == 1 ? "receita" : "despesa";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "despesa";
    }

    private String formatarBR(double valor) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(valor);
    }

    private String formatarDataBR(String dataISO, boolean comDiaDaSemana) {
        if (dataISO == null || dataISO.isEmpty()) return "Data inválida";
        String formatoSaida = comDiaDaSemana ? "EEEE, dd 'de' MMMM" : "dd/MM/yyyy";
        SimpleDateFormat sdfSaida = new SimpleDateFormat(formatoSaida, new Locale("pt", "BR"));

        try {
            SimpleDateFormat sdfEntrada = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdfEntrada.parse(dataISO);
            return sdfSaida.format(date);
        } catch (Exception e) {
            try {
                SimpleDateFormat sdfEntrada = new SimpleDateFormat("yyyy-M-dd", Locale.getDefault());
                Date date = sdfEntrada.parse(dataISO);
                return sdfSaida.format(date);
            } catch (Exception e2) {
                return dataISO; // Retorna o valor original se ambos os parsings falharem
            }
        }
    }

    @Override
    public void onFabDespesaCartaoClick(int idUsuario) {}

    @Override
    public void onDespesaSalva() {
        carregarTransacoes();
    }
}
