package com.example.app1;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;

import java.util.Calendar;

public class TelaFaturaCartao extends AppCompatActivity {

    private TextView txtMes;
    private TextView txtAno;

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

        //pegar o id do cartão
        int idCartao = getIntent().getIntExtra("id_cartao", -1);
        if (idCartao != -1) {
            // use o idCartao para carregar a fatura correspondente
        }

        txtMes = findViewById(R.id.txtMes);
        txtAno = findViewById(R.id.txtAno);

        Calendar agora = Calendar.getInstance();
        String[] nomesMes = {
                "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        };
        txtMes.setText(nomesMes[agora.get(Calendar.MONTH)]);
        txtAno.setText(String.valueOf(agora.get(Calendar.YEAR)));

        LinearLayout btnMesAno = findViewById(R.id.btnMesAno);
        btnMesAno.setOnClickListener(v -> showMonthYearPicker());

        //carrega o menu inferior
        // Cria bundle com argumentos, se necessário
        //Bundle args = new Bundle();
        //args.putInt("botaoInativo", BottomMenuFragment.FATURA); // ou outro identificador para indicar o menu ativo
        BottomMenuFragment fragment = new BottomMenuFragment();
        //fragment.setArguments(args);

        // Adiciona o fragmento ao container
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.menu_container, fragment)
                .commit();

    }

    private void showMonthYearPicker() {
        // Obtém mês e ano já selecionados (exibidos nas TextViews)
        String mesSelecionado = txtMes.getText().toString();
        int anoSelecionado = Integer.parseInt(txtAno.getText().toString());

        // Lista de meses para localizar o índice
        String[] nomesMes = {
                "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        };

        // Descobre o índice do mês atual mostrado no TextView
        int mesIndex = 0;
        for (int i = 0; i < nomesMes.length; i++) {
            if (nomesMes[i].equalsIgnoreCase(mesSelecionado)) {
                mesIndex = i;
                break;
            }
        }

        // Usa o mês e ano selecionados como base no diálogo
        MonthYearPickerDialogFragment dialogFragment =
                MonthYearPickerDialogFragment.getInstance(mesIndex, anoSelecionado);

        dialogFragment.setOnDateSetListener((year, monthOfYear) -> {
            txtMes.setText(nomesMes[monthOfYear]);
            txtAno.setText(String.valueOf(year));
        });

        dialogFragment.show(getSupportFragmentManager(), "MonthYearPickerDialog");
    }

}