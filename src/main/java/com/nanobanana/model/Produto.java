package com.nanobanana.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(length = 50)
    private String codigoBarras;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 100)
    private String categoria;

    @Column(precision = 10, scale = 2)
    private BigDecimal precoCusto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoVenda;

    @Column(nullable = false)
    private int stockAtual = 0;

    @Column(nullable = false)
    private int stockMinimo = 5;

    private Integer stockMaximo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public BigDecimal getPrecoCusto() { return precoCusto; }
    public void setPrecoCusto(BigDecimal precoCusto) { this.precoCusto = precoCusto; }

    public BigDecimal getPrecoVenda() { return precoVenda; }
    public void setPrecoVenda(BigDecimal precoVenda) { this.precoVenda = precoVenda; }

    public int getStockAtual() { return stockAtual; }
    public void setStockAtual(int stockAtual) { this.stockAtual = stockAtual; }

    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    public Integer getStockMaximo() { return stockMaximo; }
    public void setStockMaximo(Integer stockMaximo) { this.stockMaximo = stockMaximo; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    public boolean stockBaixo() {
        return stockAtual <= stockMinimo;
    }

    public boolean semStock() {
        return stockAtual <= 0;
    }

    public boolean abaixoDeCusto(BigDecimal preco) {
        return precoCusto != null && preco.compareTo(precoCusto) < 0;
    }

    public BigDecimal getValorStock() {
        if (precoVenda == null) return BigDecimal.ZERO;
        return precoVenda.multiply(BigDecimal.valueOf(stockAtual));
    }
}
