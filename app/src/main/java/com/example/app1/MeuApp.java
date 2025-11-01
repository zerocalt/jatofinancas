package com.example.app1;

import android.app.Application;

public class MeuApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Inicia o processo de auditoria e geração de faturas em uma thread separada
        // para não bloquear a interface principal do aplicativo.
        new Thread(() -> {
            GerenciadorDeFatura.processarFaturasPendentes(getApplicationContext());
        }).start();
    }
}
