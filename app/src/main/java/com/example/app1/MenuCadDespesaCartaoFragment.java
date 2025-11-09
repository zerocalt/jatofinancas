package com.example.app1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.app1.data.CartaoDAO;
import com.example.app1.data.CategoriaDAO;
import com.example.app1.utils.DateUtils;
import com.example.app1.utils.MascaraMonetaria;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Fragment para cadastro/edição de despesa no cartão (atualizado para o novo esquema de BD).
 */
public class MenuCadDespesaCartaoFragment extends Fragment implements MenuCadCategoriaFragment.OnCategoriaSalvaListener {

    private static final String TAG = "MenuCadDespesaCartao";

    private View overlayDespesaCartao;
    private View slidingMenuDespesaCartao;
    private TextInputLayout tilNomeDespesaCartao, tilCategoriaDespesa, tilDataDespesaCartao, tilValorDespesa;
    private TextInputEditText inputNomeDespesaCartao, inputDataDespesaCartao, inputValorDespesa;
    private MaterialAutoCompleteTextView autoCompleteCategoria, autoCompleteCartao;
    private int idUsuarioLogado = -1;
    private int idCartao = -1;

    private Button btnSalvarDespesaCartao;
    private OnBackPressedCallback backCallback;
    private MaterialAutoCompleteTextView autoCompleteParcelas;
    private TextInputEditText inputObservacao;
    private ImageButton btnAddCategoria;

    private int idTransacaoEditando = -1;

