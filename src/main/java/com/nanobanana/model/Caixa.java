package com.nanobanana.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "caixas")
public class Caixa {

    public enum Status {
        ABERTO, FECHADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID usuarioAberturaId;

    private UUID usuarioFechoId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorAbertura;

    @Column(precision = 12, scale = 2)
    private BigDecimal valorFechoInformado;

    @Column(precision = 12, scale = 2)
    private BigDecimal valorEsperado;

    @Column(precision = 12, scale = 2)
    private BigDecimal diferenca;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ABERTO;

    @Column(nullable = false)
    private LocalDateTime dataAbertura = LocalDateTime.now();

    private LocalDateTime dataFecho;

    @Column(columnDefinition = "TEXT")
    private String observacaoAbertura;

    @Column(columnDefinition = "TEXT")
    private String observacaoFecho;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getUsuarioAberturaId() { return usuarioAberturaId; }
    public void setUsuarioAberturaId(UUID usuarioAberturaId) { this.usuarioAberturaId = usuarioAberturaId; }

    public UUID getUsuarioFechoId() { return usuarioFechoId; }
    public void setUsuarioFechoId(UUID usuarioFechoId) { this.usuarioFechoId = usuarioFechoId; }

    public BigDecimal getValorAbertura() { return valorAbertura; }
    public void setValorAbertura(BigDecimal valorAbertura) { this.valorAbertura = valorAbertura; }

    public BigDecimal getValorFechoInformado() { return valorFechoInformado; }
    public void setValorFechoInformado(BigDecimal valorFechoInformado) { this.valorFechoInformado = valorFechoInformado; }

    public BigDecimal getValorEsperado() { return valorEsperado; }
    public void setValorEsperado(BigDecimal valorEsperado) { this.valorEsperado = valorEsperado; }

    public BigDecimal getDiferenca() { return diferenca; }
    public void setDiferenca(BigDecimal diferenca) { this.diferenca = diferenca; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDateTime dataAbertura) { this.dataAbertura = dataAbertura; }

    public LocalDateTime getDataFecho() { return dataFecho; }
    public void setDataFecho(LocalDateTime dataFecho) { this.dataFecho = dataFecho; }

    public String getObservacaoAbertura() { return observacaoAbertura; }
    public void setObservacaoAbertura(String observacaoAbertura) { this.observacaoAbertura = observacaoAbertura; }

    public String getObservacaoFecho() { return observacaoFecho; }
    public void setObservacaoFecho(String observacaoFecho) { this.observacaoFecho = observacaoFecho; }

    public boolean isAberto() { return status == Status.ABERTO; }
}
