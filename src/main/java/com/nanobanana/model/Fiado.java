package com.nanobanana.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REGISTRO DE FIADO
 * Vendedores podem registrar fiados, mas o dono vê tudo.
 */
@Entity
@Table(name = "fiados")
public class Fiado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID vendaId; // venda original que gerou o fiado

    @Column(nullable = false, length = 255)
    private String nomeCliente;

    @Column(length = 50)
    private String telefoneCliente;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private boolean pago = false;

    private LocalDateTime dataPagamento;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getVendaId() { return vendaId; }
    public void setVendaId(UUID vendaId) { this.vendaId = vendaId; }

    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }

    public String getTelefoneCliente() { return telefoneCliente; }
    public void setTelefoneCliente(String telefoneCliente) { this.telefoneCliente = telefoneCliente; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public boolean isPago() { return pago; }
    public void setPago(boolean pago) { this.pago = pago; }

    public LocalDateTime getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDateTime dataPagamento) { this.dataPagamento = dataPagamento; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
}
