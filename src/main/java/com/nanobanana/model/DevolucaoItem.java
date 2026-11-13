package com.nanobanana.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "devolucao_itens")
public class DevolucaoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID devolucaoId;

    @Column(nullable = false)
    private UUID vendaItemId;

    @Column(nullable = false)
    private UUID produtoId;

    @Column(nullable = false, length = 255)
    private String nomeProduto;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDevolucaoId() { return devolucaoId; }
    public void setDevolucaoId(UUID devolucaoId) { this.devolucaoId = devolucaoId; }

    public UUID getVendaItemId() { return vendaItemId; }
    public void setVendaItemId(UUID vendaItemId) { this.vendaItemId = vendaItemId; }

    public UUID getProdutoId() { return produtoId; }
    public void setProdutoId(UUID produtoId) { this.produtoId = produtoId; }

    public String getNomeProduto() { return nomeProduto; }
    public void setNomeProduto(String nomeProduto) { this.nomeProduto = nomeProduto; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
