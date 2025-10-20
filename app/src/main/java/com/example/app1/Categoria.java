package com.example.app1;

public class Categoria {
    public int id;
    public String nome;
    public String cor;

    public Categoria(int id, String nome, String cor) {
        this.id = id;
        this.nome = nome;
        this.cor = cor;
    }

    @Override
    public String toString() {
        return nome;
    }
}