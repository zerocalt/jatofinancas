package com.example.app1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.github.dewinjm.monthyearpicker.MonthFormat;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialog;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;

import java.util.Calendar;
import java.util.Locale;

public class TelaPrincipal extends AppCompatActivity {
    private TextView txtMes;
    private TextView txtAno;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_principal);

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
        btnMesAno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMonthYearPicker();
            }
        });

        //Carrega o Menu de Baixo
        Bundle args = new Bundle();
        args.putInt("botaoInativo", BottomMenuFragment.PRINCIPAL); // ou PRINCIPAL etc.
        BottomMenuFragment fragment = new BottomMenuFragment();
        fragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.menu_container, fragment) // menu_container é um FrameLayout na tela
                .commit();


        // Botão de Logout (seguro, sem depender de MainActivity.instancia)
        Button btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                try {
                    // Recria o MasterKey
                    MasterKey masterKey = new MasterKey.Builder(getApplicationContext())
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build();

                    // Usa o mesmo EncryptedSharedPreferences
                    SharedPreferences prefs = EncryptedSharedPreferences.create(
                            getApplicationContext(),
                            "secure_login_prefs",
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    );

                    // Remove o login
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("saved_email");
                    editor.commit(); // ou apply()

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

        dialogFragment.setOnDateSetListener(new MonthYearPickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(int year, int monthOfYear) {
                String[] nomesMes = {
                        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
                };
                txtMes.setText(nomesMes[monthOfYear]);
                txtAno.setText(String.valueOf(year));
            }
        });

        dialogFragment.show(getSupportFragmentManager(), "MonthYearPickerDialog");
    }
}