package com.example.app1;

public class Categoria {
    public String nome;
    public String cor;

    public Categoria(String nome, String cor) {
        this.nome = nome;
        this.cor = cor;
    }

    @Override
    public String toString() {
        return nome;
    }
}