    public static MenuCadDespesaCartaoFragment newInstance(int idUsuario, int idCartao) {
        MenuCadDespesaCartaoFragment fragment = new MenuCadDespesaCartaoFragment();
        Bundle args = new Bundle();
        args.putInt("id_usuario", idUsuario);
        args.putInt("id_cartao", idCartao);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCategoriaSalva(String nomeCategoria) {
        List<Categoria> categorias = CategoriaDAO.carregarCategorias(requireContext(), idUsuarioLogado);
        CategoriasDropdownAdapter categoriasAdapter = new CategoriasDropdownAdapter(requireContext(), categorias);
        autoCompleteCategoria.setAdapter(categoriasAdapter);

        for (int i = 0; i < categoriasAdapter.getCount(); i++) {
            Categoria item = categoriasAdapter.getItem(i);
            if (item != null && item.getNome().equals(nomeCategoria)) {
                autoCompleteCategoria.setText(item.getNome(), false);
                break;
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                fecharMenu(false);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_menu_cad_despesa_cartao, container, false);

        if (getArguments() != null) {
            idUsuarioLogado = getArguments().getInt("id_usuario", -1);
            idCartao = getArguments().getInt("id_cartao", -1);
        }

        bindViews(root);
        setupUI(root);

        return root;
    }

    private void bindViews(View root) {
        overlayDespesaCartao = root.findViewById(R.id.overlayDespesaCartao);
        slidingMenuDespesaCartao = root.findViewById(R.id.slidingMenuDespesaCartao);
        tilNomeDespesaCartao = root.findViewById(R.id.textInputNomeDespesaCartao);
        tilCategoriaDespesa = root.findViewById(R.id.textInputCategoriaDespesa);
        tilDataDespesaCartao = root.findViewById(R.id.textInputDataDespesaCartao);
        tilValorDespesa = root.findViewById(R.id.textInputValorDespesa);
        inputNomeDespesaCartao = root.findViewById(R.id.inputNomeDespesaCartao);
        inputDataDespesaCartao = root.findViewById(R.id.inputDataDespesaCartao);
        inputValorDespesa = root.findViewById(R.id.inputValorDespesa);
        autoCompleteCategoria = root.findViewById(R.id.autoCompleteCategoria);
        autoCompleteCartao = root.findViewById(R.id.autoCompleteCartao);
        inputObservacao = root.findViewById(R.id.inputObservacao);
        autoCompleteParcelas = root.findViewById(R.id.autoCompleteParcelas);
        btnSalvarDespesaCartao = root.findViewById(R.id.btnSalvarDespesaCartao);
        btnAddCategoria = root.findViewById(R.id.btnAddCategoria);
    }

    private void setupUI(View root) {
        com.google.android.material.materialswitch.MaterialSwitch switchDespesaFixa = root.findViewById(R.id.switchDespesaFixa);

        inputNomeDespesaCartao.addTextChangedListener(new SimpleTextWatcher(() -> tilNomeDespesaCartao.setError(null)));
        inputDataDespesaCartao.setOnClickListener(v -> DateUtils.openDatePicker(requireContext(), inputDataDespesaCartao));
        inputDataDespesaCartao.setFocusable(false);
        inputDataDespesaCartao.setClickable(true);

        List<Categoria> categorias = CategoriaDAO.carregarCategorias(requireContext(), idUsuarioLogado);
        CategoriasDropdownAdapter categoriasAdapter = new CategoriasDropdownAdapter(requireContext(), categorias);
        autoCompleteCategoria.setAdapter(categoriasAdapter);

        List<Cartao> cartoes = CartaoDAO.buscarCartoesAtivosPorUsuario(requireContext(), idUsuarioLogado);
        autoCompleteCartao.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, cartoes));

        if (idCartao != -1) {
            for (Cartao c : cartoes) {
                if (c.getId() == idCartao) {
                    autoCompleteCartao.setText(c.getNome(), false);
                    break;
                }
            }
        } else if (!cartoes.isEmpty()) {
            Cartao primeiro = cartoes.get(0);
            autoCompleteCartao.setText(primeiro.getNome(), false);
            idCartao = primeiro.getId();
        }

        autoCompleteCartao.setOnItemClickListener((parent, view, position, id) -> idCartao = ((Cartao) parent.getItemAtPosition(position)).getId());
        autoCompleteCategoria.setOnItemClickListener((parent, view, position, id) -> {
            Categoria categoria = (Categoria) parent.getItemAtPosition(position);
            if(categoria != null) {
                autoCompleteCategoria.setText(categoria.getNome(), false);
            }
            tilCategoriaDespesa.setError(null);
        });

        inputValorDespesa.addTextChangedListener(new MascaraMonetaria(inputValorDespesa));

        overlayDespesaCartao.setOnClickListener(v -> fecharMenu(false));
        btnSalvarDespesaCartao.setOnClickListener(v -> {
            if (idTransacaoEditando > 0) {
                atualizarDespesa(idTransacaoEditando);
            } else {
                salvarNovaDespesa();
            }
        });

        btnAddCategoria.setOnClickListener(v -> {
            FrameLayout containerCategoria = requireActivity().findViewById(R.id.fragmentContainerCategoria);
            containerCategoria.setVisibility(View.VISIBLE);

            MenuCadCategoriaFragment fragment = MenuCadCategoriaFragment.newInstance(idUsuarioLogado);
            fragment.setOnCategoriaSalvaListener(this);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerCategoria, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        inputDataDespesaCartao.setText(sdf.format(new java.util.Date()));

        ArrayAdapter<String> parcelasAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        autoCompleteParcelas.setAdapter(parcelasAdapter);

        inputValorDespesa.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                atualizarOpcoesParcelas(s.toString(), parcelasAdapter);
            }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });

