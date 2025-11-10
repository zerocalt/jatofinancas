package com.example.app1;

public class Cartao {
    private int id;
    private int idUsuario;
    private int idConta;
    private String nome;
    private double limite;
    private int dataVencimentoFatura;
    private int dataFechamentoFatura;
    private String cor;
    private String bandeira;
    private int ativo;
    private String dataHoraCadastro;

    // 🔹 Construtor completo
    public Cartao(int id, int idUsuario, int idConta, String nome, double limite,
                  int dataVencimentoFatura, int dataFechamentoFatura,
                  String cor, String bandeira, int ativo, String dataHoraCadastro) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.idConta = idConta;
        this.nome = nome;
        this.limite = limite;
        this.dataVencimentoFatura = dataVencimentoFatura;
        this.dataFechamentoFatura = dataFechamentoFatura;
        this.cor = cor;
        this.bandeira = bandeira;
        this.ativo = ativo;
        this.dataHoraCadastro = dataHoraCadastro;
    }

    // 🔹 Construtor simplificado (útil para preencher spinner/autocomplete)
    public Cartao(int id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    // 🔹 Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public int getIdConta() { return idConta; }
    public void setIdConta(int idConta) { this.idConta = idConta; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public void setDataVencimentoFatura(int dataVencimentoFatura) { this.dataVencimentoFatura = dataVencimentoFatura; }

    public void setCor(String cor) { this.cor = cor; }

    public String getBandeira() { return bandeira; }
    public void setBandeira(String bandeira) { this.bandeira = bandeira; }

    public int getAtivo() { return ativo; }
    public void setAtivo(int ativo) { this.ativo = ativo; }

    public String getDataHoraCadastro() { return dataHoraCadastro; }
    public void setDataHoraCadastro(String dataHoraCadastro) { this.dataHoraCadastro = dataHoraCadastro; }

    // 🔹 Para exibir o nome no AutoCompleteTextView
    @Override
    public String toString() {
        return nome;
    }
}