package com.example.app1.interfaces; // Sugestão: crie um pacote 'interfaces'

public interface BottomMenuListener {
    // A Activity Pai usará este método para iniciar o Fragment de Cadastro
    void onFabDespesaCartaoClick(int idUsuario);
}