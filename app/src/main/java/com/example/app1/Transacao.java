package com.example.app1;

public class Transacao {

    private Integer id;
    private Integer idUsuario;
    private String tipo;                 // DESPESA / RECEITA
    private String descricao;
    private Double valor;
    private String data;                 // data_movimentacao
    private Integer pago;                // 0/1
    private String dataPagamento;
    private Integer recorrente;          // 0 = única, 1 = fixa, 2 = parcelada
    private Integer totalParcelas;       // null para transação única e fixa
    private Integer numeroParcela;       // null exceto em parceladas
    private Integer idMestre;            // id da transação original (para parcelada)
    private String categoria;
    private Integer idCartao;

    // Getters e Setters
    // -----------------------------------------------------

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public Integer getPago() { return pago; }
    public void setPago(Integer pago) { this.pago = pago; }

    public String getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(String dataPagamento) { this.dataPagamento = dataPagamento; }

    public Integer getRecorrente() { return recorrente; }
    public void setRecorrente(Integer recorrente) { this.recorrente = recorrente; }

    public Integer getTotalParcelas() { return totalParcelas; }
    public void setTotalParcelas(Integer totalParcelas) { this.totalParcelas = totalParcelas; }

    public Integer getNumeroParcela() { return numeroParcela; }
    public void setNumeroParcela(Integer numeroParcela) { this.numeroParcela = numeroParcela; }

    public Integer getIdMestre() { return idMestre; }
    public void setIdMestre(Integer idMestre) { this.idMestre = idMestre; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public Integer getIdCartao() { return idCartao; }
    public void setIdCartao(Integer idCartao) { this.idCartao = idCartao; }
}