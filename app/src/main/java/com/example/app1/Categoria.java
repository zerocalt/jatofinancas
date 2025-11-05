package com.example.app1;

public class Categoria {
    private int id;
    private String nome;
    private String cor;
    private double totalDespesa;
    private boolean emUso; // Novo campo

    // Construtor principal
    public Categoria(int id, String nome, String cor, double totalDespesa, boolean emUso) {
        this.id = id;
        this.nome = nome;
        this.cor = cor;
        this.totalDespesa = totalDespesa;
        this.emUso = emUso;
    }

    // Construtor simplificado para compatibilidade
    public Categoria(int id, String nome, String cor) {
        this(id, nome, cor, 0, false);
    }

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

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public double getTotalDespesa() {
        return totalDespesa;
    }

    public void setTotalDespesa(double totalDespesa) {
        this.totalDespesa = totalDespesa;
    }

    public boolean isEmUso() {
        return emUso;
    }

    public void setEmUso(boolean emUso) {
        this.emUso = emUso;
    }
}
