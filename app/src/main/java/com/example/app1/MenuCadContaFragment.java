package com.example.app1;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

    private MeuDbHelper dbHelper;
    private SQLiteDatabase db;

    private int idUsuarioLogado = -1;

    private OnBackPressedCallback backCallback;

    // Interface para callback quando uma conta for salva
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

        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                fecharMenu();
                if (getActivity() != null) {
                    View container = getActivity().findViewById(R.id.fragmentContainerConta);
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

        dbHelper = new MeuDbHelper(requireContext());

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
                    .setPositiveButton("Confirmar", new ColorEnvelopeListener() {
                        @Override
                        public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                            String hex = "#" + envelope.getHexCode();
                            inputCorConta.setText(hex);
                            inputCorConta.setTextColor(envelope.getColor());
                        }
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
        slidingMenuConta.animate().translationY(slidingMenuConta.getHeight()).setDuration(300)
                .withEndAction(() -> {
                    slidingMenuConta.setVisibility(View.GONE);
                    overlayConta.setVisibility(View.GONE);
                    backCallback.setEnabled(false);
                }).start();
    }

    private void salvarConta() {
        String nome = inputNomeConta.getText().toString().trim();
        String saldoStr = inputSaldoConta.getText().toString().trim();
        saldoStr = saldoStr.replace("R$", "").replaceAll("[^0-9,]", "").trim();
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
                saldo = Double.parseDouble(saldoStr.replace(",", "."));
            }
        } catch (NumberFormatException e) {
            Snackbar.make(requireView(), "Saldo inválido. Use apenas números.", Snackbar.LENGTH_LONG).show();
            return;
        }

        int tipoConta = 0;
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
                tipoConta = 0;
        }

        if (cor.isEmpty()) {
            cor = "#000000";
        }

        int mostrar = (radioGroupMostrar.getCheckedRadioButtonId() == R.id.radioMostrarSim) ? 1 : 0;

        db = dbHelper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("nome", nome);
        valores.put("saldo", saldo);
        valores.put("tipo_conta", tipoConta);
        valores.put("cor", cor);
        valores.put("mostrar_na_tela_inicial", mostrar);
        valores.put("id_usuario", idUsuarioLogado);

        long res = db.insert("contas", null, valores);
        db.close();

        if (res != -1) {
            fecharMenu();
            limparCampos();
            if (listener != null) {
                listener.onContaSalva(nome);
            }
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
}