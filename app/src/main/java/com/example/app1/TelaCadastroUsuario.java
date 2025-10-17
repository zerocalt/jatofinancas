package com.example.app1;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class TelaCadastroUsuario extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tela_cadastro_usuario);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextInputLayout outNome = findViewById(R.id.outNome);
        TextInputLayout outTel = findViewById(R.id.outTel);
        TextInputLayout outEmail = findViewById(R.id.outEmail);
        TextInputLayout outSenha = findViewById(R.id.outSenha);
        TextInputLayout outSenha2 = findViewById(R.id.outSenha2);

        TextInputEditText nome = findViewById(R.id.nome);
        TextInputEditText tel = findViewById(R.id.tel);
        tel.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        TextInputEditText email = findViewById(R.id.email);
        TextInputEditText senha = findViewById(R.id.senha);
        TextInputEditText senha2 = findViewById(R.id.senha2);

        Button btnCadastrar = findViewById(R.id.btnCadastrar);
        LinearLayout btnSalvartopo = findViewById(R.id.btnSalvartopo);

        //verifica se os campos estão com erro, quando começar a digitar sai o erro do campo
        nome.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                outNome.setError(null);
                outNome.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        tel.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                outTel.setError(null);
                outTel.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        email.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                outEmail.setError(null);
                outEmail.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        senha.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                outSenha.setError(null);
                outSenha.setErrorEnabled(false);
                outSenha2.setError(null);
                outSenha2.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        senha2.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                outSenha2.setError(null);
                outSenha2.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        View.OnClickListener procedimentoCadastrar = v -> {
            boolean valid = true;

            outNome.setError(null);
            outTel.setError(null);
            outEmail.setError(null);
            outSenha.setError(null);
            outSenha2.setError(null);

            if (nome.getText() == null || nome.getText().toString().trim().isEmpty()) {
                outNome.setErrorEnabled(true);
                outNome.setError("Preencha o nome");
                valid = false;
            }
            if (tel.getText() == null || tel.getText().toString().trim().isEmpty()) {
                outTel.setErrorEnabled(true);
                outTel.setError("Preencha o telefone");
                valid = false;
            }
            if (email.getText() == null || email.getText().toString().trim().isEmpty()) {
                outEmail.setErrorEnabled(true);
                outEmail.setError("Preencha o e-mail");
                valid = false;
            }
            if (senha.getText() == null || senha.getText().toString().trim().isEmpty()) {
                outSenha.setErrorEnabled(true);
                outSenha.setError("Preencha a senha");
                valid = false;
            }
            if (senha2.getText() == null || senha2.getText().toString().trim().isEmpty()) {
                outSenha2.setErrorEnabled(true);
                outSenha2.setError("Confirme a senha");
                valid = false;
            }
            if (senha.getText() != null && senha2.getText() != null &&
                    !senha.getText().toString().equals(senha2.getText().toString())) {
                outSenha2.setErrorEnabled(true);
                outSenha2.setError("As senhas não conferem");
                valid = false;
            }

            if (valid) {
                SQLiteDatabase db = new MeuDbHelper(TelaCadastroUsuario.this).getWritableDatabase();
                boolean sucesso = CadUsuario.cadastrarUsuario(
                        db,
                        nome.getText().toString(),
                        tel.getText().toString(),
                        email.getText().toString(),
                        senha.getText().toString(),
                        senha2.getText().toString(),
                        this);

                if (sucesso) {
                    finish(); // encerra a Activity e volta para a anterior
                }
            }
        };

        btnCadastrar.setOnClickListener(procedimentoCadastrar);
        btnSalvartopo.setOnClickListener(procedimentoCadastrar);

    }

}