package com.example.app1.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.example.app1.R;
import com.example.app1.data.ContaDAO;
import com.example.app1.Conta;
import com.example.app1.utils.MascaraMonetaria;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class PopupSaldoUtil {

    public interface OnSaldoAtualizadoListener {
        void onSaldoAtualizado();
    }

    public static void mostrarPopupEditarSaldo(Context context, Conta conta, OnSaldoAtualizadoListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Editar Saldo da Conta");

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_saldo, null);

        TextInputLayout inputLayoutSaldo = view.findViewById(R.id.textInputSaldoConta);
        TextInputEditText inputSaldo = view.findViewById(R.id.inputSaldoConta);
        inputSaldo.addTextChangedListener(new MascaraMonetaria(inputSaldo));

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        inputSaldo.setText(format.format(conta.getSaldo()));

        builder.setView(view);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String saldoStr = inputSaldo.getText().toString();
            double saldo = 0;
            try {
                Number number = format.parse(saldoStr);
                saldo = number.doubleValue();
            } catch (ParseException e) {
                saldo = conta.getSaldo();
            }

            boolean sucesso = ContaDAO.atualizarSaldoConta(context, conta.getId(), saldo);
            if (sucesso) {
                Toast.makeText(context, "Saldo atualizado!", Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onSaldoAtualizado();
            } else {
                Toast.makeText(context, "Erro ao atualizar saldo.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.create().show();
    }
}