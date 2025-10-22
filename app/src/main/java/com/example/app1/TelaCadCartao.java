package com.example.app1;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private TextInputLayout tilNomeConta;
    private TextInputEditText inputCor;

    private MenuCadContaFragment menuCadContaFragment;
    private int idUsuarioLogado = -1;

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
        inputCor = findViewById(R.id.inputCor);

        tilNomeCartao = findViewById(R.id.textInputNomeCartao);
        tilNomeConta = findViewById(R.id.menuConta);

        TextInputLayout outNomeCartao = findViewById(R.id.textInputNomeCartao);
        TextInputEditText VERnomeCartao = findViewById(R.id.inputNomeCartao);

        //verifica se os campos estão com erro, quando começar a digitar sai o erro do campo
        VERnomeCartao.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                outNomeCartao.setError(null);
                outNomeCartao.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

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

        //abre o menu de cor
        inputCor.setOnClickListener(v -> {
            new ColorPickerDialog.Builder(this)
                    .setTitle("Escolha uma cor")
                    .setPositiveButton("Confirmar", new ColorEnvelopeListener() {
                        @Override
                        public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                            String hex = "#" + envelope.getHexCode();
                            inputCor.setText(hex);
                            inputCor.setTextColor(envelope.getColor());
                        }
                    })
                    .setNegativeButton("Cancelar", (dialogInterface, i) -> dialogInterface.dismiss())
                    .attachAlphaSlideBar(true)
                    .attachBrightnessSlideBar(true)
                    .show();
        });

        // Colocar máscara monetária
        TextInputEditText inputLimite = findViewById(R.id.inputLimite);
        inputLimite.addTextChangedListener(new MascaraMonetaria(inputLimite));

        // Popula menu bandeira
        String[] bandeiras = getResources().getStringArray(R.array.bandeiraCartao);
        MaterialAutoCompleteTextView autoCompleteBandeira = findViewById(R.id.autoCompleteBandeira);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bandeiras);
        autoCompleteBandeira.setAdapter(arrayAdapter);

        // Popula menu conta com placeholder e contas do usuário
        MaterialAutoCompleteTextView autoCompleteConta = findViewById(R.id.autoCompleteConta);
        carregarListaContas(null);

        //Fragment AddConta
        idUsuarioLogado = sharedPreferences.getInt(KEY_USER_ID, -1);
        menuCadContaFragment = MenuCadContaFragment.newInstance(idUsuarioLogado);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerConta, menuCadContaFragment)
                .commit();

        menuCadContaFragment.setOnContaSalvaListener(nomeConta -> {
            carregarListaContas(nomeConta); // atualiza e seleciona nova conta
        });

        View btnAddConta = findViewById(R.id.btnAddConta);
        btnAddConta.setOnClickListener(v -> abrirMenuConta());

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

                String bandeiraSelecionada = ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteBandeira)).getText().toString().trim();
                String corHex = ((TextInputEditText) findViewById(R.id.inputCor)).getText().toString().trim();
                String contaSelecionada = autoCompleteConta.getText().toString().trim();

                // Validação obrigatória do nome do cartão
                if (nomeCartao.isEmpty()) {
                    tilNomeCartao.setError("Informe o nome do cartão");
                    Snackbar.make(v, "Por favor, preencha o nome do cartão", Snackbar.LENGTH_LONG).show();
                    return;
                } else {
                    tilNomeCartao.setError(null);
                }

                // Validação obrigatória da conta
                if (contaSelecionada.isEmpty() || contaSelecionada.equals("Escolha uma Conta")) {
                    tilNomeConta.setError("Informe a conta do cartão");
                    Snackbar.make(v, "Por favor, selecione uma conta válida", Snackbar.LENGTH_LONG).show();
                    return;
                } else {
                    tilNomeConta.setError(null);
                }

                // Recuperar id da conta selecionada
                int idContaSelecionada = -1;
                try (SQLiteDatabase dbRead = new MeuDbHelper(this).getReadableDatabase();
                     Cursor cursorConta = dbRead.rawQuery(
                             "SELECT id FROM contas WHERE id_usuario = ? AND nome = ?",
                             new String[]{String.valueOf(idUsuarioLogado), contaSelecionada})) {
                    if (cursorConta.moveToFirst()) {
                        idContaSelecionada = cursorConta.getInt(cursorConta.getColumnIndexOrThrow("id"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (idContaSelecionada == -1) {
                    Snackbar.make(v, "Conta selecionada inválida", Snackbar.LENGTH_LONG).show();
                    return;
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

                try (SQLiteDatabase dbWrite = new MeuDbHelper(this).getWritableDatabase()) {
                    ContentValues valores = new ContentValues();
                    valores.put("nome", nomeCartao);
                    valores.put("limite", limite);
                    valores.put("data_vencimento_fatura", diaVencimento);
                    valores.put("data_fechamento_fatura", diaFechamento);
                    valores.put("cor", corHex);
                    valores.put("bandeira", bandeiraSelecionada);
                    valores.put("ativo", ativo);
                    valores.put("id_usuario", idUsuarioLogado);
                    valores.put("id_conta", idContaSelecionada);

                    long resultado = dbWrite.insert("cartoes", null, valores);

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
                } catch (Exception e) {
                    e.printStackTrace();
                    Snackbar.make(v, "Erro ao salvar cartão.", Snackbar.LENGTH_LONG).show();
                }
            });
        }
    }

    private void carregarListaContas(String nomeContaSelecionada) {
        List<String> contas = new ArrayList<>();
        contas.add("Escolha uma Conta");
        int idUsuarioLogado = sharedPreferences.getInt(KEY_USER_ID, -1);
        if (idUsuarioLogado != -1) {
            MeuDbHelper dbHelper = new MeuDbHelper(this);
            try (SQLiteDatabase dbRead = dbHelper.getReadableDatabase();
                 Cursor cursorConta = dbRead.rawQuery("SELECT nome FROM contas WHERE id_usuario = ?", new String[]{String.valueOf(idUsuarioLogado)})) {
                while (cursorConta.moveToNext()) {
                    contas.add(cursorConta.getString(cursorConta.getColumnIndexOrThrow("nome")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        MaterialAutoCompleteTextView autoCompleteConta = findViewById(R.id.autoCompleteConta);
        ArrayAdapter<String> adapterConta = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, contas);
        autoCompleteConta.setAdapter(adapterConta);

        if (nomeContaSelecionada != null && contas.contains(nomeContaSelecionada)) {
            autoCompleteConta.setText(nomeContaSelecionada, false);
        } else {
            autoCompleteConta.setText(contas.get(0), false);
        }
    }

    //abre e fecha o menuAddConta
    private void abrirMenuConta() {
        findViewById(R.id.fragmentContainerConta).setVisibility(View.VISIBLE);
        menuCadContaFragment.abrirMenu();
    }

    private void fecharMenuConta() {
        menuCadContaFragment.fecharMenu();
        findViewById(R.id.fragmentContainerConta).setVisibility(View.GONE);
    }

    //fecha apenas os menus e não a tela
    @Override
    public void onBackPressed() {
        // Fecha primeiro o menu Adicionar Conta, se estiver aberto e visível
        View containerConta = findViewById(R.id.fragmentContainerConta);
        if (containerConta != null && containerConta.getVisibility() == View.VISIBLE) {
            fecharMenuConta();
            return;
        }
        // Fecha o menu Adicionar Cartão de Crédito se estiver aberto e visível
        if (slidingMenu.getVisibility() == View.VISIBLE) {
            slidingMenu.animate().translationY(slidingMenu.getHeight())
                    .setDuration(300)
                    .withEndAction(() -> {
                        slidingMenu.setVisibility(View.GONE);
                        overlay.setVisibility(View.GONE);
                        fabAddCartao.setVisibility(View.VISIBLE);
                    }).start();
            return;
        }

        // Se nenhum menu estiver aberto, chama comportamento padrão
        super.onBackPressed();
    }


    private void limparCampos() {
        ((TextInputEditText) findViewById(R.id.inputNomeCartao)).setText("");
        ((TextInputEditText) findViewById(R.id.inputLimite)).setText("");
        ((TextInputEditText) findViewById(R.id.inputDiaVencimento)).setText("");
        ((TextInputEditText) findViewById(R.id.inputDiaFechamento)).setText("");
        ((TextInputEditText) findViewById(R.id.inputCor)).setText("");
        ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteBandeira)).setText("");
        ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteConta)).setText("Escolha uma Conta", false);
        ((android.widget.RadioGroup) findViewById(R.id.radioGroupAtivo)).check(R.id.radioAtivo);
    }

    private void mostrarCartoesDoUsuario(int idUsuario) {
        Cursor cursor = db.rawQuery(
                "SELECT id, nome, bandeira, limite, data_vencimento_fatura, data_fechamento_fatura, cor " +
                        "FROM cartoes WHERE id_usuario = ? AND ativo = 1",
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
                int idCartao = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String nomeCartao = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                String bandeira = cursor.getString(cursor.getColumnIndexOrThrow("bandeira"));
                double limite = cursor.getDouble(cursor.getColumnIndexOrThrow("limite"));
                int diaVenc = cursor.getInt(cursor.getColumnIndexOrThrow("data_vencimento_fatura"));
                int diaFech = cursor.getInt(cursor.getColumnIndexOrThrow("data_fechamento_fatura"));

                double valorParcial = 0.0; // sempre zero por enquanto

                LinearLayout item = (LinearLayout) inflater.inflate(R.layout.item_cartao, listaCartoes, false);

                TextView txtNomeCartao = item.findViewById(R.id.NomeCartao);
                ImageView icTipoCartao = item.findViewById(R.id.icTipoCartao);
                TextView txtLimite = item.findViewById(R.id.txtLimite);
                TextView txtDiaVencimento = item.findViewById(R.id.txtDiaVencimento);
                TextView txtDiaFechamento = item.findViewById(R.id.txtDiaFechamento);
                TextView txtValorParcial = item.findViewById(R.id.txtValorParcial);
                TextView txtPorcentagem = item.findViewById(R.id.txtPorcentagem);
                ProgressBar barraLimite = item.findViewById(R.id.barraLimite);
                Button btnAdicionarDespesa = item.findViewById(R.id.btnAdicionarDespesa);

                NumberFormat formatoBR = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

                txtNomeCartao.setText(nomeCartao);
                txtLimite.setText(formatoBR.format(limite));
                txtDiaVencimento.setText("Venc: " + diaVenc);
                txtDiaFechamento.setText(" | Fech: " + diaFech);
                txtValorParcial.setText(formatoBR.format(valorParcial));

                double porcentagem = (limite > 0) ? (valorParcial / limite) * 100.0 : 0.0;
                txtPorcentagem.setText(String.format("%.2f", porcentagem) + "%");
                barraLimite.setMax(100);
                barraLimite.setProgress((int) porcentagem);

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

                //cor do cartao
                String corHex = cursor.getString(cursor.getColumnIndexOrThrow("cor"));


                // Pega o fundo original (que é seu drawable arredondado)
                Drawable fundoOriginal = item.getBackground();
                if (fundoOriginal instanceof GradientDrawable) {
                    GradientDrawable fundo = (GradientDrawable) fundoOriginal.mutate();
                    try {
                        // Aplica a cor no shape mantendo as bordas arredondadas
                        fundo.setColor(Color.parseColor(corHex));
                    } catch (IllegalArgumentException e) {
                        fundo.setColor(Color.parseColor("#2F2F2F")); // cor padrão
                    }
                } else {
                    // fallback caso o fundo não seja um GradientDrawable
                    try {
                        item.setBackgroundColor(Color.parseColor(corHex));
                    } catch (IllegalArgumentException e) {
                        item.setBackgroundColor(Color.parseColor("#2F2F2F"));
                    }
                }

                btnAdicionarDespesa.setOnClickListener(v -> {
                    // Cria e abre o fragmento de adicionar despesa, passando idUsuario e idCartao
                    MenuCadDespesaCartaoFragment despesaFragment = MenuCadDespesaCartaoFragment.newInstance(idUsuarioLogado, idCartao);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainerDespesa, despesaFragment)
                            .commit();
                });

                //botão para abrir a Fatura
                Button btnFaturaCartao = item.findViewById(R.id.btnFaturaCartao);
                btnFaturaCartao.setOnClickListener(v -> {
                    // Cria um intent para abrir a Activity de fatura
                    Intent intent = new Intent(TelaCadCartao.this, TelaFaturaCartao.class);

                    // Envia o ID do cartão como parâmetro
                    intent.putExtra("id_cartao", idCartao);

                    // Inicia a nova Activity
                    startActivity(intent);
                });

                listaCartoes.addView(item);
            }

            listaCartoes.setVisibility(View.VISIBLE);
        }
        cursor.close();
    }

}