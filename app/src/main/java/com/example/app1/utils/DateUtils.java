package com.example.app1.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

}