package com.example.app1;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
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

import com.example.app1.MeuDbHelper;
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

    private MeuDbHelper dbHelper;
    private SQLiteDatabase db;

    private int idUsuarioLogado = -1;
    private OnBackPressedCallback backCallback;

    // Callback para avisar o fragment de despesa que uma categoria foi criada
    public interface OnCategoriaSalvaListener {
        void onCategoriaSalva(String nomeCategoria);
    }

    private OnCategoriaSalvaListener listener;

    public void setOnCategoriaSalvaListener(OnCategoriaSalvaListener listener) {
        this.listener = listener;
    }

    public static MenuCadCategoriaFragment newInstance(int idUsuario) {
        MenuCadCategoriaFragment fragment = new MenuCadCategoriaFragment();
        Bundle args = new Bundle();
        args.putInt("id_usuario", idUsuario);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            idUsuarioLogado = getArguments().getInt("id_usuario", -1);
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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_menu_cad_categoria, container, false);

        overlayCategoria = root.findViewById(R.id.overlayCategoria);
        slidingMenuCategoria = root.findViewById(R.id.slidingMenuCategoria);
        tilNomeCategoria = root.findViewById(R.id.textInputNomeCategoria);
        inputNomeCategoria = root.findViewById(R.id.inputNomeCategoria);
        inputCorCategoria = root.findViewById(R.id.inputCorCategoria);
        btnSalvarCategoria = root.findViewById(R.id.btnSalvarCategoria);

        dbHelper = new MeuDbHelper(requireContext());

        // Fecha o menu tocando fora
        overlayCategoria.setOnClickListener(v -> fecharMenu());

        // Picker de cor
        inputCorCategoria.setOnClickListener(v -> {
            new ColorPickerDialog.Builder(requireContext())
                    .setTitle("Escolha uma cor")
                    .setPositiveButton("Confirmar", new ColorEnvelopeListener() {
                        @Override
                        public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                            String hex = "#" + envelope.getHexCode();
                            inputCorCategoria.setText(hex);
                            inputCorCategoria.setTextColor(envelope.getColor());
                        }
                    })
                    .setNegativeButton("Cancelar", (dialog, i) -> dialog.dismiss())
                    .attachAlphaSlideBar(true)
                    .attachBrightnessSlideBar(true)
                    .show();
        });

        // Botão salvar
        btnSalvarCategoria.setOnClickListener(v -> salvarCategoria());

        return root;
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
                    getParentFragmentManager().beginTransaction().remove(this).commit();
                    View container = requireActivity().findViewById(R.id.fragmentContainerCategoria);
                    if (container != null) {
                        container.setVisibility(View.GONE);
                    }
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
        if (idUsuarioLogado == -1) {
            Snackbar.make(requireView(), "Usuário inválido.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        // Cria o objeto
        Categoria categoria = new Categoria(nome, cor);
        // Salva no banco
        boolean sucesso = com.example.app1.data.CategoriaDAO.inserirCategoria(requireContext(), categoria, idUsuarioLogado);
        if (sucesso) {
            Snackbar.make(requireView(), "Categoria salva com sucesso!", Snackbar.LENGTH_SHORT).show();
            // Notifica o fragmento de despesa para atualizar o spinner
            if (listener != null) {
                listener.onCategoriaSalva(categoria.getNome());
            }
            limparCampos();
            fecharMenu();
        } else {
            Snackbar.make(requireView(), "Erro ao salvar categoria.", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Esconde o container da categoria ao sair
        if (isAdded()) {
            FrameLayout container = requireActivity().findViewById(R.id.fragmentContainerCategoria);
            if (container != null) {
                container.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        abrirMenu();
    }
}