package com.example.app1;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

        EditText nome = findViewById(R.id.nome);
        EditText tel = findViewById(R.id.tel);
        tel.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        EditText email = findViewById(R.id.email);
        EditText senha = findViewById(R.id.senha);
        EditText senha2 = findViewById(R.id.senha2);
        Button btnCadastrar = findViewById(R.id.btnCadastrar);
        LinearLayout btnSalvartopo = findViewById(R.id.btnSalvartopo);

        View.OnClickListener procedimentoCadastrar = v -> {
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
                finish();  // encerra a Activity e volta para a anterior
            }
        };

        btnCadastrar.setOnClickListener(procedimentoCadastrar);
        btnSalvartopo.setOnClickListener(procedimentoCadastrar);

    }

}