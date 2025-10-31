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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import android.widget.Toast;

import com.example.app1.utils.MascaraMonetaria;
import com.example.app1.utils.MenuHelper;
import com.example.app1.data.CartaoDAO;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

    private Integer idCartaoEdicao = null; // null = adicionar, valor = editar

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

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        try {
            MasterKey masterKey = new MasterKey.Builder(getApplicationContext()).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            sharedPreferences = EncryptedSharedPreferences.create(getApplicationContext(), PREFS_NAME, masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        }

        idUsuarioLogado = sharedPreferences.getInt(KEY_USER_ID, -1);

        setupListeners();

        if (idUsuarioLogado == -1) {
            semCartao.setVisibility(View.VISIBLE);
            listaCartoes.setVisibility(View.GONE);
        } else {
            MeuDbHelper dbHelper = new MeuDbHelper(this);
            db = dbHelper.getReadableDatabase();
            mostrarCartoesDoUsuarioAsync(idUsuarioLogado);
        }
    }

    private void setupListeners() {
        fabAddCartao.setOnClickListener(v -> {
            idCartaoEdicao = null;
            ((TextView) findViewById(R.id.tituloSlidingMenu)).setText("Adicionar Cartão de Crédito");
            ((Button) findViewById(R.id.btnSalvarCartao)).setText("Salvar Cartão");

            overlay.setVisibility(View.VISIBLE);
            slidingMenu.setVisibility(View.VISIBLE);
            fabAddCartao.setVisibility(View.GONE);
            slidingMenu.post(() -> {
                slidingMenu.setTranslationY(slidingMenu.getHeight());
                slidingMenu.animate().translationY(0).setDuration(300).start();
            });
        });

        overlay.setOnClickListener(v -> closeSlidingMenu());

        inputCor.setOnClickListener(v -> {
            new ColorPickerDialog.Builder(this)
                    .setTitle("Escolha uma cor")
                    .setPositiveButton("Confirmar", (ColorEnvelopeListener) (envelope, fromUser) -> {
                        String hex = "#" + envelope.getHexCode();
                        inputCor.setText(hex);
                        inputCor.setTextColor(envelope.getColor());
                    })
                    .setNegativeButton("Cancelar", (dialogInterface, i) -> dialogInterface.dismiss())
                    .attachAlphaSlideBar(true)
                    .attachBrightnessSlideBar(true)
                    .show();
        });

        ((TextInputEditText) findViewById(R.id.inputLimite)).addTextChangedListener(new MascaraMonetaria(findViewById(R.id.inputLimite)));

        String[] bandeiras = getResources().getStringArray(R.array.bandeiraCartao);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bandeiras);
        ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteBandeira)).setAdapter(arrayAdapter);

        carregarListaContas(null);

        menuCadContaFragment = MenuCadContaFragment.newInstance(idUsuarioLogado);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerConta, menuCadContaFragment)
                .commit();

        menuCadContaFragment.setOnContaSalvaListener(this::carregarListaContas);

        findViewById(R.id.btnAddConta).setOnClickListener(v -> {
             // Lógica para abrir o menu de cadastro de conta
        });
        findViewById(R.id.btnSalvarCartao).setOnClickListener(v -> salvarCartao(v, idCartaoEdicao));
    }

    private void closeSlidingMenu() {
        slidingMenu.animate().translationY(slidingMenu.getHeight())
                .setDuration(300)
                .withEndAction(() -> {
                    slidingMenu.setVisibility(View.GONE);
                    overlay.setVisibility(View.GONE);
                    fabAddCartao.setVisibility(View.VISIBLE);
                }).start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if(idUsuarioLogado != -1){
            mostrarCartoesDoUsuarioAsync(idUsuarioLogado);
        }
    }

    private Date[] calcularIntervaloFatura(int diaFechamento) {
        Calendar hoje = Calendar.getInstance();
        if (diaFechamento <= 0) {
            Calendar inicio = (Calendar) hoje.clone();
            inicio.set(Calendar.DAY_OF_MONTH, 1);
            Calendar fim = (Calendar) hoje.clone();
            fim.set(Calendar.DAY_OF_MONTH, fim.getActualMaximum(Calendar.DAY_OF_MONTH));
            return new Date[]{inicio.getTime(), fim.getTime()};
        }

        Calendar inicio = (Calendar) hoje.clone();
        inicio.add(Calendar.MONTH, -1);
        int ultimoDiaMesAnterior = inicio.getActualMaximum(Calendar.DAY_OF_MONTH);
        int diaInicio = diaFechamento + 1;
        if (diaInicio > ultimoDiaMesAnterior) diaInicio = ultimoDiaMesAnterior;
        inicio.set(Calendar.DAY_OF_MONTH, diaInicio);

        Calendar fim = (Calendar) hoje.clone();
        int ultimoDiaMesAtual = fim.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (diaFechamento > ultimoDiaMesAtual) diaFechamento = ultimoDiaMesAtual;
        fim.set(Calendar.DAY_OF_MONTH, diaFechamento);

        return new Date[]{inicio.getTime(), fim.getTime()};
    }

    private double calcularValorFaturaParcialComDespesasFixas(int idCartao, int diaFechamento) {
        double valorTotal = 0.0;
        MeuDbHelper dbHelper = new MeuDbHelper(this);
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            Date[] intervalo = calcularIntervaloFatura(diaFechamento);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dataInicio = sdf.format(intervalo[0]);
            String dataFim = sdf.format(intervalo[1]);

            String query =
                    "SELECT DISTINCT valor_parcela FROM (" +
                            "  SELECT p.valor AS valor_parcela " +
                            "  FROM parcelas_cartao p " +
                            "  INNER JOIN transacoes_cartao t ON t.id = p.id_transacao_cartao " +
                            "  WHERE t.id_cartao = ? AND p.data_vencimento BETWEEN ? AND ? " +
                            "  UNION " +
                            "  SELECT t.valor AS valor_parcela " +
                            "  FROM transacoes_cartao t " +
                            "  WHERE t.id_cartao = ? AND t.parcelas = 1 AND (t.recorrente IS NULL OR t.recorrente = 0) AND t.data_compra BETWEEN ? AND ? " +
                            "  UNION " +
                            "  SELECT d.valor AS valor_parcela " +
                            "  FROM despesas_recorrentes_cartao d " +
                            "  INNER JOIN transacoes_cartao t ON t.id = d.id_transacao_cartao " +
                            "  WHERE t.id_cartao = ? AND d.data_inicial <= ? AND (d.data_final IS NULL OR d.data_final >= ?) " +
                            "    AND d.data_inicial = (" +
                            "       SELECT MAX(d2.data_inicial) " +
                            "       FROM despesas_recorrentes_cartao d2 " +
                            "       WHERE d2.id_transacao_cartao = d.id_transacao_cartao AND d2.data_inicial <= ?" +
                            "    )" +
                            ")";

            String[] args = new String[]{
                    String.valueOf(idCartao), dataInicio, dataFim,
                    String.valueOf(idCartao), dataInicio, dataFim,
                    String.valueOf(idCartao), dataFim, dataInicio, dataFim
            };

            try (Cursor cursor = db.rawQuery(query, args)) {
                if (cursor.moveToFirst()) {
                    int idxValor = cursor.getColumnIndexOrThrow("valor_parcela");
                    do {
                        valorTotal += cursor.getDouble(idxValor);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return valorTotal;
    }

    private double calcularParcelasFuturas(int idCartao, int diaFechamento) {
        double totalParcelasFuturas = 0.0;
        Date[] intervalo = calcularIntervaloFatura(diaFechamento);
        Date fimFatura = intervalo[1];
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dataFimFatura = sdf.format(fimFatura);

        try (SQLiteDatabase dbRead = new MeuDbHelper(this).getReadableDatabase()) {
            String sql = "SELECT SUM(p.valor) AS total " +
                    "FROM parcelas_cartao p " +
                    "INNER JOIN transacoes_cartao t ON t.id = p.id_transacao_cartao " +
                    "WHERE t.id_cartao = ? " +
                    "AND p.paga = 0 " +
                    "AND p.data_vencimento > ?";

            try (Cursor cursor = dbRead.rawQuery(sql, new String[]{String.valueOf(idCartao), dataFimFatura})) {
                if (cursor.moveToFirst()) {
                    totalParcelasFuturas = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return totalParcelasFuturas;
    }

    private void mostrarCartoesDoUsuarioAsync(int idUsuario) {
        Executor executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<CartaoModel> cartoes = new ArrayList<>();
            try (SQLiteDatabase dbRead = new MeuDbHelper(this).getReadableDatabase();
                 Cursor cursor = dbRead.rawQuery(
                         "SELECT id, nome, bandeira, limite, data_vencimento_fatura, data_fechamento_fatura, cor, ativo, id_conta " +
                                 "FROM cartoes WHERE id_usuario = ? " +
                                 "ORDER BY ativo DESC, nome ASC",
                         new String[]{String.valueOf(idUsuario)})) {

                while (cursor.moveToNext()) {
                    CartaoModel c = new CartaoModel();
                    c.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    c.nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                    c.bandeira = cursor.getString(cursor.getColumnIndexOrThrow("bandeira"));
                    c.limite = cursor.getDouble(cursor.getColumnIndexOrThrow("limite"));
                    c.diaVencimento = cursor.getInt(cursor.getColumnIndexOrThrow("data_vencimento_fatura"));
                    c.diaFechamento = cursor.getInt(cursor.getColumnIndexOrThrow("data_fechamento_fatura"));
                    c.cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                    c.ativo = cursor.getInt(cursor.getColumnIndexOrThrow("ativo"));
                    c.idConta = cursor.getInt(cursor.getColumnIndexOrThrow("id_conta"));
                    cartoes.add(c);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            handler.post(() -> {
                listaCartoes.removeAllViews();
                semCartao.setVisibility(cartoes.isEmpty() ? View.VISIBLE : View.GONE);

                LayoutInflater inflater = getLayoutInflater();
                NumberFormat formatoBR = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

                for (CartaoModel c : cartoes) {
                    double valorFaturaParcial = calcularValorFaturaParcialComDespesasFixas(c.id, c.diaFechamento);
                    double limiteComprometido = CartaoDAO.calcularLimiteUtilizado(this, c.id);
                    double limiteDisponivel = c.limite - limiteComprometido;
                    if (limiteDisponivel < 0) limiteDisponivel = 0;

                    LinearLayout item = (LinearLayout) inflater.inflate(R.layout.item_cartao, listaCartoes, false);

                    ((TextView) item.findViewById(R.id.NomeCartao)).setText(c.nome);
                    ((TextView) item.findViewById(R.id.txtValorParcial)).setText(formatoBR.format(valorFaturaParcial));
                    ((TextView) item.findViewById(R.id.txtLimite)).setText(formatoBR.format(limiteDisponivel));
                    ((TextView) item.findViewById(R.id.txtDiaVencimento)).setText("Venc: " + c.diaVencimento);
                    ((TextView) item.findViewById(R.id.txtDiaFechamento)).setText(" | Fech: " + c.diaFechamento);

                    item.findViewById(R.id.icMenuCartao).setOnClickListener(v -> {
                        MenuHelper.MenuItemData[] items = new MenuHelper.MenuItemData[]{
                                new MenuHelper.MenuItemData("Editar", R.drawable.ic_edit, () -> abrirSlidingMenuParaEdicao(c.id)),
                                new MenuHelper.MenuItemData("Excluir", R.drawable.ic_delete, () -> {
                                    new android.app.AlertDialog.Builder(TelaCadCartao.this)
                                            .setTitle("Excluir cartão")
                                            .setMessage("Tem certeza que deseja excluir este cartão? Ao confirmar, todas as despesas e faturas relacionadas a ele serão permanentemente removidas.")
                                            .setPositiveButton("Sim", (dialog, which) -> {
                                                try (SQLiteDatabase dbWrite = new MeuDbHelper(TelaCadCartao.this).getWritableDatabase()) {
                                                    int linhas = dbWrite.delete("cartoes", "id = ?", new String[]{String.valueOf(c.id)});
                                                    if (linhas > 0) {
                                                        android.widget.Toast.makeText(TelaCadCartao.this, "Cartão excluído com sucesso", android.widget.Toast.LENGTH_SHORT).show();
                                                        mostrarCartoesDoUsuarioAsync(idUsuarioLogado);
                                                    } else {
                                                        android.widget.Toast.makeText(TelaCadCartao.this, "Erro ao excluir cartão", android.widget.Toast.LENGTH_SHORT).show();
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            })
                                            .setNegativeButton("Cancelar", null)
                                            .show();
                                })
                        };
                        MenuHelper.showMenu(TelaCadCartao.this, v, items);
                    });

                    double porcentagem = c.limite > 0 ? ((c.limite - limiteDisponivel) / c.limite) * 100 : 0;
                    ((TextView) item.findViewById(R.id.txtPorcentagem)).setText(String.format("%.2f", porcentagem) + "%");
                    ((ProgressBar) item.findViewById(R.id.barraLimite)).setProgress((int) porcentagem);

                    ImageView icTipoCartao = item.findViewById(R.id.icTipoCartao);
                    if (c.bandeira != null) {
                        switch (c.bandeira.toLowerCase()) {
                            case "visa": icTipoCartao.setImageResource(R.drawable.visa); break;
                            case "mastercard": icTipoCartao.setImageResource(R.drawable.mastercard); break;
                            default: icTipoCartao.setImageResource(R.drawable.ic_credit_card_5px); break;
                        }
                    } else {
                        icTipoCartao.setImageResource(R.drawable.ic_credit_card);
                    }

                    Drawable fundoOriginal = item.getBackground();
                    if (fundoOriginal instanceof GradientDrawable) {
                        GradientDrawable fundo = (GradientDrawable) fundoOriginal.mutate();
                        try {
                            if (c.ativo == 0) {
                                fundo.setColor(Color.parseColor("#CCC"));
                            } else {
                                fundo.setColor(Color.parseColor(c.cor));
                            }
                        } catch (IllegalArgumentException e) {
                            fundo.setColor(Color.parseColor("#2F2F2F"));
                        }
                    }

                    if (c.ativo == 0) {
                        item.findViewById(R.id.linhaFaturaParcial).setVisibility(View.GONE);
                        item.findViewById(R.id.linhaBarraLimite).setVisibility(View.GONE);
                        item.findViewById(R.id.linhaBotoes).setVisibility(View.GONE);
                    } else {
                        item.findViewById(R.id.btnAdicionarDespesa).setOnClickListener(v -> {
                            MenuCadDespesaCartaoFragment despesaFragment = MenuCadDespesaCartaoFragment.newInstance(idUsuarioLogado, c.id);
                            despesaFragment.setOnDespesaSalvaListener(() -> mostrarCartoesDoUsuarioAsync(idUsuarioLogado));
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragmentContainerDespesa, despesaFragment)
                                    .addToBackStack(null)
                                    .commit();
                        });

                        item.findViewById(R.id.btnFaturaCartao).setOnClickListener(v -> {
                            Intent intent = new Intent(TelaCadCartao.this, TelaFaturaCartao.class);
                            intent.putExtra("id_cartao", c.id);
                            intent.putExtra("id_usuario", idUsuarioLogado);
                            startActivity(intent);
                        });
                    }
                    listaCartoes.addView(item);
                }
                listaCartoes.setVisibility(cartoes.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void abrirSlidingMenuParaEdicao(int idCartao) {
        idCartaoEdicao = idCartao;

        overlay.setVisibility(View.VISIBLE);
        slidingMenu.setVisibility(View.VISIBLE);
        fabAddCartao.setVisibility(View.GONE);
        slidingMenu.post(() -> {
            slidingMenu.setTranslationY(slidingMenu.getHeight());
            slidingMenu.animate().translationY(0).setDuration(300).start();
        });

        ((TextView) findViewById(R.id.tituloSlidingMenu)).setText("Editar Cartão de Crédito");
        ((Button) findViewById(R.id.btnSalvarCartao)).setText("Editar Cartão");

        String nomeCartao = "";
        String bandeira = "";
        String cor = "#000000";
        double limite = 0;
        int diaVencimento = 0;
        int diaFechamento = 0;
        int ativo = 1;
        int idConta = -1;
        String nomeContaSelecionada = "";

        try (Cursor cursor = db.rawQuery(
                "SELECT nome, bandeira, cor, limite, data_vencimento_fatura, data_fechamento_fatura, ativo, id_conta " +
                        "FROM cartoes WHERE id = ?",
                new String[]{String.valueOf(idCartao)})) {

            if (cursor.moveToFirst()) {
                nomeCartao = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                bandeira = cursor.getString(cursor.getColumnIndexOrThrow("bandeira"));
                cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                limite = cursor.getDouble(cursor.getColumnIndexOrThrow("limite"));
                diaVencimento = cursor.getInt(cursor.getColumnIndexOrThrow("data_vencimento_fatura"));
                diaFechamento = cursor.getInt(cursor.getColumnIndexOrThrow("data_fechamento_fatura"));
                ativo = cursor.getInt(cursor.getColumnIndexOrThrow("ativo"));
                idConta = cursor.getInt(cursor.getColumnIndexOrThrow("id_conta"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (idConta != -1) {
            try (Cursor cursorConta = db.rawQuery(
                    "SELECT nome FROM contas WHERE id = ?",
                    new String[]{String.valueOf(idConta)})) {
                if (cursorConta.moveToFirst()) {
                    nomeContaSelecionada = cursorConta.getString(cursorConta.getColumnIndexOrThrow("nome"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ((TextInputEditText) findViewById(R.id.inputNomeCartao)).setText(nomeCartao);
        ((TextInputEditText) findViewById(R.id.inputLimite)).setText(String.format(Locale.getDefault(), "%.2f", limite));
        ((TextInputEditText) findViewById(R.id.inputDiaVencimento)).setText(String.valueOf(diaVencimento));
        ((TextInputEditText) findViewById(R.id.inputDiaFechamento)).setText(String.valueOf(diaFechamento));
        ((TextInputEditText) findViewById(R.id.inputCor)).setText(cor);
        ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteBandeira)).setText(bandeira, false);

        carregarListaContas(nomeContaSelecionada);

        ((android.widget.RadioGroup) findViewById(R.id.radioGroupAtivo)).check(ativo == 1 ? R.id.radioAtivo : R.id.radioDesativado);
    }

    private static class CartaoModel {
        public int ativo;
        public int idConta;
        int id;
        String nome;
        String bandeira;
        double limite;
        int diaVencimento;
        int diaFechamento;
        String cor;
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

    private void abrirMenuConta() {
        // Lógica para abrir o menu de cadastro de conta precisa ser implementada
    }

    private void fecharMenuConta() {
        // Lógica para fechar o menu de cadastro de conta precisa ser implementada
    }

    private void salvarCartao(View v, Integer idCartaoEdicao) {
        String nomeCartao = ((TextInputEditText) findViewById(R.id.inputNomeCartao)).getText().toString().trim();
        String limiteStr = ((TextInputEditText) findViewById(R.id.inputLimite)).getText().toString().trim().replace("R$", "").replaceAll("[^0-9,]", "").trim();
        String diaVencimentoStr = ((TextInputEditText) findViewById(R.id.inputDiaVencimento)).getText().toString().trim();
        String diaFechamentoStr = ((TextInputEditText) findViewById(R.id.inputDiaFechamento)).getText().toString().trim();
        String bandeiraSelecionada = ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteBandeira)).getText().toString().trim();
        String corHex = ((TextInputEditText) findViewById(R.id.inputCor)).getText().toString().trim();
        String contaSelecionada = ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteConta)).getText().toString().trim();

        if (nomeCartao.isEmpty()) {
            tilNomeCartao.setError("Informe o nome do cartão");
            return;
        }
        if (contaSelecionada.isEmpty() || contaSelecionada.equals("Escolha uma Conta")) {
            tilNomeConta.setError("Informe a conta do cartão");
            return;
        }

        int idContaSelecionada = -1;
        try (SQLiteDatabase dbRead = new MeuDbHelper(this).getReadableDatabase();
             Cursor cursorConta = dbRead.rawQuery("SELECT id FROM contas WHERE id_usuario = ? AND nome = ?", new String[]{String.valueOf(idUsuarioLogado), contaSelecionada})) {
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
            if (!diaVencimentoStr.isEmpty()) diaVencimento = Integer.parseInt(diaVencimentoStr);
            if (!diaFechamentoStr.isEmpty()) diaFechamento = Integer.parseInt(diaFechamentoStr);
        } catch (NumberFormatException e) {
            Snackbar.make(v, "Campos numéricos inválidos", Snackbar.LENGTH_LONG).show();
            return;
        }

        if (corHex.isEmpty()) corHex = "#000000";
        if (bandeiraSelecionada.isEmpty()) bandeiraSelecionada = "Outros";

        int ativo = ((android.widget.RadioGroup) findViewById(R.id.radioGroupAtivo)).getCheckedRadioButtonId() == R.id.radioAtivo ? 1 : 0;

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

            if (idCartaoEdicao != null) {
                int linhas = dbWrite.update("cartoes", valores, "id = ?", new String[]{String.valueOf(idCartaoEdicao)});
                if (linhas > 0) {
                    Toast.makeText(this, "Cartão atualizado com sucesso", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Erro ao atualizar cartão", Toast.LENGTH_SHORT).show();
                }
            } else {
                long resultado = dbWrite.insert("cartoes", null, valores);
                if (resultado != -1) {
                    Toast.makeText(this, "Cartão salvo com sucesso", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Erro ao salvar cartão", Toast.LENGTH_SHORT).show();
                }
            }

            mostrarCartoesDoUsuarioAsync(idUsuarioLogado);
            closeSlidingMenu();
            limparCampos();
            idCartaoEdicao = null;

        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(v, "Erro ao salvar cartão", Snackbar.LENGTH_LONG).show();
        }
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
}
