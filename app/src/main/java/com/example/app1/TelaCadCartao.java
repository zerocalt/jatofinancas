package com.example.app1;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import android.content.SharedPreferences;

import com.example.app1.utils.MascaraMonetaria;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.Arrays;

public class TelaCadCartao extends AppCompatActivity {

    private LinearLayout semCartao;
    private LinearLayout listaCartoes;
    private ConstraintLayout mainLayout;
    private SQLiteDatabase db;
    private SharedPreferences sharedPreferences;

    private FloatingActionButton fabAddCartao;
    private LinearLayout slidingMenu;
    private View overlay;

    private static final String PREFS_NAME = "secure_login_prefs";
    private static final String KEY_USER_ID = "saved_user_id";

    private TextInputLayout tilNomeCartao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_cad_cartao);

        mainLayout = findViewById(R.id.main);
        semCartao = findViewById(R.id.semCartao);
        listaCartoes = findViewById(R.id.listaCartoes);
        fabAddCartao = findViewById(R.id.addcartao);
        overlay = findViewById(R.id.overlay);
        slidingMenu = findViewById(R.id.slidingMenu);

        tilNomeCartao = findViewById(R.id.textInputNomeCartao);

        // Ajuste para edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa SharedPreferences segura
        try {
            MasterKey masterKey = new MasterKey.Builder(getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    getApplicationContext(),
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        }

        fabAddCartao.setOnClickListener(v -> {
            overlay.setVisibility(View.VISIBLE);
            slidingMenu.setVisibility(View.VISIBLE);
            fabAddCartao.setVisibility(View.GONE);
            slidingMenu.post(() -> {
                slidingMenu.setTranslationY(slidingMenu.getHeight());
                slidingMenu.animate().translationY(0).setDuration(300).start();
            });
        });

        overlay.setOnClickListener(v -> {
            slidingMenu.animate().translationY(slidingMenu.getHeight())
                    .setDuration(300)
                    .withEndAction(() -> {
                        slidingMenu.setVisibility(View.GONE);
                        overlay.setVisibility(View.GONE);
                        fabAddCartao.setVisibility(View.VISIBLE);
                    }).start();
        });

        // Colocar máscara monetária
        TextInputEditText inputLimite = findViewById(R.id.inputLimite);
        inputLimite.addTextChangedListener(new MascaraMonetaria(inputLimite));

        // Popula menu bandeira
        String[] bandeiras = getResources().getStringArray(R.array.bandeiraCartao);
        MaterialAutoCompleteTextView autoCompleteBandeira = findViewById(R.id.autoCompleteBandeira);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bandeiras);
        autoCompleteBandeira.setAdapter(arrayAdapter);

        // Abrir palheta para cor escolha
        TextInputEditText inputCor = findViewById(R.id.inputCor);
        inputCor.setOnClickListener(v -> {
            new ColorPickerDialog.Builder(this)
                    .setTitle("Escolha uma cor")
                    .setPreferenceName("ColorPickerDialog")
                    .setPositiveButton("Confirmar", new ColorEnvelopeListener() {
                        @Override
                        public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                            String hexColor = "#" + envelope.getHexCode();
                            inputCor.setText(hexColor);
                            inputCor.setBackgroundColor(envelope.getColor());

                            int corTexto = (envelope.getColor() == Color.BLACK) ? Color.WHITE : Color.BLACK;
                            inputCor.setTextColor(corTexto);
                        }
                    })
                    .setNegativeButton("Cancelar", (dialogInterface, i) -> dialogInterface.dismiss())
                    .attachAlphaSlideBar(true)
                    .attachBrightnessSlideBar(true)
                    .show();
        });

        // Recupera id usuário logado e configura interface e listener salvar caso valido
        int idUsuarioLogado = sharedPreferences.getInt(KEY_USER_ID, -1);
        if (idUsuarioLogado == -1) {
            semCartao.setVisibility(View.VISIBLE);
            listaCartoes.setVisibility(View.GONE);
        } else {
            semCartao.setVisibility(View.GONE);
            listaCartoes.setVisibility(View.VISIBLE);

            MeuDbHelper dbHelper = new MeuDbHelper(this);
            db = dbHelper.getReadableDatabase();
            mostrarCartoesDoUsuario(idUsuarioLogado);

            Button btnSalvarCartao = findViewById(R.id.btnSalvarCartao);
            btnSalvarCartao.setOnClickListener(v -> {
                String nomeCartao = ((TextInputEditText) findViewById(R.id.inputNomeCartao)).getText().toString().trim();

                String limiteStr = ((TextInputEditText) findViewById(R.id.inputLimite)).getText().toString().trim();
                limiteStr = limiteStr.replace("R$", "").replaceAll("[^0-9,]", "").trim();

                String diaVencimentoStr = ((TextInputEditText) findViewById(R.id.inputDiaVencimento)).getText().toString().trim();
                String diaFechamentoStr = ((TextInputEditText) findViewById(R.id.inputDiaFechamento)).getText().toString().trim();

                String corHex = ((TextInputEditText) findViewById(R.id.inputCor)).getText().toString().trim();
                String bandeiraSelecionada = ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteBandeira)).getText().toString().trim();

                // Validação obrigatória do nome do cartão
                if (nomeCartao.isEmpty()) {
                    tilNomeCartao.setError("Informe o nome do cartão");
                    Snackbar.make(v, "Por favor, preencha o nome do cartão", Snackbar.LENGTH_LONG).show();
                    return;
                } else {
                    tilNomeCartao.setError(null);
                }

                double limite = 0;
                int diaVencimento = 0;
                int diaFechamento = 0;

                try {
                    if (!limiteStr.isEmpty()) {
                        limite = Double.parseDouble(limiteStr.replace(",", "."));
                    }
                    if (!diaVencimentoStr.isEmpty()) {
                        diaVencimento = Integer.parseInt(diaVencimentoStr);
                    }
                    if (!diaFechamentoStr.isEmpty()) {
                        diaFechamento = Integer.parseInt(diaFechamentoStr);
                    }
                } catch (NumberFormatException e) {
                    Snackbar.make(v, "Campos numéricos inválidos", Snackbar.LENGTH_LONG).show();
                    return;
                }

                if (corHex.isEmpty()) {
                    corHex = "#000000";
                }
                if (bandeiraSelecionada.isEmpty()) {
                    bandeiraSelecionada = "Outros";
                }

                int ativo = 0;
                int radioCheckedId = ((android.widget.RadioGroup) findViewById(R.id.radioGroupAtivo)).getCheckedRadioButtonId();
                if (radioCheckedId == R.id.radioAtivo) ativo = 1;

                try {
                    db = dbHelper.getWritableDatabase();
                    ContentValues valores = new ContentValues();
                    valores.put("nome", nomeCartao);
                    valores.put("limite", limite);
                    valores.put("data_vencimento_fatura", diaVencimento);
                    valores.put("data_fechamento_fatura", diaFechamento);
                    valores.put("cor", corHex);
                    valores.put("bandeira", bandeiraSelecionada);
                    valores.put("ativo", ativo);
                    valores.put("id_usuario", idUsuarioLogado);

                    long resultado = db.insert("cartoes", null, valores);
                    if (resultado != -1) {
                        Snackbar.make(v, "Cartão salvo com sucesso!", Snackbar.LENGTH_LONG).show();

                        mostrarCartoesDoUsuario(idUsuarioLogado);

                        slidingMenu.animate().translationY(slidingMenu.getHeight())
                                .setDuration(300)
                                .withEndAction(() -> {
                                    slidingMenu.setVisibility(View.GONE);
                                    overlay.setVisibility(View.GONE);
                                    fabAddCartao.setVisibility(View.VISIBLE);
                                }).start();

                        limparCampos();

                    } else {
                        Snackbar.make(v, "Erro ao salvar cartão.", Snackbar.LENGTH_LONG).show();
                    }
                    db.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    Snackbar.make(v, "Erro ao salvar cartão.", Snackbar.LENGTH_LONG).show();
                }
            });
        }
    }

    private void limparCampos() {
        ((TextInputEditText) findViewById(R.id.inputNomeCartao)).setText("");
        ((TextInputEditText) findViewById(R.id.inputLimite)).setText("");
        ((TextInputEditText) findViewById(R.id.inputDiaVencimento)).setText("");
        ((TextInputEditText) findViewById(R.id.inputDiaFechamento)).setText("");
        ((TextInputEditText) findViewById(R.id.inputCor)).setText("");
        ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteBandeira)).setText("");
        ((android.widget.RadioGroup) findViewById(R.id.radioGroupAtivo)).check(R.id.radioAtivo);
    }

    private void mostrarCartoesDoUsuario(int idUsuario) {
        Cursor cursor = db.rawQuery(
                "SELECT nome, bandeira FROM cartoes WHERE id_usuario = ? AND ativo = 1",
                new String[]{String.valueOf(idUsuario)}
        );

        if (cursor.getCount() == 0) {
            semCartao.setVisibility(View.VISIBLE);
            listaCartoes.setVisibility(View.GONE);
        } else {
            semCartao.setVisibility(View.GONE);
            listaCartoes.removeAllViews();

            LayoutInflater inflater = getLayoutInflater();

            while (cursor.moveToNext()) {
                String nomeCartao = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                String bandeira = cursor.getString(cursor.getColumnIndexOrThrow("bandeira"));
                LinearLayout item = (LinearLayout) inflater.inflate(R.layout.item_cartao, listaCartoes, false);
                TextView txtNomeCartao = item.findViewById(R.id.NomeCartao);
                ImageView icTipoCartao = item.findViewById(R.id.icTipoCartao);
                txtNomeCartao.setText(nomeCartao);

                // Muda o icone conforme a bandeira
                if (bandeira != null) {
                    switch (bandeira.toLowerCase()) {
                        case "visa":
                            icTipoCartao.setImageResource(R.drawable.visa);
                            break;
                        case "mastercard":
                            icTipoCartao.setImageResource(R.drawable.mastercard);
                            break;
                        default:
                            icTipoCartao.setImageResource(R.drawable.ic_credit_card_5px);
                            break;
                    }
                } else {
                    icTipoCartao.setImageResource(R.drawable.ic_credit_card);
                }

                listaCartoes.addView(item);
            }

            listaCartoes.setVisibility(View.VISIBLE);
        }
        cursor.close();
    }
}