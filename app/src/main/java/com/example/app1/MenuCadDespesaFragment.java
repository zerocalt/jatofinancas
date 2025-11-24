package com.example.app1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.app1.data.CategoriaDAO;
import com.example.app1.data.ContaDAO;
import com.example.app1.data.TransacoesDAO;
import com.example.app1.utils.DateUtils;
import com.example.app1.utils.MascaraMonetaria;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MenuCadDespesaFragment extends Fragment implements MenuCadCategoriaFragment.OnCategoriaSalvaListener, MenuCadContaFragment.OnContaSalvaListener {

    private static final int MAX_VALUE = 99;
    private static final int MIN_VALUE = 2;

    private View overlayDespesa;
    private View slidingMenuDespesa;
    private TextInputEditText inputValorDespesa, inputDataDespesa, inputObservacao;
    private MaterialAutoCompleteTextView inputNomeDespesa;
    private EditText inputQuantRep;
    private Button btnSalvarDespesa;
    private ImageButton btnIncrement, btnDecrement, btnAddCategoria, btnAddConta;
    private TextView menuRepete;
    private LinearLayout containerRepeticao, containerQuantidade;
    private MaterialSwitch switchDespesaFixa;
    private MaterialAutoCompleteTextView autoCompleteCategoria, autoCompleteConta, autoCompletePeriodo;
    private TextInputLayout textInputValorDespesa, textInputNomeDespesa, textInputCategoriaDespesa, textInputDataDespesa, textInputPeriodo;

    private OnBackPressedCallback backCallback;
    private int idUsuario = -1;
    private int idTransacaoEditando = -1;
    private final Conta[] contaSelecionada = {null};
    private int valorPeriodoSelecionado = -1;

    private OnTransacaoSalvaListener onTransacaoSalvaListener;
    private CompoundButton.OnCheckedChangeListener switchFixaListener;

    @Override
    public void onCategoriaSalva(String nomeCategoria) {
        // Recarrega as categorias
        List<Categoria> categorias = CategoriaDAO.carregarCategorias(requireContext(), idUsuario);
        CategoriasDropdownAdapter categoriaAdapter = new CategoriasDropdownAdapter(requireContext(), categorias);
        autoCompleteCategoria.setAdapter(categoriaAdapter);

        // Encontra e seleciona a nova categoria
        for (int i = 0; i < categoriaAdapter.getCount(); i++) {
            Categoria item = categoriaAdapter.getItem(i);
            if (item != null && item.getNome().equals(nomeCategoria)) {
                autoCompleteCategoria.setText(item.getNome(), false);
                break;
            }
        }
    }

    @Override
    public void onContaSalva(String nomeConta) {
        // Recarrega as contas
        List<Conta> contas = ContaDAO.carregarListaContas(requireContext(), idUsuario);
        ArrayAdapter<Conta> contaAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, contas);
        autoCompleteConta.setAdapter(contaAdapter);

        // Encontra e seleciona a nova conta
        for (int i = 0; i < contaAdapter.getCount(); i++) {
            Conta item = contaAdapter.getItem(i);
            if (item != null && item.getNome().equals(nomeConta)) {
                autoCompleteConta.setText(item.getNome(), false);
                contaSelecionada[0] = item;
                break;
            }
        }
    }

    public interface OnTransacaoSalvaListener {
        void onTransacaoSalva();
    }

    public void setOnTransacaoSalvaListener(OnTransacaoSalvaListener listener) {
        this.onTransacaoSalvaListener = listener;
    }

    public static MenuCadDespesaFragment newInstance(int idUsuario, String tipoMovimento) {
        MenuCadDespesaFragment fragment = new MenuCadDespesaFragment();
        Bundle args = new Bundle();
        args.putInt("id_usuario", idUsuario);
        args.putString("tipoMovimento", tipoMovimento);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            idUsuario = getArguments().getInt("id_usuario", -1);
        }

        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                fecharMenu();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_menu_cad_despesa, container, false);
        setupViews(root);
        setupListeners(root);
        carregarDadosIniciais();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String tipoMovimento = getArguments() != null ? getArguments().getString("tipoMovimento", "despesa") : "despesa";
        configuraInterfacePorTipo(tipoMovimento);
        abrirMenu();
        if (idTransacaoEditando != -1) {
            carregarDadosParaEdicao(idTransacaoEditando);
        }
    }

    public void editarTransacao(int idTransacao) {
        this.idTransacaoEditando = idTransacao;
        if (getView() != null) {
            carregarDadosParaEdicao(idTransacao);
        }
    }

    @SuppressWarnings("unchecked")
    private void carregarDadosParaEdicao(int idTransacao) {
        String tipoMovimento = getArguments() != null ? getArguments().getString("tipoMovimento", "despesa") : "despesa";
        configuraInterfacePorTipo(tipoMovimento.equals("receita") ? getString(R.string.editar_receita) : getString(R.string.editar_despesa));

        try (MeuDbHelper dbHelper = new MeuDbHelper(requireContext());
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {

            int idParaCarregar = idTransacao;
            int statusOriginal = -1;

            try (Cursor cursorInfo = db.rawQuery("SELECT id_mestre, pago, recebido FROM transacoes WHERE id = ?", new String[]{String.valueOf(idTransacao)})) {
                if (cursorInfo.moveToFirst()) {
                    int idMestre = cursorInfo.getInt(cursorInfo.getColumnIndexOrThrow("id_mestre"));
                    if (idMestre > 0) {
                        idParaCarregar = idMestre;
                    }
                    statusOriginal = "receita".equals(tipoMovimento)
                            ? cursorInfo.getInt(cursorInfo.getColumnIndexOrThrow("recebido"))
                            : cursorInfo.getInt(cursorInfo.getColumnIndexOrThrow("pago"));
                }
            }

            try (Cursor cursor = db.rawQuery("SELECT * FROM transacoes WHERE id = ?", new String[]{String.valueOf(idParaCarregar)})) {
                if (cursor.moveToFirst()) {
                    this.idTransacaoEditando = idParaCarregar;

                    inputNomeDespesa.setText(cursor.getString(cursor.getColumnIndexOrThrow("descricao")));
                    inputValorDespesa.setText(String.format(Locale.GERMAN, "%.2f", cursor.getDouble(cursor.getColumnIndexOrThrow("valor"))));
                    inputObservacao.setText(cursor.getString(cursor.getColumnIndexOrThrow("observacao")));

                    try {
                        String dataDoBanco = cursor.getString(cursor.getColumnIndexOrThrow("data_movimentacao"));
                        inputDataDespesa.setText(DateUtils.converterDataParaPtBR(dataDoBanco));
                    } catch (Exception e) {
                        inputDataDespesa.setText("");
                    }

                    View view = getView();
                    if (view != null && statusOriginal != -1) {
                        MaterialSwitch switchStatus = view.findViewById(R.id.switchDespesaPago);
                        switchStatus.setChecked(statusOriginal == 1);
                    }

                    // Categoria
                    int idCategoria = cursor.getInt(cursor.getColumnIndexOrThrow("id_categoria"));
                    CategoriasDropdownAdapter adapterCategoria = (CategoriasDropdownAdapter) autoCompleteCategoria.getAdapter();
                    if (adapterCategoria != null) {
                        for (int i = 0; i < adapterCategoria.getCount(); i++) {
                            Categoria item = adapterCategoria.getItem(i);
                            if (item != null && item.getId() == idCategoria) {
                                autoCompleteCategoria.setText(item.getNome(), false);
                                break;
                            }
                        }
                    }

                    // Conta
                    int idConta = cursor.getInt(cursor.getColumnIndexOrThrow("id_conta"));
                    ArrayAdapter<Conta> adapterConta = (ArrayAdapter<Conta>) autoCompleteConta.getAdapter();
                    if (adapterConta != null) {
                        for (int i = 0; i < adapterConta.getCount(); i++) {
                            Conta item = adapterConta.getItem(i);
                            if (item != null && item.getId() == idConta) {
                                autoCompleteConta.setText(item.getNome(), false);
                                contaSelecionada[0] = item;
                                break;
                            }
                        }
                    }

                    // Recorrência
                    menuRepete.setVisibility(View.VISIBLE);
                    boolean isRecorrente = cursor.getInt(cursor.getColumnIndexOrThrow("recorrente")) == 1;

                    switchDespesaFixa.setOnCheckedChangeListener(null);

                    if (isRecorrente) {
                        containerRepeticao.setVisibility(View.VISIBLE);
                        containerQuantidade.setVisibility(View.VISIBLE);

                        int totalParcelas = cursor.getInt(cursor.getColumnIndexOrThrow("totalParcelas"));
                        int numeroParcela = cursor.getInt(cursor.getColumnIndexOrThrow("numeroParcela"));

                        boolean isFixa = (totalParcelas == 0);
                        inputQuantRep.setText(totalParcelas > 0 ? String.valueOf(totalParcelas) : "");
                        switchDespesaFixa.setChecked(isFixa);

                        inputQuantRep.setEnabled(!isFixa);
                        btnIncrement.setEnabled(!isFixa);
                        btnDecrement.setEnabled(!isFixa);
                        autoCompletePeriodo.setEnabled(!isFixa);

                        int repetirPeriodo = cursor.getInt(cursor.getColumnIndexOrThrow("repetir_periodo"));
                        if (autoCompletePeriodo.getAdapter() != null && repetirPeriodo > 0) {
                            autoCompletePeriodo.setText(autoCompletePeriodo.getAdapter().getItem(repetirPeriodo - 1).toString(), false);
                            valorPeriodoSelecionado = repetirPeriodo;
                        }

                        // Abre o menu para a despesa fixa automaticamente
                        abrirMenu();
                    } else {
                        limparCamposRecorrencia();
                    }

                    switchDespesaFixa.setOnCheckedChangeListener(switchFixaListener);
                }
            }
        }
    }

    private void configuraInterfacePorTipo(String tipo) {
        View view = getView();
        if (view == null) return;

        TextView titulo = view.findViewById(R.id.tituloDespesa);
        TextView textoTipo = view.findViewById(R.id.textoTipo);
        TextView textoFixa = view.findViewById(R.id.textoFixa);

        boolean isReceita = tipo.toLowerCase().contains("receita");

        titulo.setText(tipo.startsWith("Editar") ? (isReceita ? R.string.editar_receita : R.string.editar_despesa) : (isReceita ? R.string.adicionar_receita : R.string.adicionar_despesa));
        textInputValorDespesa.setHint(isReceita ? getString(R.string.valor_da_receita) : getString(R.string.valor_da_despesa));
        textInputNomeDespesa.setHint(isReceita ? getString(R.string.descricao_da_receita) : getString(R.string.descricao_da_despesa));
        btnSalvarDespesa.setText(tipo.startsWith("Editar") ? R.string.salvar_alteracoes : (isReceita ? R.string.salvar_receita : R.string.salvar_despesa));
        textoTipo.setText(isReceita ? R.string.recebido : R.string.pago);
        menuRepete.setText(isReceita ? R.string.receita_repete : R.string.despesa_repete);
        textoFixa.setText(isReceita ? getString(R.string.essa_receita_repete_todos_os_meses) : getString(R.string.essa_despesa_repete_todos_os_meses));
        textInputCategoriaDespesa.setHint(isReceita ? getString(R.string.categoria_da_receita) : getString(R.string.categoria_da_despesa));
        textInputDataDespesa.setHint(isReceita ? getString(R.string.data_da_receita) : getString(R.string.data_da_despesa));
    }

    @SuppressWarnings("unchecked")
    private void salvarDespesa() {
        if (!validarCampos()) return;

        String tipoMovimento = getArguments() != null ? getArguments().getString("tipoMovimento", "despesa") : "despesa";

        String valorDespesa = inputValorDespesa.getText() != null ? inputValorDespesa.getText().toString().trim() : "";
        String nomeDespesa = inputNomeDespesa.getText() != null ? inputNomeDespesa.getText().toString().trim() : "";
        String categoria = autoCompleteCategoria.getText() != null ? autoCompleteCategoria.getText().toString().trim() : "";
        String dataInput = inputDataDespesa.getText() != null ? inputDataDespesa.getText().toString().trim() : "";
        int idContaNova = (contaSelecionada[0] != null) ? contaSelecionada[0].getId() : -1;
        int tipo = tipoMovimento.equals("receita") ? 1 : 2;

        int periodo = valorPeriodoSelecionado;
        boolean despesaFixa = switchDespesaFixa.isChecked();
        int quantidade = getNumberFromEditText();
        String observacao = inputObservacao.getText() != null ? inputObservacao.getText().toString().trim() : "";

        View view = getView();
        int statusMarcado = 0;
        if (view != null) {
            MaterialSwitch switchDespesaPago = view.findViewById(R.id.switchDespesaPago);
            if (switchDespesaPago != null) {
                statusMarcado = switchDespesaPago.isChecked() ? 1 : 0;
            }
        }

        int pago = (tipo == 2) ? statusMarcado : 0;
        int recebido = (tipo == 1) ? statusMarcado : 0;

        int idCategoriaSelecionada = -1;
        ArrayAdapter<Categoria> adapter = (ArrayAdapter<Categoria>) autoCompleteCategoria.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                Categoria item = adapter.getItem(i);
                if (item != null && item.getNome().equals(categoria)) {
                    idCategoriaSelecionada = item.getId();
                    break;
                }
            }
        }

        double valor;
        String dataParaBanco;
        try {
            String valorLimpo = valorDespesa.replace("R$", "").replaceAll("[^0-9,.]", "").replace(".", "").replace(",", ".");
            valor = Double.parseDouble(valorLimpo);
            dataParaBanco = DateUtils.converterDataParaISO(dataInput);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Valor ou data inválidos", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean sucesso;

        boolean isRecorrente = containerRepeticao.getVisibility() == View.VISIBLE;

        if (isRecorrente) {
            if (!despesaFixa && valorPeriodoSelecionado <= 0) {
                textInputPeriodo.setError(getString(R.string.selecione_um_periodo));
                return;
            } else {
                textInputPeriodo.setError(null);
            }

            int totalParcelas = getNumberFromEditText(); // agora representa totalParcelas

            if (!despesaFixa && totalParcelas < MIN_VALUE) {
                Toast.makeText(getContext(), String.format(getString(R.string.quantidade_minima_parcelas), MIN_VALUE), Toast.LENGTH_SHORT).show();
                return;
            }

            if (idTransacaoEditando > 0) {
                TransacoesDAO.ajustarSaldoParaEdicao(requireContext(), idTransacaoEditando, idContaNova, valor);
                TransacoesDAO.excluirTransacao(requireContext(), idTransacaoEditando, "transacao");
                sucesso = false;
            } else {
                sucesso = true;
            }

            if (despesaFixa) {
                sucesso = TransacoesDAO.salvarTransacaoFixa(requireContext(), idUsuario, idContaNova, valor, tipo, pago, recebido, dataParaBanco, nomeDespesa, idCategoriaSelecionada, observacao, periodo);
                if (sucesso && idTransacaoEditando > 0) {
                    TransacoesDAO.atualizarFilhasNaoPagas(requireContext(), idTransacaoEditando, valor, idCategoriaSelecionada, nomeDespesa, observacao);
                }
            } else {
                sucesso = TransacoesDAO.salvarTransacaoRecorrente(requireContext(), idUsuario, idContaNova, valor, tipo, pago, recebido, dataParaBanco, nomeDespesa, idCategoriaSelecionada, observacao, totalParcelas, periodo, idTransacaoEditando);
            }
        } else {
            if (idTransacaoEditando > 0) {
                TransacoesDAO.ajustarSaldoParaEdicao(requireContext(), idTransacaoEditando, idContaNova, valor);
                TransacoesDAO.excluirTransacao(requireContext(), idTransacaoEditando, "transacao");
            }
            sucesso = TransacoesDAO.salvarTransacaoUnica(requireContext(), idUsuario, idContaNova, valor, tipo, pago, recebido, dataParaBanco, nomeDespesa, idCategoriaSelecionada, observacao);
        }

        if (sucesso) {
            String msg = (tipoMovimento.equals("receita") ? "Receita" : "Despesa") + (idTransacaoEditando > 0 ? " atualizada" : " salva") + " com sucesso!";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            if (onTransacaoSalvaListener != null) {
                onTransacaoSalvaListener.onTransacaoSalva();
            }
            fecharMenu();
        } else {
            Toast.makeText(getContext(), "Erro ao salvar!", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupViews(View root) {
        slidingMenuDespesa = root.findViewById(R.id.slidingMenuDespesa);
        overlayDespesa = root.findViewById(R.id.overlayDespesa);
        inputValorDespesa = root.findViewById(R.id.inputValorDespesa);
        inputNomeDespesa = root.findViewById(R.id.inputNomeDespesa);
        inputDataDespesa = root.findViewById(R.id.inputDataDespesa);
        inputQuantRep = root.findViewById(R.id.inputQuantRep);
        inputObservacao = root.findViewById(R.id.inputObservacao);
        btnSalvarDespesa = root.findViewById(R.id.btnSalvarDespesa);
        btnIncrement = root.findViewById(R.id.btn_increment);
        btnDecrement = root.findViewById(R.id.btn_decrement);
        menuRepete = root.findViewById(R.id.menuRepete);
        containerRepeticao = root.findViewById(R.id.container_repeticao);
        containerQuantidade = root.findViewById(R.id.container_quantidade);
        switchDespesaFixa = root.findViewById(R.id.switchDespesaFixa);
        autoCompleteCategoria = root.findViewById(R.id.autoCompleteCategoria);
        autoCompleteConta = root.findViewById(R.id.autoCompleteConta);
        autoCompletePeriodo = root.findViewById(R.id.autoCompletePeriodo);
        textInputValorDespesa = root.findViewById(R.id.textInputValorDespesa);
        textInputNomeDespesa = root.findViewById(R.id.textInputNomeDespesa);
        textInputCategoriaDespesa = root.findViewById(R.id.textInputCategoriaDespesa);
        textInputDataDespesa = root.findViewById(R.id.textInputDataDespesa);
        textInputPeriodo = root.findViewById(R.id.textInputPeriodo);
        btnAddCategoria = root.findViewById(R.id.btnAddCategoria);
        btnAddConta = root.findViewById(R.id.btnAddConta);
    }

    private void setupListeners(View root) {
        overlayDespesa.setOnClickListener(v -> fecharMenu());
        btnSalvarDespesa.setOnClickListener(v -> salvarDespesa());
        inputValorDespesa.addTextChangedListener(new MascaraMonetaria(inputValorDespesa));
        inputDataDespesa.setOnClickListener(v -> DateUtils.openDatePicker(requireContext(), inputDataDespesa));
        inputDataDespesa.setFocusable(false);
        inputDataDespesa.setClickable(true);
        inputDataDespesa.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        autoCompleteCategoria.setOnItemClickListener((parent, view, position, id) -> {
            Categoria categoriaSelecionada = (Categoria) parent.getItemAtPosition(position);
            if (categoriaSelecionada != null) {
                autoCompleteCategoria.setText(categoriaSelecionada.getNome(), false);
            }
        });

        menuRepete.setOnClickListener(v -> {
            if (containerRepeticao.getVisibility() == View.GONE) {
                containerRepeticao.setVisibility(View.VISIBLE);
            } else {
                containerRepeticao.setVisibility(View.GONE);
            }
        });

        btnIncrement.setOnClickListener(v -> {
            int valorAtual = getNumberFromEditText();
            if (valorAtual == 0) {
                inputQuantRep.setText(String.valueOf(MIN_VALUE));
            } else if (valorAtual < MAX_VALUE) {
                inputQuantRep.setText(String.valueOf(valorAtual + 1));
            }
        });

        btnDecrement.setOnClickListener(v -> {
            int valorAtual = getNumberFromEditText();
            if (valorAtual > MIN_VALUE) {
                inputQuantRep.setText(String.valueOf(valorAtual - 1));
            } else {
                inputQuantRep.setText("");
            }
        });

        switchFixaListener = (buttonView, isChecked) -> {
            inputQuantRep.setEnabled(!isChecked);
            btnIncrement.setEnabled(!isChecked);
            btnDecrement.setEnabled(!isChecked);
            autoCompletePeriodo.setEnabled(!isChecked);

            if (isChecked) {
                inputQuantRep.setText("");
                autoCompletePeriodo.setText("", false);
                valorPeriodoSelecionado = -1;
            }
        };
        switchDespesaFixa.setOnCheckedChangeListener(switchFixaListener);

        autoCompletePeriodo.setOnItemClickListener((parent, view, position, id) -> valorPeriodoSelecionado = position + 1);
        autoCompleteConta.setOnItemClickListener((parent, view, position, id) -> contaSelecionada[0] = (Conta) parent.getItemAtPosition(position));

        // 👉 Abre fragment de cadastro de CATEGORIA
        btnAddCategoria.setOnClickListener(v -> {
            FrameLayout containerCategoria = requireActivity().findViewById(R.id.fragmentContainerCategoria);
            containerCategoria.setVisibility(View.VISIBLE);

            MenuCadCategoriaFragment fragment = MenuCadCategoriaFragment.newInstance(idUsuario);
            fragment.setOnCategoriaSalvaListener(this);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerCategoria, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        // 👉 Abre fragment de cadastro de CONTA
        btnAddConta.setOnClickListener(v -> {
            FrameLayout containerConta = requireActivity().findViewById(R.id.fragmentContainerConta);
            containerConta.setVisibility(View.VISIBLE);

            MenuCadContaFragment fragment = MenuCadContaFragment.newInstance(idUsuario);
            fragment.setOnContaSalvaListener(this);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerConta, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        inputNomeDespesa.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String texto = s.toString();
                if (texto.length() >= 1) {
                    List<String> sugestoes = buscarNomesDespesa(texto);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, sugestoes);
                    inputNomeDespesa.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }
            }
        });

        inputNomeDespesa.setOnItemClickListener((parent, view, position, id) -> {
            String nomeSelecionado = (String) parent.getItemAtPosition(position);
            preencherCamposPorNome(nomeSelecionado);
        });

    }

    private int getNumberFromEditText() {
        try {
            String text = inputQuantRep.getText() != null ? inputQuantRep.getText().toString() : "";
            if (text.isEmpty()) return 0;
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void carregarDadosIniciais() {
        List<Categoria> categorias = CategoriaDAO.carregarCategorias(requireContext(), idUsuario);
        CategoriasDropdownAdapter categoriaAdapter = new CategoriasDropdownAdapter(requireContext(), categorias);
        autoCompleteCategoria.setAdapter(categoriaAdapter);

        List<Conta> contas = ContaDAO.carregarListaContas(requireContext(), idUsuario);
        ArrayAdapter<Conta> contaAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, contas);
        autoCompleteConta.setAdapter(contaAdapter);

        String[] periodos = {"Semanal", "Mensal", "Bimestral", "Trimestral", "Semestral", "Anual"};
        ArrayAdapter<String> periodoAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, periodos);
        autoCompletePeriodo.setAdapter(periodoAdapter);
        limparCamposRecorrencia();
    }

    private boolean validarCampos() {
        boolean valido = true;
        if (inputValorDespesa.getText() == null || inputValorDespesa.getText().toString().trim().isEmpty()) {
            textInputValorDespesa.setError("Informe um valor");
            valido = false;
        } else {
            textInputValorDespesa.setError(null);
        }

        if (inputNomeDespesa.getText() == null || inputNomeDespesa.getText().toString().trim().isEmpty()) {
            textInputNomeDespesa.setError("Informe uma descrição");
            valido = false;
        } else {
            textInputNomeDespesa.setError(null);
        }

        return valido;
    }

    public void abrirMenu() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if(getView() == null) return;
            overlayDespesa.setVisibility(View.VISIBLE);
            slidingMenuDespesa.setVisibility(View.VISIBLE);
            slidingMenuDespesa.animate().translationY(0).setDuration(300).withEndAction(() -> {
                inputValorDespesa.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(inputValorDespesa, InputMethodManager.SHOW_IMPLICIT);
                }
            });
            backCallback.setEnabled(true);
        }, 100);
    }

    public void fecharMenu() {
        if(getView() == null) return;
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
        slidingMenuDespesa.animate().translationY(slidingMenuDespesa.getHeight()).setDuration(300).withEndAction(() -> {
            overlayDespesa.setVisibility(View.GONE);
            slidingMenuDespesa.setVisibility(View.GONE);
            idTransacaoEditando = -1;
            limparCampos();
            backCallback.setEnabled(false);
            if (getParentFragment() != null && !getParentFragment().isStateSaved()) {
                getParentFragment().getChildFragmentManager().beginTransaction().remove(this).commit();
            }
        });
    }

    private void limparCampos() {
        inputValorDespesa.setText("");
        inputNomeDespesa.setText("");
        inputDataDespesa.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        inputObservacao.setText("");
        autoCompleteCategoria.setText("");
        autoCompleteConta.setText("");
        limparCamposRecorrencia();
    }

    private void limparCamposRecorrencia() {
        switchDespesaFixa.setChecked(false);
        inputQuantRep.setText("");
        autoCompletePeriodo.setText("", false);
        valorPeriodoSelecionado = -1;
        containerRepeticao.setVisibility(View.GONE);
    }

    private List<String> buscarNomesDespesa(String texto) {
        List<String> resultados = new ArrayList<>();
        Cursor cursor = null;
        try (MeuDbHelper dbHelper = new MeuDbHelper(requireContext());
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            String query = "SELECT DISTINCT descricao FROM transacoes WHERE descricao LIKE ?";
            String[] args = new String[]{"%" + texto + "%"};
            cursor = db.rawQuery(query, args);
            while (cursor.moveToNext()) {
                resultados.add(cursor.getString(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultados;
    }

    private void preencherCamposPorNome(String nome) {
        Cursor cursor = null;
        try (MeuDbHelper dbHelper = new MeuDbHelper(requireContext());
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            String query = "SELECT t.id_categoria, c.nome AS nome_categoria, t.id_conta, co.nome AS nome_conta, t.observacao " +
                    "FROM transacoes t " +
                    "LEFT JOIN categorias c ON t.id_categoria = c.id " +
                    "LEFT JOIN contas co ON t.id_conta = co.id " +
                    "WHERE t.descricao = ?";
            String[] args = new String[]{ nome };
            cursor = db.rawQuery(query, args);
            if (cursor.moveToFirst()) {
                String nomeCategoria = cursor.getString(cursor.getColumnIndexOrThrow("nome_categoria"));
                String nomeConta = cursor.getString(cursor.getColumnIndexOrThrow("nome_conta"));
                String observacao = cursor.getString(cursor.getColumnIndexOrThrow("observacao"));

                autoCompleteCategoria.setText(nomeCategoria, false);
                autoCompleteConta.setText(nomeConta, false);
                inputObservacao.setText(observacao);
            }
        } catch (Exception e) {
            Log.e("MenuCadDespesaFragment", "Erro no SQL: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Erro ao carregar dados da despesa", Toast.LENGTH_SHORT).show();
        } finally {
            if(cursor != null) cursor.close();
        }
    }


}