        autoCompleteParcelas.setOnItemClickListener((parent, view, position, id) -> {
            int qtd = 1;
            try {
                String sel = (String) parent.getItemAtPosition(position);
                qtd = Integer.parseInt(sel.substring(0, sel.indexOf('x')).trim());
            } catch (Exception e) { /* fallback to 1 */ }
            switchDespesaFixa.setEnabled(qtd <= 1);
            if (qtd > 1) switchDespesaFixa.setChecked(false);
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (idTransacaoEditando > 0) {
            abrirMenuParaEdicao(idTransacaoEditando);
        } else {
            abrirMenu();
        }
    }

    private void atualizarOpcoesParcelas(String valorStr, ArrayAdapter<String> adapter) {
        valorStr = valorStr.replaceAll("[^0-9,.]", "").replace(".", "").replace(",", ".");
        if (valorStr.isEmpty()) {
            adapter.clear();
            autoCompleteParcelas.setText("");
            return;
        }
        try {
            double valor = Double.parseDouble(valorStr);
            NumberFormat formatoBR = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            List<String> parcelas = new ArrayList<>();
            for (int i = 1; i <= 24; i++) {
                parcelas.add(String.format(Locale.getDefault(), "%dx - %s", i, formatoBR.format(valor / i)));
            }
            adapter.clear();
            adapter.addAll(parcelas);
            adapter.notifyDataSetChanged();
            autoCompleteParcelas.setText(parcelas.get(0), false);
        } catch (Exception e) {
            adapter.clear();
            autoCompleteParcelas.setText("");
        }
    }

    public void abrirMenu() {
        overlayDespesaCartao.setVisibility(View.VISIBLE);
        slidingMenuDespesaCartao.setVisibility(View.VISIBLE);
        slidingMenuDespesaCartao.post(() -> {
            slidingMenuDespesaCartao.setTranslationY(slidingMenuDespesaCartao.getHeight());
            slidingMenuDespesaCartao.animate().translationY(0).setDuration(300).start();
            inputValorDespesa.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(inputValorDespesa, InputMethodManager.SHOW_IMPLICIT);
        });
        backCallback.setEnabled(true);
    }

    public void fecharMenu(boolean despesaSalva) {
        if (!isAdded()) return;

        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }

        if (despesaSalva) {
            Bundle result = new Bundle();
            result.putBoolean("atualizar", true);
            getParentFragmentManager().setFragmentResult("despesaSalvaRequest", result);
        }

        slidingMenuDespesaCartao.animate()
                .translationY(slidingMenuDespesaCartao.getHeight())
                .setDuration(300)
                .withEndAction(() -> {
                    if (isAdded()) {
                        overlayDespesaCartao.setVisibility(View.GONE);
                        getParentFragmentManager().beginTransaction().remove(this).commit();
                    }
                }).start();
    }

    private boolean validarCampos(ContentContainer C) {
        C.nome = inputNomeDespesaCartao.getText().toString().trim();
        C.categoriaTexto = autoCompleteCategoria.getText().toString().trim();
        C.data = inputDataDespesaCartao.getText().toString().trim();

        if (C.nome.isEmpty() || C.categoriaTexto.isEmpty() || C.data.isEmpty()) {
            if (C.nome.isEmpty()) tilNomeDespesaCartao.setError("Informe o nome");
            if (C.categoriaTexto.isEmpty()) tilCategoriaDespesa.setError("Informe a categoria");
            if (C.data.isEmpty()) tilDataDespesaCartao.setError("Informe a data");
            return false;
        }

        try {
            C.dataISO = DateUtils.converterDataParaISO(C.data) + " 00:00:00";
        } catch (ParseException e) {
            tilDataDespesaCartao.setError("Data inválida");
            return false;
        }

        String valorStr = inputValorDespesa.getText().toString().replace("R$", "").replaceAll("[^0-9,]", "").replace(".", "").replace(",", ".");
        try { C.valor = Double.parseDouble(valorStr); } catch (Exception e) { C.valor = 0; }

        if (C.valor <= 0) {
            tilValorDespesa.setError("Valor deve ser maior que zero");
            return false;
        }

        String parcelaSelecionada = autoCompleteParcelas.getText().toString();
        C.quantidadeParcelas = 1;
        if (!parcelaSelecionada.isEmpty()) {
            try { C.quantidadeParcelas = Integer.parseInt( parcelaSelecionada.substring(0, parcelaSelecionada.indexOf('x')).trim()); } catch (Exception e) { /* usa 1 */ }
        }

        C.categoria = CategoriaDAO.buscarCategoriaPorNome(requireContext(), idUsuarioLogado, C.categoriaTexto);
        if (C.categoria == null) {
            tilCategoriaDespesa.setError("Categoria inválida");
            return false;
        }

        return true;
    }

