package com.example.app1.utils;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;

public class DateUtils {

    private static final SimpleDateFormat sdfBrasileiro = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static final SimpleDateFormat sdfISO = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Converte do formato brasileiro dd/MM/yyyy para ISO yyyy-MM-dd
    public static String converterDataParaISO(String dataPtBR) throws ParseException {
        Date data = sdfBrasileiro.parse(dataPtBR);
        return sdfISO.format(data);
    }

    // Converte do formato ISO yyyy-MM-dd para formato brasileiro dd/MM/yyyy
    public static String converterDataParaPtBR(String dataISO) throws ParseException {
        Date data = sdfISO.parse(dataISO);
        return sdfBrasileiro.format(data);
    }

    // Função do input para escolher data
    public static void openDatePicker(Context context, EditText inputDataDespesa) {
        final Calendar calendar = Calendar.getInstance();

        String dataAtual = inputDataDespesa.getText().toString().trim();
        if (!dataAtual.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                calendar.setTime(sdf.parse(dataAtual));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    String dataSelecionada = String.format("%02d/%02d/%04d", selectedDayOfMonth, selectedMonth + 1, selectedYear);
                    inputDataDespesa.setText(dataSelecionada);
                }, year, month, day);

        datePickerDialog.show();
    }

}