package com.example.app1;

public class Conta {
    private int id;
    private String nome;
    private double saldo;
    private String cor;
    private int tipoConta; // Ex: 1-Corrente, 2-Poupança, 3-Investimento, 4-Outros
    private boolean mostrarNaTelaInicial;
    private boolean emUso; // Novo campo

    // Construtor completo
    public Conta(int id, String nome, double saldo, String cor, int tipoConta, boolean mostrarNaTelaInicial, boolean emUso) {
        this.id = id;
        this.nome = nome;
        this.saldo = saldo;
        this.cor = cor;
        this.tipoConta = tipoConta;
        this.mostrarNaTelaInicial = mostrarNaTelaInicial;
        this.emUso = emUso;
    }

    // Construtor simplificado para compatibilidade
    public Conta(int id, String nome, double saldo, String cor, int tipoConta, boolean mostrarNaTelaInicial) {
        this(id, nome, saldo, cor, tipoConta, mostrarNaTelaInicial, false);
    }

    // Getters e Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public int getTipoConta() {
        return tipoConta;
    }

    public void setTipoConta(int tipoConta) {
        this.tipoConta = tipoConta;
    }

    public boolean isMostrarNaTelaInicial() {
        return mostrarNaTelaInicial;
    }

    public void setMostrarNaTelaInicial(boolean mostrarNaTelaInicial) {
        this.mostrarNaTelaInicial = mostrarNaTelaInicial;
    }

    public boolean isEmUso() {
        return emUso;
    }

    public void setEmUso(boolean emUso) {
        this.emUso = emUso;
    }
}
