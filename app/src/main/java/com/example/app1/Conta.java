package com.example.app1;

public class Conta {
    private int id;
    private String nome;
    private double saldo;
    private int tipoConta;
    private String cor;
    private int mostrarNaTelaInicial;

    public Conta(int id, String nome, double saldo, int tipoConta, String cor, int mostrarNaTelaInicial) {
        this.id = id;
        this.nome = nome;
        this.saldo = saldo;
        this.tipoConta = tipoConta;
        this.cor = cor;
        this.mostrarNaTelaInicial = mostrarNaTelaInicial;
    }

    // Construtor para novas contas (sem id)
    public Conta(String nome, double saldo, int tipoConta, String cor, int mostrarNaTelaInicial) {
        this.nome = nome;
        this.saldo = saldo;
        this.tipoConta = tipoConta;
        this.cor = cor;
        this.mostrarNaTelaInicial = mostrarNaTelaInicial;
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

    public int getTipoConta() {
        return tipoConta;
    }

    public void setTipoConta(int tipoConta) {
        this.tipoConta = tipoConta;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public int getMostrarNaTelaInicial() {
        return mostrarNaTelaInicial;
    }

    public void setMostrarNaTelaInicial(int mostrarNaTelaInicial) {
        this.mostrarNaTelaInicial = mostrarNaTelaInicial;
    }

    @Override
    public String toString() {
        return nome;
    }
}