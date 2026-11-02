package com.nanobanana.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REGISTRA CADA BAIXA MANUAL DE STOCK
 * Apenas o DONO pode criar baixas (perda, validade, quebra, roubo, ajuste).
 * Este é o coração do controle anti-desvio.
 */
@Entity
@Table(name = "baixas_stock")
public class BaixaStock {

    public enum TipoBaixa {
        PERDA,      // Produto perdido
        VALIDADE,   // Produto venceu
        QUEBRA,     // Produto quebrado
        ROUBO,      // Produto roubado
        AJUSTE      // Ajuste de inventário
    }

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoBaixa tipoBaixa;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motivo; // OBRIGATÓRIO

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataBaixa = LocalDateTime.now();

    @Column(length = 255)
    private String hash; // anti-fraude

    @PrePersist
    public void gerarHash() {
        String conteudo = tipoBaixa + "|" + produtoId + "|" + quantidade + "|" + dataBaixa;
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

    public TipoBaixa getTipoBaixa() { return tipoBaixa; }
    public void setTipoBaixa(TipoBaixa tipoBaixa) { this.tipoBaixa = tipoBaixa; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public LocalDateTime getDataBaixa() { return dataBaixa; }
    public void setDataBaixa(LocalDateTime dataBaixa) { this.dataBaixa = dataBaixa; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}
