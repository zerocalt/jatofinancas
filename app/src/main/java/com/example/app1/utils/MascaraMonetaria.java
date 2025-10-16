package com.example.app1.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.NumberFormat;
import java.util.Locale;

public class MascaraMonetaria implements TextWatcher {
    private final EditText editText;
    private boolean updating = false;

    public MascaraMonetaria(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        if (updating) return;

        updating = true;
        String cleanString = s.toString().replaceAll("[R$,.\\s]", "");

        if (cleanString.isEmpty()) {
            editText.setText("");
            updating = false;
            return;
        }

        try {
            double parsed = Double.parseDouble(cleanString) / 100;
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            String formatted = formatter.format(parsed);

            editText.setText(formatted);
            editText.setSelection(formatted.length());
        } catch (Exception e) {
            e.printStackTrace();
        }

        updating = false;
    }
}
