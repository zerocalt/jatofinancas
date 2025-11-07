package com.example.app1;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.app1.data.CategoriaDAO;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

public class MenuCadCategoriaFragment extends Fragment {

    private View overlayCategoria;
    private View slidingMenuCategoria;
    private TextInputLayout tilNomeCategoria;
    private TextInputEditText inputNomeCategoria, inputCorCategoria;
    private Button btnSalvarCategoria;

    private int idUsuarioLogado = -1;
    private int categoriaId = -1;
    private OnBackPressedCallback backCallback;

    public interface OnCategoriaSalvaListener {
        void onCategoriaSalva(String nomeCategoria);
    }

    private OnCategoriaSalvaListener listener;

    public void setOnCategoriaSalvaListener(OnCategoriaSalvaListener listener) {
        this.listener = listener;
    }

    public static MenuCadCategoriaFragment newInstance(int idUsuario) {
        return newInstance(idUsuario, -1);
    }

    public static MenuCadCategoriaFragment newInstance(int idUsuario, int categoriaId) {
        MenuCadCategoriaFragment fragment = new MenuCadCategoriaFragment();
        Bundle args = new Bundle();
        args.putInt("id_usuario", idUsuario);
        args.putInt("categoria_id", categoriaId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            idUsuarioLogado = getArguments().getInt("id_usuario", -1);
            categoriaId = getArguments().getInt("categoria_id", -1);
        }

        backCallback = new OnBackPressedCallback(true) {
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
        View root = inflater.inflate(R.layout.fragment_menu_cad_categoria, container, false);

        overlayCategoria = root.findViewById(R.id.overlayCategoria);
        slidingMenuCategoria = root.findViewById(R.id.slidingMenuCategoria);
        tilNomeCategoria = root.findViewById(R.id.textInputNomeCategoria);
        inputNomeCategoria = root.findViewById(R.id.inputNomeCategoria);
        inputCorCategoria = root.findViewById(R.id.inputCorCategoria);
        btnSalvarCategoria = root.findViewById(R.id.btnSalvarCategoria);

        overlayCategoria.setOnClickListener(v -> fecharMenu());

        inputCorCategoria.setOnClickListener(v -> {
            new ColorPickerDialog.Builder(requireContext())
                    .setTitle("Escolha uma cor")
                    .setPositiveButton("Confirmar", (ColorEnvelopeListener) (envelope, fromUser) -> {
                        String hex = "#" + envelope.getHexCode();
                        inputCorCategoria.setText(hex);
                        inputCorCategoria.setTextColor(envelope.getColor());
                    })
                    .setNegativeButton("Cancelar", (dialog, i) -> dialog.dismiss())
                    .attachAlphaSlideBar(true)
                    .attachBrightnessSlideBar(true)
                    .show();
        });

        btnSalvarCategoria.setOnClickListener(v -> salvarCategoria());

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (categoriaId != -1) {
            carregarDadosCategoria();
        }
        abrirMenu();
    }

    private void carregarDadosCategoria() {
        Categoria categoria = CategoriaDAO.buscarCategoriaPorId(getContext(), categoriaId);
        if (categoria != null) {
            preencherFormulario(categoria);
        }
    }

    private void preencherFormulario(Categoria categoria) {
        inputNomeCategoria.setText(categoria.getNome());
        inputCorCategoria.setText(categoria.getCor());
        try {
            inputCorCategoria.setTextColor(Color.parseColor(categoria.getCor()));
        } catch (IllegalArgumentException e) {
            inputCorCategoria.setTextColor(Color.BLACK);
        }
    }

    private void limparCampos() {
        inputNomeCategoria.setText("");
        inputCorCategoria.setText("");
    }

    public void abrirMenu() {
        overlayCategoria.setVisibility(View.VISIBLE);
        slidingMenuCategoria.setVisibility(View.VISIBLE);
        slidingMenuCategoria.post(() -> {
            slidingMenuCategoria.setTranslationY(slidingMenuCategoria.getHeight());
            slidingMenuCategoria.animate().translationY(0).setDuration(300).start();
        });
        backCallback.setEnabled(true);
    }

    private void fecharMenu() {
        if (isAdded()) {
            slidingMenuCategoria.animate().translationY(slidingMenuCategoria.getHeight()).setDuration(300).withEndAction(() -> {
                if (isAdded()) {
                    overlayCategoria.setVisibility(View.GONE);
                    getParentFragmentManager().beginTransaction().remove(this).commit();
                }
            }).start();
        }
    }

    private void salvarCategoria() {
        String nome = inputNomeCategoria.getText().toString().trim();
        String cor = inputCorCategoria.getText().toString().trim();

        if (nome.isEmpty()) {
            tilNomeCategoria.setError("Informe o nome da categoria");
            return;
        } else {
            tilNomeCategoria.setError(null);
        }
        if (cor.isEmpty()) {
            cor = "#888888"; // cor padrão
        }

        Categoria categoria = new Categoria(categoriaId != -1 ? categoriaId : 0, nome, cor);

        boolean sucesso;
        if (categoriaId != -1) {
            sucesso = CategoriaDAO.atualizarCategoria(getContext(), categoria);
        } else {
            if (idUsuarioLogado == -1) {
                Snackbar.make(requireView(), "Usuário inválido.", Snackbar.LENGTH_SHORT).show();
                return;
            }
            sucesso = CategoriaDAO.inserirCategoria(getContext(), categoria, idUsuarioLogado);
        }

        if (sucesso) {
            String message = (categoriaId != -1) ? "Categoria atualizada com sucesso!" : "Categoria salva com sucesso!";
            Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onCategoriaSalva(categoria.getNome());
            }
            limparCampos();
            fecharMenu();
        } else {
            String message = (categoriaId != -1) ? "Erro ao atualizar categoria." : "Erro ao salvar categoria.";
            Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }
}
