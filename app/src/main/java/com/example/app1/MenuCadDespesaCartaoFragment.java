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

public class MenuCadDespesaCartaoFragment extends Fragment implements MenuCadCategoriaFragment.OnCategoriaSalvaListener {

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

        try (SQLiteDatabase db = new MeuDbHelper(requireContext()).getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues valores = C.toContentValues();
                valores.put("recorrente", switchDespesaFixa.isChecked() ? 1 : 0);
                valores.put("id_usuario", idUsuarioLogado);
                valores.put("id_cartao", idCartao);
                valores.put("observacao", inputObservacao.getText().toString().trim());

                long idTransacao = db.insert("transacoes_cartao", null, valores);

                if (idTransacao != -1) {
                    gerarParcelas(db, idTransacao, C.valor, C.quantidadeParcelas, C.dataISO);
                    GerenciadorDeFatura.processarFaturasPendentes(requireContext());
                }

                db.setTransactionSuccessful();
                Snackbar.make(requireView(), "Despesa salva com sucesso!", Snackbar.LENGTH_LONG).show();
                fecharMenu(true);

            } catch (Exception e) {
                Log.e("SalvarDespesa", "Erro durante transação de salvamento", e);
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e("SalvarDespesa", "Erro ao abrir DB", e);
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

        try (SQLiteDatabase db = new MeuDbHelper(requireContext()).getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues valores = C.toContentValues();
                valores.put("recorrente", switchDespesaFixa.isChecked() ? 1 : 0);
                valores.put("id_cartao", idCartao);

                db.update("transacoes_cartao", valores, "id = ?", new String[]{String.valueOf(idTransacao)});
                db.delete("parcelas_cartao", "id_transacao_cartao = ?", new String[]{String.valueOf(idTransacao)});

                gerarParcelas(db, idTransacao, C.valor, C.quantidadeParcelas, C.dataISO);

                db.setTransactionSuccessful();
                GerenciadorDeFatura.processarFaturasPendentes(requireContext()); 
                Snackbar.make(requireView(), "Despesa atualizada com sucesso!", Snackbar.LENGTH_LONG).show();
                fecharMenu(true);

            } catch (Exception e) {
                Log.e("AtualizarDespesa", "Erro durante transação de atualização", e);
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e("AtualizarDespesa", "Erro ao abrir DB", e);
        }
        btnSalvarDespesaCartao.setEnabled(true);
    }
    
    private void gerarParcelas(SQLiteDatabase db, long idTransacao, double valorTotal, int qtdParcelas, String dataISO) {
       try {
            double valorParcela = valorTotal / qtdParcelas;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(dataISO));
            
            // Pega o dia de fechamento do cartão para calcular vencimentos
            int diaFechamento = 0;
            try (Cursor c = db.rawQuery("SELECT data_fechamento_fatura FROM cartoes WHERE id = ?", new String[]{String.valueOf(idCartao)})) {
                if (c.moveToFirst()) {
                    diaFechamento = c.getInt(0);
                }
            }

            // A primeira parcela vence no próximo mês, a menos que a compra seja feita no dia do fechamento
            if (cal.get(Calendar.DAY_OF_MONTH) > diaFechamento) {
                cal.add(Calendar.MONTH, 1);
            }

            for (int i = 1; i <= qtdParcelas; i++) {
                ContentValues pVal = new ContentValues();
                pVal.put("id_transacao_cartao", idTransacao);
                pVal.put("numero_parcela", i);
                pVal.put("valor", valorParcela);
                pVal.put("paga", 0);
                pVal.putNull("id_fatura");
                pVal.put("data_vencimento", sdf.format(cal.getTime()));
                db.insert("parcelas_cartao", null, pVal);
                cal.add(Calendar.MONTH, 1); // Próxima parcela no mês seguinte
            }
        } catch (ParseException e) {
            Log.e("GerarParcelas", "Erro de parse na data ao gerar parcelas. Data: " + dataISO, e);
            throw new RuntimeException("Erro ao gerar parcelas devido a data inválida.", e);
        }
    }

    public void editarTransacao(int idTransacao) {
        this.idTransacaoEditando = idTransacao;
    }

    private void abrirMenuParaEdicao(int idTransacao) {
        ((TextView) requireView().findViewById(R.id.tituloDespesa)).setText("Editar Despesa do Cartão");
        btnSalvarDespesaCartao.setText("Salvar Alterações");
        
        try (SQLiteDatabase db = new MeuDbHelper(requireContext()).getReadableDatabase();
             Cursor c = db.rawQuery("SELECT * FROM transacoes_cartao WHERE id = ?", new String[]{String.valueOf(idTransacao)})) {
            
            if (c.moveToFirst()) {
                inputNomeDespesaCartao.setText(c.getString(c.getColumnIndexOrThrow("descricao")));
                inputValorDespesa.setText(String.format(Locale.getDefault(), "%.2f", c.getDouble(c.getColumnIndexOrThrow("valor"))));
                inputObservacao.setText(c.getString(c.getColumnIndexOrThrow("observacao")));
                inputDataDespesaCartao.setText(DateUtils.converterDataParaPtBR(c.getString(c.getColumnIndexOrThrow("data_compra"))));
                
                int idCategoria = c.getInt(c.getColumnIndexOrThrow("id_categoria"));
                Categoria cat = CategoriaDAO.buscarCategoriaPorId(requireContext(), idCategoria);
                if (cat != null) autoCompleteCategoria.setText(cat.getNome(), false);

                ((com.google.android.material.materialswitch.MaterialSwitch) requireView().findViewById(R.id.switchDespesaFixa)).setChecked(c.getInt(c.getColumnIndexOrThrow("recorrente")) == 1);
            }
        } catch (Exception e) {
            Log.e("EditarDespesa", "Erro ao carregar dados", e);
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

        ContentValues toContentValues() {
            ContentValues v = new ContentValues();
            v.put("descricao", nome);
            v.put("id_categoria", categoria.getId());
            v.put("data_compra", dataISO);
            v.put("valor", valor);
            v.put("parcelas", quantidadeParcelas);
            return v;
        }
    }
}
