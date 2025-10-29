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

            // Referências iniciais às views do layout
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

            // Remove o erro do campo nome do cartão quando digita
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

            // Ajusta layout para ser edge-to-edge (contorno da tela)
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            // Inicializa SharedPreferences segura para pegar id_usuario
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
                // Fallback para SharedPreferences simples caso erro
                sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            }

            // Botão flutuante para abrir o menu deslizante de cadastro de cartão
            fabAddCartao.setOnClickListener(v -> {
                idCartaoEdicao = null; // modo novo
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

            // Fecha menu deslizante ao clicar no overlay
            overlay.setOnClickListener(v -> {
                slidingMenu.animate().translationY(slidingMenu.getHeight())
                        .setDuration(300)
                        .withEndAction(() -> {
                            slidingMenu.setVisibility(View.GONE);
                            overlay.setVisibility(View.GONE);
                            fabAddCartao.setVisibility(View.VISIBLE);
                        }).start();
            });

            // Dialogo de seleção de cor usando ColorPicker
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

            // Aplica máscara monetária no campo limite
            TextInputEditText inputLimite = findViewById(R.id.inputLimite);
            inputLimite.addTextChangedListener(new MascaraMonetaria(inputLimite));

            // Preenche o menu suspenso de bandeiras com array de strings do resources
            String[] bandeiras = getResources().getStringArray(R.array.bandeiraCartao);
            MaterialAutoCompleteTextView autoCompleteBandeira = findViewById(R.id.autoCompleteBandeira);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bandeiras);
            autoCompleteBandeira.setAdapter(arrayAdapter);

            // Preenche menu suspenso de conta para seleção, trazendo contas do banco
            MaterialAutoCompleteTextView autoCompleteConta = findViewById(R.id.autoCompleteConta);
            carregarListaContas(null);

            // Inicializa fragment para cadastro de conta com id usuário do SharedPreferences
            idUsuarioLogado = sharedPreferences.getInt(KEY_USER_ID, -1);
            menuCadContaFragment = MenuCadContaFragment.newInstance(idUsuarioLogado);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerConta, menuCadContaFragment)
                    .commit();

            // Define listener para atualizar lista de contas após cadastro
            menuCadContaFragment.setOnContaSalvaListener(nomeConta -> {
                carregarListaContas(nomeConta);
            });

            // Botão para abrir menu cadastro conta
            View btnAddConta = findViewById(R.id.btnAddConta);
            btnAddConta.setOnClickListener(v -> abrirMenuConta());

            // Se usuário não logado mostra mensagem sem cartão
            if (idUsuarioLogado == -1) {
                semCartao.setVisibility(View.VISIBLE);
                listaCartoes.setVisibility(View.GONE);
                return;
            } else {
                semCartao.setVisibility(View.GONE);
                listaCartoes.setVisibility(View.VISIBLE);

                // Preparação do banco e mostrar cartões
                MeuDbHelper dbHelper = new MeuDbHelper(this);
                db = dbHelper.getReadableDatabase();
                mostrarCartoesDoUsuarioAsync(idUsuarioLogado);

                // Botão salvar cartão - captura e valida campos, salva no banco
                Button btnSalvarCartao = findViewById(R.id.btnSalvarCartao);
                btnSalvarCartao.setOnClickListener(v -> salvarCartao(v, idCartaoEdicao));
            }
        }

        @Override
        protected void onResume() {
            super.onResume();
            // Recalcula/atualiza a lista de cartões/faturas
            mostrarCartoesDoUsuarioAsync(idUsuarioLogado);
        }

        // Calcula o intervalo da fatura com base no dia do fechamento
        private Date[] calcularIntervaloFatura(int diaFechamento) {
            Calendar hoje = Calendar.getInstance();
            if (diaFechamento <= 0) {
                Calendar inicio = (Calendar) hoje.clone();
                inicio.set(Calendar.DAY_OF_MONTH, 1);
                Calendar fim = (Calendar) hoje.clone();
                fim.set(Calendar.DAY_OF_MONTH, fim.getActualMaximum(Calendar.DAY_OF_MONTH));
                return new Date[]{inicio.getTime(), fim.getTime()};
            }

            // início: dia seguinte do fechamento do mês anterior
            Calendar inicio = (Calendar) hoje.clone();
            inicio.add(Calendar.MONTH, -1);
            int ultimoDiaMesAnterior = inicio.getActualMaximum(Calendar.DAY_OF_MONTH);
            int diaInicio = diaFechamento + 1;
            if (diaInicio > ultimoDiaMesAnterior) diaInicio = ultimoDiaMesAnterior;
            inicio.set(Calendar.DAY_OF_MONTH, diaInicio);

            // fim: dia do fechamento do mês atual
            Calendar fim = (Calendar) hoje.clone();
            int ultimoDiaMesAtual = fim.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (diaFechamento > ultimoDiaMesAtual) diaFechamento = ultimoDiaMesAtual;
            fim.set(Calendar.DAY_OF_MONTH, diaFechamento);

            return new Date[]{inicio.getTime(), fim.getTime()};
        }

        // Calcula fatura parcial somando despesas dentro do período da fatura (parcelas do mês, despesas 1x e recorrentes)
        private double calcularValorFaturaParcialComDespesasFixas(int idCartao, int diaFechamento) {
            double valorTotal = 0.0;

            MeuDbHelper dbHelper = new MeuDbHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // Obtém intervalo de datas com base no fechamento
            Date[] intervalo = calcularIntervaloFatura(diaFechamento);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dataInicio = sdf.format(intervalo[0]);
            String dataFim = sdf.format(intervalo[1]);

            // Query ajustada usando UNION e DISTINCT para evitar duplicação
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
                    String.valueOf(idCartao), dataInicio, dataFim,  // parcelas
                    String.valueOf(idCartao), dataInicio, dataFim,  // despesas à vista
                    String.valueOf(idCartao), dataFim, dataInicio, dataFim // despesas recorrentes
            };

            try (Cursor cursor = db.rawQuery(query, args)) {
                if (cursor.moveToFirst()) {
                    int idxValor = cursor.getColumnIndexOrThrow("valor_parcela");
                    do {
                        valorTotal += cursor.getDouble(idxValor);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.close();
            }

            return valorTotal;
        }

        // Calcula o valor das parcelas futuras que ainda não entraram na fatura atual
        private double calcularParcelasFuturas(int idCartao, int diaFechamento) {
            double totalParcelasFuturas = 0.0;

            // Intervalo da fatura
            Date[] intervalo = calcularIntervaloFatura(diaFechamento);
            Date fimFatura = intervalo[1]; // último dia da fatura atual
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dataFimFatura = sdf.format(fimFatura);

            // Pega apenas parcelas não pagas com vencimento após o fim da fatura
            String sql = "SELECT SUM(p.valor) AS total " +
                    "FROM parcelas_cartao p " +
                    "INNER JOIN transacoes_cartao t ON t.id = p.id_transacao_cartao " +
                    "WHERE t.id_cartao = ? " +
                    "AND p.paga = 0 " +
                    "AND p.data_vencimento > ?";

            Log.d("FaturaCar", "idCartao: " + idCartao + ", dataFimFatura: " + dataFimFatura);

            try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idCartao), dataFimFatura})) {
                if (cursor.moveToFirst()) {
                    totalParcelasFuturas = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return totalParcelasFuturas;
        }

        // Atualiza lista de cartões na UI com cálculo dos valores atualizados
        private void mostrarCartoesDoUsuarioAsync(int idUsuario) {
            Executor executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                List<CartaoModel> cartoes = new ArrayList<>();

                // Buscar dados no banco em background
                try (SQLiteDatabase dbRead = new MeuDbHelper(this).getReadableDatabase();
                     Cursor cursor = dbRead.rawQuery(
                             "SELECT id, nome, bandeira, limite, data_vencimento_fatura, data_fechamento_fatura, cor, ativo, id_conta " +
                                     "FROM cartoes WHERE id_usuario = ? " +
                                     "ORDER BY ativo DESC, nome ASC",
                             new String[] { String.valueOf(idUsuario) })) {

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

                        // Calcula fatura parcial (já inclui parcelas e despesas recorrentes)
                        double valorFaturaParcial = calcularValorFaturaParcialComDespesasFixas(c.id, c.diaFechamento);

                        // Calcula parcelas pendentes que ainda não entraram na fatura
                        double parcelasFuturas = calcularParcelasFuturas(c.id, c.diaFechamento);

                        // Limite disponível = limite do cartão - fatura parcial
                        double limiteComprometido = CartaoDAO.calcularLimiteUtilizado(this, c.id);
                        double limiteDisponivel = c.limite - limiteComprometido;
                        //double limiteDisponivel = c.limite - valorFaturaParcial - parcelasFuturas;
                        if (limiteDisponivel < 0) limiteDisponivel = 0;

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
                        Button btnFaturaCartao = item.findViewById(R.id.btnFaturaCartao);
                        LinearLayout linhaFaturaParcial = item.findViewById(R.id.linhaFaturaParcial);
                        LinearLayout linhaBarraLimite = item.findViewById(R.id.linhaBarraLimite);
                        LinearLayout linhaBotoes = item.findViewById(R.id.linhaBotoes);

                        txtNomeCartao.setText(c.nome);
                        txtValorParcial.setText(formatoBR.format(valorFaturaParcial));
                        txtLimite.setText(formatoBR.format(limiteDisponivel));
                        txtDiaVencimento.setText("Venc: " + c.diaVencimento);
                        txtDiaFechamento.setText(" | Fech: " + c.diaFechamento);

                        ImageView icMenuCartao = item.findViewById(R.id.icMenuCartao); // botão de menu do cartão

                        // Botão de menu dos cartões para editar e excluir
                        icMenuCartao.setOnClickListener(v -> {

                            // Cria o array de itens do menu usando MenuItemData
                            MenuHelper.MenuItemData[] items = new MenuHelper.MenuItemData[]{
                                    new MenuHelper.MenuItemData("Editar", R.drawable.ic_edit, () -> {
                                        // Abre o slidingMenu para edição do cartão
                                        abrirSlidingMenuParaEdicao(c.id);
                                    }),
                                    new MenuHelper.MenuItemData("Excluir", R.drawable.ic_delete, () -> {
                                        // Mostra confirmação de exclusão
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

                            // Chama o MenuHelper passando o contexto, a view de âncora e os itens
                            MenuHelper.showMenu(TelaCadCartao.this, icMenuCartao, items);
                        });

                        // Porcentagem de uso do limite
                        double porcentagem = c.limite > 0 ? ((c.limite - limiteDisponivel) / c.limite) * 100 : 0;
                        txtPorcentagem.setText(String.format("%.2f", porcentagem) + "%");
                        barraLimite.setMax(100);
                        barraLimite.setProgress((int) porcentagem);

                        // Icone da bandeira
                        if (c.bandeira != null) {
                            switch (c.bandeira.toLowerCase()) {
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

                        // Fundo com a cor do cartão
                        Drawable fundoOriginal = item.getBackground();
                        if (fundoOriginal instanceof GradientDrawable) {
                            GradientDrawable fundo = (GradientDrawable) fundoOriginal.mutate();
                            try {
                                if (c.ativo == 0) {
                                    fundo.setColor(Color.parseColor("#CCC")); // cinza para inativos
                                } else {
                                    fundo.setColor(Color.parseColor(c.cor)); // cor original para ativos
                                }
                            } catch (IllegalArgumentException e) {
                                fundo.setColor(Color.parseColor("#2F2F2F"));
                            }
                        } else {
                            try {
                                if (c.ativo == 0) {
                                    item.setBackgroundColor(Color.parseColor("#CCC")); // cinza para inativos
                                } else {
                                    item.setBackgroundColor(Color.parseColor(c.cor)); // cor original
                                }
                            } catch (IllegalArgumentException e) {
                                item.setBackgroundColor(Color.parseColor("#2F2F2F"));
                            }
                        }

                        // Botão Adicionar Despesa
                        if (c.ativo == 0) {
                            // Esconde detalhes e botões
                            linhaFaturaParcial.setVisibility(View.GONE);
                            linhaBarraLimite.setVisibility(View.GONE);
                            linhaBotoes.setVisibility(View.GONE);
                        } else {
                            // Cartão ativo - mantém tudo visível
                            linhaFaturaParcial.setVisibility(View.VISIBLE);
                            linhaBarraLimite.setVisibility(View.VISIBLE);
                            linhaBotoes.setVisibility(View.VISIBLE);

                            btnAdicionarDespesa.setOnClickListener(v -> {
                                MenuCadDespesaCartaoFragment despesaFragment = MenuCadDespesaCartaoFragment.newInstance(idUsuarioLogado, c.id);
                                despesaFragment.setOnDespesaSalvaListener(() -> mostrarCartoesDoUsuarioAsync(idUsuarioLogado));
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.fragmentContainerDespesa, despesaFragment)
                                        .commit();
                            });
                        }

                        btnFaturaCartao.setOnClickListener(v -> {
                            Intent intent = new Intent(TelaCadCartao.this, TelaFaturaCartao.class);
                            intent.putExtra("id_cartao", c.id);
                            intent.putExtra("id_usuario", idUsuarioLogado); // aqui passa o id correto
                            startActivity(intent);
                        });

                        listaCartoes.addView(item);
                    }
                    listaCartoes.setVisibility(cartoes.isEmpty() ? View.GONE : View.VISIBLE);
                });
            });
        }

        //abrir o menu para editar cartão
        private void abrirSlidingMenuParaEdicao(int idCartao) {
            // Marca que estamos em modo edição
            idCartaoEdicao = idCartao;

            // Mostra o sliding menu
            overlay.setVisibility(View.VISIBLE);
            slidingMenu.setVisibility(View.VISIBLE);
            fabAddCartao.setVisibility(View.GONE);
            slidingMenu.post(() -> {
                slidingMenu.setTranslationY(slidingMenu.getHeight());
                slidingMenu.animate().translationY(0).setDuration(300).start();
            });

            // Atualiza título e botão
            TextView tituloMenu = findViewById(R.id.tituloSlidingMenu);
            tituloMenu.setText("Editar Cartão de Crédito");
            Button btnSalvar = findViewById(R.id.btnSalvarCartao);
            btnSalvar.setText("Editar Cartão");

            // Busca dados do cartão no banco
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

            // Busca o nome da conta
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

            // Preenche campos no sliding menu
            ((TextInputEditText) findViewById(R.id.inputNomeCartao)).setText(nomeCartao);
            ((TextInputEditText) findViewById(R.id.inputLimite)).setText(String.format(Locale.getDefault(), "%.2f", limite));
            ((TextInputEditText) findViewById(R.id.inputDiaVencimento)).setText(String.valueOf(diaVencimento));
            ((TextInputEditText) findViewById(R.id.inputDiaFechamento)).setText(String.valueOf(diaFechamento));
            ((TextInputEditText) findViewById(R.id.inputCor)).setText(cor);
            ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteBandeira)).setText(bandeira, false);

            // Preenche dropdown de contas com seleção correta
            carregarListaContas(nomeContaSelecionada);

            // RadioGroup Ativo / Desativado
            ((android.widget.RadioGroup) findViewById(R.id.radioGroupAtivo))
                    .check(ativo == 1 ? R.id.radioAtivo : R.id.radioDesativado);
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

        /**
         * Carrega lista de contas do usuário para dropdown de seleção.
         * @param nomeContaSelecionada conta a ser selecionada se existir.
         */
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

        /**
         * Abre o menu para cadastro de contas.
         */
        private void abrirMenuConta() {
            findViewById(R.id.fragmentContainerConta).setVisibility(View.VISIBLE);
            menuCadContaFragment.abrirMenu();
        }

        /**
         * Fecha o menu de cadastro de contas.
         */
        private void fecharMenuConta() {
            menuCadContaFragment.fecharMenu();
            findViewById(R.id.fragmentContainerConta).setVisibility(View.GONE);
        }

        /**
         * Captura os dados do formulário, valida e salva cartão.
         */
        private void salvarCartao(View v, Integer idCartaoEdicao) {
            String nomeCartao = ((TextInputEditText) findViewById(R.id.inputNomeCartao)).getText().toString().trim();

            String limiteStr = ((TextInputEditText) findViewById(R.id.inputLimite)).getText().toString().trim();
            limiteStr = limiteStr.replace("R$", "").replaceAll("[^0-9,\\.]", "").trim(); // mantém casas decimais

            String diaVencimentoStr = ((TextInputEditText) findViewById(R.id.inputDiaVencimento)).getText().toString().trim();
            String diaFechamentoStr = ((TextInputEditText) findViewById(R.id.inputDiaFechamento)).getText().toString().trim();

            String bandeiraSelecionada = ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteBandeira)).getText().toString().trim();
            String corHex = ((TextInputEditText) findViewById(R.id.inputCor)).getText().toString().trim();
            String contaSelecionada = ((MaterialAutoCompleteTextView) findViewById(R.id.autoCompleteConta)).getText().toString().trim();

            // Valida nome do cartão
            if (nomeCartao.isEmpty()) {
                tilNomeCartao.setError("Informe o nome do cartão");
                Snackbar.make(v, "Por favor, preencha o nome do cartão", Snackbar.LENGTH_LONG).show();
                return;
            } else {
                tilNomeCartao.setError(null);
            }

            // Valida conta selecionada
            if (contaSelecionada.isEmpty() || contaSelecionada.equals("Escolha uma Conta")) {
                tilNomeConta.setError("Informe a conta do cartão");
                Snackbar.make(v, "Por favor, selecione uma conta válida", Snackbar.LENGTH_LONG).show();
                return;
            } else {
                tilNomeConta.setError(null);
            }

            // Busca id da conta selecionada no banco
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
                    limiteStr = limiteStr.replace(".", "").replace(",", "."); // converte para Double corretamente
                    limite = Double.parseDouble(limiteStr);
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
                    // Atualiza cartão existente
                    int linhas = dbWrite.update("cartoes", valores, "id = ?", new String[]{String.valueOf(idCartaoEdicao)});
                    if (linhas > 0) {
                        Toast.makeText(this, "Cartão atualizado com sucesso", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Erro ao atualizar cartão", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Insere novo cartão
                    long resultado = dbWrite.insert("cartoes", null, valores);
                    if (resultado != -1) {
                        Toast.makeText(this, "Cartão salvo com sucesso", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Erro ao salvar cartão", Toast.LENGTH_SHORT).show();
                    }
                }

                // Atualiza a lista de cartões
                mostrarCartoesDoUsuarioAsync(idUsuarioLogado);

                // Fecha sliding menu
                slidingMenu.animate().translationY(slidingMenu.getHeight())
                        .setDuration(300)
                        .withEndAction(() -> {
                            slidingMenu.setVisibility(View.GONE);
                            overlay.setVisibility(View.GONE);
                            fabAddCartao.setVisibility(View.VISIBLE);
                        }).start();

                limparCampos();
                idCartaoEdicao = null;

            } catch (Exception e) {
                e.printStackTrace();
                Snackbar.make(v, "Erro ao salvar cartão", Snackbar.LENGTH_LONG).show();
            }
        }

        /**
         * Limpa todos os campos do formulário de cadastro para estado inicial.
         */
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