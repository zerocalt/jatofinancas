package com.example.app1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.content.ContentValues;

import java.util.Date;
import java.text.SimpleDateFormat;

public class MeuDbHelper extends SQLiteOpenHelper {

    private static final String DBNAME = "bdJatoFinancas.db"; // Nome do banco de dados
    private static final int DBVERSION = 1;                    // Versão do banco

    public MeuDbHelper(Context context) {
        super(context, DBNAME, null, DBVERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true); // Habilita chaves estrangeiras para integridade referencial
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabela usuarios: informações pessoais e dados do dispositivo
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS usuarios (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nome TEXT NOT NULL, " +                          // Nome completo do usuário
                        "telefone TEXT, " +                               // Telefone do usuário
                        "email TEXT NOT NULL UNIQUE, " +                  // Email para login (único)
                        "senha TEXT NOT NULL, " +                          // Senha criptografada
                        "data_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, "+  // Data de cadastro do usuário
                        "versao_android TEXT, " +                         // Versão do Android do dispositivo
                        "modelo_dispositivo TEXT, " +                     // Modelo do dispositivo do usuário
                        "ultimo_login DATETIME, " +                        // Data e hora do último login no app
                        "status INTEGER DEFAULT 1" +                       // Status do usuário (1 = ativo, 0 = inativo)
                        ")"
        );

        // Tabela categorias: categorias padrão e específicas do usuário
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS categorias (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_usuario INTEGER, " +                           // ID do usuário, NULL para categorias globais
                        "nome TEXT NOT NULL, " +                           // Nome da categoria
                        "cor TEXT, " +                                    // Cor em hexadecimal para exibir categoria
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, "+ // Data/hora criação da categoria
                        "FOREIGN KEY (id_usuario) REFERENCES usuarios(id)" +
                        ")"
        );
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_categorias_id_usuario ON categorias(id_usuario)");

        // Inserção das categorias padrões (globais)
        String[] categoriasPadroes = {
                "Alimentação", "Transporte", "Educação", "Saúde",
                "Lazer", "Contas", "Salário", "Investimentos"
        };
        String[] coresPadroes = {
                "#f44336", "#2196f3", "#4caf50", "#ffc107",
                "#9c27b0", "#795548", "#3f51b5", "#009688"
        };

        for (int i = 0; i < categoriasPadroes.length; i++) {
            ContentValues cv = new ContentValues();
            cv.putNull("id_usuario");                        // Categoria pública, sem ligação a usuário
            cv.put("nome", categoriasPadroes[i]);
            cv.put("cor", coresPadroes[i]);
            db.insert("categorias", null, cv);
        }

        // Tabela contas: contas bancárias, carteiras, etc.
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS contas (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                        "id_usuario INTEGER NOT NULL, " +                  // ID do usuário dono da conta
                        "nome TEXT NOT NULL, " +                            // Nome da conta
                        "saldo REAL DEFAULT 0, " +                          // Saldo atual
                        "tipo_conta INTEGER NOT NULL, " +                   // Tipo da conta (ex: 1 = Conta Corrente)
                        "cor TEXT, " +                                      // Cor de exibição da conta
                        "mostrar_na_tela_inicial INTEGER DEFAULT 1, " +    // Flag para mostrar na tela inicial
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " + // Data de criação
                        "FOREIGN KEY (id_usuario) REFERENCES usuarios(id)" +
                        ")"
        );
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_contas_id_usuario ON contas(id_usuario)");

        // Tabela transacoes: receitas, despesas, transferências
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS transacoes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_usuario INTEGER NOT NULL, " +                   // ID do usuário
                        "id_conta INTEGER NOT NULL, " +                     // Conta associada
                        "valor REAL NOT NULL, " +                            // Valor da transação
                        "tipo INTEGER NOT NULL, " +                          // Tipo da transação (1: Receita, 2: Despesa, 3: Transferência)
                        "pago INTEGER DEFAULT 0, " +                         // Indicador se foi pago (0 = não, 1 = sim)
                        "recebido INTEGER DEFAULT 0, " +                     // Indicador se foi recebido (0 = não, 1 = sim)
                        "data_movimentacao DATETIME NOT NULL, " +           // Data da movimentação
                        "descricao TEXT, " +                                 // Descrição opcional
                        "id_categoria INTEGER, " +                           // Categoria da transação
                        "observacao TEXT, " +                                // Observação
                        "recorrente INTEGER DEFAULT 0, " +                   // Indicador se é recorrente
                        "repetir_qtd INTEGER DEFAULT 0, " +                  // Quantidade de repetições
                        "repetir_periodo INTEGER DEFAULT 0, " +              // Período de recorrência (ex: 0 - normal, 1 - semanal, 2 - mensal, 3 - bimestral, 4 - trimestral, 5 - semestral, 6 - anual)
                        "id_mestre INTEGER, " +                               // para informar de qual transação é recorrente
                        "recorrente_ativo INTEGER DEFAULT 0, " +             // Indicador se a recorrência está ativa (0 = não, 1 = sim)
                        "id_conta_destino INTEGER, " +                        // Conta destino para transferências
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " + // Data/hora cadastro
                        "FOREIGN KEY (id_usuario) REFERENCES usuarios(id), " +
                        "FOREIGN KEY (id_conta) REFERENCES contas(id), " +
                        "FOREIGN KEY (id_categoria) REFERENCES categorias(id), " +
                        "FOREIGN KEY (id_conta_destino) REFERENCES contas(id)" +
                        ")"
        );
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transacoes_id_usuario ON transacoes(id_usuario)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transacoes_id_conta ON transacoes(id_conta)");

