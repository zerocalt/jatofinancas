package com.example.app1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.app1.data.ContaDAO;
import com.example.app1.utils.MascaraMonetaria;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

public class MenuCadContaFragment extends Fragment {

    private View overlayConta;
    private View slidingMenuConta;
    private Button btnSalvarConta;
    private TextInputLayout tilNomeConta;
    private TextInputEditText inputNomeConta, inputSaldoConta, inputCorConta;
    private MaterialAutoCompleteTextView autoCompleteTipoConta;
    private RadioGroup radioGroupMostrar;

    private int idUsuarioLogado = -1;

    private OnBackPressedCallback backCallback;

    public interface OnContaSalvaListener {
        void onContaSalva(String nomeConta);
    }

    private OnContaSalvaListener listener;

    public void setOnContaSalvaListener(OnContaSalvaListener listener) {
        this.listener = listener;
    }

    public static MenuCadContaFragment newInstance(int idUsuario) {
        MenuCadContaFragment fragment = new MenuCadContaFragment();
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.menu_adicionar_conta, container, false);

        overlayConta = root.findViewById(R.id.overlayConta);
        slidingMenuConta = root.findViewById(R.id.slidingMenuConta);

        tilNomeConta = root.findViewById(R.id.textInputNomeConta);
        inputNomeConta = root.findViewById(R.id.inputNomeConta);
        inputSaldoConta = root.findViewById(R.id.inputSaldoConta);
        inputCorConta = root.findViewById(R.id.inputCorConta);
        autoCompleteTipoConta = root.findViewById(R.id.autoCompleteTipoConta);
        radioGroupMostrar = root.findViewById(R.id.radioGroupMostrarNaTelaInicial);
        btnSalvarConta = root.findViewById(R.id.btnSalvarConta);

        inputNomeConta.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilNomeConta.setError(null);
                tilNomeConta.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        String[] tiposConta = {"Corrente", "Poupança", "Investimento", "Outros"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, tiposConta);
        autoCompleteTipoConta.setAdapter(adapter);

        inputSaldoConta.addTextChangedListener(new MascaraMonetaria(inputSaldoConta));

        inputCorConta.setOnClickListener(v -> {
            new ColorPickerDialog.Builder(requireContext())
                    .setTitle("Escolha uma cor")
                    .setPositiveButton("Confirmar", (ColorEnvelopeListener) (envelope, fromUser) -> {
                        String hex = "#" + envelope.getHexCode();
                        inputCorConta.setText(hex);
                        inputCorConta.setTextColor(envelope.getColor());
                    })
                    .setNegativeButton("Cancelar", (dialogInterface, i) -> dialogInterface.dismiss())
                    .attachAlphaSlideBar(true)
                    .attachBrightnessSlideBar(true)
                    .show();
        });

        overlayConta.setOnClickListener(v -> fecharMenu());
        btnSalvarConta.setOnClickListener(v -> salvarConta());

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        abrirMenu();
    }

    public void abrirMenu() {
        overlayConta.setVisibility(View.VISIBLE);
        slidingMenuConta.setVisibility(View.VISIBLE);
        slidingMenuConta.post(() -> {
            slidingMenuConta.setTranslationY(slidingMenuConta.getHeight());
            slidingMenuConta.animate().translationY(0).setDuration(300).start();
        });
        backCallback.setEnabled(true);
    }

    public void fecharMenu() {
        if (isAdded()) {
            slidingMenuConta.animate().translationY(slidingMenuConta.getHeight()).setDuration(300).withEndAction(() -> {
                if (isAdded()) {
                    getParentFragmentManager().beginTransaction().remove(this).commit();
                    View container = requireActivity().findViewById(R.id.fragmentContainerConta);
                    if (container != null) {
                        container.setVisibility(View.GONE);
                    }
                }
            }).start();
        }
    }

    private void salvarConta() {
        String nome = inputNomeConta.getText().toString().trim();
        String saldoStr = inputSaldoConta.getText().toString().trim();
        String cor = inputCorConta.getText().toString().trim();
        String tipoStr = autoCompleteTipoConta.getText().toString().trim();

        if (nome.isEmpty()) {
            tilNomeConta.setError("Informe o nome da conta");
            return;
        } else {
            tilNomeConta.setError(null);
        }

        double saldo = 0;
        try {
             if (!saldoStr.isEmpty()) {
                String valorLimpo = saldoStr.replace("R$", "").replaceAll("[^0-9,.]", "").replace(".", "").replace(",", ".");
                saldo = Double.parseDouble(valorLimpo);
            }
        } catch (NumberFormatException e) {
            Snackbar.make(requireView(), "Saldo inválido.", Snackbar.LENGTH_LONG).show();
            return;
        }

        if (cor.isEmpty()) {
            cor = "#000000";
        }

        int tipoConta;
        switch (tipoStr.toLowerCase()) {
            case "corrente":
                tipoConta = 1;
                break;
            case "poupança":
                tipoConta = 2;
                break;
            case "investimento":
                tipoConta = 3;
                break;
            default:
                tipoConta = 0; // Outros
        }

        int mostrar = (radioGroupMostrar.getCheckedRadioButtonId() == R.id.radioMostrarSim) ? 1 : 0;

        Conta novaConta = new Conta(nome, saldo, tipoConta, cor, mostrar);

        boolean sucesso = ContaDAO.inserirConta(requireContext(), novaConta, idUsuarioLogado);

        if (sucesso) {
            Snackbar.make(requireView(), "Conta salva com sucesso!", Snackbar.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onContaSalva(nome);
            }
            limparCampos();
            fecharMenu();
        } else {
            Snackbar.make(requireView(), "Erro ao salvar conta.", Snackbar.LENGTH_LONG).show();
        }
    }

    private void limparCampos() {
        inputNomeConta.setText("");
        inputSaldoConta.setText("");
        inputCorConta.setText("");
        autoCompleteTipoConta.setText("");
        radioGroupMostrar.check(R.id.radioMostrarSim);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isAdded()) {
            FrameLayout container = requireActivity().findViewById(R.id.fragmentContainerConta);
            if (container != null) {
                container.setVisibility(View.GONE);
            }
        }
    }
}