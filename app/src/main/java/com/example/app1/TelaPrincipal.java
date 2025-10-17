package com.example.app1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.github.dewinjm.monthyearpicker.MonthYearPickerDialog;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;

import java.util.Calendar;

public class TelaPrincipal extends BaseActivity {
    private TextView txtMes;
    private TextView txtAno;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_principal);
        setSystemBarsColor();

        final View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right, systemBarsInsets.bottom);
            return insets;
        });

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

        Bundle args = new Bundle();
        args.putInt("botaoInativo", BottomMenuFragment.PRINCIPAL);
        BottomMenuFragment fragment = new BottomMenuFragment();
        fragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.menu_container, fragment)
                .commit();

        Button btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                try {
                    MasterKey masterKey = new MasterKey.Builder(getApplicationContext())
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build();

                    SharedPreferences prefs = EncryptedSharedPreferences.create(
                            getApplicationContext(),
                            "secure_login_prefs",
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    );

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("saved_email");
                    editor.commit();

                    Toast.makeText(this, "Sessão encerrada", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(TelaPrincipal.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("from_logout", true);
                    startActivity(intent);
                    finish();

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Erro ao limpar sessão", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(this, "Botão de logout não encontrado!", Toast.LENGTH_LONG).show();
        }
    }

    private void showMonthYearPicker() {
        Calendar calendar = Calendar.getInstance();
        int yearSelected = calendar.get(Calendar.YEAR);
        int monthSelected = calendar.get(Calendar.MONTH);

        MonthYearPickerDialogFragment dialogFragment = MonthYearPickerDialogFragment
                .getInstance(monthSelected, yearSelected);

        dialogFragment.setOnDateSetListener((year, monthOfYear) -> {
            String[] nomesMes = {
                    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
            };
            txtMes.setText(nomesMes[monthOfYear]);
            txtAno.setText(String.valueOf(year));
        });

        dialogFragment.show(getSupportFragmentManager(), "MonthYearPickerDialog");
    }
}