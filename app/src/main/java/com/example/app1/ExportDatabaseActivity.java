package com.example.app1;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExportDatabaseActivity extends AppCompatActivity {

    private EditText edtSQL;
    private Button btnGerarSQL, btnExecutarSQL, btnCopiar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_database);

        edtSQL = findViewById(R.id.edtSQL);
        edtSQL.setMovementMethod(new ScrollingMovementMethod()); // permitir scroll

        btnGerarSQL = findViewById(R.id.btnGerarSQL);
        btnExecutarSQL = findViewById(R.id.btnExecutarSQL);
        btnCopiar = findViewById(R.id.btnCopiar);

        btnGerarSQL.setOnClickListener(v -> gerarSQL());
        btnExecutarSQL.setOnClickListener(v -> executarSQL());
        btnCopiar.setOnClickListener(v -> copiarSQL());
    }

    private void copiarSQL() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("SQL Export", edtSQL.getText().toString());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "SQL copiado para a área de transferência", Toast.LENGTH_SHORT).show();
    }

    private void gerarSQL() {
        StringBuilder sqlBuilder = new StringBuilder();
        try (MeuDbHelper dbHelper = new MeuDbHelper(this);
             SQLiteDatabase db = dbHelper.getReadableDatabase()) {

            // Buscar todas as tabelas do usuário no banco de dados
            List<String> tabelas = new ArrayList<>();
            try (Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%'", null)) {
                if (c.moveToFirst()) {
                    do {
                        tabelas.add(c.getString(0));
                    } while (c.moveToNext());
                }
            }

            for (String tabela : tabelas) {
                try (Cursor c = db.rawQuery("SELECT * FROM " + tabela, null)) {
                    int colCount = c.getColumnCount();
                    while (c.moveToNext()) {
                        sqlBuilder.append(String.format(Locale.ROOT, "INSERT OR REPLACE INTO %s (", tabela));
                        for (int i = 0; i < colCount; i++) {
                            sqlBuilder.append(c.getColumnName(i));
                            if (i < colCount - 1) sqlBuilder.append(", ");
                        }
                        sqlBuilder.append(") VALUES (");
                        for (int i = 0; i < colCount; i++) {
                            int type = c.getType(i);
                            switch (type) {
                                case Cursor.FIELD_TYPE_NULL:
                                    sqlBuilder.append("NULL");
                                    break;
                                case Cursor.FIELD_TYPE_INTEGER:
                                    sqlBuilder.append(c.getLong(i));
                                    break;
                                case Cursor.FIELD_TYPE_FLOAT:
                                    sqlBuilder.append(c.getDouble(i));
                                    break;
                                case Cursor.FIELD_TYPE_STRING:
                                    sqlBuilder.append("'").append(c.getString(i).replace("'", "''")).append("'");
                                    break;
                                case Cursor.FIELD_TYPE_BLOB:
                                    sqlBuilder.append("NULL"); // simplificação para blobs
                                    break;
                            }
                            if (i < colCount - 1) sqlBuilder.append(", ");
                        }
                        sqlBuilder.append(");\n");
                    }
                }
            }

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao gerar SQL: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("ExportDB", "Erro gerarSQL", e);
        }

        edtSQL.setText(sqlBuilder.toString());
    }

    private void executarSQL() {
        String sql = edtSQL.getText().toString();
        if (sql.isEmpty()) {
            Toast.makeText(this, "Cole os comandos SQL para executar", Toast.LENGTH_SHORT).show();
            return;
        }

        try (MeuDbHelper dbHelper = new MeuDbHelper(this);
             SQLiteDatabase db = dbHelper.getWritableDatabase()) {

            db.beginTransaction();
            String[] comandos = sql.split(";\\s*\\n"); // separa por ponto e vírgula seguido de nova linha
            for (String cmd : comandos) {
                cmd = cmd.trim();
                if (!cmd.isEmpty()) {
                    db.execSQL(cmd);
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();

            Toast.makeText(this, "SQL executado com sucesso!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao executar SQL: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("ExportDB", "Erro executarSQL", e);
        }
    }
}
