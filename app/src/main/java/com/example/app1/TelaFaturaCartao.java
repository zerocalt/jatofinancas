package com.example.app1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class TelaFaturaCartao extends AppCompatActivity {

    private TextView txtMes, txtAno, txtNomeCartao, diaFechamento, diaVencimento, valorFatura, txtDataDespesa;
    private ImageView icCartao;
    private LinearLayout blocoDespesas;
    private int idCartao;
    private int dataFechamento = 0;
    private int dataVencimento = 0;
    private String bandeiraCartao = "outros";

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

        // ID do cartão
        idCartao = getIntent().getIntExtra("id_cartao", -1);

        // Referências
        txtMes = findViewById(R.id.txtMes);
        txtAno = findViewById(R.id.txtAno);
        txtNomeCartao = findViewById(R.id.txtNomeCartao);
        icCartao = findViewById(R.id.icCartao);
        diaFechamento = findViewById(R.id.diaFechamento);
        diaVencimento = findViewById(R.id.diaVencimento);
        valorFatura = findViewById(R.id.valorFatura);
        txtDataDespesa = findViewById(R.id.txtDataDespesa);
        blocoDespesas = findViewById(R.id.blocoDespesa);

        // Inicializa mês/ano
        Calendar agora = Calendar.getInstance();
        String[] nomesMes = {
                "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        };
        txtMes.setText(nomesMes[agora.get(Calendar.MONTH)]);
        txtAno.setText(String.valueOf(agora.get(Calendar.YEAR)));

        // MENU DE MÊS/ANO
        LinearLayout btnMesAno = findViewById(R.id.btnMesAno);
        btnMesAno.setOnClickListener(v -> showMonthYearPicker());

        // Carrega o menu inferior
        BottomMenuFragment fragment = new BottomMenuFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.menu_container, fragment)
                .commit();

        // Carregar Cabeçalho (Nome, datas, bandeira)
        carregarCabecalhoCartao();

        // Carregar fatura do mês vigente
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

    // Carregue dados do cartão selecionado (cabecalho)
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
            // Ícone bandeira
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

    // Constrói intervalo da fatura e carrega as despesas
    private void carregarFatura() {
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

        // --- Calcula o início e o fim da fatura (considerando fechamento)
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
        // Atualiza a label de data da tela (dia da lista)
        txtDataDespesa.setText(String.format(Locale.getDefault(), "%tA %<td/%<tm/%<tY", inicio));

        // --- Consulta despesas do cartão no período (despesas normais, parcelas, fixas)
        SQLiteDatabase db = new MeuDbHelper(this).getReadableDatabase();

        // Recupera despesas comuns e recorrentes
        Cursor c = db.rawQuery(
                "SELECT t.id, t.descricao, t.valor, t.id_categoria, t.data_compra, t.recorrente, t.parcelas, " +
                        "cat.nome AS categoria_nome, cat.cor " +
                        "FROM transacoes_cartao t " +
                        "LEFT JOIN categorias cat ON cat.id = t.id_categoria " +
                        "WHERE t.id_cartao = ? " +
                        "AND (" +
                        "   (? <= t.data_compra AND t.data_compra <= ?)" +   // Normal: dentro do período
                        "     OR (t.recorrente = 1 AND t.data_compra <= ?)" + // Fixa: criada antes ou até o fim
                        ")",
                new String[]{String.valueOf(idCartao), dataInicio, dataFim, dataFim}
        );

        // Limpa o bloco de despesas para popular dinâmico
        blocoDespesas.removeAllViews();
        double totalFatura = 0;
        boolean temDespesas = false;

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        while (c.moveToNext()) {
            int recorrente = c.getInt(c.getColumnIndexOrThrow("recorrente"));
            double valor = c.getDouble(c.getColumnIndexOrThrow("valor"));
            String descricao = c.getString(c.getColumnIndexOrThrow("descricao"));
            String categoria = c.getString(c.getColumnIndexOrThrow("categoria_nome"));
            String corCategoria = c.getString(c.getColumnIndexOrThrow("cor"));
            String dataCompra = c.getString(c.getColumnIndexOrThrow("data_compra"));
            int idCategoria = c.getInt(c.getColumnIndexOrThrow("id_categoria"));
            int qtdParcelas = c.getInt(c.getColumnIndexOrThrow("parcelas"));

            // Ajuste valor caso seja despesa fixa: buscar na tabela de histórico se houver alteração!
            if (recorrente == 1) {
                valor = buscarValorDespesaFixaNoPeriodo(db, c.getInt(c.getColumnIndexOrThrow("id")), dataFim);
            }
            // Parcelas: somar apenas as parcelas do período atual
            if (qtdParcelas > 1) {
                // Busca total das parcelas dessa transação com data no range
                valor = buscarSomaParcelasNoPeriodo(db, c.getInt(c.getColumnIndexOrThrow("id")), dataInicio, dataFim);
                if (valor == 0) // Nenhuma parcela nesse intervalo
                    continue;
            }
            totalFatura += valor;
            temDespesas = true;

            // Monta o bloco visual para cada despesa encontrada
            View item = inflater.inflate(R.layout.item_despesa_fatura, blocoDespesas, false);

            // Círculo colorido c/inicial
            TextView inicialCat = item.findViewById(R.id.txtIconCategoria);
            inicialCat.setText(categoria != null && categoria.length() > 0 ? categoria.substring(0, 1) : "?");
            GradientDrawable circle = (GradientDrawable) inicialCat.getBackground();
            if (corCategoria != null) circle.setColor(Color.parseColor(corCategoria));

            ((TextView) item.findViewById(R.id.tituloDespesa)).setText(descricao);
            ((TextView) item.findViewById(R.id.tituloCategoria)).setText(categoria != null ? categoria : "Outros");
            ((TextView) item.findViewById(R.id.valorDespesa)).setText(formatarBR(valor));

            blocoDespesas.addView(item);
        }

        c.close();
        db.close();

        // Valor total na tela
        valorFatura.setText(formatarBR(totalFatura));
        if (!temDespesas) {
            // Se não há despesas, mostre aviso e zere os campos
            TextView aviso = new TextView(this);
            aviso.setText("[translate:Nenhuma despesa encontrada para este período]");
            aviso.setTextColor(Color.GRAY);
            aviso.setTextSize(16f);
            aviso.setPadding(16, 16, 16, 16);
            blocoDespesas.addView(aviso);
            valorFatura.setText("R$ 0,00");
        }
    }

    // Busca valor atual da despesa recorrente até a data referência
    private double buscarValorDespesaFixaNoPeriodo(SQLiteDatabase db, int idTransacaoCartao, String dataRef) {
        double valor = 0;
        Cursor cur = db.rawQuery(
                "SELECT valor FROM despesas_recorrentes_cartao WHERE id_transacao_cartao = ? AND data_inicial <= ? " +
                        "ORDER BY data_inicial DESC LIMIT 1",
                new String[]{String.valueOf(idTransacaoCartao), dataRef}
        );
        if (cur.moveToFirst()) valor = cur.getDouble(0);
        cur.close();
        return valor;
    }

    // Busca soma das parcelas dentro do período
    private double buscarSomaParcelasNoPeriodo(SQLiteDatabase db, int idTransacaoCartao, String dataInicio, String dataFim) {
        double soma = 0;
        Cursor cur = db.rawQuery(
                "SELECT SUM(valor) FROM parcelas_cartao WHERE id_transacao_cartao = ? AND data_vencimento >= ? AND data_vencimento <= ?",
                new String[]{String.valueOf(idTransacaoCartao), dataInicio, dataFim}
        );
        if (cur.moveToFirst()) soma = cur.getDouble(0);
        cur.close();
        return soma;
    }

    private String formatarBR(double valor) {
        NumberFormat formatoBR = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return formatoBR.format(valor);
    }
}