package com.example.app1;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.app1.data.ContaDAO;
import com.example.app1.utils.DateUtils;
import com.example.app1.utils.MascaraMonetaria;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MenuTransferenciaFragment extends Fragment {

    private TextInputLayout tilValorTransferencia, tilDataTransferencia, tilContaOrigem, tilContaDestino;
    private TextInputEditText inputValorTransferencia, inputDataTransferencia;
    private MaterialAutoCompleteTextView autoCompleteContaOrigem, autoCompleteContaDestino;
    private Button btnSalvarTransferencia;
    private View overlayTransferencia, slidingTransferencia;
    private OnBackPressedCallback backCallback;
    private int idUsuarioLogado = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                fecharMenu(false);
            }
        };
        if (getArguments() != null) {
            idUsuarioLogado = getArguments().getInt("id_usuario", -1);
        }
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_menu_transferencia, container, false);

        if (getArguments() != null) {
            idUsuarioLogado = getArguments().getInt("id_usuario", -1);
        }

        bindViews(root);

        overlayTransferencia.setOnClickListener(v -> fecharMenu(false));
        inputValorTransferencia.addTextChangedListener(new MascaraMonetaria(inputValorTransferencia));
        inputDataTransferencia.setOnClickListener(v -> DateUtils.openDatePicker(requireContext(), inputDataTransferencia));
        inputDataTransferencia.setFocusable(false);
        inputDataTransferencia.setClickable(true);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        inputDataTransferencia.setText(sdf.format(new java.util.Date()));

        List<Conta> contas = ContaDAO.carregarListaContas(requireContext(), idUsuarioLogado);
        ArrayAdapter<Conta> contaAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, contas);
        autoCompleteContaOrigem.setAdapter(contaAdapter);
        autoCompleteContaDestino.setAdapter(contaAdapter);

        btnSalvarTransferencia.setOnClickListener(v -> {
            limparErros();
            if (validarCampos()) {
                Toast.makeText(getContext(), "Validação OK: pode salvar", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /*
        if (idTransacaoEditando > 0) {
            abrirMenuParaEdicao(idTransacaoEditando);
        } else {
            abrirMenu();
        }
         */
        abrirMenu();
    }

    private void bindViews(View root) {
        overlayTransferencia = root.findViewById(R.id.overlayTransferencia);
        slidingTransferencia = root.findViewById(R.id.slidingTransferencia);
        tilValorTransferencia = root.findViewById(R.id.textInputValorTransferencia);
        tilDataTransferencia = root.findViewById(R.id.textInputDataTransferencia);
        tilContaOrigem = root.findViewById(R.id.menuContaOrigem);
        tilContaDestino = root.findViewById(R.id.menuContaDestino);

        inputValorTransferencia = root.findViewById(R.id.inputValorTransferencia);
        inputDataTransferencia = root.findViewById(R.id.inputDataTransferencia);
        autoCompleteContaOrigem = root.findViewById(R.id.autoCompleteContaOrigem);
        autoCompleteContaDestino = root.findViewById(R.id.autoCompleteContaDestino);

        btnSalvarTransferencia = root.findViewById(R.id.btnSalvarTransferencia);
    }

    public void abrirMenu() {
        overlayTransferencia.setVisibility(View.VISIBLE);
        slidingTransferencia.setVisibility(View.VISIBLE);
        slidingTransferencia.post(() -> {
            slidingTransferencia.setTranslationY(slidingTransferencia.getHeight());
            slidingTransferencia.animate().translationY(0).setDuration(300).start();
            inputValorTransferencia.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(inputValorTransferencia, InputMethodManager.SHOW_IMPLICIT);
        });
        backCallback.setEnabled(true);
    }

    public void fecharMenu(boolean transferenciaSalva) {
        if (!isAdded()) return;

        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }

        if (transferenciaSalva) {
            Bundle result = new Bundle();
            result.putBoolean("atualizar", true);
            getParentFragmentManager().setFragmentResult("transferenciaSalvaRequest", result);
        }

        slidingTransferencia.animate()
                .translationY(slidingTransferencia.getHeight())
                .setDuration(300)
                .withEndAction(() -> {
                    if (isAdded()) {
                        overlayTransferencia.setVisibility(View.GONE);
                        getParentFragmentManager().beginTransaction().remove(this).commit();
                    }
                }).start();
    }


    private void limparErros() {
        tilValorTransferencia.setError(null);
        tilDataTransferencia.setError(null);
        tilContaOrigem.setError(null);
        tilContaDestino.setError(null);
    }

    private boolean validarCampos() {
        boolean valido = true;

        String valorStr = inputValorTransferencia.getText().toString().trim();
        String dataStr = inputDataTransferencia.getText().toString().trim();
        String contaOrigem = autoCompleteContaOrigem.getText().toString().trim();
        String contaDestino = autoCompleteContaDestino.getText().toString().trim();

        if (TextUtils.isEmpty(valorStr)) {
            tilValorTransferencia.setError("Informe o valor da transferência");
            valido = false;
        } else {
            try {
                Double.parseDouble(valorStr.replace(",", "."));
            } catch (NumberFormatException e) {
                tilValorTransferencia.setError("Valor inválido");
                valido = false;
            }
        }

        if (TextUtils.isEmpty(dataStr)) {
            tilDataTransferencia.setError("Informe a data da transferência");
            valido = false;
        }

        if (TextUtils.isEmpty(contaOrigem)) {
            tilContaOrigem.setError("Informe a conta de origem");
            valido = false;
        }

        if (TextUtils.isEmpty(contaDestino)) {
            tilContaDestino.setError("Informe a conta de destino");
            valido = false;
        }

        return valido;
    }
}
