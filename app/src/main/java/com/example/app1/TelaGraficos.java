package com.example.app1;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.app1.data.GraficosDAO;
import com.example.app1.utils.CustomMarkerView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class TelaGraficos extends AppCompatActivity {

    private TextView txtMes, txtAno;
    private PieChart pieChart;
    private BarChart barChart;
    private LineChart lineChart;
    private int idUsuarioLogado = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_graficos);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        idUsuarioLogado = getIntent().getIntExtra("id_usuario", -1);

        bindViews();
        setupInitialDate();
        setupListeners();
        
        IMarker marker = new CustomMarkerView(this, R.layout.marker_view);
        pieChart.setMarker(marker);
        barChart.setMarker(marker);
        lineChart.setMarker(marker);

        carregarGraficoPizza();
        carregarGraficoBarras();
        carregarGraficoTendencia();
    }

    private void bindViews() {
        txtMes = findViewById(R.id.txtMes);
        txtAno = findViewById(R.id.txtAno);
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        lineChart = findViewById(R.id.lineChart);
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

    private int getMesIndex(String mes) {
        String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        return Arrays.asList(nomesMes).indexOf(mes);
    }

    private void showMonthYearPicker() {
        int mesIndex = getMesIndex(txtMes.getText().toString());
        int anoSelecionado = Integer.parseInt(txtAno.getText().toString());

        MonthYearPickerDialogFragment dialogFragment = MonthYearPickerDialogFragment.getInstance(mesIndex, anoSelecionado);
        dialogFragment.setOnDateSetListener((year, monthOfYear) -> {
            String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
            txtMes.setText(nomesMes[monthOfYear]);
            txtAno.setText(String.valueOf(year));
            carregarGraficoPizza();
        });
        dialogFragment.show(getSupportFragmentManager(), null);
    }

    private void carregarGraficoPizza() {
        int ano = Integer.parseInt(txtAno.getText().toString());
        int mesIndex = getMesIndex(txtMes.getText().toString());

        List<GraficosDAO.CategoriaGasto> dadosCategorias = GraficosDAO.getDespesasPorCategoria(this, idUsuarioLogado, ano, mesIndex);

        if (dadosCategorias.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("Nenhuma despesa encontrada para este mês");
            pieChart.setNoDataTextColor(Color.WHITE);
            pieChart.invalidate();
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        for (GraficosDAO.CategoriaGasto categoria : dadosCategorias) {
            entries.add(new PieEntry(categoria.total, categoria.nome));
            try {
                colors.add(Color.parseColor(categoria.cor));
            } catch (Exception e) {
                colors.add(Color.GRAY);
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));
        dataSet.setSliceSpace(3f);

        PieData pieData = new PieData(dataSet);
        
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.getLegend().setTextColor(Color.WHITE);
        pieChart.animateY(1400);
        pieChart.invalidate(); 
    }

    private void carregarGraficoBarras() {
        List<GraficosDAO.BalancoMensal> balanco = GraficosDAO.getFluxoDeCaixaMensal(this, idUsuarioLogado);
        if (balanco.isEmpty()) {
            barChart.clear();
            barChart.setNoDataText("Não há dados suficientes para exibir o balanço.");
            barChart.setNoDataTextColor(Color.WHITE);
            barChart.invalidate();
            return;
        }

        ArrayList<BarEntry> receitasEntries = new ArrayList<>();
        ArrayList<BarEntry> despesasEntries = new ArrayList<>();
        final ArrayList<String> xAxisLabels = new ArrayList<>();

        for (int i = 0; i < balanco.size(); i++) {
            GraficosDAO.BalancoMensal item = balanco.get(i);
            receitasEntries.add(new BarEntry(i, item.receitas));
            despesasEntries.add(new BarEntry(i, item.despesas));
            xAxisLabels.add(item.mesAno);
        }

        BarDataSet receitasDataSet = new BarDataSet(receitasEntries, "Receitas");
        receitasDataSet.setColor(Color.parseColor("#006400"));

        BarDataSet despesasDataSet = new BarDataSet(despesasEntries, "Despesas");
        despesasDataSet.setColor(Color.parseColor("#E45757"));

        BarData barData = new BarData(receitasDataSet, despesasDataSet);

        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        float barWidth = 0.3f;
        barData.setBarWidth(barWidth);

        barChart.setData(barData);
        barChart.groupBars(0f, groupSpace, barSpace);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setTextColor(Color.WHITE);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setCenterAxisLabels(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(balanco.size());
        xAxis.setTextColor(Color.WHITE);

        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(1400);
        barChart.invalidate();
    }

    private void carregarGraficoTendencia() {
        List<GraficosDAO.BalancoMensal> tendencia = GraficosDAO.getTendenciaMensal(this, idUsuarioLogado);
        if (tendencia.isEmpty()) {
            lineChart.clear();
            lineChart.setNoDataText("Não há dados suficientes para exibir a tendência.");
            lineChart.setNoDataTextColor(Color.WHITE);
            lineChart.invalidate();
            return;
        }

        ArrayList<Entry> receitasEntries = new ArrayList<>();
        ArrayList<Entry> despesasEntries = new ArrayList<>();
        final ArrayList<String> xAxisLabels = new ArrayList<>();

        for (int i = 0; i < tendencia.size(); i++) {
            GraficosDAO.BalancoMensal item = tendencia.get(i);
            receitasEntries.add(new Entry(i, item.receitas));
            despesasEntries.add(new Entry(i, item.despesas));
            xAxisLabels.add(item.mesAno);
        }

        LineDataSet receitasDataSet = new LineDataSet(receitasEntries, "Receitas");
        receitasDataSet.setColor(Color.parseColor("#006400"));
        receitasDataSet.setCircleColor(Color.parseColor("#006400"));
        receitasDataSet.setLineWidth(2f);
        receitasDataSet.setCircleRadius(4f);

        LineDataSet despesasDataSet = new LineDataSet(despesasEntries, "Despesas");
        despesasDataSet.setColor(Color.parseColor("#E45757"));
        despesasDataSet.setCircleColor(Color.parseColor("#E45757"));
        despesasDataSet.setLineWidth(2f);
        despesasDataSet.setCircleRadius(4f);

        LineData lineData = new LineData(receitasDataSet, despesasDataSet);
        lineChart.setData(lineData);

        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setTextColor(Color.WHITE);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setTextColor(Color.WHITE);

        lineChart.getAxisLeft().setTextColor(Color.WHITE);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.animateX(1400);
        lineChart.invalidate();
    }
}
