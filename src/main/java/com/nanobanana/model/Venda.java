package com.nanobanana.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * VENDA REGISTRADA PELO SISTEMA
 * O stock baixa automaticamente quando a venda é finalizada.
 * Vendedores podem vender, mas NÃO podem cancelar sem autorização.
 */
@Entity
@Table(name = "vendas")
public class Venda {

    public enum Status {
        CONCLUIDA,
        CANCELADA,
        PENDENTE
    }

    public enum MetodoPagamento {
        CASH,
        MPESA,
        EMOLA,
        FIADO,
        CARTAO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID vendedorId; // quem vendeu

    private UUID clienteId; // cliente (se registrado)

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private MetodoPagamento metodoPagamento;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private Status status = Status.CONCLUIDA;

    // Dados do fiado (se aplicável)
    private String nomeClienteFiado;
    private String telefoneClienteFiado;

    @Column(precision = 10, scale = 2)
    private BigDecimal valorPago;

    @Column(precision = 10, scale = 2)
    private BigDecimal troco;

    private LocalDateTime dataHora = LocalDateTime.now();

    // Dados de cancelamento
    private UUID canceladaPor; // quem cancelou (só DONO/GERENTE)
    @Column(columnDefinition = "TEXT")
    private String motivoCancelamento; // OBRIGATÓRIO se cancelar

    @Column(length = 255)
    private String hash; // anti-fraude

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @PrePersist
    public void gerarHash() {
        String conteudo = id + "|" + tenantId + "|" + total + "|" + dataHora + "|" + status;
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

    public UUID getVendedorId() { return vendedorId; }
    public void setVendedorId(UUID vendedorId) { this.vendedorId = vendedorId; }

    public UUID getClienteId() { return clienteId; }
    public void setClienteId(UUID clienteId) { this.clienteId = clienteId; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDesconto() { return desconto; }
    public void setDesconto(BigDecimal desconto) { this.desconto = desconto; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public MetodoPagamento getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(MetodoPagamento metodoPagamento) { this.metodoPagamento = metodoPagamento; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getNomeClienteFiado() { return nomeClienteFiado; }
    public void setNomeClienteFiado(String nomeClienteFiado) { this.nomeClienteFiado = nomeClienteFiado; }

    public String getTelefoneClienteFiado() { return telefoneClienteFiado; }
    public void setTelefoneClienteFiado(String telefoneClienteFiado) { this.telefoneClienteFiado = telefoneClienteFiado; }

    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }

    public BigDecimal getTroco() { return troco; }
    public void setTroco(BigDecimal troco) { this.troco = troco; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public UUID getCanceladaPor() { return canceladaPor; }
    public void setCanceladaPor(UUID canceladaPor) { this.canceladaPor = canceladaPor; }

    public String getMotivoCancelamento() { return motivoCancelamento; }
    public void setMotivoCancelamento(String motivoCancelamento) { this.motivoCancelamento = motivoCancelamento; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    // Compatibility getters for admin templates
    public String getFormaPagamento() {
        return metodoPagamento != null ? metodoPagamento.name() : "";
    }

    public BigDecimal getValor() {
        return total;
    }

    public java.time.LocalDate getData() {
        return dataHora != null ? dataHora.toLocalDate() : null;
    }

    public boolean isFiado() {
        return metodoPagamento == MetodoPagamento.FIADO;
    }
}
