package com.example.app1;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.app1.data.CartaoDAO;
import com.example.app1.data.ContaDAO;
import com.example.app1.data.TransacoesCartaoDAO;
import com.example.app1.utils.MascaraMonetaria;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;


import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelaCadCartao extends AppCompatActivity implements CartaoAdapter.OnCartaoListener {

    private LinearLayout semCartao;
    private RecyclerView listaCartoes;
    private FloatingActionButton fabAddCartao, btnAddConta;
    private LinearLayout slidingMenu;
    private View overlay;
    private FrameLayout fragmentContainerConta;

    private SharedPreferences sharedPreferences;
    private int idUsuarioLogado = -1;
    private Integer idCartaoEdicao = null;

    private CartaoAdapter adapter;
    private final List<CartaoModel> cartaoList = new ArrayList<>();
    private List<Conta> listaDeContas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_cad_cartao);

        getSupportFragmentManager().setFragmentResultListener("despesaSalvaRequest", this, (requestKey, bundle) -> {
            if (bundle.getBoolean("atualizar")) {
                carregarCartoesAsync();
            }
        });

        bindViews();
        setupSharedPreferences();
        setupRecyclerView();
        setupListeners();
        carregarDropdowns();

        carregarCartoesAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarCartoesAsync();
        carregarDropdowns(); // Refresh account list on return
    }

    private void bindViews() {
        semCartao = findViewById(R.id.semCartao);
        listaCartoes = findViewById(R.id.listaCartoes);
        fabAddCartao = findViewById(R.id.addcartao);
        overlay = findViewById(R.id.overlay);
        slidingMenu = findViewById(R.id.slidingMenu);
        btnAddConta = findViewById(R.id.btnAddConta);
        fragmentContainerConta = findViewById(R.id.fragmentContainerConta);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupSharedPreferences() {
        try {
            MasterKey masterKey = new MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            sharedPreferences = EncryptedSharedPreferences.create(this, "secure_login_prefs", masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            idUsuarioLogado = sharedPreferences.getInt("saved_user_id", -1);
        } catch (GeneralSecurityException | IOException e) {
            sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
            idUsuarioLogado = sharedPreferences.getInt("saved_user_id", -1);
        }
    }

    private void setupRecyclerView() {
        listaCartoes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartaoAdapter(this, cartaoList, this);
        listaCartoes.setAdapter(adapter);
    }

    private void setupListeners() {
        fabAddCartao.setOnClickListener(v -> abrirSlidingMenuParaEdicao(null));
        overlay.setOnClickListener(v -> closeSlidingMenu());

        btnAddConta.setOnClickListener(v -> {
            fragmentContainerConta.setVisibility(View.VISIBLE);
            MenuCadContaFragment fragment = MenuCadContaFragment.newInstance(idUsuarioLogado);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerConta, fragment)
                    .addToBackStack(null) // Add to back stack to handle back press correctly
                    .commit();
        });

        ((TextInputEditText) findViewById(R.id.inputCor)).setOnClickListener(v -> {
            new ColorPickerDialog.Builder(this)
                .setTitle("Escolha uma cor")
                .setPositiveButton("Selecionar", (ColorEnvelopeListener) (envelope, fromUser) -> {
                        TextInputEditText inputCor = findViewById(R.id.inputCor);
                        String hex = "#" + envelope.getHexCode();
                        inputCor.setText(hex);
                        inputCor.setTextColor(envelope.getColor());
                    })
                .setNegativeButton("Cancelar",
                    (dialogInterface, i) -> dialogInterface.dismiss())
                .attachAlphaSlideBar(true) 
                .attachBrightnessSlideBar(true) 
                .setBottomSpace(12) 
                .show();
        });

        ((TextInputEditText) findViewById(R.id.inputLimite)).addTextChangedListener(new MascaraMonetaria(findViewById(R.id.inputLimite)));
        findViewById(R.id.btnSalvarCartao).setOnClickListener(v -> salvarCartao());
    }

    private void carregarDropdowns() {
        // Carregar Bandeiras
        MaterialAutoCompleteTextView autoCompleteBandeira = findViewById(R.id.autoCompleteBandeira);
        String[] bandeiras = {"Visa", "Mastercard", "Elo", "American Express", "Hipercard", "Outros"};
        ArrayAdapter<String> bandeirasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bandeiras);
        autoCompleteBandeira.setAdapter(bandeirasAdapter);

        // Carregar Contas
        MaterialAutoCompleteTextView autoCompleteConta = findViewById(R.id.autoCompleteConta);
        listaDeContas = ContaDAO.carregarListaContas(this, idUsuarioLogado);
        ArrayAdapter<Conta> contasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, listaDeContas);
        autoCompleteConta.setAdapter(contasAdapter);
    }

    public void carregarCartoesAsync() {
        if (idUsuarioLogado == -1) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<CartaoModel> cartoes = CartaoDAO.buscarCartoes(this, idUsuarioLogado);
            handler.post(() -> {
                cartaoList.clear();
                cartaoList.addAll(cartoes);
                adapter.notifyDataSetChanged();
                semCartao.setVisibility(cartoes.isEmpty() ? View.VISIBLE : View.GONE);
                listaCartoes.setVisibility(cartoes.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void abrirSlidingMenuParaEdicao(@Nullable Integer idCartao) {
        this.idCartaoEdicao = idCartao;

        if (idCartao != null) {
            ((TextView) findViewById(R.id.tituloSlidingMenu)).setText("Editar Cartão de Crédito");
            ((Button) findViewById(R.id.btnSalvarCartao)).setText("Salvar Alterações");

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                CartaoModel cartao = CartaoDAO.buscarCartaoPorId(this, idCartao);
                handler.post(() -> {
                    if (cartao != null) {
                        ((TextInputEditText) findViewById(R.id.inputNomeCartao)).setText(cartao.nome);
                        ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteBandeira)).setText(cartao.bandeira, false);
                        ((TextInputEditText) findViewById(R.id.inputLimite)).setText(String.format(Locale.GERMAN, "%.2f", cartao.limite));
                        ((TextInputEditText) findViewById(R.id.inputDiaVencimento)).setText(String.valueOf(cartao.diaVencimento));
                        ((TextInputEditText) findViewById(R.id.inputDiaFechamento)).setText(String.valueOf(cartao.diaFechamento));
                        ((TextInputEditText) findViewById(R.id.inputCor)).setText(cartao.cor);
                        if (cartao.ativo == 1) {
                            ((RadioButton) findViewById(R.id.radioAtivo)).setChecked(true);
                        } else {
                            ((RadioButton) findViewById(R.id.radioDesativado)).setChecked(true);
                        }

                        // Selecionar conta no dropdown
                        MaterialAutoCompleteTextView autoCompleteConta = findViewById(R.id.autoCompleteConta);
                        for (int i = 0; i < listaDeContas.size(); i++) {
                            if (listaDeContas.get(i).getId() == cartao.idConta) {
                                autoCompleteConta.setText(listaDeContas.get(i).getNome(), false);
                                break;
                            }
                        }
                    }
                });
            });
        } else {
            ((TextView) findViewById(R.id.tituloSlidingMenu)).setText("Adicionar Cartão de Crédito");
            ((Button) findViewById(R.id.btnSalvarCartao)).setText("Salvar Cartão");
            limparCampos();
        }

        overlay.setVisibility(View.VISIBLE);
        slidingMenu.setVisibility(View.VISIBLE);
        fabAddCartao.hide();
        slidingMenu.setTranslationY(slidingMenu.getHeight());
        slidingMenu.animate().translationY(0).setDuration(300).start();
    }

    private void limparCampos() {
        ((TextInputEditText) findViewById(R.id.inputNomeCartao)).setText("");
        ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteBandeira)).setText("", false);
        ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteConta)).setText("", false);
        ((TextInputEditText) findViewById(R.id.inputLimite)).setText("");
        ((TextInputEditText) findViewById(R.id.inputDiaVencimento)).setText("");
        ((TextInputEditText) findViewById(R.id.inputDiaFechamento)).setText("");
        ((TextInputEditText) findViewById(R.id.inputCor)).setText("");
        ((RadioButton) findViewById(R.id.radioAtivo)).setChecked(true);
    }

    private void closeSlidingMenu() {
        slidingMenu.animate().translationY(slidingMenu.getHeight())
                .setDuration(300)
                .withEndAction(() -> {
                    slidingMenu.setVisibility(View.GONE);
                    overlay.setVisibility(View.GONE);
                    fabAddCartao.show();
                }).start();
    }

    private void salvarCartao() {
        String nome = ((TextInputEditText) findViewById(R.id.inputNomeCartao)).getText().toString().trim();
        String bandeira = ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteBandeira)).getText().toString().trim();
        String limiteStr = ((TextInputEditText) findViewById(R.id.inputLimite)).getText().toString().replaceAll("[^0-9,.]", "").replace(".", "").replace(",", ".");
        String vencimentoStr = ((TextInputEditText) findViewById(R.id.inputDiaVencimento)).getText().toString().trim();
        String fechamentoStr = ((TextInputEditText) findViewById(R.id.inputDiaFechamento)).getText().toString().trim();
        String cor = ((TextInputEditText) findViewById(R.id.inputCor)).getText().toString().trim();
        String nomeConta = ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteConta)).getText().toString().trim();

        RadioGroup radioGroupAtivo = findViewById(R.id.radioGroupAtivo);
        boolean ativo = radioGroupAtivo.getCheckedRadioButtonId() == R.id.radioAtivo;

        if (TextUtils.isEmpty(nome) || TextUtils.isEmpty(vencimentoStr) || TextUtils.isEmpty(fechamentoStr) || TextUtils.isEmpty(nomeConta)) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        int idConta = -1;
        for (Conta conta : listaDeContas) {
            if (conta.getNome().equals(nomeConta)) {
                idConta = conta.getId();
                break;
            }
        }

        if (idConta == -1) {
            Toast.makeText(this, "Conta inválida", Toast.LENGTH_SHORT).show();
            return;
        }

        double limite = TextUtils.isEmpty(limiteStr) ? 0.0 : Double.parseDouble(limiteStr);
        int vencimento = Integer.parseInt(vencimentoStr);
        int fechamento = Integer.parseInt(fechamentoStr);

        MeuDbHelper dbHelper = new MeuDbHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Integer fechamentoAntigo = null;
        Integer vencimentoAntigo = null;

        // Se for edição, buscar fechamento e vencimento antigos
        if (idCartaoEdicao != null) {
            Cursor cursor = db.query("cartoes",
                    new String[]{"data_fechamento_fatura", "data_vencimento_fatura"},
                    "id = ?", new String[]{String.valueOf(idCartaoEdicao)},
                    null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    fechamentoAntigo = cursor.getInt(cursor.getColumnIndexOrThrow("data_fechamento_fatura"));
                    vencimentoAntigo = cursor.getInt(cursor.getColumnIndexOrThrow("data_vencimento_fatura"));
                }
                cursor.close();
            }
        }

        ContentValues values = new ContentValues();
        values.put("nome", nome);
        values.put("bandeira", bandeira);
        values.put("limite", limite);
        values.put("data_vencimento_fatura", vencimento);
        values.put("data_fechamento_fatura", fechamento);
        values.put("cor", cor);
        values.put("ativo", ativo ? 1 : 0);
        values.put("id_usuario", idUsuarioLogado);
        values.put("id_conta", idConta);

        boolean sucesso;
        if (idCartaoEdicao == null) {
            long newRowId = db.insert("cartoes", null, values);
            sucesso = newRowId != -1;
        } else {
            int rowsAffected = db.update("cartoes", values, "id = ?", new String[]{String.valueOf(idCartaoEdicao)});
            sucesso = rowsAffected > 0;

            // Se mudou o fechamento, reprocessar as faturas
            if (sucesso && fechamentoAntigo != null && fechamentoAntigo != fechamento) {
                TransacoesCartaoDAO dao = new TransacoesCartaoDAO();
                dao.reprocessarFaturasAbertasPorFechamento(db, idCartaoEdicao, fechamentoAntigo, fechamento, vencimento);
            }
        }

        if (sucesso) {
            Toast.makeText(this, "Cartão salvo com sucesso!", Toast.LENGTH_SHORT).show();
            carregarCartoesAsync();
            closeSlidingMenu();
        } else {
            Toast.makeText(this, "Erro ao salvar o cartão.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onEditClick(int idCartao) {
        abrirSlidingMenuParaEdicao(idCartao);
    }

    @Override
    public void onDeleteClick(int idCartao) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Cartão")
                .setMessage("Tem certeza? Todas as despesas e faturas relacionadas serão removidas.")
                .setPositiveButton("Sim", (dialog, which) -> {
                    boolean sucesso = CartaoDAO.excluirCartao(this, idCartao);
                    if (sucesso) {
                        Toast.makeText(this, "Cartão excluído!", Toast.LENGTH_SHORT).show();
                        carregarCartoesAsync();
                    } else {
                        Toast.makeText(this, "Erro ao excluir.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    @Override
    public void onAddDespesaClick(int idCartao) {
        FrameLayout container = findViewById(R.id.fragmentContainerDespesa); 
        container.setVisibility(View.VISIBLE);
        MenuCadDespesaCartaoFragment despesaFragment = MenuCadDespesaCartaoFragment.newInstance(idUsuarioLogado, idCartao);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerDespesa, despesaFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onFaturaClick(int idCartao, int idUsuario) {
        Intent intent = new Intent(this, TelaFaturaCartao.class);
        intent.putExtra("id_cartao", idCartao);
        intent.putExtra("id_usuario", idUsuario);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        // If a fragment is showing, pop it from the back stack
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            fragmentContainerConta.setVisibility(View.GONE);
        } 
        // If the sliding menu is visible, close it
        else if (slidingMenu.getVisibility() == View.VISIBLE) {
            closeSlidingMenu();
        } 
        // Otherwise, perform the default back action (finish activity)
        else {
            super.onBackPressed();
        }
    }

    public static class CartaoModel {
        public int id, diaVencimento, diaFechamento, ativo, idConta, idUsuario;
        public String nome, bandeira, cor;
        public double limite, valorFaturaParcial;
    }
}
