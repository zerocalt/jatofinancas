package com.example.app1;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class MeuDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "bdJatoFinancas.db";
    private static final int DB_VERSION = 2;

    public MeuDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true); // Habilita chaves estrangeiras
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabela: usuarios
        // Armazena os dados de login e dispositivo de cada usuário
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS usuarios (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +                    // ID único do usuário
                        "nome TEXT NOT NULL, " +                                      // Nome completo
                        "telefone TEXT, " +                                           // Telefone (opcional)
                        "email TEXT NOT NULL UNIQUE, " +                              // Email (único)
                        "senha TEXT NOT NULL, " +                                     // Senha (deve estar criptografada)
                        "data_cad DATETIME DEFAULT CURRENT_TIMESTAMP, " +             // Data de cadastro
                        "versao_android TEXT, " +                                     // Versão do Android do dispositivo
                        "modelo_dispositivo TEXT, " +                                 // Modelo do celular
                        "ultimo_login DATETIME, " +                                   // Último acesso
                        "status INTEGER DEFAULT 1" +                                  // 1 = ativo, 0 = inativo
                        ")"
        );

        // Tabela: categorias
        // Categorias personalizadas por usuário para despesas/receitas
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS categorias (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +                    // ID da categoria
                        "id_usuario INTEGER, " +                             // Dono da categoria
                        "nome TEXT NOT NULL, " +                                      // Nome (ex: Transporte)
                        "cor TEXT, " +                                                // Cor de identificação visual
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +   // Data de criação
                        "FOREIGN KEY (id_usuario) REFERENCES usuarios(id)" +
                        ")"
        );

        // Tabela: contas
        // Contas bancárias, carteira, investimentos, etc.
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS contas (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +                    // ID da conta
                        "id_usuario INTEGER NOT NULL, " +                             // Dono da conta
                        "nome TEXT NOT NULL, " +                                      // Nome da conta (ex: Banco X)
                        "saldo REAL DEFAULT 0, " +                                    // Saldo atual
                        "tipo_conta INTEGER NOT NULL, " +                             // Tipo: 1=Corrente, 2=Poupança, etc.
                        "cor TEXT, " +                                                // Cor para identificação
                        "mostrar_na_tela_inicial INTEGER DEFAULT 1, " +               // 1=Sim, 0=Não
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +   // Data de criação
                        "FOREIGN KEY (id_usuario) REFERENCES usuarios(id)" +
                        ")"
        );

        // Tabela: transacoes
        // Movimentações financeiras (receitas, despesas, transferências)
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS transacoes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +                    // ID da transação
                        "id_usuario INTEGER NOT NULL, " +                             // Dono da transação
                        "id_conta INTEGER NOT NULL, " +                               // Conta de origem/destino
                        "valor REAL NOT NULL, " +                                     // Valor da movimentação
                        "tipo INTEGER NOT NULL, " +                                   // 1=Receita, 2=Despesa, 3=Cartão, 4=Transferência
                        "pago INTEGER DEFAULT 0, " +                                  // 0=não, 1=sim (para despesas)
                        "recebido INTEGER DEFAULT 0, " +                              // 0=não, 1=sim (para receitas)
                        "data_movimentacao DATETIME NOT NULL, " +                     // Data da transação
                        "descricao TEXT, " +                                          // Título breve
                        "id_categoria INTEGER, " +                                    // Categoria da transação
                        "observacao TEXT, " +                                         // Detalhes adicionais
                        "recorrente INTEGER DEFAULT 0, " +                            // 1=lançamento recorrente
                        "repetir_qtd INTEGER DEFAULT 0, " +                           // Quantas vezes repete
                        "repetir_periodo TEXT, " +                                    // 'mensal', 'semanal', etc.
                        "id_conta_destino INTEGER, " +                                // Usado em transferências
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +   // Data de inclusão
                        "FOREIGN KEY (id_usuario) REFERENCES usuarios(id), " +
                        "FOREIGN KEY (id_conta) REFERENCES contas(id), " +
                        "FOREIGN KEY (id_categoria) REFERENCES categorias(id), " +
                        "FOREIGN KEY (id_conta_destino) REFERENCES contas(id)" +
                        ")"
        );

        // Tabela: cartoes
        // Cartões de crédito cadastrados pelo usuário
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS cartoes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +                    // ID do cartão
                        "id_usuario INTEGER NOT NULL, " +                             // Dono do cartão
                        "id_conta INTEGER NOT NULL, " +                               // Conta associada ao cartão
                        "nome TEXT NOT NULL, " +                                      // Nome do cartão (ex: Meu Visa)
                        "limite REAL NOT NULL, " +                                    // Limite total do cartão
                        "data_vencimento_fatura INTEGER NOT NULL, " +                 // Dia do vencimento (ex: 10)
                        "data_fechamento_fatura INTEGER NOT NULL, " +                 // Dia do fechamento (ex: 25)
                        "cor TEXT, " +                                                // Cor visual
                        "bandeira TEXT, " +                                           // Bandeira: Visa, Mastercard, etc.
                        "ativo INTEGER DEFAULT 1, " +                                 // 1=ativo, 0=inativo
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +   // Data de cadastro
                        "FOREIGN KEY (id_usuario) REFERENCES usuarios(id)," +
                        "FOREIGN KEY (id_conta) REFERENCES contas(id)" +
                        ")"
        );

        // Tabela: faturas
        // Faturas mensais de cada cartão
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS faturas (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +                    // ID da fatura
                        "id_cartao INTEGER NOT NULL, " +                              // Cartão associado
                        "mes INTEGER NOT NULL, " +                                    // Mês (1 a 12)
                        "ano INTEGER NOT NULL, " +                                    // Ano (ex: 2025)
                        "valor_total REAL DEFAULT 0, " +                              // Soma das parcelas
                        "status INTEGER DEFAULT 0, " +                                // 0=aberta, 1=paga
                        "data_pagamento DATETIME, " +                                 // Quando foi paga
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +   // Data de criação
                        "FOREIGN KEY (id_cartao) REFERENCES cartoes(id)" +
                        ")"
        );

        // Tabela: transacoes_cartao
        // Compras feitas no cartão de crédito
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS transacoes_cartao (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +                    // ID da compra no cartão
                        "id_usuario INTEGER NOT NULL, " +                             // Dono da transação
                        "id_cartao INTEGER NOT NULL, " +                              // Cartão usado
                        "id_fatura INTEGER, " +                                       // Fatura atual (pode mudar com parcelas)
                        "descricao TEXT NOT NULL, " +                                 // Descrição da compra
                        "valor REAL NOT NULL, " +                                     // Valor total da compra
                        "id_categoria INTEGER, " +                                    // Categoria da despesa
                        "data_compra DATETIME NOT NULL, " +                           // Data da compra
                        "parcelas INTEGER DEFAULT 1, " +                              // Número de parcelas
                        "observacao TEXT, " +                                         // Detalhes
                        "recorrente INTEGER DEFAULT 0, " +                            // Despesa recorrente
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +   // Data de cadastro
                        "FOREIGN KEY (id_usuario) REFERENCES usuarios(id), " +
                        "FOREIGN KEY (id_cartao) REFERENCES cartoes(id), " +
                        "FOREIGN KEY (id_fatura) REFERENCES faturas(id), " +
                        "FOREIGN KEY (id_categoria) REFERENCES categorias(id)" +
                        ")"
        );

        // Tabela: despesas_recorrentes_cartao
        // Tabela para armazenar histórico de valores das despesas recorrentes no cartão
        // Permite registrar mudanças de valor ao longo do tempo sem perder histórico
        // Cada registro indica o valor da despesa a partir de uma data específica
        // possibilitando calcular corretamente o valor vigente em faturas passadas e futuras.
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS despesas_recorrentes_cartao (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +                          // ID da despesa recorrente no cartão
                        "id_transacao_cartao INTEGER NOT NULL, " +                         // FK para transacao recorrente original em transacoes_cartao
                        "valor REAL NOT NULL, " +                                           // Valor vigente a partir da data_inicial
                        "data_inicial DATETIME NOT NULL, " +                               // Data de início da vigência desse valor
                        "data_final DATETIME, " +                                          // Data fim da vigência (opcional para facilitar consultas)
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +        // Data/hora do registro da alteração
                        "FOREIGN KEY (id_transacao_cartao) REFERENCES transacoes_cartao(id)" +
                        ")"
        );

        // Tabela: parcelas_cartao
        // Cada parcela de uma compra no cartão
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS parcelas_cartao (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +                    // ID da parcela
                        "id_transacao_cartao INTEGER NOT NULL, " +                    // Compra original
                        "numero_parcela INTEGER NOT NULL, " +                         // Número da parcela (1ª, 2ª...)
                        "valor REAL NOT NULL, " +                                     // Valor da parcela
                        "id_fatura INTEGER, " +                                       // Fatura onde será cobrada
                        "data_vencimento DATETIME NOT NULL, " +                       // Data prevista
                        "paga INTEGER DEFAULT 0, " +                                  // 0=não, 1=sim
                        "data_pagamento DATETIME, " +                                 // Data real do pagamento
                        "data_hora_cadastro DATETIME DEFAULT CURRENT_TIMESTAMP, " +   // Data de criação
                        "FOREIGN KEY (id_transacao_cartao) REFERENCES transacoes_cartao(id), " +
                        "FOREIGN KEY (id_fatura) REFERENCES faturas(id)" +
                        ")"
        );

        //populando a tabela categorias
        db.execSQL(
                "INSERT INTO categorias (id_usuario, nome, cor) VALUES" +
                "(NULL, 'Alimentação', '#E15759')," +
                "(NULL, 'Cuidados pessoais', '#FF9DA7')," +
                "(NULL, 'Educação', '#76B7B2')," +
                "(NULL, 'Impostos e taxas', '#BCBD22')," +
                "(NULL, 'Investimentos', '#17BECF')," +
                "(NULL, 'Lazer', '#EDC948')," +
                "(NULL, 'Moradia', '#4E79A7')," +
                "(NULL, 'Outros', '#7F7F7F')," +
                "(NULL, 'Presentes/Doações', '#A0CBE8')," +
                "(NULL, 'Saúde', '#59A14F')," +
                "(NULL, 'Seguros', '#9C755F')," +
                "(NULL, 'Serviços e assinaturas', '#8C564B')," +
                "(NULL, 'Transporte', '#F28E2B')," +
                "(NULL, 'Vestuário', '#B07AA1');"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Em produção, faça migrações! Aqui estamos recriando.
        db.execSQL("DROP TABLE IF EXISTS parcelas_cartao");
        db.execSQL("DROP TABLE IF EXISTS transacoes_cartao");
        db.execSQL("DROP TABLE IF EXISTS faturas");
        db.execSQL("DROP TABLE IF EXISTS cartoes");
        db.execSQL("DROP TABLE IF EXISTS transacoes");
        db.execSQL("DROP TABLE IF EXISTS contas");
        db.execSQL("DROP TABLE IF EXISTS categorias");
        db.execSQL("DROP TABLE IF EXISTS usuarios");
        onCreate(db);
    }
}
