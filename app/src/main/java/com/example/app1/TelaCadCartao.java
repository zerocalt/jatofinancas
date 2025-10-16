package com.example.app1;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import android.content.SharedPreferences;

public class TelaCadCartao extends AppCompatActivity {

    private LinearLayout semCartao;
    private LinearLayout listaCartoes;
    private ConstraintLayout mainLayout;
    private SQLiteDatabase db;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "secure_login_prefs";
    private static final String KEY_USER_ID = "saved_user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_cad_cartao);

        mainLayout = findViewById(R.id.main);
        semCartao = findViewById(R.id.semCartao);
        listaCartoes = findViewById(R.id.listaCartoes);

        // Ajuste para edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa SharedPreferences segura
        try {
            MasterKey masterKey = new MasterKey.Builder(getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    getApplicationContext(),
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        }

        int idUsuarioLogado = sharedPreferences.getInt(KEY_USER_ID, -1);
        if (idUsuarioLogado == -1) {
            // Usuário não logado: tratar caso ou fechar activity
            semCartao.setVisibility(View.VISIBLE);
            listaCartoes.setVisibility(View.GONE);
            return;
        }

        // Inicializa banco de dados
        MeuDbHelper dbHelper = new MeuDbHelper(this);
        db = dbHelper.getReadableDatabase();

        mostrarCartoesDoUsuario(idUsuarioLogado);
    }

    private void mostrarCartoesDoUsuario(int idUsuario) {
        Cursor cursor = db.rawQuery(
                "SELECT nome FROM cartoes WHERE id_usuario = ? AND ativo = 1",
                new String[]{String.valueOf(idUsuario)}
        );

        if (cursor.getCount() == 0) {
            semCartao.setVisibility(View.VISIBLE);
            listaCartoes.setVisibility(View.GONE);
        } else {
            semCartao.setVisibility(View.GONE);
            listaCartoes.removeAllViews();

            LayoutInflater inflater = getLayoutInflater();

            while (cursor.moveToNext()) {
                String nomeCartao = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                // Infla o item_cartao.xml, que deve ser criado conforme seu layout individual de cartão
                LinearLayout item = (LinearLayout) inflater.inflate(R.layout.item_cartao, listaCartoes, false);
                TextView txtNomeCartao = item.findViewById(R.id.NomeCartao);
                txtNomeCartao.setText(nomeCartao);
                listaCartoes.addView(item);
            }

            listaCartoes.setVisibility(View.VISIBLE);
        }
        cursor.close();
    }
}