    private void salvarNovaDespesa() {
        btnSalvarDespesaCartao.setEnabled(false);
        ContentContainer C = new ContentContainer();
        if (!validarCampos(C)) {
            btnSalvarDespesaCartao.setEnabled(true);
            return;
        }

        com.google.android.material.materialswitch.MaterialSwitch switchDespesaFixa = requireView().findViewById(R.id.switchDespesaFixa);

        boolean precisaProcessarFaturas = false;

        try (MeuDbHelper helper = new MeuDbHelper(requireContext());
             SQLiteDatabase db = helper.getWritableDatabase()) {

            db.beginTransaction();
            try {
                ContentValues valores = new ContentValues();
                valores.put("descricao", C.nome);
                valores.put("id_categoria", C.categoria.getId());
                valores.put("data_compra", C.dataISO);
                valores.put("valor_total", C.valor);
                valores.put("qtd_parcelas", C.quantidadeParcelas);
                valores.put("fixa", switchDespesaFixa.isChecked() ? 1 : 0);
                valores.put("id_usuario", idUsuarioLogado);
                valores.put("id_cartao", idCartao);
                valores.put("observacao", inputObservacao.getText().toString().trim());
                valores.put("status", 1);

                long idTransacao = db.insert("transacoes_cartao", null, valores);

                if (idTransacao != -1) {
                    gerarParcelas(db, idTransacao, C, switchDespesaFixa.isChecked());
                    precisaProcessarFaturas = true;
                }

                db.setTransactionSuccessful();
                Snackbar.make(requireView(), "Despesa salva com sucesso!", Snackbar.LENGTH_LONG).show();
                fecharMenu(true);

            } catch (Exception e) {
                Log.e(TAG, "Erro durante transação de salvamento", e);
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao abrir DB", e);
        }

        // Processa faturas pendentes em thread separada (se quiser manter o GerenciadorDeFatura)
        if (precisaProcessarFaturas) {
            new Thread(() -> {
                try {
                    GerenciadorDeFatura.processarFaturasPendentes(requireContext());
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao processar faturas após salvar", e);
                }
            }).start();
        }

        btnSalvarDespesaCartao.setEnabled(true);
    }

    private void atualizarDespesa(int idTransacao) {
        btnSalvarDespesaCartao.setEnabled(false);
        ContentContainer C = new ContentContainer();
        if (!validarCampos(C)) {
            btnSalvarDespesaCartao.setEnabled(true);
            return;
        }

        com.google.android.material.materialswitch.MaterialSwitch switchDespesaFixa = requireView().findViewById(R.id.switchDespesaFixa);

        boolean precisaProcessarFaturas = false;

        try (MeuDbHelper helper = new MeuDbHelper(requireContext());
             SQLiteDatabase db = helper.getWritableDatabase()) {

            db.beginTransaction();
            try {
                ContentValues valores = new ContentValues();
                valores.put("descricao", C.nome);
                valores.put("id_categoria", C.categoria.getId());
                valores.put("data_compra", C.dataISO);
                valores.put("valor_total", C.valor);
                valores.put("qtd_parcelas", C.quantidadeParcelas);
                valores.put("fixa", switchDespesaFixa.isChecked() ? 1 : 0);
                valores.put("id_cartao", idCartao);

                db.update("transacoes_cartao", valores, "id = ?", new String[]{String.valueOf(idTransacao)});

                // remover parcelas antigas ligadas à transação e recriar
                db.delete("parcelas_cartao", "id_transacao = ?", new String[]{String.valueOf(idTransacao)});

                gerarParcelas(db, idTransacao, C, switchDespesaFixa.isChecked());

                db.setTransactionSuccessful();
                Snackbar.make(requireView(), "Despesa atualizada com sucesso!", Snackbar.LENGTH_LONG).show();
                fecharMenu(true);

                precisaProcessarFaturas = true;
            } catch (Exception e) {
                Log.e(TAG, "Erro durante transação de atualização", e);
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao abrir DB", e);
        }

        if (precisaProcessarFaturas) {
            new Thread(() -> {
                try {
                    GerenciadorDeFatura.processarFaturasPendentes(requireContext());
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao processar faturas após atualizar", e);
                }
            }).start();
        }

        btnSalvarDespesaCartao.setEnabled(true);
    }

    /**
     * Gera as parcelas no banco (parcelas_cartao).
     * Para cada parcela, associa a fatura correspondente (cria fatura se necessário)
     */
    private void gerarParcelas(SQLiteDatabase db, long idTransacao, ContentContainer C, boolean despesaFixa) {
        try {
            double valorParcela = C.valor / C.quantidadeParcelas;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(C.dataISO));

            // Pega o dia de fechamento do cartão para calcular vencimentos
            int diaFechamento = 0;
            int diaVencimento = 0;
            try (Cursor c = db.rawQuery("SELECT data_fechamento_fatura, data_vencimento_fatura FROM cartoes WHERE id = ?", new String[]{String.valueOf(idCartao)})) {
                if (c.moveToFirst()) {
                    diaFechamento = c.getInt(0);
                    diaVencimento = c.getInt(1);
                }
            }

            // A primeira parcela vence no próximo mês se compra após fechamento
            if (diaFechamento > 0 && cal.get(Calendar.DAY_OF_MONTH) > diaFechamento) {
                cal.add(Calendar.MONTH, 1);
            }

            for (int i = 1; i <= C.quantidadeParcelas; i++) {
                ContentValues pVal = new ContentValues();
                pVal.put("id_transacao", idTransacao);
                pVal.put("id_usuario", idUsuarioLogado);
                pVal.put("id_cartao", idCartao);
                pVal.put("descricao", C.nome);
                pVal.put("valor", valorParcela);
                pVal.put("num_parcela", i);
                pVal.put("total_parcelas", C.quantidadeParcelas);
                pVal.put("data_compra", C.dataISO);
                pVal.put("data_vencimento", sdf.format(cal.getTime()));
                pVal.put("paga", 0);
                pVal.put("fixa", despesaFixa ? 1 : 0);
                pVal.put("id_categoria", C.categoria.getId());
                pVal.put("observacao", inputObservacao.getText().toString().trim());

                // Determina fatura (mes/ano) a partir da data_vencimento
                Calendar venc = (Calendar) cal.clone();
                int mes = venc.get(Calendar.MONTH) + 1; // 1..12
                int ano = venc.get(Calendar.YEAR);

                long idFatura = localizarOuCriarFaturaEAtualizar(db, idCartao, mes, ano, valorParcela, pVal);

                if (idFatura != -1) {
                    pVal.put("id_fatura", idFatura);
                }

                db.insert("parcelas_cartao", null, pVal);
                cal.add(Calendar.MONTH, 1); // Próxima parcela no mês seguinte
            }

        } catch (ParseException e) {
            Log.e(TAG, "Erro de parse na data ao gerar parcelas. Data: " + C.dataISO, e);
            throw new RuntimeException("Erro ao gerar parcelas devido a data inválida.", e);
        }
    }

    /**
     * Localiza fatura por id_cartao, mes e ano. Se não existir, cria.
     * Também atualiza valor_total da fatura (somando o valorParcela).
     * Se criar a fatura, também adiciona parcelas das despesas fixas para aquela fatura.
     *
     * @param db SQLiteDatabase (dentro de transação)
     * @param idCartao cartão
     * @param mes mês (1..12)
     * @param ano ano (ex: 2025)
     * @param valorParcela valor a somar
     * @param parcelaValues ContentValues com parcela (usado ao criar fixas)
     * @return id da fatura encontrada ou criada
     */
    private long localizarOuCriarFaturaEAtualizar(SQLiteDatabase db, int idCartao, int mes, int ano, double valorParcela, ContentValues parcelaValues) {
        long idFatura = -1;
        try (Cursor c = db.rawQuery("SELECT id FROM faturas WHERE id_cartao = ? AND mes = ? AND ano = ?", new String[]{String.valueOf(idCartao), String.valueOf(mes), String.valueOf(ano)})) {
            if (c.moveToFirst()) {
                idFatura = c.getLong(0);
                db.execSQL("UPDATE faturas SET valor_total = COALESCE(valor_total,0) + ? WHERE id = ?", new Object[]{valorParcela, idFatura});
                return idFatura;
            }
        }

        // não encontrou: cria fatura (data_vencimento será o dia de vencimento do cartão nesse mes)
        // precisamos obter dia de vencimento e construir uma data_vencimento YYYY-MM-dd 00:00:00
        int diaVencimento = 1;
        try (Cursor cc = db.rawQuery("SELECT data_vencimento_fatura FROM cartoes WHERE id = ?", new String[]{String.valueOf(idCartao)})) {
            if (cc.moveToFirst()) diaVencimento = cc.getInt(0);
        }

        String dataVencimentoStr;
        try {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, ano);
            c.set(Calendar.MONTH, mes - 1);
            // ajusta diaVencimento para não exceder o maximo do mês
            int max = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            int dia = Math.min(Math.max(1, diaVencimento), max);
            c.set(Calendar.DAY_OF_MONTH, dia);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            dataVencimentoStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(c.getTime());
        } catch (Exception ex) {
            dataVencimentoStr = String.format(Locale.getDefault(), "%04d-%02d-05 00:00:00", ano, mes); // fallback
        }

        ContentValues fVals = new ContentValues();
        fVals.put("id_cartao", idCartao);
        fVals.put("mes", mes);
        fVals.put("ano", ano);
        fVals.put("data_vencimento", dataVencimentoStr);
        fVals.put("valor_total", valorParcela);
        fVals.put("status", 0);

        idFatura = db.insert("faturas", null, fVals);

        if (idFatura != -1) {
            // Ao criar a fatura devemos adicionar as despesas fixas (fixa = 1) do cartão nessa fatura.
            // Regra: para cada transacao_cartao com fixa=1 (do mesmo cartão), cria-se uma parcela simples na fatura.
            try (Cursor fixas = db.rawQuery("SELECT id, descricao, valor_total, id_categoria, observacao FROM transacoes_cartao WHERE id_cartao = ? AND fixa = 1 AND status = 1", new String[]{String.valueOf(idCartao)})) {
                while (fixas.moveToNext()) {
                    long idTransacaoFixa = fixas.getLong(fixas.getColumnIndexOrThrow("id"));
                    String desc = fixas.getString(fixas.getColumnIndexOrThrow("descricao"));
                    double valorFixa = fixas.getDouble(fixas.getColumnIndexOrThrow("valor_total"));
                    int idCat = fixas.isNull(fixas.getColumnIndexOrThrow("id_categoria")) ? 0 : fixas.getInt(fixas.getColumnIndexOrThrow("id_categoria"));
                    String obs = fixas.getString(fixas.getColumnIndexOrThrow("observacao"));

                    // Evitar duplicidade: verificar se já existe parcela fixa para esta fatura e transacao
                    try (Cursor chk = db.rawQuery("SELECT id FROM parcelas_cartao WHERE id_transacao = ? AND id_fatura = ?", new String[]{String.valueOf(idTransacaoFixa), String.valueOf(idFatura)})) {
                        if (chk.moveToFirst()) {
                            continue; // já existe
                        }
                    }

                    ContentValues pF = new ContentValues();
                    pF.put("id_transacao", idTransacaoFixa);
                    pF.put("id_usuario", idUsuarioLogado);
                    pF.put("id_cartao", idCartao);
                    pF.put("descricao", desc);
                    pF.put("valor", valorFixa);
                    pF.put("num_parcela", 1);
                    pF.put("total_parcelas", 1);
                    pF.put("data_compra", parcelaValues.getAsString("data_compra")); // usa data da parcela atual como referência
                    pF.put("data_vencimento", fVals.getAsString("data_vencimento"));
                    pF.put("fixa", 1);
                    pF.put("paga", 0);
                    if (idCat != 0) pF.put("id_categoria", idCat);
                    if (obs != null) pF.put("observacao", obs);
                    pF.put("id_fatura", idFatura);

                    db.insert("parcelas_cartao", null, pF);

                    // soma ao valor_total da fatura
                    db.execSQL("UPDATE faturas SET valor_total = COALESCE(valor_total,0) + ? WHERE id = ?", new Object[]{valorFixa, idFatura});
                }
            }

            Log.i(TAG, "Fatura criada id=" + idFatura + " para cartão " + idCartao + " mes=" + mes + " ano=" + ano);
        }

        return idFatura;
    }

