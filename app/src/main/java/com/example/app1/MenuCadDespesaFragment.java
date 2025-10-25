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

import com.example.app1.data.CategoriaDAO;
import com.example.app1.utils.MascaraMonetaria;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class MenuCadDespesaFragment extends Fragment {

    private static final String ARG_ID_USUARIO = "id_usuario";
    private int idUsuario;
    private View slidingMenuDespesa;
    private View overlayDespesa;
    private TextInputEditText inputValorDespesa;
    private OnBackPressedCallback backCallback;
    private MaterialAutoCompleteTextView autoCompleteCategoria;
    private TextInputLayout tilValorDespesa;

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

        inputValorDespesa = root.findViewById(R.id.inputValorDespesa);

        // fecha o menu Despesa clicando fora dele
        overlayDespesa.setOnClickListener(v -> fecharMenu());

        // coloca mascara no inPutValor
        inputValorDespesa.addTextChangedListener(new MascaraMonetaria(inputValorDespesa));

        // Carrega categorias
        List<Categoria> categorias = CategoriaDAO.carregarCategorias(requireContext(), idUsuario);
        CategoriasDropdownAdapter adapter = new CategoriasDropdownAdapter(requireContext(), categorias);
        autoCompleteCategoria.setAdapter(adapter);

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

}