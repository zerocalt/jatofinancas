package com.example.app1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.app1.data.CartaoDAO;
import com.example.app1.data.TransacoesCartaoDAO; // ainda usado na CartaoModel se precisar
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelaCadCartao extends AppCompatActivity implements CartaoAdapter.OnCartaoListener {

    private LinearLayout semCartao;
    private RecyclerView listaCartoes;
    private FloatingActionButton fabAddCartao;
    private FrameLayout containerFragment;

    private SharedPreferences sharedPreferences;
    private int idUsuarioLogado = -1;

    private CartaoAdapter adapter;
    private final List<CartaoModel> cartaoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_cad_cartao);

        // Resultado vindo de fragments (CadCartaoFragment / MenuCadDespesaCartaoFragment etc.)
        getSupportFragmentManager().setFragmentResultListener("despesaSalvaRequest", this, (requestKey, bundle) -> {
            if (bundle.getBoolean("atualizar")) {
                carregarCartoesAsync();
            }
        });

        bindViews();
        setupSharedPreferences();
        setupRecyclerView();
        setupListeners();

        carregarCartoesAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarCartoesAsync();
    }

    private void bindViews() {
        semCartao = findViewById(R.id.semCartao);
        listaCartoes = findViewById(R.id.listaCartoes);
        fabAddCartao = findViewById(R.id.addcartao);
        containerFragment = findViewById(R.id.containerFragment);

        // Insets básicos (status/nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Opcional: se quiser ajustar fragment com teclado aqui também
        View container = findViewById(R.id.containerFragment);
        if (container != null) {
            ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
                boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
                int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                        v.getPaddingRight(), imeVisible ? imeHeight - 110 : 0);
                return insets;
            });
        }
    }

    private void setupSharedPreferences() {
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            sharedPreferences = EncryptedSharedPreferences.create(
                    this,
                    "secure_login_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
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
        // Novo cartão -> abre fragment de cadastro (modo inclusão)
        fabAddCartao.setOnClickListener(v -> abrirCadCartaoFragment(null));
    }

    private void abrirCadCartaoFragment(@Nullable Integer idCartaoEdicao) {
        if (idUsuarioLogado == -1) {
            Toast.makeText(this, "Usuário não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }
        containerFragment.setVisibility(View.VISIBLE);

        fragment_cad_cartao frag = fragment_cad_cartao.newInstance(idUsuarioLogado, idCartaoEdicao);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFragment, frag)
                .addToBackStack(null)
                .commit();
    }

    public void carregarCartoesAsync() {
        if (idUsuarioLogado == -1) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            List<CartaoModel> cartoes = CartaoDAO.buscarCartoes(this, idUsuarioLogado);
            runOnUiThread(() -> {
                cartaoList.clear();
                cartaoList.addAll(cartoes);
                adapter.notifyDataSetChanged();
                semCartao.setVisibility(cartoes.isEmpty() ? View.VISIBLE : View.GONE);
                listaCartoes.setVisibility(cartoes.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    // Clique editar no item do RecyclerView -> abre fragment em modo edição
    @Override
    public void onEditClick(int idCartao) {
        abrirCadCartaoFragment(idCartao);
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
        containerFragment.setVisibility(View.VISIBLE);
        MenuCadDespesaCartaoFragment despesaFragment =
                MenuCadDespesaCartaoFragment.newInstance(idUsuarioLogado, idCartao);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFragment, despesaFragment)
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
        // Se há fragment na pilha, fecha fragment e esconde container
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            containerFragment.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    // Mantém o modelo interno (se ainda for usado em outros lugares)
    public static class CartaoModel {
        public int id, diaVencimento, diaFechamento, ativo, idConta, idUsuario;
        public String nome, bandeira, cor;
        public double limite, valorFaturaParcial;
    }
}
