package com.example.app1;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

public class MeuApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Inicia o processo de auditoria e geração de faturas em uma thread separada
        // executa sempre que abre

        new Thread(() -> {
            GerenciadorDeFatura.verificarELancarDespesasFixas(getApplicationContext());
        }).start();



        //executa 1x por dia
        /*
        new Thread(() -> {
            SharedPreferences prefs = getSharedPreferences("faturas_prefs", MODE_PRIVATE);
            long ultimaExecucao = prefs.getLong("ultima_execucao", 0);
            long agora = System.currentTimeMillis();

            // Executa apenas 1 vez por dia (24h = 86.400.000 ms)
            if (agora - ultimaExecucao > 24 * 60 * 60 * 1000) {
                GerenciadorDeFatura.processarFaturasPendentes(getApplicationContext());
                prefs.edit().putLong("ultima_execucao", agora).apply();
                Log.i("MeuApp", "Faturas processadas automaticamente.");
            } else {
                Log.i("MeuApp", "Faturas já processadas recentemente. Ignorando.");
            }
        }).start();
        */
    }
}
