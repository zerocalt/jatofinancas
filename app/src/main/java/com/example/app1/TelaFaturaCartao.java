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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.app1.interfaces.BottomMenuListener;
import com.example.app1.utils.MenuHelper;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TelaFaturaCartao extends AppCompatActivity implements BottomMenuListener {

    private static final String TAG = "TelaFaturaCartao";

    private TextView txtMes, txtAno, txtNomeCartao, diaFechamento, diaVencimento, valorFatura;
    private ImageView icCartao;
    private LinearLayout blocoDespesas;
    private int idCartao;
    private int dataFechamento = 0;
    private int dataVencimento = 0;
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

        if (idCartao == -1 || idUsuarioLogado == -1) {
            Toast.makeText(this, "Erro: ID do cartão ou usuário inválido.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        getSupportFragmentManager().setFragmentResultListener("despesaSalvaRequest", this, (requestKey, bundle) -> {
            if (bundle.getBoolean("atualizar")) {
                carregarFatura();
            }
        });

        bindViews();
        setupInitialDate();
        setupListeners();

        getSupportFragmentManager().beginTransaction().replace(R.id.menu_container, new BottomMenuFragment()).commit();
        carregarCabecalhoCartao();
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarFatura();
    }

    private void bindViews() {
        txtMes = findViewById(R.id.txtMes);
        txtAno = findViewById(R.id.txtAno);
        txtNomeCartao = findViewById(R.id.txtNomeCartao);
        icCartao = findViewById(R.id.icCartao);
        diaFechamento = findViewById(R.id.diaFechamento);
        diaVencimento = findViewById(R.id.diaVencimento);
        valorFatura = findViewById(R.id.valorFatura);
        blocoDespesas = findViewById(R.id.blocoDespesa);
    }

    private void setupInitialDate() {
        Calendar agora = Calendar.getInstance();
        String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        txtMes.setText(nomesMes[agora.get(Calendar.MONTH)]);
        txtAno.setText(String.valueOf(agora.get(Calendar.YEAR)));
    }

    private void setupListeners() {
        findViewById(R.id.btnMesAno).setOnClickListener(v -> showMonthYearPicker());
    }

    private void showMonthYearPicker() {
        final String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        int mesIndex = 0;
        for (int i = 0; i < nomesMes.length; i++) {
            if (nomesMes[i].equalsIgnoreCase(txtMes.getText().toString())) {
                mesIndex = i;
                break;
            }
        }

        MonthYearPickerDialogFragment dialogFragment = MonthYearPickerDialogFragment.getInstance(mesIndex, Integer.parseInt(txtAno.getText().toString()));
        dialogFragment.setOnDateSetListener((year, monthOfYear) -> {
            txtMes.setText(nomesMes[monthOfYear]);
            txtAno.setText(String.valueOf(year));
            carregarFatura();
        });
        dialogFragment.show(getSupportFragmentManager(), null);
    }

    private void carregarCabecalhoCartao() {
        try (MeuDbHelper dbHelper = new MeuDbHelper(this); SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT nome, data_fechamento_fatura, data_vencimento_fatura, bandeira FROM cartoes WHERE id = ?", new String[]{String.valueOf(idCartao)})) {
            if (cursor.moveToFirst()) {
                txtNomeCartao.setText(cursor.getString(cursor.getColumnIndexOrThrow("nome")));
                dataFechamento = cursor.getInt(cursor.getColumnIndexOrThrow("data_fechamento_fatura"));
                dataVencimento = cursor.getInt(cursor.getColumnIndexOrThrow("data_vencimento_fatura"));
                String bandeira = cursor.getString(cursor.getColumnIndexOrThrow("bandeira"));
                diaFechamento.setText(String.valueOf(dataFechamento));
                diaVencimento.setText(String.valueOf(dataVencimento));
                if (bandeira != null) {
                    switch (bandeira.toLowerCase(Locale.ROOT)) {
                        case "visa": icCartao.setImageResource(R.drawable.visa); break;
                        case "mastercard": icCartao.setImageResource(R.drawable.mastercard); break;
                        default: icCartao.setImageResource(R.drawable.ic_credit_card); break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar cabeçalho", e);
        }
    }

    public void carregarFatura(int idCartaoParam) {
        this.idCartao = idCartaoParam;
        carregarFatura(); // chama o método atual que faz o carregamento
    }

    public void carregarFatura() {
        blocoDespesas.removeAllViews();
        double totalFatura = 0;
        boolean temDespesas = false;
        String ultimoDia = "";

        try {
            Calendar inicio = Calendar.getInstance();
            Calendar fim = Calendar.getInstance();
            int mes = Arrays.asList(new String[]{"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"}).indexOf(txtMes.getText().toString());
            int ano = Integer.parseInt(txtAno.getText().toString());

            inicio.set(ano, mes, dataFechamento > 0 ? dataFechamento + 1 : 1);
            if (dataFechamento > 0) inicio.add(Calendar.MONTH, -1);
            fim.set(ano, mes, dataFechamento > 0 ? dataFechamento : inicio.getActualMaximum(Calendar.DAY_OF_MONTH));

            String dataInicio = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(inicio.getTime());
            String dataFim = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(fim.getTime());

            try (MeuDbHelper dbHelper = new MeuDbHelper(this); SQLiteDatabase db = dbHelper.getReadableDatabase()) {
                String query = "SELECT t.data_compra AS data, p.valor AS valor_parcela, t.descricao, t.id_categoria, cat.nome AS categoria_nome, " +
                        "cat.cor, t.id AS id_transacao_cartao, t.recorrente, p.numero_parcela, t.parcelas, p.data_vencimento " +
                        "FROM parcelas_cartao p INNER JOIN transacoes_cartao t ON t.id = p.id_transacao_cartao LEFT JOIN categorias cat ON cat.id = t.id_categoria WHERE t.id_cartao = ? " +
                        "ORDER BY t.data_compra DESC";

                Cursor cur = db.rawQuery(query, new String[]{String.valueOf(idCartao)});
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                while (cur.moveToNext()) {
                    if (!dateIsInRange(cur.getString(cur.getColumnIndexOrThrow("data_vencimento")), dataInicio, dataFim)) continue;

                    temDespesas = true;
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
                    totalFatura += valor;
                    View item = inflater.inflate(R.layout.item_despesa_fatura, blocoDespesas, false);

                    String categoria = cur.getString(cur.getColumnIndexOrThrow("categoria_nome"));
                    ((TextView) item.findViewById(R.id.tituloDespesa)).setText(cur.getString(cur.getColumnIndexOrThrow("descricao")));
                    ((TextView) item.findViewById(R.id.tituloCategoria)).setText(categoria != null ? categoria : "Outros");
                    ((TextView) item.findViewById(R.id.valorDespesa)).setText(formatarBR(valor));

                    TextView inicialCat = item.findViewById(R.id.txtIconCategoria);
                    inicialCat.setText(categoria != null && !categoria.isEmpty() ? categoria.substring(0, 1) : "?");
                    if (inicialCat.getBackground() instanceof GradientDrawable) {
                        String corCategoria = cur.getString(cur.getColumnIndexOrThrow("cor"));
                        if (corCategoria != null) ((GradientDrawable) inicialCat.getBackground()).setColor(Color.parseColor(corCategoria));
                    }

                    int recorrente = cur.getInt(cur.getColumnIndexOrThrow("recorrente"));
                    Integer numeroParcela = cur.isNull(cur.getColumnIndexOrThrow("numero_parcela")) ? null : cur.getInt(cur.getColumnIndexOrThrow("numero_parcela"));
                    int totalParcelas = cur.getInt(cur.getColumnIndexOrThrow("parcelas"));
                    String tipoLabel = "";
                    if (numeroParcela != null && totalParcelas > 1) tipoLabel = " (" + numeroParcela + "/" + totalParcelas + ")";
                    else if (recorrente == 1) tipoLabel = " (fixa)";
                    TextView labelTipo = item.findViewById(R.id.labelTipoDespesa);
                    labelTipo.setText(tipoLabel);
                    labelTipo.setVisibility(tipoLabel.isEmpty() ? View.GONE : View.VISIBLE);

                    int idTransacaoCartao = cur.getInt(cur.getColumnIndexOrThrow("id_transacao_cartao"));
                    item.setOnClickListener(v -> mostrarMenuDespesa(item, idTransacaoCartao, recorrente, numeroParcela, totalParcelas));
                    blocoDespesas.addView(item);
                }
                cur.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar fatura", e);
        }

        valorFatura.setText(formatarBR(totalFatura));
        if (!temDespesas) {
            TextView aviso = new TextView(this);
            aviso.setText("Nenhuma despesa encontrada para este período");
            blocoDespesas.addView(aviso);
        }
    }

    private boolean dateIsInRange(String date, String startDate, String endDate) {
        if (date == null) return false;
        String dateOnly = date.split(" ")[0];
        return dateOnly.compareTo(startDate) >= 0 && dateOnly.compareTo(endDate) <= 0;
    }

    private String formatarBR(double valor) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(valor);
    }

    private String formatarDataBR(String dataISO) {
        if (dataISO == null) return "";
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dataISO);
            return new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("pt", "BR")).format(date);
        } catch (ParseException e) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dataISO);
                return new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("pt", "BR")).format(date);
            } catch (ParseException ex) {
                return dataISO;
            }
        }
    }

    private void mostrarMenuDespesa(View itemDespesa, int idTransacaoCartao, int recorrente, Integer numeroParcela, int totalParcelas) {
        MenuHelper.MenuItemData[] menuItens = {
                new MenuHelper.MenuItemData("Editar", R.drawable.ic_edit, () -> {
                    MenuCadDespesaCartaoFragment fragment = MenuCadDespesaCartaoFragment.newInstance(idUsuarioLogado, idCartao);
                    getSupportFragmentManager().beginTransaction().replace(R.id.containerFragment, fragment).addToBackStack(null).commit();
                    fragment.editarTransacao(idTransacaoCartao);
                }),
                new MenuHelper.MenuItemData("Excluir", R.drawable.ic_delete, () -> {
                    new AlertDialog.Builder(this).setTitle("Excluir despesa").setMessage("Tem certeza?")
                            .setPositiveButton("Sim", (dialog, which) -> {
                                try (SQLiteDatabase db = new MeuDbHelper(this).getWritableDatabase()) {
                                    if (recorrente == 1) {
                                        db.execSQL("UPDATE despesas_recorrentes_cartao SET data_final = ? WHERE id_transacao_cartao = ?", new Object[]{calcularDataFinalDespesaRecorrente(), idTransacaoCartao});
                                    } else {
                                        db.delete("transacoes_cartao", "id = ?", new String[]{String.valueOf(idTransacaoCartao)});
                                    }
                                    carregarFatura();
                                    Toast.makeText(this, "Despesa excluída", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(this, "Erro ao excluir", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Não", null).show();
                })
        };
        MenuHelper.showMenu(this, itemDespesa, menuItens);
    }

    private String calcularDataFinalDespesaRecorrente() {
        Calendar agora = Calendar.getInstance();
        if (dataFechamento > 0) {
            agora.set(Calendar.DAY_OF_MONTH, dataFechamento);
            agora.add(Calendar.MONTH, -1);
        } else {
            agora.set(Calendar.DAY_OF_MONTH, 1);
            agora.add(Calendar.DAY_OF_MONTH, -1);
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(agora.getTime());
    }

    @Override
    public void onFabDespesaCartaoClick(int idUsuario) {
        MenuCadDespesaCartaoFragment fragment = MenuCadDespesaCartaoFragment.newInstance(idUsuario, idCartao);
        getSupportFragmentManager().beginTransaction().replace(R.id.containerFragment, fragment).addToBackStack(null).commit();
    }

    public int getIdCartao() {
        return idCartao;
    }

    public int getIdUsuario() {
        return idUsuarioLogado;
    }

}