package com.example.app1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;

import com.example.app1.data.CategoriaDAO;
import com.example.app1.data.ContaDAO;
import com.example.app1.utils.MascaraMonetaria;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuCadDespesaFragment extends Fragment {

    private static final String ARG_ID_USUARIO = "id_usuario";
    private int idUsuario;
    private View slidingMenuDespesa;
    private View overlayDespesa;
    private TextInputEditText inputValorDespesa, inputDataDespesa;
    private OnBackPressedCallback backCallback;
    private MaterialAutoCompleteTextView autoCompleteCategoria;
    private TextInputLayout tilValorDespesa, tilDataDespesa;

    private MenuCadContaFragment menuCadContaFragment;

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

        autoCompleteCategoria = root.findViewById(R.id.autoCompleteCategoria);
        slidingMenuDespesa = root.findViewById(R.id.slidingMenuDespesa);
        overlayDespesa = root.findViewById(R.id.overlayDespesa);

        tilValorDespesa = root.findViewById(R.id.textInputValorDespesa);
        tilDataDespesa = root.findViewById(R.id.textInputDataDespesa);

        inputValorDespesa = root.findViewById(R.id.inputValorDespesa);
        inputDataDespesa = root.findViewById(R.id.inputDataDespesa);

        // fecha o menu Despesa clicando fora dele
        overlayDespesa.setOnClickListener(v -> fecharMenu());

        // coloca mascara no inPutValor
        inputValorDespesa.addTextChangedListener(new MascaraMonetaria(inputValorDespesa));

        // campo data
        inputDataDespesa.setOnClickListener(v -> openDatePicker());
        inputDataDespesa.setFocusable(false);
        inputDataDespesa.setClickable(true);
        // Define a data atual formatada
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        String dataAtual = sdf.format(new java.util.Date());
        inputDataDespesa.setText(dataAtual);

        // Carrega categorias
        List<Categoria> categorias = CategoriaDAO.carregarCategorias(requireContext(), idUsuario);
        CategoriasDropdownAdapter adapter = new CategoriasDropdownAdapter(requireContext(), categorias);
        autoCompleteCategoria.setAdapter(adapter);

        // Carrega contas
        // Preenche menu suspenso de conta para seleção, trazendo contas do banco
        List<String> contas = ContaDAO.carregarListaContas(requireContext(), idUsuario);
        MaterialAutoCompleteTextView autoCompleteConta = root.findViewById(R.id.autoCompleteConta);
        ArrayAdapter<String> adapterConta = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, contas);
        autoCompleteConta.setAdapter(adapterConta);

        // Botão para abrir menu cadastro conta
        View btnAddConta = root.findViewById(R.id.btnAddConta);
        btnAddConta.setOnClickListener(v -> abrirMenuConta());
        menuCadContaFragment = MenuCadContaFragment.newInstance(idUsuario);
        menuCadContaFragment.setOnContaSalvaListener(nomeConta -> {
            // Recarrega lista de contas do banco
            List<String> contasAtualizadas = ContaDAO.carregarListaContas(requireContext(), idUsuario);

            // Atualiza o adapter
            ArrayAdapter<String> novoAdapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    contasAtualizadas);
            autoCompleteConta.setAdapter(novoAdapter);

            // Define a nova conta como selecionada
            autoCompleteConta.setText(nomeConta, false);

            // Fecha o menu de conta
            fecharMenuConta();
        });
        // Adiciona o fragment dentro do container (só se ainda não tiver sido adicionado)
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerConta, menuCadContaFragment)
                .commitNow(); // commitNow garante que ele já está disponível
        // Configura o listener de retorno


        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        abrirMenu();

        /*
        // Se alguém pediu para abrir a edição, faz aqui
        if (idTransacaoEditando != -1) {
            abrirMenuEditarDespesa(idTransacaoEditando);
            idTransacaoEditando = -1; // reset
        }
        */
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

    private void openDatePicker() {
        final java.util.Calendar calendar = java.util.Calendar.getInstance();

        String dataAtual = inputDataDespesa.getText().toString().trim();
        if (!dataAtual.isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                calendar.setTime(sdf.parse(dataAtual));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH);
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    String dataSelecionada = String.format("%02d/%02d/%04d", selectedDayOfMonth, selectedMonth + 1, selectedYear);
                    inputDataDespesa.setText(dataSelecionada);
                }, year, month, day);

        datePickerDialog.show();
    }

}