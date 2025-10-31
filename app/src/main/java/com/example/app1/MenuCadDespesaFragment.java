package com.example.app1;

import android.content.ContentValues;
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

    private int idUsuario;
    private View slidingMenuDespesa;
    private View overlayDespesa;
    private TextInputEditText inputValorDespesa, inputDataDespesa, inputNomeDespesa, inputObservacao;
    private OnBackPressedCallback backCallback;
    private MaterialAutoCompleteTextView autoCompleteCategoria, autoCompleteConta, autoCompletePeriodo;
    private TextInputLayout textInputValorDespesa, textInputDataDespesa, menuConta, textInputCategoriaDespesa, textInputNomeDespesa, textInputPeriodo;

    private final int MAX_VALUE = 500;
    private final int MIN_VALUE = 2;

    private EditText inputQuantRep;
    private ImageButton btnIncrement, btnDecrement;
    private TextView menuRepete;
    private LinearLayout containerRepeticao, containerQuantidade;
    private MaterialSwitch switchDespesaFixa;
    private Button btnSalvarDespesa;
    final Conta[] contaSelecionada = new Conta[1];
    private int valorPeriodoSelecionado = -1;

    private int idTransacaoEditando = -1;
    private OnTransacaoSalvaListener onTransacaoSalvaListener;

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
            idUsuario = getArguments().getInt("id_usuario");
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
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String tipoMovimento = getArguments() != null ? getArguments().getString("tipoMovimento", "despesa") : "despesa";
        configuraInterfacePorTipo(tipoMovimento);

        abrirMenu();

        if (idTransacaoEditando != -1) {
            carregarDadosParaEdicao(idTransacaoEditando, tipoMovimento);
        }
    }

    public void editarTransacao(int idTransacao) {
        this.idTransacaoEditando = idTransacao;
        if (getView() != null) {
            String tipoMovimento = getArguments() != null ? getArguments().getString("tipoMovimento", "despesa") : "despesa";
            carregarDadosParaEdicao(idTransacao, tipoMovimento);
        }
    }

    private void carregarDadosParaEdicao(int idTransacao, String tipoMovimento) {
        String titulo = tipoMovimento.equals("receita") ? "Editar Receita" : "Editar Despesa";
        configuraInterfacePorTipo(titulo);
        menuRepete.setVisibility(View.GONE);
        containerRepeticao.setVisibility(View.GONE);

        SQLiteDatabase db = new MeuDbHelper(requireContext()).getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM transacoes WHERE id = ?", new String[]{String.valueOf(idTransacao)})) {
            if (cursor.moveToFirst()) {
                inputNomeDespesa.setText(cursor.getString(cursor.getColumnIndexOrThrow("descricao")));
                inputValorDespesa.setText(String.format(Locale.GERMAN, "%.2f", cursor.getDouble(cursor.getColumnIndexOrThrow("valor"))));
                inputObservacao.setText(cursor.getString(cursor.getColumnIndexOrThrow("observacao")));

                try {
                    String dataDoBanco = cursor.getString(cursor.getColumnIndexOrThrow("data_movimentacao"));
                    inputDataDespesa.setText(DateUtils.converterDataParaPtBR(dataDoBanco));
                } catch (Exception e) {
                    inputDataDespesa.setText("");
                }

                MaterialSwitch switchStatus = getView().findViewById(R.id.switchDespesaPago);
                int status = "receita".equals(tipoMovimento) ? cursor.getInt(cursor.getColumnIndexOrThrow("recebido")) : cursor.getInt(cursor.getColumnIndexOrThrow("pago"));
                switchStatus.setChecked(status == 1);

                int idCategoria = cursor.getInt(cursor.getColumnIndexOrThrow("id_categoria"));
                ArrayAdapter<Categoria> adapterCategoria = (ArrayAdapter<Categoria>) autoCompleteCategoria.getAdapter();
                for (int i = 0; i < adapterCategoria.getCount(); i++) {
                    if (adapterCategoria.getItem(i).getId() == idCategoria) {
                        autoCompleteCategoria.setText(adapterCategoria.getItem(i).getNome(), false);
                        break;
                    }
                }

                int idConta = cursor.getInt(cursor.getColumnIndexOrThrow("id_conta"));
                ArrayAdapter<Conta> adapterConta = (ArrayAdapter<Conta>) autoCompleteConta.getAdapter();
                for (int i = 0; i < adapterConta.getCount(); i++) {
                    if (adapterConta.getItem(i).getId() == idConta) {
                        autoCompleteConta.setText(adapterConta.getItem(i).getNome(), false);
                        contaSelecionada[0] = adapterConta.getItem(i);
                        break;
                    }
                }
            }
        }
    }

    private void configuraInterfacePorTipo(String tipo) {
        if(getView() == null) return;
        TextView titulo = getView().findViewById(R.id.tituloDespesa);
        TextView textoTipo = getView().findViewById(R.id.textoTipo);
        TextView textoFixa = getView().findViewById(R.id.textoFixa);

        if (tipo.toLowerCase().contains("receita")) {
            titulo.setText(tipo.startsWith("Editar") ? "Editar Receita" : "Adicionar Receita");
            textInputValorDespesa.setHint("* Valor da Receita (ex: 1234,56)");
            textInputNomeDespesa.setHint("* Descrição da Receita");
            btnSalvarDespesa.setText(tipo.startsWith("Editar") ? "Salvar Alterações" : "Salvar Receita");
            textoTipo.setText("Recebido");
            menuRepete.setText("RECEITA REPETE?");
            textoFixa.setText("Essa Receita repete todos os meses?");
            textInputCategoriaDespesa.setHint("* Categoria da Receita");
            textInputDataDespesa.setHint("Data da Receita");
        } else {
            titulo.setText(tipo.startsWith("Editar") ? "Editar Despesa" : "Adicionar Despesa");
            textInputValorDespesa.setHint("* Valor da Despesa (ex: 1234,56)");
            textInputNomeDespesa.setHint("* Descrição da Despesa");
            btnSalvarDespesa.setText(tipo.startsWith("Editar") ? "Salvar Alterações" : "Salvar Despesa");
            textoTipo.setText("Pago");
            menuRepete.setText("DESPESA REPETE?");
            textoFixa.setText("Essa Despesa repete todos os meses?");
            textInputCategoriaDespesa.setHint("* Categoria da Despesa");
            textInputDataDespesa.setHint("Data da Despesa");
        }
    }

    private void salvarDespesa() {
        String tipoMovimento = getArguments().getString("tipoMovimento", "despesa");
        if (!validarCampos()) return;

        boolean sucesso = false;

        if (idTransacaoEditando > 0) {
            // LÓGICA DE EDIÇÃO
            ContentValues values = new ContentValues();
            try {
                String valorLimpo = inputValorDespesa.getText().toString().replaceAll("[^0-9,]", "").replace(",", ".");
                double valor = Double.parseDouble(valorLimpo);
                values.put("valor", valor);
                values.put("data_movimentacao", DateUtils.converterDataParaISO(inputDataDespesa.getText().toString()));
            } catch (Exception e) {
                Toast.makeText(getContext(), "Valor ou data inválidos.", Toast.LENGTH_SHORT).show();
                return;
            }
            values.put("descricao", inputNomeDespesa.getText().toString());
            values.put("observacao", inputObservacao.getText().toString());

            try (SQLiteDatabase db = new MeuDbHelper(getContext()).getWritableDatabase()) {
                int rows = db.update("transacoes", values, "id = ?", new String[]{String.valueOf(idTransacaoEditando)});
                sucesso = rows > 0;
            }
        } else {
            // LÓGICA DE CRIAÇÃO (ORIGINAL RESTAURADA)
            String valorDespesa = inputValorDespesa.getText().toString().trim();
            String nomeDespesa = inputNomeDespesa.getText().toString().trim();
            String categoria = autoCompleteCategoria.getText().toString().trim();
            String dataInput = inputDataDespesa.getText().toString().trim();
            int idConta = (contaSelecionada[0] != null) ? contaSelecionada[0].getId() : -1;
            int tipo = tipoMovimento.equals("receita") ? 1 : 2;
            int periodo = valorPeriodoSelecionado;
            int despesaFixa = switchDespesaFixa.isChecked() ? 1 : 0;
            int quantidade = getNumberFromEditText();
            String observacao = inputObservacao.getText().toString().trim();
            int statusMarcado = ((MaterialSwitch) getView().findViewById(R.id.switchDespesaPago)).isChecked() ? 1 : 0;

            int pago = (tipo == 2) ? statusMarcado : 0;
            int recebido = (tipo == 1) ? statusMarcado : 0;

            Categoria categoriaSelecionada = null;
            ArrayAdapter<Categoria> adapter = (ArrayAdapter<Categoria>) autoCompleteCategoria.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i) != null && adapter.getItem(i).getNome().equals(categoria)) {
                    categoriaSelecionada = adapter.getItem(i);
                    break;
                }
            }
            int idCategoriaSelecionada = (categoriaSelecionada != null) ? categoriaSelecionada.getId() : -1;

            double valor;
            String dataParaBanco;
            try {
                String valorLimpo = valorDespesa.replace("R$", "").replaceAll("[^0-9,]", "").replace(",", ".");
                valor = Double.parseDouble(valorLimpo);
                dataParaBanco = DateUtils.converterDataParaISO(dataInput);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Valor ou data inválidos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (despesaFixa == 1 || quantidade > 1) {
                sucesso = TransacoesDAO.salvarTransacaoRecorrente(requireContext(), idUsuario, idConta, valor, tipo, pago, recebido, dataParaBanco, nomeDespesa, idCategoriaSelecionada, observacao, quantidade, periodo, 1);
            } else {
                sucesso = TransacoesDAO.salvarTransacaoUnica(requireContext(), idUsuario, idConta, valor, tipo, pago, recebido, dataParaBanco, nomeDespesa, idCategoriaSelecionada, observacao);
            }
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
        menuConta = root.findViewById(R.id.menuConta);
        textInputPeriodo = root.findViewById(R.id.textInputPeriodo);
    }

    private void setupListeners(View root) {
        overlayDespesa.setOnClickListener(v -> fecharMenu());
        btnSalvarDespesa.setOnClickListener(v -> salvarDespesa());
        inputValorDespesa.addTextChangedListener(new MascaraMonetaria(inputValorDespesa));
        inputDataDespesa.setOnClickListener(v -> DateUtils.openDatePicker(requireContext(), inputDataDespesa));
        inputDataDespesa.setFocusable(false);
        inputDataDespesa.setClickable(true);
        inputDataDespesa.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        
        menuRepete.setOnClickListener(v -> {
            if (containerRepeticao.getVisibility() == View.GONE) {
                containerRepeticao.setVisibility(View.VISIBLE);
            } else {
                containerRepeticao.setVisibility(View.GONE);
            }
        });

        btnIncrement.setOnClickListener(v -> {
            int valorAtual = getNumberFromEditText();
            if (valorAtual < MIN_VALUE) {
                inputQuantRep.setText(String.valueOf(MIN_VALUE));
            } else if (valorAtual < MAX_VALUE) {
                valorAtual++;
                inputQuantRep.setText(String.valueOf(valorAtual));
            }
        });

        btnDecrement.setOnClickListener(v -> {
            int valorAtual = getNumberFromEditText();
            if (valorAtual > MIN_VALUE) {
                valorAtual--;
                inputQuantRep.setText(String.valueOf(valorAtual));
            } else if (valorAtual <= MIN_VALUE) {
                inputQuantRep.setText("");
            }
        });

        switchDespesaFixa.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                containerQuantidade.setEnabled(false);
                containerQuantidade.setAlpha(0.5f);
                textInputPeriodo.setEnabled(false);
                textInputPeriodo.setAlpha(0.5f);
                inputQuantRep.setText("");
                autoCompletePeriodo.setText("");
                valorPeriodoSelecionado = -1;
            } else {
                containerQuantidade.setEnabled(true);
                containerQuantidade.setAlpha(1.0f);
                textInputPeriodo.setEnabled(true);
                textInputPeriodo.setAlpha(1.0f);
            }
        });
        
        String[] periodosTexto = {"Mensal", "Semanal", "Anual", "Bimestral", "Trimestral", "Semestral"};
        int[] periodosValores = {2, 1, 6, 3, 4, 5};
        ArrayAdapter<String> adapterPeriodo = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, periodosTexto);
        autoCompletePeriodo.setAdapter(adapterPeriodo);
        autoCompletePeriodo.setOnItemClickListener((parent, view1, position, id) -> {
            valorPeriodoSelecionado = periodosValores[position];
        });

        carregarCategorias();
        carregarContas();
    }
    
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

    private void carregarCategorias() {
        List<Categoria> categorias = CategoriaDAO.carregarCategorias(requireContext(), idUsuario);
        CategoriasDropdownAdapter adapter = new CategoriasDropdownAdapter(requireContext(), categorias);
        autoCompleteCategoria.setAdapter(adapter);
    }

    private void carregarContas() {
        List<Conta> contas = ContaDAO.carregarListaContas(requireContext(), idUsuario);
        ArrayAdapter<Conta> adapterConta = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, contas);
        autoCompleteConta.setAdapter(adapterConta);
        autoCompleteConta.setOnItemClickListener((parent, view, position, id) -> {
            contaSelecionada[0] = (Conta) parent.getItemAtPosition(position);
        });
    }

    private boolean validarCampos() {
        boolean valido = true;
        if (inputValorDespesa.getText().toString().trim().isEmpty()) {
            textInputValorDespesa.setError("Informe o valor");
            valido = false;
        }
        if (inputNomeDespesa.getText().toString().trim().isEmpty()) {
            textInputNomeDespesa.setError("Informe a descrição");
            valido = false;
        }
        if (autoCompleteCategoria.getText().toString().trim().isEmpty()) {
            textInputCategoriaDespesa.setError("Selecione uma categoria");
            valido = false;
        }
        if (autoCompleteConta.getText().toString().trim().isEmpty()) {
            menuConta.setError("Selecione uma conta");
            valido = false;
        }
        return valido;
    }

    public void fecharMenu() {
        slidingMenuDespesa.animate().translationY(slidingMenuDespesa.getHeight()).setDuration(300).withEndAction(() -> {
            slidingMenuDespesa.setVisibility(View.GONE);
            overlayDespesa.setVisibility(View.GONE);
            backCallback.setEnabled(false);
            getParentFragmentManager().beginTransaction().remove(this).commit();
        }).start();
    }

    public void abrirMenu() {
        overlayDespesa.setVisibility(View.VISIBLE);
        slidingMenuDespesa.setVisibility(View.VISIBLE);
        slidingMenuDespesa.post(() -> {
            slidingMenuDespesa.setTranslationY(slidingMenuDespesa.getHeight());
            slidingMenuDespesa.animate().translationY(0).setDuration(300).start();
            inputValorDespesa.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(inputValorDespesa, InputMethodManager.SHOW_IMPLICIT);
        });
        backCallback.setEnabled(true);
    }
}