    public void editarTransacao(int idTransacao) {
        this.idTransacaoEditando = idTransacao;
    }

    private void abrirMenuParaEdicao(int idTransacao) {
        ((TextView) requireView().findViewById(R.id.tituloDespesa)).setText("Editar Despesa do Cartão");
        btnSalvarDespesaCartao.setText("Salvar Alterações");

        try (MeuDbHelper helper = new MeuDbHelper(requireContext());
             SQLiteDatabase db = helper.getReadableDatabase();
             Cursor c = db.rawQuery("SELECT * FROM transacoes_cartao WHERE id = ?", new String[]{String.valueOf(idTransacao)})) {

            if (c.moveToFirst()) {
                inputNomeDespesaCartao.setText(c.getString(c.getColumnIndexOrThrow("descricao")));
                // valor_total
                double valor = c.getDouble(c.getColumnIndexOrThrow("valor_total"));
                inputValorDespesa.setText(String.format(Locale.getDefault(), "%.2f", valor));
                inputObservacao.setText(c.getString(c.getColumnIndexOrThrow("observacao")));
                inputDataDespesaCartao.setText(DateUtils.converterDataParaPtBR(c.getString(c.getColumnIndexOrThrow("data_compra"))));

                int idCategoria = c.isNull(c.getColumnIndexOrThrow("id_categoria")) ? -1 : c.getInt(c.getColumnIndexOrThrow("id_categoria"));
                if (idCategoria != -1) {
                    Categoria cat = CategoriaDAO.buscarCategoriaPorId(requireContext(), idCategoria);
                    if (cat != null) autoCompleteCategoria.setText(cat.getNome(), false);
                }

                ((com.google.android.material.materialswitch.MaterialSwitch) requireView().findViewById(R.id.switchDespesaFixa)).setChecked(c.getInt(c.getColumnIndexOrThrow("fixa")) == 1);

                int qtd = c.getInt(c.getColumnIndexOrThrow("qtd_parcelas"));
                ArrayAdapter<String> parcelasAdapter = (ArrayAdapter<String>) autoCompleteParcelas.getAdapter();
                if (parcelasAdapter != null && qtd > 0) {
                    NumberFormat formatoBR = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
                    parcelasAdapter.clear();
                    for (int i = 1; i <= 24; i++) {
                        parcelasAdapter.add(String.format(Locale.getDefault(), "%dx - %s", i, formatoBR.format(valor / i)));
                    }
                    autoCompleteParcelas.setText(String.format(Locale.getDefault(), "%dx - %s", qtd, NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(valor / qtd)), false);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar dados", e);
        }

        abrirMenu();
    }

    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable callback;
        SimpleTextWatcher(Runnable callback) { this.callback = callback; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { if (callback != null) callback.run(); }
        @Override public void afterTextChanged(android.text.Editable s) {}
    }

    private static class ContentContainer {
        String nome, categoriaTexto, data, dataISO;
        double valor;
        int quantidadeParcelas;
        Categoria categoria;
    }

}