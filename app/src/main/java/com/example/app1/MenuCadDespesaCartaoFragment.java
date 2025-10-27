package com.example.app1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.app1.utils.DateUtils;
import com.example.app1.utils.MenuBottomUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.app1.utils.MascaraMonetaria;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import com.example.app1.data.CategoriaDAO;

public class MenuCadDespesaCartaoFragment extends Fragment {

    private View overlayDespesaCartao;
    private View slidingMenuDespesaCartao;
    private TextInputLayout tilNomeDespesaCartao, tilCategoriaDespesa, tilDataDespesaCartao, tilValorDespesa;
    private TextInputEditText inputNomeDespesaCartao, inputDataDespesaCartao, inputValorDespesa;
    private MaterialAutoCompleteTextView autoCompleteCategoria, autoCompleteCartao;
    private int idUsuarioLogado = -1;
    private int idCartao = -1;

    private android.widget.Button btnSalvarDespesaCartao;
    private OnBackPressedCallback backCallback;
    private MaterialAutoCompleteTextView autoCompleteParcelas;
    private TextInputEditText inputObservacao;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            idUsuarioLogado = getArguments().getInt("id_usuario", -1);
            idCartao = getArguments().getInt("id_cartao", -1);
        }

        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                fecharMenu();
                if (getActivity() != null) {
                    View container = getActivity().findViewById(R.id.rootMenuConta);
                    if (container != null) container.setVisibility(View.GONE);
                }
                setEnabled(false);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_menu_cad_despesa_cartao, container, false);

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

        // Switch de despesa fixa
        com.google.android.material.materialswitch.MaterialSwitch switchDespesaFixa = root.findViewById(R.id.switchDespesaFixa);

        inputNomeDespesaCartao.addTextChangedListener(new SimpleTextWatcher(() -> tilNomeDespesaCartao.setError(null)));
        inputDataDespesaCartao.setOnClickListener(v -> DateUtils.openDatePicker(requireContext(), inputDataDespesaCartao));
        inputDataDespesaCartao.setFocusable(false);
        inputDataDespesaCartao.setClickable(true);

        // Carrega categorias
        List<Categoria> categorias = CategoriaDAO.carregarCategorias(requireContext(), idUsuarioLogado);
        CategoriasDropdownAdapter adapter = new CategoriasDropdownAdapter(requireContext(), categorias);
        autoCompleteCategoria.setAdapter(adapter);

        // Carrega cartões
        List<String> cartoes = carregarCartoesUsuario(requireContext(), idUsuarioLogado);
        ArrayAdapter<String> adapterCartao = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, cartoes);
        autoCompleteCartao.setAdapter(adapterCartao);

        autoCompleteCategoria.setOnItemClickListener((parent, view, position, id) -> {
            Categoria c = (Categoria) parent.getItemAtPosition(position);
            autoCompleteCategoria.setText(c.getNome(), false);
            tilCategoriaDespesa.setError(null);
        });

        // Preenche o nome do cartão e bloqueia edição
        if (idCartao != -1) {
            try (SQLiteDatabase dbRead = new MeuDbHelper(requireContext()).getReadableDatabase()) {
                try (Cursor cursor = dbRead.rawQuery("SELECT nome FROM cartoes WHERE id = ?", new String[]{String.valueOf(idCartao)})) {
                    if (cursor.moveToFirst()) {
                        autoCompleteCartao.setText(cursor.getString(cursor.getColumnIndexOrThrow("nome")), false);
                    }
                }
            }
        }

        inputValorDespesa.addTextChangedListener(new MascaraMonetaria(inputValorDespesa));

        overlayDespesaCartao.setOnClickListener(v -> fecharMenu());

        btnSalvarDespesaCartao = root.findViewById(R.id.btnSalvarDespesaCartao);
        btnSalvarDespesaCartao.setOnClickListener(v -> salvarDespesa());

        // Define a data atual formatada
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        String dataAtual = sdf.format(new java.util.Date());
        inputDataDespesaCartao.setText(dataAtual);

        // Cria o adapter vazio para parcelas
        ArrayAdapter<String> parcelasAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        autoCompleteParcelas.setAdapter(parcelasAdapter);

        // Atualiza parcelas conforme o valor digitado
        inputValorDespesa.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                atualizarOpcoesParcelas(s.toString(), parcelasAdapter, autoCompleteParcelas);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });

        // Listener que desativa o switch se usuário escolher mais de 1 parcela
        autoCompleteParcelas.setOnItemClickListener((parent, view, position, id) -> {
            String parcelaSelecionada = (String) parent.getItemAtPosition(position);

            int qtd = 1;
            if (parcelaSelecionada.contains("x")) {
                try {
                    qtd = Integer.parseInt(parcelaSelecionada.substring(0, parcelaSelecionada.indexOf('x')).trim());
                } catch (Exception e) {
                    qtd = 1;
                }
            }

            if (qtd > 1) {
                switchDespesaFixa.setChecked(false);
                switchDespesaFixa.setEnabled(false);
            } else {
                switchDespesaFixa.setEnabled(true);
            }
        });

        // Botão para abrir menu cadastro categoria
        View btnAddCategoria = root.findViewById(R.id.btnAddCategoria);
        MenuCadCategoriaFragment menuCadCategoriaFragment = MenuCadCategoriaFragment.newInstance(idUsuarioLogado);

        // Listener para atualizar lista após salvar
        menuCadCategoriaFragment.setOnCategoriaSalvaListener(nomeCategoria -> {
            // Recarrega categorias
            List<Categoria> categoriasAtualizadas = CategoriaDAO.carregarCategorias(requireContext(), idUsuarioLogado);
            CategoriasDropdownAdapter novoAdapter = new CategoriasDropdownAdapter(requireContext(), categoriasAtualizadas);
            autoCompleteCategoria.setAdapter(novoAdapter);
            autoCompleteCategoria.setText(nomeCategoria, false);
            fecharMenuCategoria();
        });
        // Adiciona o fragment
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerCategoria, menuCadCategoriaFragment)
                .commitNow();
        // Abre o menu de categoria
        btnAddCategoria.setOnClickListener(v -> abrirMenuCategoria());

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        abrirMenu();

        // Se alguém pediu para abrir a edição, faz aqui
        if (idTransacaoEditando != -1) {
            abrirMenuEditarDespesa(idTransacaoEditando);
            idTransacaoEditando = -1; // reset
        }
    }

    // Novo método para setar a transação a editar
    public void editarTransacao(int idTransacao) {
        idTransacaoEditando = idTransacao;
    }

    // Função auxiliar para atualizar opções do adapter
    private void atualizarOpcoesParcelas(String valorStr, ArrayAdapter<String> adapter, MaterialAutoCompleteTextView autoComplete) {
        valorStr = valorStr.replaceAll("[^0-9,\\.]", "").trim();
        if (valorStr.isEmpty()) {
            adapter.clear();
            autoComplete.setText("");
            return;
        }

        double valor;
        try {
            valorStr = valorStr.replace(".", "");
            valor = Double.parseDouble(valorStr.replace(",", "."));
        } catch (Exception e) {
            adapter.clear();
            autoComplete.setText("");
            return;
        }

        NumberFormat formatoBR = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        ArrayList<String> parcelas = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            double resultadoParcela = valor / i;
            String parcelaStr = i + "x - " + formatoBR.format(resultadoParcela);
            parcelas.add(parcelaStr);
        }
        adapter.clear();
        adapter.addAll(parcelas);
        adapter.notifyDataSetChanged();
        autoComplete.setText(parcelas.get(0), false);
    }


    public void abrirMenu() {
        overlayDespesaCartao.setVisibility(View.VISIBLE);
        slidingMenuDespesaCartao.setVisibility(View.VISIBLE);
        slidingMenuDespesaCartao.post(() -> {
            slidingMenuDespesaCartao.setTranslationY(slidingMenuDespesaCartao.getHeight());
            slidingMenuDespesaCartao.animate().translationY(0).setDuration(300).start();

            // Solicita foco e mostra teclado
            inputValorDespesa.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(inputValorDespesa, InputMethodManager.SHOW_IMPLICIT);

            // Esconde o FAB se existir na Activity
            View fab = requireActivity().findViewById(R.id.addcartao);
            if (fab != null && fab.getVisibility() == View.VISIBLE) {
                if (fab instanceof FloatingActionButton)
                    ((FloatingActionButton) fab).hide();
                else
                    fab.setVisibility(View.GONE);
            }
        });
        backCallback.setEnabled(true);
    }

    public void fecharMenu() {
        slidingMenuDespesaCartao.animate()
                .translationY(slidingMenuDespesaCartao.getHeight())
                .setDuration(300)
                .withEndAction(() -> {
                    slidingMenuDespesaCartao.setVisibility(View.GONE);
                    overlayDespesaCartao.setVisibility(View.GONE);
                    backCallback.setEnabled(false);

                    // Reexibe o FAB se existir
                    View fab = requireActivity().findViewById(R.id.addcartao);
                    if (fab != null && fab.getVisibility() != View.VISIBLE) {
                        if (fab instanceof FloatingActionButton)
                            ((FloatingActionButton) fab).show();
                        else
                            fab.setVisibility(View.VISIBLE);
                    }
                }).start();
    }

    private void salvarDespesa() {
        String nome = inputNomeDespesaCartao.getText().toString().trim();
        String categoriaTexto = autoCompleteCategoria.getText().toString().trim();
        String observacao = inputObservacao.getText().toString().trim();
        String data = inputDataDespesaCartao.getText().toString().trim();
        String dataISO = null;
        try {
            dataISO = DateUtils.converterDataParaISO(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String parcelaSelecionada = autoCompleteParcelas.getText().toString();
        int quantidadeParcelas = 1; // padrão
        if (!parcelaSelecionada.isEmpty() && parcelaSelecionada.contains("x")) {
            try {
                quantidadeParcelas = Integer.parseInt(parcelaSelecionada.substring(0, parcelaSelecionada.indexOf('x')).trim());
            } catch (Exception e) { }
        }

        boolean despesaRecorrente = false;
        com.google.android.material.materialswitch.MaterialSwitch switchDespesaFixa = requireView().findViewById(R.id.switchDespesaFixa);
        if (switchDespesaFixa != null) {
            despesaRecorrente = switchDespesaFixa.isChecked();
        }

        // Validações
        if (nome.isEmpty()) {
            tilNomeDespesaCartao.setError("Informe o nome da despesa");
            return;
        }
        if (categoriaTexto.isEmpty()) {
            tilCategoriaDespesa.setError("Informe a categoria");
            return;
        }
        if (data.isEmpty()) {
            tilDataDespesaCartao.setError("Informe a data da despesa");
            return;
        }

        String valorStr = inputValorDespesa.getText().toString().trim();
        // remove "R$" e espaços
        valorStr = valorStr.replace("R$", "").replaceAll("[^0-9,]", "").trim();
        // substitui pontos (milhares) e converte vírgula em ponto
        valorStr = valorStr.replace(".", "").replace(",", ".");
        //valorStr = valorStr.replace("R$", "").replaceAll("[^0-9,]", "").trim();

        double valor = 0;
        try {
            if (!valorStr.isEmpty()) {
                valor = Double.parseDouble(valorStr);
            }
        } catch (NumberFormatException e) {
            Snackbar.make(requireView(), "Valor inválido", Snackbar.LENGTH_LONG).show();
            return;
        }

        if (idUsuarioLogado == -1) {
            Snackbar.make(requireView(), "Usuário inválido", Snackbar.LENGTH_LONG).show();
            return;
        }

        // Busca categoria selecionada
        Categoria categoriaSelecionada = null;
        ArrayAdapter<Categoria> adapter = (ArrayAdapter<Categoria>) autoCompleteCategoria.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            Categoria c = adapter.getItem(i);
            if (c != null && c.getNome().equals(categoriaTexto)) {
                categoriaSelecionada = c;
                break;
            }
        }
        if (categoriaSelecionada == null) {
            tilCategoriaDespesa.setError("Informe uma categoria válida");
            return;
        }

        SQLiteDatabase db = new MeuDbHelper(requireContext()).getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues valores = new ContentValues();
            valores.put("descricao", nome);
            valores.put("id_categoria", categoriaSelecionada.getId());
            valores.put("data_compra", dataISO);
            valores.put("valor", valor);
            valores.put("parcelas", quantidadeParcelas);
            valores.put("observacao", observacao);
            valores.put("id_usuario", idUsuarioLogado);
            valores.put("id_cartao", idCartao);
            valores.put("recorrente", despesaRecorrente ? 1 : 0);

            long idTransacao;
            if (idTransacaoEditando > 0) {
                // Atualiza transação existente
                idTransacao = idTransacaoEditando;
                db.update("transacoes_cartao", valores, "id = ?", new String[]{String.valueOf(idTransacaoEditando)});

                // Atualiza ou insere despesas recorrentes
                if (despesaRecorrente) {
                    ContentValues valoresRecorrentes = new ContentValues();
                    valoresRecorrentes.put("valor", valor);
                    valoresRecorrentes.put("data_inicial", dataISO);

                    Cursor cursorRecorrente = db.rawQuery(
                            "SELECT id FROM despesas_recorrentes_cartao WHERE id_transacao_cartao = ?",
                            new String[]{String.valueOf(idTransacaoEditando)});
                    if (cursorRecorrente.moveToFirst()) {
                        int idRecorrente = cursorRecorrente.getInt(cursorRecorrente.getColumnIndexOrThrow("id"));
                        db.update("despesas_recorrentes_cartao", valoresRecorrentes, "id = ?", new String[]{String.valueOf(idRecorrente)});
                    } else {
                        valoresRecorrentes.put("id_transacao_cartao", idTransacao);
                        valoresRecorrentes.putNull("data_final");
                        db.insert("despesas_recorrentes_cartao", null, valoresRecorrentes);
                    }
                    cursorRecorrente.close();
                }

                // Atualiza parcelas existentes ou cria novas
                db.delete("parcelas_cartao", "id_transacao_cartao = ?", new String[]{String.valueOf(idTransacaoEditando)});
            } else {
                // Insere nova transação
                idTransacao = db.insert("transacoes_cartao", null, valores);

                // Insere recorrentes se necessário
                if (idTransacao != -1 && despesaRecorrente) {
                    ContentValues valoresRecorrentes = new ContentValues();
                    valoresRecorrentes.put("id_transacao_cartao", idTransacao);
                    valoresRecorrentes.put("valor", valor);
                    valoresRecorrentes.put("data_inicial", dataISO);
                    valoresRecorrentes.putNull("data_final");
                    db.insert("despesas_recorrentes_cartao", null, valoresRecorrentes);
                }
            }

            // Gera parcelas
            if (idTransacao != -1 && quantidadeParcelas > 1) {
                double valorParcela = valor / quantidadeParcelas;
                java.text.SimpleDateFormat sdfIso = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                try { calendar.setTime(sdfIso.parse(dataISO)); } catch(Exception e) {}

                for (int i = 1; i <= quantidadeParcelas; i++) {
                    ContentValues valoresParcela = new ContentValues();
                    valoresParcela.put("id_transacao_cartao", idTransacao);
                    valoresParcela.put("numero_parcela", i);
                    valoresParcela.put("valor", valorParcela);
                    valoresParcela.put("paga", 0);
                    valoresParcela.putNull("id_fatura");
                    String dataVencimento = sdfIso.format(calendar.getTime());
                    valoresParcela.put("data_vencimento", dataVencimento);
                    db.insert("parcelas_cartao", null, valoresParcela);
                    calendar.add(java.util.Calendar.MONTH, 1);
                }
            }

            db.setTransactionSuccessful();
            Snackbar.make(requireView(), "Despesa salva com sucesso!", Snackbar.LENGTH_LONG).show();
            fecharMenu();
            if (listener != null) listener.onDespesaSalva();
            limparCampos();
            idTransacaoEditando = -1; // reseta para próxima inclusão
        } catch (Exception e) {
            Snackbar.make(requireView(), "Erro ao salvar despesa: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    private void limparCampos() {
        inputNomeDespesaCartao.setText("");
        inputDataDespesaCartao.setText("");
        inputValorDespesa.setText("");
        autoCompleteCategoria.setText("");
    }

    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable callback;
        public SimpleTextWatcher(Runnable callback) { this.callback = callback; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (callback != null) callback.run();
        }
        @Override public void afterTextChanged(android.text.Editable s) {}
    }

    public interface OnDespesaSalvaListener {
        void onDespesaSalva();
    }
    private OnDespesaSalvaListener listener;

    public void setOnDespesaSalvaListener(OnDespesaSalvaListener listener) {
        this.listener = listener;
    }

    // substitua o abrirMenuEditarDespesa existente por este
    public void abrirMenuEditarDespesa(int idTransacao) {
        idTransacaoEditando = idTransacao;
        TextView tituloDespesa = requireView().findViewById(R.id.tituloDespesa);
        tituloDespesa.setText("Editar Despesa do Cartão");
        btnSalvarDespesaCartao.setText("Editar Despesa");
        btnSalvarDespesaCartao.setOnClickListener(v -> atualizarDespesa(idTransacao));

        SQLiteDatabase db = new MeuDbHelper(requireContext()).getReadableDatabase();

        // Busca dados da transação
        Cursor cursor = db.rawQuery(
                "SELECT t.descricao, t.valor, t.parcelas, t.data_compra, t.id_categoria, t.observacao, t.recorrente " +
                        "FROM transacoes_cartao t WHERE t.id = ?",
                new String[]{String.valueOf(idTransacao)}
        );

        if (cursor.moveToFirst()) {
            String descricao = cursor.getString(cursor.getColumnIndexOrThrow("descricao"));
            double valor = cursor.getDouble(cursor.getColumnIndexOrThrow("valor"));
            int parcelas = cursor.getInt(cursor.getColumnIndexOrThrow("parcelas"));
            String dataCompra = cursor.getString(cursor.getColumnIndexOrThrow("data_compra"));
            int idCategoria = cursor.getInt(cursor.getColumnIndexOrThrow("id_categoria"));
            String observacao = cursor.getString(cursor.getColumnIndexOrThrow("observacao"));
            int recorrenteInt = cursor.getInt(cursor.getColumnIndexOrThrow("recorrente"));
            boolean recorrente = recorrenteInt == 1;

            // Preenche campos básicos
            inputNomeDespesaCartao.setText(descricao);
            inputValorDespesa.setText(String.format(Locale.getDefault(), "%.2f", valor));
            inputObservacao.setText(observacao);

            try {
                inputDataDespesaCartao.setText(DateUtils.converterDataParaPtBR(dataCompra));
            } catch (Exception e) {
                e.printStackTrace();
                inputDataDespesaCartao.setText("");
            }

            // 🔹 Carrega categorias antes de selecionar
            List<Categoria> categorias = CategoriaDAO.carregarCategorias(requireContext(), idUsuarioLogado);
            CategoriasDropdownAdapter adapterCategoria = new CategoriasDropdownAdapter(requireContext(), categorias);
            autoCompleteCategoria.setAdapter(adapterCategoria);

            // Seleciona categoria correta
            for (Categoria c : categorias) {
                if (c.getId() == idCategoria) {
                    autoCompleteCategoria.setText(c.getNome(), false);
                    break;
                }
            }

            // Preenche parcelas
            ArrayAdapter<String> adapterParcelas = (ArrayAdapter<String>) autoCompleteParcelas.getAdapter();
            NumberFormat formatoBR = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            atualizarOpcoesParcelas(String.valueOf(formatoBR.format(valor)), adapterParcelas, autoCompleteParcelas);
            autoCompleteParcelas.setText(parcelas + "x - " + formatoBR.format(valor / parcelas), false);

            // Preenche switch de recorrente
            com.google.android.material.materialswitch.MaterialSwitch switchDespesaFixa = requireView().findViewById(R.id.switchDespesaFixa);
            switchDespesaFixa.setChecked(recorrente);
        }

        cursor.close();
        db.close();

        abrirMenu(); // Abre o menu deslizando
    }


    private void atualizarDespesa(int idTransacaoCartao) {
        String nome = inputNomeDespesaCartao.getText().toString().trim();
        String categoriaTexto = autoCompleteCategoria.getText().toString().trim();
        String observacao = inputObservacao.getText().toString().trim();
        String data = inputDataDespesaCartao.getText().toString().trim();
        String dataISO = null;
        try { dataISO = DateUtils.converterDataParaISO(data); } catch (Exception ignored) {}

        String valorStr = inputValorDespesa.getText().toString().trim().replace("R$", "").replaceAll("[^0-9,]", "").trim();
        double valor = 0;
        try { if (!valorStr.isEmpty()) valor = Double.parseDouble(valorStr.replace(",", ".")); }
        catch (Exception ignored) {}

        String parcelaSelecionada = autoCompleteParcelas.getText().toString();
        int quantidadeParcelas = 1;
        if (!parcelaSelecionada.isEmpty() && parcelaSelecionada.contains("x")) {
            try { quantidadeParcelas = Integer.parseInt(parcelaSelecionada.substring(0, parcelaSelecionada.indexOf('x')).trim()); }
            catch (Exception ignored) {}
        }

        boolean despesaRecorrente = false;
        com.google.android.material.materialswitch.MaterialSwitch switchDespesaFixa = requireView().findViewById(R.id.switchDespesaFixa);
        if (switchDespesaFixa != null) despesaRecorrente = switchDespesaFixa.isChecked();

        if (nome.isEmpty()) { tilNomeDespesaCartao.setError("Informe o nome da despesa"); return; }
        if (categoriaTexto.isEmpty()) { tilCategoriaDespesa.setError("Informe a categoria"); return; }
        if (data.isEmpty()) { tilDataDespesaCartao.setError("Informe a data da despesa"); return; }

        Categoria categoriaSelecionada = null;
        ArrayAdapter<Categoria> adapter = (ArrayAdapter<Categoria>) autoCompleteCategoria.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            Categoria c = adapter.getItem(i);
            if (c != null && c.getNome().equals(categoriaTexto)) {
                categoriaSelecionada = c;
                break;
            }
        }
        if (categoriaSelecionada == null) { tilCategoriaDespesa.setError("Informe uma categoria válida"); return; }

        // Abre apenas UM DB
        SQLiteDatabase db = new MeuDbHelper(requireContext()).getWritableDatabase();
        db.beginTransaction();
        try {
            // FECHA recorrência antiga
            ContentValues fecharRecorrente = new ContentValues();
            java.text.SimpleDateFormat sdfIso = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dataFinal = sdfIso.format(new java.util.Date()); // ou calcula o dia do fechamento
            fecharRecorrente.put("data_final", dataFinal);
            db.update("despesas_recorrentes_cartao", fecharRecorrente,
                    "id_transacao_cartao = ? AND data_final IS NULL",
                    new String[]{String.valueOf(idTransacaoCartao)});

            // ATUALIZA transação
            ContentValues valores = new ContentValues();
            valores.put("descricao", nome);
            valores.put("id_categoria", categoriaSelecionada.getId());
            valores.put("data_compra", dataISO);
            valores.put("valor", valor);
            valores.put("parcelas", quantidadeParcelas);
            valores.put("observacao", observacao);
            valores.put("recorrente", despesaRecorrente ? 1 : 0);
            valores.put("id_cartao", idCartao);
            db.update("transacoes_cartao", valores, "id = ?", new String[]{String.valueOf(idTransacaoCartao)});

            // CRIA nova recorrência
            if (despesaRecorrente) {
                ContentValues novaRecorrente = new ContentValues();
                novaRecorrente.put("id_transacao_cartao", idTransacaoCartao);
                novaRecorrente.put("valor", valor);
                novaRecorrente.put("data_inicial", sdfIso.format(new java.util.Date()));
                novaRecorrente.putNull("data_final");
                db.insert("despesas_recorrentes_cartao", null, novaRecorrente);
            }

            // ATUALIZA parcelas apenas se for mais de 1x
            db.delete("parcelas_cartao", "id_transacao_cartao = ?", new String[]{String.valueOf(idTransacaoCartao)});
            if (quantidadeParcelas > 1) {
                double valorParcela = valor / quantidadeParcelas;
                java.util.Calendar cal = java.util.Calendar.getInstance();
                try { cal.setTime(sdfIso.parse(dataISO)); } catch (Exception ignored) {}

                for (int i = 1; i <= quantidadeParcelas; i++) {
                    ContentValues valoresParcela = new ContentValues();
                    valoresParcela.put("id_transacao_cartao", idTransacaoCartao);
                    valoresParcela.put("numero_parcela", i);
                    valoresParcela.put("valor", valorParcela);
                    valoresParcela.put("paga", 0);
                    valoresParcela.putNull("id_fatura");
                    valoresParcela.put("data_vencimento", sdfIso.format(cal.getTime()));
                    db.insert("parcelas_cartao", null, valoresParcela);
                    cal.add(java.util.Calendar.MONTH, 1);
                }
            }

            db.setTransactionSuccessful();
            Snackbar.make(requireView(), "Despesa atualizada com sucesso!", Snackbar.LENGTH_LONG).show();
            fecharMenu();
            if (listener != null) listener.onDespesaSalva();
            limparCampos();

        } catch (Exception e) {
            Snackbar.make(requireView(), "Erro ao atualizar despesa: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    private List<String> carregarCartoesUsuario(Context ctx, int idUsuario) {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT id, nome FROM cartoes WHERE id_usuario = ? AND ativo = 1 ORDER BY nome COLLATE NOCASE ASC";
        try (SQLiteDatabase db = new MeuDbHelper(ctx).getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idUsuario)})) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                    lista.add(nome);
                } while (cursor.moveToNext());
            }
        }
        return lista;
    }

    private void abrirMenuCategoria() {
        View container = requireView().findViewById(R.id.fragmentContainerCategoria);
        container.setVisibility(View.VISIBLE);
        MenuCadCategoriaFragment frag = (MenuCadCategoriaFragment)
                getChildFragmentManager().findFragmentById(R.id.fragmentContainerCategoria);
        if (frag != null) frag.abrirMenu();
    }

    private void fecharMenuCategoria() {
        MenuCadCategoriaFragment frag = (MenuCadCategoriaFragment)
                getChildFragmentManager().findFragmentById(R.id.fragmentContainerCategoria);
        if (frag != null) frag.fecharMenu();
        View container = requireView().findViewById(R.id.fragmentContainerCategoria);
        container.setVisibility(View.GONE);
    }

}