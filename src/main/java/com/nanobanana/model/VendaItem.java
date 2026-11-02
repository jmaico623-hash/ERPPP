package com.nanobanana.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ITENS DE UMA VENDA
 * Cada item registra qual produto foi vendido e em qual quantidade.
 */
@Entity
@Table(name = "venda_itens")
public class VendaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID vendaId;

    @Column(nullable = false)
    private UUID produtoId;

    @Column(nullable = false, length = 255)
    private String nomeProduto;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getVendaId() { return vendaId; }
    public void setVendaId(UUID vendaId) { this.vendaId = vendaId; }

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

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
