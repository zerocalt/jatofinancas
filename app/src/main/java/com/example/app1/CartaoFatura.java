package com.example.app1;

public class CartaoFatura {
    private Cartao cartao;
    private double valorFatura;

    public CartaoFatura(Cartao cartao, double valorFatura) {
        this.cartao = cartao;
        this.valorFatura = valorFatura;
    }

    public Cartao getCartao() {
        return cartao;
    }

    public double getValorFatura() {
        return valorFatura;
    }
}
