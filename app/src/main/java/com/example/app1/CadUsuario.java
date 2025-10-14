package com.example.app1;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import android.util.Base64;

public class CadUsuario {

    private static final int SALT_LENGTH = 16; // bytes
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // bits

    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    public static String hashPassword(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + Base64.encodeToString(hash, Base64.NO_WRAP);
    }

    private static boolean emailJaCadastrado(SQLiteDatabase db, String email) {
        String consulta = "SELECT 1 FROM usuarios WHERE email = ? LIMIT 1";
        Cursor cursor = db.rawQuery(consulta, new String[]{email});
        boolean existe = cursor.moveToFirst();
        cursor.close();
        return existe;
    }

    public static boolean cadastrarUsuario(SQLiteDatabase db, String nome, String telefone, String email, String senha, String senha2, Context context) {
        try {
            String nomeDigitado = nome.trim();
            String telDigitado = telefone.trim();
            String emailDigitado = email.trim();
            String senhaDigitada = senha.trim();
            String senha2Digitada = senha2.trim();

            // Verifica campos obrigatórios
            if (nomeDigitado.isEmpty() || telDigitado.isEmpty() || emailDigitado.isEmpty() || senhaDigitada.isEmpty() || senha2Digitada.isEmpty()) {
                Toast.makeText(context, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Verifica se as senhas conferem
            if (!senhaDigitada.equals(senha2Digitada)) {
                Toast.makeText(context, "As senhas não conferem!", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Verifica se e-mail já cadastrado
            if (emailJaCadastrado(db, emailDigitado)) {
                Toast.makeText(context, "Este e-mail já está cadastrado!", Toast.LENGTH_SHORT).show();
                return false;
            }

            byte[] salt = generateSalt();
            String senhaHashed = hashPassword(senhaDigitada, salt);

            String dataAtual = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", new Locale("pt", "BR")).format(new Date());
            String versaoAndroid = android.os.Build.VERSION.RELEASE;
            String modeloDispositivo = android.os.Build.MODEL;

            ContentValues valores = new ContentValues();
            valores.put("nome", nomeDigitado);
            valores.put("telefone", telDigitado);
            valores.put("email", emailDigitado);
            valores.put("senha", senhaHashed);
            valores.put("data_cad", dataAtual);
            valores.put("versao_android", versaoAndroid);
            valores.put("modelo_dispositivo", modeloDispositivo);
            valores.put("ultimo_login", "");  // deixado vazio no cadastro
            valores.put("status", "ativo");

            long idInserido = db.insert("usuarios", null, valores);

            if (idInserido != -1) {
                Toast.makeText(context, "Usuário cadastrado com sucesso!", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(context, "Falha ao cadastrar usuário.", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Erro ao cadastrar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }
}