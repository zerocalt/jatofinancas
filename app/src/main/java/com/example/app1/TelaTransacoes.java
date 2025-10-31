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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.app1.interfaces.BottomMenuListener;
import com.example.app1.utils.MenuHelper;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TelaTransacoes extends AppCompatActivity implements BottomMenuListener {

    private TextView txtMes, txtAno, txtNenhumaTransacao;
    private LinearLayout blocoReceitas, blocoDespesas, blocoCartao;
    private LinearLayout secaoReceitas, secaoDespesas, secaoCartao;
    private int idUsuarioLogado = -1;

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

        boolean hasReceitas = carregarSecao("receita");
        boolean hasDespesas = carregarSecao("despesa");
        boolean hasCartao = carregarSecao("cartao");

        secaoReceitas.setVisibility(hasReceitas ? View.VISIBLE : View.GONE);
        secaoDespesas.setVisibility(hasDespesas ? View.VISIBLE : View.GONE);
        secaoCartao.setVisibility(hasCartao ? View.VISIBLE : View.GONE);

        if (!hasReceitas && !hasDespesas && !hasCartao) {
            txtNenhumaTransacao.setVisibility(View.VISIBLE);
        } else {
            txtNenhumaTransacao.setVisibility(View.GONE);
        }
    }

    private boolean carregarSecao(String tipoSecao) {
        SQLiteDatabase db = null;
        Cursor cur = null;
        boolean hasContent = false;
        try {
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

            db = new MeuDbHelper(this).getReadableDatabase();
            String query;
            String[] args;

            LinearLayout layoutDestino;

            switch (tipoSecao) {
                case "receita":
                    query = "SELECT t.id, t.descricao, t.valor, t.data_movimentacao as data, c.nome as categoria_nome, c.cor as categoria_cor, 'transacao' as tipo, t.recorrente, t.repetir_qtd as parcelas, 1 as numero_parcela FROM transacoes t LEFT JOIN categorias c ON t.id_categoria = c.id WHERE t.id_usuario = ? AND t.tipo = 1 AND t.data_movimentacao BETWEEN ? AND ? AND (t.id_mestre IS NOT NULL OR t.recorrente = 0) ORDER BY t.data_movimentacao DESC";
                    args = new String[]{String.valueOf(idUsuarioLogado), dataInicio, dataFim};
                    layoutDestino = blocoReceitas;
                    break;
                case "despesa":
                    query = "SELECT t.id, t.descricao, t.valor, t.data_movimentacao as data, c.nome as categoria_nome, c.cor as categoria_cor, 'transacao' as tipo, t.recorrente, t.repetir_qtd as parcelas, 1 as numero_parcela FROM transacoes t LEFT JOIN categorias c ON t.id_categoria = c.id WHERE t.id_usuario = ? AND t.tipo = 2 AND t.data_movimentacao BETWEEN ? AND ? AND (t.id_mestre IS NOT NULL OR t.recorrente = 0) ORDER BY t.data_movimentacao DESC";
                    args = new String[]{String.valueOf(idUsuarioLogado), dataInicio, dataFim};
                    layoutDestino = blocoDespesas;
                    break;
                case "cartao":
                    query = "SELECT * FROM ( " +
                            "SELECT tc.id, tc.descricao, p.valor, p.data_vencimento as data, c.nome as categoria_nome, c.cor as categoria_cor, 'transacao_cartao' as tipo, tc.recorrente, tc.parcelas, p.numero_parcela " +
                            "FROM parcelas_cartao p JOIN transacoes_cartao tc ON p.id_transacao_cartao = tc.id LEFT JOIN categorias c ON tc.id_categoria = c.id " +
                            "WHERE tc.id_usuario = ? AND p.data_vencimento BETWEEN ? AND ? " +
                            "UNION ALL " +
                            "SELECT tc.id, tc.descricao, tc.valor, tc.data_compra as data, c.nome as categoria_nome, c.cor as categoria_cor, 'transacao_cartao' as tipo, tc.recorrente, tc.parcelas, 1 as numero_parcela " +
                            "FROM transacoes_cartao tc LEFT JOIN categorias c ON tc.id_categoria = c.id " +
                            "WHERE tc.id_usuario = ? AND (tc.parcelas = 1 OR tc.parcelas IS NULL) AND tc.recorrente = 0 AND tc.data_compra BETWEEN ? AND ? " +
                            "UNION ALL " +
                            "SELECT drc.id_transacao_cartao as id, tc.descricao, drc.valor, drc.data_inicial as data, c.nome as categoria_nome, c.cor as categoria_cor, 'transacao_cartao' as tipo, 1 as recorrente, 1 as parcelas, 1 as numero_parcela " + 
                            "FROM despesas_recorrentes_cartao drc JOIN transacoes_cartao tc ON drc.id_transacao_cartao = tc.id LEFT JOIN categorias c ON tc.id_categoria = c.id " +
                            "WHERE tc.id_usuario = ? AND drc.data_inicial <= ? AND (drc.data_final IS NULL OR drc.data_final >= ?) " +
                            ") ORDER BY data DESC";
                    args = new String[]{String.valueOf(idUsuarioLogado), dataInicio, dataFim, String.valueOf(idUsuarioLogado), dataInicio, dataFim, String.valueOf(idUsuarioLogado), dataFim, dataInicio};
                    layoutDestino = blocoCartao;
                    break;
                default:
                    return false;
            }

            cur = db.rawQuery(query, args);
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (cur != null && cur.getCount() > 0) {
                hasContent = true;
                String ultimoDia = "";
                while (cur.moveToNext()) {
                    String dataRegistro = cur.getString(cur.getColumnIndexOrThrow("data"));
                    String dataFormatada = formatarDataBR(dataRegistro, true);

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
                    View item = createTransacaoView(cur, inflater, tipoSecao);
                    layoutDestino.addView(item);
                }
            }
        } catch (Exception e) {
            Log.e("TelaTransacoes", "Erro ao carregar seção " + tipoSecao, e);
        } finally {
            if (cur != null) cur.close();
            if (db != null) db.close();
        }
        return hasContent;
    }

    private View createTransacaoView(Cursor cur, LayoutInflater inflater, String tipo) {
        View item = inflater.inflate(R.layout.item_transacao, null, false);
        
        double valor = cur.getDouble(cur.getColumnIndexOrThrow("valor"));
        String descricao = cur.getString(cur.getColumnIndexOrThrow("descricao"));
        int idTransacao = cur.getInt(cur.getColumnIndexOrThrow("id"));
        String categoria = cur.getString(cur.getColumnIndexOrThrow("categoria_nome"));
        String corCategoria = cur.getString(cur.getColumnIndexOrThrow("categoria_cor"));
        int recorrente = cur.getInt(cur.getColumnIndexOrThrow("recorrente"));
        int parcelas = cur.getInt(cur.getColumnIndexOrThrow("parcelas"));
        int numeroParcela = cur.getInt(cur.getColumnIndexOrThrow("numero_parcela"));

        ((TextView) item.findViewById(R.id.tituloTransacao)).setText(descricao);
        ((TextView) item.findViewById(R.id.tituloCategoria)).setText(categoria != null ? categoria : "Outros");
        
        TextView valorView = item.findViewById(R.id.valorTransacao);
        valorView.setText(formatarBR(valor));

        TextView inicialCat = item.findViewById(R.id.txtIconCategoria);
        inicialCat.setText(categoria != null && !categoria.isEmpty() ? categoria.substring(0, 1) : "?");

        Drawable background = inicialCat.getBackground();
        if (background instanceof GradientDrawable) {
            GradientDrawable circle = (GradientDrawable) background.mutate();
            if (corCategoria != null && !corCategoria.isEmpty()) {
                try {
                    circle.setColor(Color.parseColor(corCategoria));
                } catch (IllegalArgumentException e) { /* Cor inválida, usa padrão */ }
            }
        }

        if (tipo.equals("receita")) {
            valorView.setTextColor(Color.parseColor("#008000"));
        } else {
            valorView.setTextColor(Color.parseColor("#E45757"));
        }
        
        TextView labelTipo = item.findViewById(R.id.labelTipoTransacao);
        String tipoLabel = "";
        if (recorrente == 1) {
            tipoLabel = "(fixa)";
        } else if (parcelas > 1) {
            tipoLabel = "(" + numeroParcela + "/" + parcelas + ")";
        }

        if (!tipoLabel.isEmpty()) {
            labelTipo.setText(tipoLabel);
            labelTipo.setVisibility(View.VISIBLE);
        } else {
            labelTipo.setVisibility(View.GONE);
        }

        final String finalTipo = tipo;
        item.setOnClickListener(v -> mostrarMenuTransacao(v, idTransacao, finalTipo));
        return item;
    }

    private void mostrarMenuTransacao(View itemView, int idTransacao, String tipo) {
        MenuHelper.MenuItemData[] menuItems = new MenuHelper.MenuItemData[]{
                new MenuHelper.MenuItemData("Editar", R.drawable.ic_edit, () -> {
                    if ("transacao_cartao".equals(tipo)) {
                        MenuCadDespesaCartaoFragment fragment = MenuCadDespesaCartaoFragment.newInstance(idUsuarioLogado, -1);
                        fragment.setOnDespesaSalvaListener(this::carregarTransacoes);
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
                new MenuHelper.MenuItemData("Excluir", R.drawable.ic_delete, () -> {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Excluir Transação")
                            .setMessage("Tem certeza que deseja excluir esta transação?")
                            .setPositiveButton("Sim", (dialog, which) -> excluirTransacao(idTransacao, tipo))
                            .setNegativeButton("Não", null)
                            .show();
                })
        };
        MenuHelper.showMenu(this, itemView, menuItems);
    }
    
    private void excluirTransacao(int id, String tipo) {
        try (SQLiteDatabase db = new MeuDbHelper(this).getWritableDatabase()) {
            if ("transacao_cartao".equals(tipo)) {
                db.delete("parcelas_cartao", "id_transacao_cartao = ?", new String[]{String.valueOf(id)});
                db.delete("transacoes_cartao", "id = ?", new String[]{String.valueOf(id)});
            } else {
                db.delete("transacoes", "id = ?", new String[]{String.valueOf(id)});
            }
            Toast.makeText(this, "Transação excluída com sucesso!", Toast.LENGTH_SHORT).show();
            carregarTransacoes();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao excluir transação.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getTipoMovimento(int idTransacao) {
        try (SQLiteDatabase db = new MeuDbHelper(this).getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT tipo FROM transacoes WHERE id = ?", new String[]{String.valueOf(idTransacao)})) {
            if (cursor.moveToFirst()) {
                int tipo = cursor.getInt(cursor.getColumnIndexOrThrow("tipo"));
                return tipo == 1 ? "receita" : "despesa";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "despesa";
    }

    private String formatarBR(double valor) {
        NumberFormat formatoBR = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return formatoBR.format(valor);
    }

    private String formatarDataBR(String dataISO, boolean comDiaDaSemana) {
        if (dataISO == null || dataISO.isEmpty()) {
            return "Data inválida";
        }
        try {
            SimpleDateFormat sdfISO = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdfISO.parse(dataISO);
            String formato = comDiaDaSemana ? "EEEE, dd/MM/yyyy" : "dd/MM/yyyy";
            SimpleDateFormat sdfBR = new SimpleDateFormat(formato, new Locale("pt", "BR"));
            return sdfBR.format(date);
        } catch (Exception e) {
            try {
                SimpleDateFormat sdfISO = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = sdfISO.parse(dataISO);
                String formato = comDiaDaSemana ? "EEEE, dd/MM/yyyy" : "dd/MM/yyyy";
                SimpleDateFormat sdfBR = new SimpleDateFormat(formato, new Locale("pt", "BR"));
                return sdfBR.format(date);
            } catch (Exception e2) {
                return dataISO;
            }
        }
    }

    @Override
    public void onFabDespesaCartaoClick(int idUsuario) {
        // Delegado para o BottomMenuFragment
    }
}
