package com.example.app1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.example.app1.interfaces.BottomMenuListener;
import com.example.app1.utils.MenuHelper;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

public class TelaFaturaCartao extends AppCompatActivity implements BottomMenuListener {

    private TextView txtMes, txtAno, txtNomeCartao, diaFechamento, diaVencimento, valorFatura;
    private ImageView icCartao;
    private LinearLayout blocoDespesas;
    private int idCartao;
    private int dataFechamento = 0;
    private int dataVencimento = 0;
    private String bandeiraCartao = "outros";
    private int idUsuarioLogado = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tela_fatura_cartao);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        idCartao = getIntent().getIntExtra("id_cartao", -1);
        idUsuarioLogado = getIntent().getIntExtra("id_usuario", -1);

        txtMes = findViewById(R.id.txtMes);
        txtAno = findViewById(R.id.txtAno);
        txtNomeCartao = findViewById(R.id.txtNomeCartao);
        icCartao = findViewById(R.id.icCartao);
        diaFechamento = findViewById(R.id.diaFechamento);
        diaVencimento = findViewById(R.id.diaVencimento);
        valorFatura = findViewById(R.id.valorFatura);
        blocoDespesas = findViewById(R.id.blocoDespesa);

        Calendar agora = Calendar.getInstance();
        String[] nomesMes = {
                "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        };
        txtMes.setText(nomesMes[agora.get(Calendar.MONTH)]);
        txtAno.setText(String.valueOf(agora.get(Calendar.YEAR)));

        LinearLayout btnMesAno = findViewById(R.id.btnMesAno);
        btnMesAno.setOnClickListener(v -> showMonthYearPicker());

        BottomMenuFragment fragment = new BottomMenuFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.menu_container, fragment)
                .commit();

        carregarCabecalhoCartao();
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarFatura();
    }

    private void showMonthYearPicker() {
        final String[] nomesMes = {
                "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        };
        String mesSelecionado = txtMes.getText().toString();
        int anoSelecionado = Integer.parseInt(txtAno.getText().toString());
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
            carregarFatura();
        });

        dialogFragment.show(getSupportFragmentManager(), "MonthYearPickerDialog");
    }

    private void carregarCabecalhoCartao() {
        SQLiteDatabase db = new MeuDbHelper(this).getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT nome, data_fechamento_fatura, data_vencimento_fatura, bandeira FROM cartoes WHERE id = ?",
                new String[]{String.valueOf(idCartao)}
        );
        if (cursor.moveToFirst()) {
            String nomeCartao = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
            dataFechamento = cursor.getInt(cursor.getColumnIndexOrThrow("data_fechamento_fatura"));
            dataVencimento = cursor.getInt(cursor.getColumnIndexOrThrow("data_vencimento_fatura"));
            bandeiraCartao = cursor.getString(cursor.getColumnIndexOrThrow("bandeira"));
            txtNomeCartao.setText(nomeCartao);
            diaFechamento.setText(String.valueOf(dataFechamento));
            diaVencimento.setText(String.valueOf(dataVencimento));
            if (bandeiraCartao != null) {
                switch (bandeiraCartao.toLowerCase(Locale.ROOT)) {
                    case "visa":
                        icCartao.setImageResource(R.drawable.visa);
                        break;
                    case "mastercard":
                        icCartao.setImageResource(R.drawable.mastercard);
                        break;
                    default:
                        icCartao.setImageResource(R.drawable.ic_credit_card);
                        break;
                }
            }
        }
        cursor.close();
        db.close();
    }

    public void carregarFatura() {
        String mesStr = txtMes.getText().toString();
        int ano = Integer.parseInt(txtAno.getText().toString());

        int mes = 0;
        final String[] nomesMes = {
                "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        };
        for (int i = 0; i < nomesMes.length; i++) {
            if (nomesMes[i].equalsIgnoreCase(mesStr)) {
                mes = i;
                break;
            }
        }

        Calendar inicio = Calendar.getInstance();
        Calendar fim = Calendar.getInstance();
        if (dataFechamento > 0) {
            inicio.set(ano, mes, dataFechamento + 1);
            inicio.add(Calendar.MONTH, -1);
            fim.set(ano, mes, dataFechamento);
        } else {
            inicio.set(ano, mes, 1);
            fim.set(ano, mes, inicio.getActualMaximum(Calendar.DAY_OF_MONTH));
        }
        String dataInicio = String.format("%04d-%02d-%02d", inicio.get(Calendar.YEAR), inicio.get(Calendar.MONTH) + 1, inicio.get(Calendar.DAY_OF_MONTH));
        String dataFim = String.format("%04d-%02d-%02d", fim.get(Calendar.YEAR), fim.get(Calendar.MONTH) + 1, fim.get(Calendar.DAY_OF_MONTH));

        SQLiteDatabase db = new MeuDbHelper(this).getReadableDatabase();

        String query =
                "SELECT t.data_compra AS data, p.valor AS valor_parcela, t.descricao, t.id_categoria, cat.nome AS categoria_nome, " +
                        "cat.cor, t.id AS id_transacao_cartao, t.recorrente, p.numero_parcela, t.parcelas " +
                        "FROM parcelas_cartao p " +
                        "INNER JOIN transacoes_cartao t ON t.id = p.id_transacao_cartao " +
                        "LEFT JOIN categorias cat ON cat.id = t.id_categoria " +
                        "WHERE t.id_cartao = ? AND t.parcelas > 1 AND p.data_vencimento >= ? AND p.data_vencimento <= ? " +

                        "UNION ALL " +

                        "SELECT t.data_compra AS data, t.valor AS valor_parcela, t.descricao, t.id_categoria, cat.nome AS categoria_nome, cat.cor, " +
                        "t.id AS id_transacao_cartao, t.recorrente, NULL AS numero_parcela, t.parcelas " +
                        "FROM transacoes_cartao t " +
                        "LEFT JOIN categorias cat ON cat.id = t.id_categoria " +
                        "WHERE t.id_cartao = ? AND (t.parcelas = 1 OR t.parcelas IS NULL) AND (t.recorrente IS NULL OR t.recorrente = 0) " +
                        "AND t.data_compra >= ? AND t.data_compra <= ? " +

                        "UNION ALL " +

                        "SELECT d.data_inicial AS data, d.valor AS valor_parcela, t.descricao, t.id_categoria, cat.nome AS categoria_nome, cat.cor, " +
                        "t.id AS id_transacao_cartao, t.recorrente, NULL AS numero_parcela, t.parcelas " +
                        "FROM despesas_recorrentes_cartao d " +
                        "INNER JOIN transacoes_cartao t ON t.id = d.id_transacao_cartao " +
                        "LEFT JOIN categorias cat ON cat.id = t.id_categoria " +
                        "WHERE t.id_cartao = ? AND d.data_inicial <= ? AND (d.data_final IS NULL OR d.data_final >= ?) " +
                        "AND d.data_inicial = (SELECT MAX(d2.data_inicial) FROM despesas_recorrentes_cartao d2 " +
                        "WHERE d2.id_transacao_cartao = d.id_transacao_cartao AND d2.data_inicial <= ?) " +

                        "ORDER BY data ASC";

        Cursor cur = db.rawQuery(query, new String[]{
                String.valueOf(idCartao), dataInicio, dataFim,
                String.valueOf(idCartao), dataInicio, dataFim,
                String.valueOf(idCartao), dataFim, dataInicio, dataFim
        });

        blocoDespesas.removeAllViews();
        double totalFatura = 0;
        boolean temDespesas = false;
        String ultimoDia = "";

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        HashSet<Integer> idsRecorrentesExibidos = new HashSet<>();

        while (cur.moveToNext()) {
            int idTransacaoCartao = cur.getInt(cur.getColumnIndexOrThrow("id_transacao_cartao"));
            int recorrente = cur.getInt(cur.getColumnIndexOrThrow("recorrente"));
            Integer numeroParcela = null;
            if (!cur.isNull(cur.getColumnIndexOrThrow("numero_parcela"))) {
                numeroParcela = cur.getInt(cur.getColumnIndexOrThrow("numero_parcela"));
            }
            int totalParcelas = cur.getInt(cur.getColumnIndexOrThrow("parcelas"));

            if (recorrente == 1 && idsRecorrentesExibidos.contains(idTransacaoCartao)) {
                continue;
            } else if (recorrente == 1) {
                idsRecorrentesExibidos.add(idTransacaoCartao);
            }

            final int finalIdTransacaoCartao = idTransacaoCartao;
            final int finalRecorrente = recorrente;
            final Integer finalNumeroParcela = numeroParcela;
            final int finalTotalParcelas = totalParcelas;

            String dataRegistro = cur.getString(cur.getColumnIndexOrThrow("data"));
            String dataFormatada = formatarDataBR(dataRegistro);
            if (!dataFormatada.equals(ultimoDia)) {
                TextView dataLabel = new TextView(this);
                dataLabel.setText(dataFormatada);
                dataLabel.setTypeface(Typeface.DEFAULT_BOLD);
                dataLabel.setTextColor(Color.GRAY);
                dataLabel.setTextSize(13f);
                dataLabel.setPadding(4, 16, 4, 8);
                blocoDespesas.addView(dataLabel);
                ultimoDia = dataFormatada;
            }

            double valor = cur.getDouble(cur.getColumnIndexOrThrow("valor_parcela"));
            String descricao = cur.getString(cur.getColumnIndexOrThrow("descricao"));
            String categoria = cur.getString(cur.getColumnIndexOrThrow("categoria_nome"));
            String corCategoria = cur.getString(cur.getColumnIndexOrThrow("cor"));

            String tipoLabel = "";
            if (numeroParcela != null) {
                tipoLabel = " (" + numeroParcela + "/" + totalParcelas + ")";
            } else if (recorrente == 1) {
                tipoLabel = " (fixa)";
            }

            totalFatura += valor;
            temDespesas = true;

            View item = inflater.inflate(R.layout.item_despesa_fatura, blocoDespesas, false);
            TextView inicialCat = item.findViewById(R.id.txtIconCategoria);
            inicialCat.setText(categoria != null && categoria.length() > 0 ? categoria.substring(0, 1) : "?");
            GradientDrawable circle = (GradientDrawable) inicialCat.getBackground();
            if (corCategoria != null) circle.setColor(Color.parseColor(corCategoria));

            TextView txtTitulo = item.findViewById(R.id.tituloDespesa);
            TextView labelTipo = item.findViewById(R.id.labelTipoDespesa);

            txtTitulo.setText(descricao);

            if (!tipoLabel.isEmpty()) {
                labelTipo.setText(tipoLabel);
                labelTipo.setVisibility(View.VISIBLE);
            } else {
                labelTipo.setVisibility(View.GONE);
            }

            ((TextView) item.findViewById(R.id.tituloCategoria)).setText(categoria != null ? categoria : "Outros");
            ((TextView) item.findViewById(R.id.valorDespesa)).setText(formatarBR(valor));

            item.setOnClickListener(v -> mostrarMenuDespesa(
                    item,
                    finalIdTransacaoCartao,
                    finalRecorrente,
                    finalNumeroParcela,
                    finalTotalParcelas
            ));

            blocoDespesas.addView(item);
        }

        cur.close();
        db.close();

        valorFatura.setText(formatarBR(totalFatura));
        if (!temDespesas) {
            TextView aviso = new TextView(this);
            aviso.setText("Nenhuma despesa encontrada para este período");
            aviso.setTextColor(Color.GRAY);
            aviso.setTextSize(16f);
            aviso.setPadding(16, 16, 16, 16);
            blocoDespesas.addView(aviso);
            valorFatura.setText("R$ 0,00");
        }
    }


    private String formatarBR(double valor) {
        NumberFormat formatoBR = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return formatoBR.format(valor);
    }

    private String formatarDataBR(String dataISO) {
        try {
            SimpleDateFormat sdfISO = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdfISO.parse(dataISO);
            SimpleDateFormat sdfBR = new SimpleDateFormat("EEEE dd/MM/yyyy", new Locale("pt", "BR"));
            return sdfBR.format(date);
        } catch (Exception e) {
            return dataISO;
        }
    }

    private void mostrarMenuDespesa(View itemDespesa, int idTransacaoCartao, int recorrente, Integer numeroParcela, int totalParcelas) {
        final int finalIdTransacaoCartao = idTransacaoCartao;
        final int finalRecorrente = recorrente;
        final Integer finalNumeroParcela = numeroParcela;
        final int finalTotalParcelas = totalParcelas;

        MenuHelper.MenuItemData[] menuItens = new MenuHelper.MenuItemData[] {
                new MenuHelper.MenuItemData("Editar", R.drawable.ic_edit, new MenuHelper.MenuItemClickListener() {
                    @Override
                    public void onClick() {
                        MenuCadDespesaCartaoFragment fragment = MenuCadDespesaCartaoFragment.newInstance(idUsuarioLogado, idCartao);

                        fragment.setOnDespesaSalvaListener(new MenuCadDespesaCartaoFragment.OnDespesaSalvaListener() {
                            @Override
                            public void onDespesaSalva() {
                                new Handler(Looper.getMainLooper()).post(() -> carregarFatura());
                            }
                        });

                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.containerFragment, fragment)
                                .addToBackStack(null)
                                .commit();

                        fragment.editarTransacao(finalIdTransacaoCartao);
                    }
                }),
                new MenuHelper.MenuItemData("Excluir", R.drawable.ic_delete, new MenuHelper.MenuItemClickListener() {
                    @Override
                    public void onClick() {
                        new android.app.AlertDialog.Builder(TelaFaturaCartao.this)
                                .setTitle("Excluir despesa")
                                .setMessage("Tem certeza que deseja excluir esta despesa?\n\n" +
                                        "- Despesa 1x: será removida definitivamente.\n" +
                                        "- Despesa parcelada: toda a transação e suas parcelas serão removidas.\n" +
                                        "- Despesa recorrente: a recorrência será encerrada a partir deste mês (apenas meses atuais e seguintes serão removidos).")
                                .setPositiveButton("Sim", (dialog, which) -> {
                                    SQLiteDatabase db = new MeuDbHelper(TelaFaturaCartao.this).getWritableDatabase();
                                    try {
                                        if (finalRecorrente == 1) {
                                            String dataFinal = calcularDataFinalDespesaRecorrente();
                                            db.execSQL("UPDATE despesas_recorrentes_cartao SET data_final = ? WHERE id_transacao_cartao = ?",
                                                    new Object[]{dataFinal, finalIdTransacaoCartao});
                                        } else if (finalNumeroParcela != null && finalTotalParcelas > 1) {
                                            db.delete("parcelas_cartao", "id_transacao_cartao = ?", new String[]{String.valueOf(finalIdTransacaoCartao)});
                                            db.delete("transacoes_cartao", "id = ?", new String[]{String.valueOf(finalIdTransacaoCartao)});
                                        } else {
                                            db.delete("transacoes_cartao", "id = ?", new String[]{String.valueOf(finalIdTransacaoCartao)});
                                        }

                                        carregarFatura();
                                        Toast.makeText(TelaFaturaCartao.this, "Despesa excluída com sucesso", Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Toast.makeText(TelaFaturaCartao.this, "Erro ao excluir despesa", Toast.LENGTH_SHORT).show();
                                    } finally {
                                        db.close();
                                    }
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                    }
                })
        };

        MenuHelper.showMenu(this, itemDespesa, menuItens);
    }

    private String calcularDataFinalDespesaRecorrente() {
        Calendar agora = Calendar.getInstance();
        Calendar dataFinal = Calendar.getInstance();

        if (dataFechamento > 0) {
            dataFinal.set(agora.get(Calendar.YEAR), agora.get(Calendar.MONTH), dataFechamento);
            dataFinal.add(Calendar.MONTH, -1);
        } else {
            dataFinal.set(agora.get(Calendar.YEAR), agora.get(Calendar.MONTH), 1);
            dataFinal.add(Calendar.DAY_OF_MONTH, -1);
        }

        return String.format("%04d-%02d-%02d",
                dataFinal.get(Calendar.YEAR),
                dataFinal.get(Calendar.MONTH) + 1,
                dataFinal.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onFabDespesaCartaoClick(int idUsuario) {
        MenuCadDespesaCartaoFragment fragment = MenuCadDespesaCartaoFragment.newInstance(idUsuario, idCartao);

        fragment.setOnDespesaSalvaListener(() -> {
            new Handler(Looper.getMainLooper()).post(() -> carregarFatura());
        });

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.containerFragment, fragment) // USA O CONTAINER PADRÃO
                .addToBackStack(null)
                .commit();
    }

}
