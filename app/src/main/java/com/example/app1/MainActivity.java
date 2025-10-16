package com.example.app1;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity {

    private MeuDbHelper dbHelper;
    private static final String PREFS_NAME = "secure_login_prefs";
    private static final String KEY_EMAIL = "saved_email";
    private static final String KEY_USER_ID = "saved_user_id";  // CHAVE PARA O ID DO USUARIO
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText login = findViewById(R.id.email);
        EditText senha = findViewById(R.id.senha);
        Button seuBotao = findViewById(R.id.seuBotao);
        TextView cadTexto = findViewById(R.id.cadTexto);

        dbHelper = new MeuDbHelper(this);

        // Inicializa o EncryptedSharedPreferences
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
            // Em caso de falha, use SharedPreferences padrão (não recomendado em produção)
            sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        }

        // Verifica se veio do logout — se sim, não redireciona
        if (getIntent().getBooleanExtra("from_logout", false)) {
            // Não verifica login salvo
            return;
        }
        // Verifica se há login salvo
        verificarLoginSalvo();

        seuBotao.setOnClickListener(v -> {
            String loginDigitado = login.getText().toString().trim();
            String senhaDigitada = senha.getText().toString().trim();

            if (autenticarUsuario(loginDigitado, senhaDigitada)) {
                salvarLogin(loginDigitado);
                Intent intent = new Intent(MainActivity.this, TelaPrincipal.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Erro! E-mail ou Senha Incorretos!", Toast.LENGTH_SHORT).show();
            }
        });

        cadTexto.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TelaCadastroUsuario.class);
            startActivity(intent);
        });
    }

    // Verifica se há login salvo e se o usuário ainda é ativo
    private void verificarLoginSalvo() {
        String emailSalvo = sharedPreferences.getString(KEY_EMAIL, null);
        if (emailSalvo != null && usuarioEstaAtivo(emailSalvo)) {
            Intent intent = new Intent(MainActivity.this, TelaPrincipal.class);
            startActivity(intent);
            finish();
        }
    }

    // Salva o e-mail de forma segura
    private void salvarLogin(String email) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    // Salva o id do usuário de forma segura
    private void salvarUserId(int userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_USER_ID, userId);
        editor.apply();
    }

    // Verifica no banco se o usuário ainda está ativo
    private boolean usuarioEstaAtivo(String email) {
        var db = dbHelper.getReadableDatabase();
        var cursor = db.rawQuery(
                "SELECT 1 FROM usuarios WHERE email = ? AND status = 'ativo' LIMIT 1",
                new String[]{email}
        );
        boolean ativo = cursor.moveToFirst();
        cursor.close();
        db.close();
        return ativo;
    }

    // Autenticação com senha e salvamento do userId
    private boolean autenticarUsuario(String loginDigitado, String senhaDigitada) {
        var db = dbHelper.getReadableDatabase();
        var cursor = db.rawQuery(
                "SELECT id, senha FROM usuarios WHERE email = ? AND status = 'ativo' LIMIT 1",
                new String[]{loginDigitado}
        );

        if (!cursor.moveToFirst()) {
            cursor.close();
            db.close();
            return false;
        }

        int userId = cursor.getInt(0); // id na primeira coluna
        String senhaArmazenada = cursor.getString(1); // senha na segunda coluna

        cursor.close();
        db.close();

        try {
            boolean senhaValida = verificarSenha(senhaDigitada, senhaArmazenada);
            if (senhaValida) {
                salvarUserId(userId); // salva o id do usuário
            }
            return senhaValida;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Verifica hash da senha
    private boolean verificarSenha(String senhaDigitada, String senhaArmazenada) throws Exception {
        String[] partes = senhaArmazenada.split(":");
        if (partes.length != 2) return false;

        byte[] salt = Base64.decode(partes[0], Base64.NO_WRAP);
        String hashEsperado = partes[1];

        KeySpec spec = new PBEKeySpec(senhaDigitada.toCharArray(), salt, 65536, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hashDigitado = factory.generateSecret(spec).getEncoded();
        String hashDigitadoBase64 = Base64.encodeToString(hashDigitado, Base64.NO_WRAP);

        return hashDigitadoBase64.equals(hashEsperado);
    }
}
