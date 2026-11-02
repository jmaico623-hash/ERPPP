package com.nanobanana.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REGISTRA CADA ENTRADA DE STOCK
 * Apenas o DONO pode criar entradas.
 * Este é o coração do controle anti-desvio.
 */
@Entity
@Table(name = "entradas_stock")
public class EntradaStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID produtoId;

    @Column(nullable = false)
    private int quantidade; // sempre positivo

    @Column(nullable = false)
    private UUID donoId; // quem autorizou (SEMPRE DONO)

    @Column(length = 255)
    private String fornecedor;

    @Column(length = 100)
    private String notaFiscal;

    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal precoCusto;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataEntrada = LocalDateTime.now();

    @Column(length = 255)
    private String hash; // anti-fraude

    @PrePersist
    public void gerarHash() {
        String conteudo = tipo() + "|" + produtoId + "|" + quantidade + "|" + dataEntrada;
        this.hash = sha256(conteudo);
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String tipo() {
        return "ENTRADA";
    }

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getProdutoId() { return produtoId; }
    public void setProdutoId(UUID produtoId) { this.produtoId = produtoId; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public UUID getDonoId() { return donoId; }
    public void setDonoId(UUID donoId) { this.donoId = donoId; }

    public String getFornecedor() { return fornecedor; }
    public void setFornecedor(String fornecedor) { this.fornecedor = fornecedor; }

    public String getNotaFiscal() { return notaFiscal; }
    public void setNotaFiscal(String notaFiscal) { this.notaFiscal = notaFiscal; }

    public java.math.BigDecimal getPrecoCusto() { return precoCusto; }
    public void setPrecoCusto(java.math.BigDecimal precoCusto) { this.precoCusto = precoCusto; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public LocalDateTime getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDateTime dataEntrada) { this.dataEntrada = dataEntrada; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}
