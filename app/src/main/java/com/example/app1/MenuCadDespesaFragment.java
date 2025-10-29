package com.example.app1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.app1.data.CategoriaDAO;
import com.example.app1.data.ContaDAO;
import com.example.app1.data.TransacoesDAO;
import com.example.app1.utils.DateUtils;
import com.example.app1.utils.MascaraMonetaria;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MenuCadDespesaFragment extends Fragment {

    private static final String ARG_ID_USUARIO = "id_usuario";
    private int idUsuario;
    private View slidingMenuDespesa;
    private View overlayDespesa;
    private TextInputEditText inputValorDespesa, inputDataDespesa, inputNomeDespesa;
    private OnBackPressedCallback backCallback;
    private MaterialAutoCompleteTextView autoCompleteCategoria, autoCompleteConta, autoCompletePeriodo;
    private TextInputLayout textInputValorDespesa, textInputDataDespesa, menuConta, textInputConta, textInputCategoria;

    private MenuCadContaFragment menuCadContaFragment;

    // define a quantidade de parcelas que pode escolher
    private final int MAX_VALUE = 500; // Exemplo: máximo de 12 parcelas
    private final int MIN_VALUE = 2;  // Mínimo de 1 parcela

    private EditText inputQuantRep;
    private ImageButton btnIncrement;
    private ImageButton btnDecrement;
    private TextView menuRepete;
    private LinearLayout containerRepeticao, containerQuantidade;
    private MaterialSwitch switchDespesaFixa;
    private TextInputLayout textInputPeriodo, textInputNomeDespesa, textInputCategoriaDespesa, textInputPago;
    private Button btnSalvarDespesa;
    // Variável para guardar a conta selecionada
    final Conta[] contaSelecionada = new Conta[1];

    private int valorPeriodoSelecionado = -1; // valor numérico do período

    public static MenuCadDespesaFragment newInstance(int idUsuario) {
        MenuCadDespesaFragment fragment = new MenuCadDespesaFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ID_USUARIO, idUsuario);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            idUsuario = getArguments().getInt(ARG_ID_USUARIO);
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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Infla o layout XML que você mandou
        View root = inflater.inflate(R.layout.fragment_menu_cad_despesa, container, false);

        slidingMenuDespesa = root.findViewById(R.id.slidingMenuDespesa);
        overlayDespesa = root.findViewById(R.id.overlayDespesa);

        inputValorDespesa = root.findViewById(R.id.inputValorDespesa);
        inputNomeDespesa = root.findViewById(R.id.inputNomeDespesa);
        inputDataDespesa = root.findViewById(R.id.inputDataDespesa);
        inputQuantRep = root.findViewById(R.id.inputQuantRep);

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
        menuConta = root.findViewById(R.id.menuConta);
        textInputPeriodo = root.findViewById(R.id.textInputPeriodo);

        // fecha o menu Despesa clicando fora dele
        overlayDespesa.setOnClickListener(v -> fecharMenu());

        // coloca mascara no inPutValor
        inputValorDespesa.addTextChangedListener(new MascaraMonetaria(inputValorDespesa));

        // campo data
        inputDataDespesa.setOnClickListener(v -> DateUtils.openDatePicker(requireContext(), inputDataDespesa));
        inputDataDespesa.setFocusable(false);
        inputDataDespesa.setClickable(true);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        String dataAtual = sdf.format(new java.util.Date());
        inputDataDespesa.setText(dataAtual);

        // Carrega categorias
        List<Categoria> categorias = CategoriaDAO.carregarCategorias(requireContext(), idUsuario);
        CategoriasDropdownAdapter adapter = new CategoriasDropdownAdapter(requireContext(), categorias);
        autoCompleteCategoria.setAdapter(adapter);

        // Carrega contas
        // Preenche menu suspenso de conta para seleção, trazendo contas do banco
        List<Conta> contas = ContaDAO.carregarListaContas(requireContext(), idUsuario);
        MaterialAutoCompleteTextView autoCompleteConta = root.findViewById(R.id.autoCompleteConta);
        ArrayAdapter<Conta> adapterConta = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, contas);
        autoCompleteConta.setAdapter(adapterConta);
        // Quando o usuário escolhe uma conta
        autoCompleteConta.setOnItemClickListener((parent, view, position, id) -> {
            contaSelecionada[0] = (Conta) parent.getItemAtPosition(position);
            //Log.d("ContaSelecionada", "ID: " + contaSelecionada[0].getId() + " Nome: " + contaSelecionada[0].getNome());
        });

        // Botão para abrir menu cadastro conta
        View btnAddConta = root.findViewById(R.id.btnAddConta);
        btnAddConta.setOnClickListener(v -> abrirMenuConta());
        menuCadContaFragment = MenuCadContaFragment.newInstance(idUsuario);
        menuCadContaFragment.setOnContaSalvaListener(nomeConta -> {
            // Recarrega lista de contas do banco
            List<Conta> contasAtualizadas = ContaDAO.carregarListaContas(requireContext(), idUsuario);

            // Atualiza o adapter
            ArrayAdapter<Conta> novoAdapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    contasAtualizadas);
            autoCompleteConta.setAdapter(novoAdapter);

            // Encontra a conta recém-criada na lista (pelo nome)
            for (Conta c : contasAtualizadas) {
                if (c.getNome().equalsIgnoreCase(nomeConta)) {
                    contaSelecionada[0] = c;
                    autoCompleteConta.setText(c.getNome(), false);
                    //Log.d("ContaSelecionada", "Nova conta cadastrada ID: " + c.getId());
                    break;
                }
            }

            // Fecha o menu de conta
            fecharMenuConta();
        });
        // Adiciona o fragment dentro do container (só se ainda não tiver sido adicionado)
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerConta, menuCadContaFragment)
                .commitNow(); // commitNow garante que ele já está disponível
        // Configura o listener de retorno

        // Botão para abrir menu cadastro categoria
        View btnAddCategoria = root.findViewById(R.id.btnAddCategoria);
        MenuCadCategoriaFragment menuCadCategoriaFragment = MenuCadCategoriaFragment.newInstance(idUsuario);

        // Listener para atualizar lista após salvar
        menuCadCategoriaFragment.setOnCategoriaSalvaListener(nomeCategoria -> {
            // Recarrega categorias
            List<Categoria> categoriasAtualizadas = CategoriaDAO.carregarCategorias(requireContext(), idUsuario);
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


        // Lista as opções do Período
        MaterialAutoCompleteTextView autoCompletePeriodo = root.findViewById(R.id.autoCompletePeriodo);
        // Opções exibidas (na ordem que o usuário verá)
        String[] periodosTexto = {"Mensal", "Semanal", "Anual", "Bimestral", "Trimestral", "Semestral"};
        // Valores correspondentes (mantendo a correspondência)
        int[] periodosValores = {2, 1, 6, 3, 4, 5};
        // Cria o adapter com os textos
        ArrayAdapter<String> adapterPeriodo = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                periodosTexto
        );
        // Define o adapter no campo
        autoCompletePeriodo.setAdapter(adapterPeriodo);
        // Quando o usuário selecionar um item
        autoCompletePeriodo.setOnItemClickListener((parent, view1, position, id) -> {
            String periodoSelecionado = periodosTexto[position];
            valorPeriodoSelecionado = periodosValores[position];
        });

        // Botões de quantidade de Parcelas
        inputQuantRep = root.findViewById(R.id.inputQuantRep);
        btnIncrement = root.findViewById(R.id.btn_increment);
        btnDecrement = root.findViewById(R.id.btn_decrement);
        // 1. Lógica do Botão de Aumentar (+)
        btnIncrement.setOnClickListener(v -> {
            int valorAtual = getNumberFromEditText();
            if (valorAtual < MAX_VALUE) {
                valorAtual++;
                inputQuantRep.setText(String.valueOf(valorAtual));
            }
        });
        // 2. Lógica do Botão de Diminuir (-)
        btnDecrement.setOnClickListener(v -> {
            int valorAtual = getNumberFromEditText();
            if (valorAtual > MIN_VALUE) {
                valorAtual--;
                inputQuantRep.setText(String.valueOf(valorAtual));
            }else if (valorAtual == MIN_VALUE) {
                // Se já estiver no mínimo e apertar "-", limpa o campo
                inputQuantRep.setText("");
            }
        });
        // 3. Lógica para limitar a digitação manual
        inputQuantRep.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) return; // Não faz nada se estiver vazio (o Stepper geralmente precisa de um valor)

                try {
                    int valor = Integer.parseInt(s.toString());

                    // Limita o valor ao máximo e mínimo
                    if (valor > MAX_VALUE) {
                        inputQuantRep.setText(String.valueOf(MAX_VALUE));
                        inputQuantRep.setSelection(inputQuantRep.getText().length()); // Reposiciona o cursor no final
                    } else if (valor < MIN_VALUE) {
                        // Se for menor que o mínimo (e não for um zero temporário), define para o mínimo
                        if (valor != 0) {
                            inputQuantRep.setText(String.valueOf(MIN_VALUE));
                            inputQuantRep.setSelection(inputQuantRep.getText().length());
                        }
                    }
                } catch (NumberFormatException e) {
                    // Isso é improvável de acontecer com inputType="number"
                }
            }
        });

        // apertar o botão despesa Repete aparece o resto do menu
        menuRepete = root.findViewById(R.id.menuRepete);
        containerRepeticao = root.findViewById(R.id.container_repeticao);
        menuRepete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verifica se o container está invisível (GONE)
                if (containerRepeticao.getVisibility() == View.GONE) {
                    // Se estiver GONE, mostra
                    containerRepeticao.setVisibility(View.VISIBLE);
                } else {
                    // Se estiver VISIBLE, esconde
                    containerRepeticao.setVisibility(View.GONE);
                }
            }
        });

        // ativou o botao depesa fixa desativa o resto
        switchDespesaFixa.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // A variável 'isChecked' será true se o switch estiver ativado (Despesa Fixa)
            // Se Despesa Fixa estiver ATIVADO (isChecked = true)
            if (isChecked) {
                // 1. Desativa a Quantidade (Stepper)
                // Definimos o container como desabilitado, mudando a aparência
                containerQuantidade.setEnabled(false);
                // Opcional: podemos também escurecer o visual
                containerQuantidade.setAlpha(0.5f);
                // 2. Desativa o Período (Dropdown)
                textInputPeriodo.setEnabled(false);
                textInputPeriodo.setAlpha(0.5f); // Opcional: escurece o visual

                // RESET: limpa quantidade e período
                inputQuantRep.setText("");
                autoCompletePeriodo.setText(""); // limpa seleção
                valorPeriodoSelecionado = -1; // reset interno
            } else {
                // Se Despesa Fixa estiver DESATIVADO (isChecked = false)
                // 1. Ativa a Quantidade (Stepper)
                containerQuantidade.setEnabled(true);
                containerQuantidade.setAlpha(1.0f); // Restaura o brilho
                // 2. Ativa o Período (Dropdown)
                textInputPeriodo.setEnabled(true);
                textInputPeriodo.setAlpha(1.0f); // Restaura o brilho
            }
        });

        // Botão Salvar Despesa
        btnSalvarDespesa.setOnClickListener(v -> {
            if (validarCampos()) {
                salvarDespesa();
            }
        });

        // Limpa os erros da validação
        inputValorDespesa.addTextChangedListener(new SimpleTextWatcher(() -> textInputValorDespesa.setError(null)));
        inputNomeDespesa.addTextChangedListener(new SimpleTextWatcher(() -> textInputNomeDespesa.setError(null)));
        autoCompleteCategoria.addTextChangedListener(new SimpleTextWatcher(() -> textInputCategoriaDespesa.setError(null)));
        inputDataDespesa.addTextChangedListener(new SimpleTextWatcher(() -> textInputDataDespesa.setError(null)));
        autoCompleteConta.addTextChangedListener(new SimpleTextWatcher(() -> menuConta.setError(null)));

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String tipoMovimento = getArguments() != null ? getArguments().getString("tipoMovimento", "despesa") : "despesa";

        TextView titulo = view.findViewById(R.id.tituloDespesa);
        TextInputLayout textInputValor = view.findViewById(R.id.textInputValorDespesa);
        TextInputLayout textInputNome = view.findViewById(R.id.textInputNomeDespesa);
        Button btnSalvar = view.findViewById(R.id.btnSalvarDespesa);
        TextView labelRepete = view.findViewById(R.id.menuRepete);
        TextView textoTipo = view.findViewById(R.id.textoTipo);
        TextInputLayout textInputCategoriaDespesa = view.findViewById(R.id.textInputCategoriaDespesa);
        TextInputLayout textInputDataDespesa = view.findViewById(R.id.textInputDataDespesa);
        TextView textoFixa = view.findViewById(R.id.textoFixa);




        if (tipoMovimento.equals("receita")) {
            titulo.setText("Adicionar Receita");
            textInputValor.setHint("* Valor da Receita (ex: 1234,56)");
            textInputNome.setHint("* Descrição da Receita");
            labelRepete.setText("RECEITA REPETE?");
            btnSalvar.setText("Salvar Receita");
            textoTipo.setText("Recebido");
            textInputCategoriaDespesa.setHint("* Categoria da Receita");
            textInputDataDespesa.setHint("Data da Receita");
            textoFixa.setText("Essa Receita repete todos os meses?");
        } else {
            titulo.setText("Adicionar Despesa");
            textInputValor.setHint("* Valor da Despesa (ex: 1234,56)");
            textInputNome.setHint("* Descrição da Despesa");
            labelRepete.setText("DESPESA REPETE?");
            btnSalvar.setText("Salvar Despesa");
            textoTipo.setText("Pago");
            textInputCategoriaDespesa.setHint("* Categoria da Despesa");
            textInputDataDespesa.setHint("Data da Despesa");
            textoFixa.setText("Essa Despesa repete todos os meses?");
        }

        abrirMenu();

    }

    public void fecharMenu() {
        slidingMenuDespesa.animate()
                .translationY(slidingMenuDespesa.getHeight())
                .setDuration(300)
                .withEndAction(() -> {
                    slidingMenuDespesa.setVisibility(View.GONE);
                    overlayDespesa.setVisibility(View.GONE);
                    backCallback.setEnabled(false);

                }).start();
    }

    public void abrirMenu() {
        overlayDespesa.setVisibility(View.VISIBLE);
        slidingMenuDespesa.setVisibility(View.VISIBLE);
        slidingMenuDespesa.post(() -> {
            slidingMenuDespesa.setTranslationY(slidingMenuDespesa.getHeight());
            slidingMenuDespesa.animate().translationY(0).setDuration(300).start();

            // Solicita foco e mostra teclado
            inputValorDespesa.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(inputValorDespesa, InputMethodManager.SHOW_IMPLICIT);

        });
        backCallback.setEnabled(true);
    }

    // Abre o menu para cadastro de contas.
    private void abrirMenuConta() {
        View container = requireView().findViewById(R.id.fragmentContainerConta);
        container.setVisibility(View.VISIBLE);
        menuCadContaFragment.abrirMenu();
    }

    // Fecha o menu de cadastro de contas.
    private void fecharMenuConta() {
        menuCadContaFragment.fecharMenu();
        View container = requireView().findViewById(R.id.fragmentContainerConta);
        container.setVisibility(View.GONE);
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

    /**
     * Auxiliar para obter o valor do EditText com segurança,
     * retornando 0 se o campo estiver vazio ou for inválido.
     */
    private int getNumberFromEditText() {
        String texto = inputQuantRep.getText().toString();
        if (texto.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean validarCampos() {
        boolean valido = true;
        String valor = inputValorDespesa.getText().toString().trim();
        String nome = inputNomeDespesa.getText().toString().trim();
        String categoria = autoCompleteCategoria.getText().toString().trim();
        String data = inputDataDespesa.getText().toString().trim();
        String conta = autoCompleteConta.getText().toString().trim();
        // Limpa erros antigos
        textInputValorDespesa.setError(null);
        textInputNomeDespesa.setError(null);
        textInputCategoriaDespesa.setError(null);
        textInputDataDespesa.setError(null);
        menuConta.setError(null);
        if (valor.isEmpty()) {
            textInputValorDespesa.setError("Informe o valor da despesa/receita");
            valido = false;
        }
        if (nome.isEmpty()) {
            textInputNomeDespesa.setError("Informe o nome da despesa/receita");
            valido = false;
        }
        if (categoria.isEmpty()) {
            textInputCategoriaDespesa.setError("Selecione uma categoria");
            valido = false;
        }
        if (data.isEmpty()) {
            textInputDataDespesa.setError("Selecione uma data");
            valido = false;
        }
        if (conta.isEmpty()) {
            menuConta.setError("Selecione uma conta");
            valido = false;
        }
        // Dá foco no primeiro campo inválido
        if (!valido) {
            if (valor.isEmpty()) inputValorDespesa.requestFocus();
            else if (nome.isEmpty()) inputNomeDespesa.requestFocus();
            else if (categoria.isEmpty()) autoCompleteCategoria.requestFocus();
            else if (data.isEmpty()) inputDataDespesa.requestFocus();
            else if (conta.isEmpty()) autoCompleteConta.requestFocus();
        }

        // Verifica se tem valor na quantidade, se sim, valida tb o periodo
        String quantidadeStr = inputQuantRep.getText().toString().trim();
        int quantidade = quantidadeStr.isEmpty() ? 0 : Integer.parseInt(quantidadeStr);
        // Se houver quantidade, validar período
        if (quantidade > 0 && (valorPeriodoSelecionado == -1 || autoCompletePeriodo.getText().toString().isEmpty())) {
            textInputPeriodo.setError("Selecione um período para a quantidade");
            valido = false;
        } else {
            textInputPeriodo.setError(null);
        }

        return valido;
    }

    // utilitário pequeno para reduzir boilerplate ao usar TextWatcher
    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable callback;
        public SimpleTextWatcher(Runnable callback) { this.callback = callback; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (callback != null) callback.run();
        }
        @Override public void afterTextChanged(android.text.Editable s) {}
    }

    private void salvarDespesa() {
        String tipoMovimento = getArguments() != null ? getArguments().getString("tipoMovimento", "despesa") : "despesa";
        // Pegando os valores do formulário
        String valorDespesa = inputValorDespesa.getText().toString().trim();
        String nomeDespesa = inputNomeDespesa.getText().toString().trim();
        String categoria = autoCompleteCategoria.getText().toString().trim();
        String dataInput = inputDataDespesa.getText().toString().trim(); // dd/MM/yyyy
        int idConta = (contaSelecionada[0] != null) ? contaSelecionada[0].getId() : -1;
        int tipo = tipoMovimento.equals("receita") ? 1 : 2; // 1 = Receita, 2 = Despesa
        int periodo = valorPeriodoSelecionado == -1 ? 0 : valorPeriodoSelecionado;
        int despesaFixa = switchDespesaFixa.isChecked() ? 1 : 0;
        int quantidade = getNumberFromEditText();
        String observacao = ((TextInputEditText) requireView().findViewById(R.id.inputObservacao)).getText().toString().trim();

        // Estado do switchDespesaPago ou Recebido
        MaterialSwitch switchDespesaPago = requireView().findViewById(R.id.switchDespesaPago);
        int statusMarcado = switchDespesaPago.isChecked() ? 1 : 0;

        // pega a categoria selecionada
        Categoria categoriaSelecionada = null;
        ArrayAdapter<Categoria> adapter = (ArrayAdapter<Categoria>) autoCompleteCategoria.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            Categoria c = adapter.getItem(i);
            if (c != null && c.getNome().equals(categoria)) {
                categoriaSelecionada = c;
                break;
            }
        }
        int idCategoriaSelecionada = categoriaSelecionada.getId();

        // Conversão de valor
        double valor;
        try {
            String valorLimpo = valorDespesa.replace("R$", "").replaceAll("[^0-9,\\.]", "").trim();
            valor = Double.parseDouble(valorLimpo.replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Valor inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Conversão de data do formulário (dd/MM/yyyy) para padrão do banco (yyyy-MM-dd)
        String dataParaBanco = "";
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat sdfBanco = new SimpleDateFormat("yyyy-MM-dd");
            Date data = sdfInput.parse(dataInput);
            dataParaBanco = sdfBanco.format(data);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Data inválida", Toast.LENGTH_SHORT).show();
            return;
        }

        // Define os campos "pago" e "recebido" de acordo com o tipo
        int pago = 0;
        int recebido = 0;

        if (tipo == 1) { // receita
            recebido = statusMarcado;
        } else { // despesa
            pago = statusMarcado;
        }

        // Inserir no banco
        boolean sucesso;
        if (despesaFixa == 1 || quantidade > 1) {
            sucesso = TransacoesDAO.salvarTransacaoRecorrente(
                    requireContext(),
                    idUsuario,
                    idConta,
                    valor,
                    tipo,
                    pago,
                    recebido,
                    dataParaBanco,
                    nomeDespesa,
                    idCategoriaSelecionada,
                    observacao,
                    quantidade,
                    periodo,
                    1 // recorrente ativo
            );
        } else {
            sucesso = TransacoesDAO.salvarTransacaoUnica(
                    requireContext(),
                    idUsuario,
                    idConta,
                    valor,
                    tipo,
                    pago,
                    recebido,
                    dataParaBanco,
                    nomeDespesa,
                    idCategoriaSelecionada,
                    observacao
            );
        }

        if (sucesso) {
            String msg = tipo == 1 ? "Receita salva com sucesso!" : "Despesa salva com sucesso!";
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            // Fecha o menu
            getParentFragmentManager().beginTransaction()
                    .remove(this)
                    .commit();
        } else {
            Toast.makeText(requireContext(), "Erro ao salvar!", Toast.LENGTH_SHORT).show();
        }
    }

}