        // Tabela transacoes_parcelas
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS parcelas_transacoes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_transacao INTEGER NOT NULL, " +
                        "numero_parcela INTEGER NOT NULL, " +
                        "valor REAL NOT NULL, " +
                        "data_vencimento DATETIME NOT NULL, " +
                        "pago INTEGER DEFAULT 0, " +
                        "data_pagamento DATETIME, " +
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (id_transacao) REFERENCES transacoes(id)" +
                        ")"
        );
        // Índices
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_parcelas_id_transacao ON parcelas_transacoes(id_transacao)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_parcelas_data_vencimento ON parcelas_transacoes(data_vencimento)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_parcelas_pago ON parcelas_transacoes(pago)");

        // Tabela cartoes: cartão de crédito/débito
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS cartoes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_usuario INTEGER NOT NULL, " +                   // ID do usuário dono do cartão
                        "id_conta INTEGER NOT NULL, " +                     // Conta associada ao cartão
                        "nome TEXT NOT NULL, " +                             // Nome / apelido do cartão
                        "limite REAL NOT NULL, " +                           // Limite disponível
                        "data_vencimento_fatura INTEGER NOT NULL, " +       // Dia do vencimento da fatura
                        "data_fechamento_fatura INTEGER NOT NULL, " +       // Dia do fechamento da fatura
                        "cor TEXT, " +                                      // Cor para exibição
                        "bandeira TEXT, " +                                 // Bandeira do cartão
                        "ativo INTEGER DEFAULT 1, " +                        // Status ativo/inativo
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +  // Data/hora cadastro
                        "FOREIGN KEY (id_usuario) REFERENCES usuarios(id), " +
                        "FOREIGN KEY (id_conta) REFERENCES contas(id)" +
                        ")"
        );
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cartoes_id_usuario ON cartoes(id_usuario)");

        // Tabela faturas: faturas mensais dos cartões
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS faturas (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_cartao INTEGER NOT NULL, " +                     // FK para cartão associado
                        "mes INTEGER NOT NULL, " +                            // Mês da fatura
                        "ano INTEGER NOT NULL, " +                            // Ano da fatura
                        "data_vencimento DATETIME NOT NULL, " +              // Data de vencimento (pegar pela data do cartão)
                        "valor_total REAL DEFAULT 0, " +                      // Valor total da fatura
                        "status INTEGER DEFAULT 0, " +                         // Status: 0 aberta, 1 paga
                        "data_pagamento DATETIME, " +                         // Data do pagamento
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +  // Data/hora cadastro
                        "FOREIGN KEY (id_cartao) REFERENCES cartoes(id)" +
                        ")"
        );
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_faturas_id_cartao ON faturas(id_cartao)");

        // Tabela transacoes_cartao: transações relacionadas a cartões
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS transacoes_cartao (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_usuario INTEGER NOT NULL, " +
                        "id_cartao INTEGER NOT NULL, " +
                        "id_fatura INTEGER, " +
                        "descricao TEXT NOT NULL, " +
                        "valor REAL NOT NULL, " +
                        "paga INTEGER DEFAULT 0, " + // 0 = não paga, 1 = paga
                        "id_categoria INTEGER, " +
                        "data_compra DATETIME NOT NULL, " +
                        "parcelas INTEGER DEFAULT 1, " +
                        "observacao TEXT, " +
                        "recorrente INTEGER DEFAULT 0, " +
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (id_usuario) REFERENCES usuarios(id), " +
                        "FOREIGN KEY (id_cartao) REFERENCES cartoes(id), " +
                        "FOREIGN KEY (id_fatura) REFERENCES faturas(id), " +
                        "FOREIGN KEY (id_categoria) REFERENCES categorias(id)" +
                        ")"
        );
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transacoes_cartao_id_cartao ON transacoes_cartao(id_cartao)");

        // Tabela despesas_recorrentes_cartao: despesas recorrentes associadas a cartões
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS despesas_recorrentes_cartao (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_transacao_cartao INTEGER NOT NULL, " +
                        "valor REAL NOT NULL, " +
                        "paga INTEGER DEFAULT 1, " + // 0 = não paga, 1 = paga
                        "data_inicial DATETIME NOT NULL, " +
                        "data_final DATETIME, " +
                        "id_mestre INTEGER, " +                               // para informar de qual transação é recorrente
                        "recorrente_ativo INTEGER DEFAULT 0, " +             // Indicador se a recorrência está ativa (0 = não, 1 = sim)
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (id_transacao_cartao) REFERENCES transacoes_cartao(id)" +
                        ")"
        );
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_despesas_recorrentes_id_transacao ON despesas_recorrentes_cartao(id_transacao_cartao)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_drc_id_mestre ON despesas_recorrentes_cartao(id_mestre)");

        // Tabela parcelas_cartao: parcelas das compras feitas no cartão
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS parcelas_cartao (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_transacao_cartao INTEGER NOT NULL, " +
                        "numero_parcela INTEGER NOT NULL, " +
                        "valor REAL NOT NULL, " +
                        "id_fatura INTEGER, " +
                        "data_vencimento DATETIME NOT NULL, " +
                        "paga INTEGER DEFAULT 0, " +
                        "data_pagamento DATETIME, " +
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (id_transacao_cartao) REFERENCES transacoes_cartao(id), " +
                        "FOREIGN KEY (id_fatura) REFERENCES faturas(id)" +
                        ")"
        );
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_parcelas_cartao_id_transacao ON parcelas_cartao(id_transacao_cartao)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Para fins de desenvolvimento, apaga tudo e recria
        // Em produção, implemente migração apropriada
        db.execSQL("DROP TABLE IF EXISTS parcelas_cartao");
        db.execSQL("DROP TABLE IF EXISTS despesas_recorrentes_cartao");
        db.execSQL("DROP TABLE IF EXISTS transacoes_cartao");
        db.execSQL("DROP TABLE IF EXISTS faturas");
        db.execSQL("DROP TABLE IF EXISTS cartoes");
        db.execSQL("DROP TABLE IF EXISTS transacoes");
        db.execSQL("DROP TABLE IF EXISTS contas");
        db.execSQL("DROP TABLE IF EXISTS categorias");
        db.execSQL("DROP TABLE IF EXISTS usuarios");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Cria para o usuário com idUsuario as categorias padrão (copiar da tabela categorias onde id_usuario é NULL)
     */
    public void criarCategoriasPadraoParaUsuario(SQLiteDatabase db, int idUsuario) {
        Cursor cursor = db.rawQuery("SELECT nome, cor FROM categorias WHERE id_usuario IS NULL", null);

        while (cursor.moveToNext()) {
            String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
            String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));

            ContentValues cv = new ContentValues();
            cv.put("id_usuario", idUsuario);
            cv.put("nome", nome);

            java.util.Date agora = new java.util.Date();
            String dataFormatada = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(agora);
            cv.put("data_hora_cadastro", dataFormatada);

            cv.put("cor", cor);

            db.insert("categorias", null, cv);
        }
        cursor.close();
    }

}