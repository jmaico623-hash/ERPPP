package com.nanobanana.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ALERTAS AUTOMÁTICOS DO SISTEMA
 * Gerados quando o sistema detecta anomalias ou situações suspeitas.
 */
@Entity
@Table(name = "alertas_sistema")
public class AlertaSistema {

    public enum Tipo {
        STOCK_BAIXO,
        MOVIMENTACAO_SUSPEITA,
        VENDA_CANCELADA,
        AJUSTE_STOCK,
        PRECO_ALTERADO,
        FORA_HORARIO,
        ESTOQUE_NEGATIVO,
        SEM_MOVIMENTACAO,
        DIFERENCA_INVENTARIO,
        VENDA_ABAIXO_CUSTO,
        MULTIPLOS_CANCELAMENTOS
    }

    public enum Severidade {
        BAIXA,
        MEDIA,
        ALTA,
        CRITICA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private Tipo tipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private Severidade severidade = Severidade.MEDIA;

    private UUID usuarioRelacionado;

    private UUID registroId;

    @Column(nullable = false)
    private boolean lido = false;

    @Column(nullable = false)
    private boolean resolvido = false;

    private LocalDateTime resolvidoEm;

    private UUID resolvidoPor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Severidade getSeveridade() { return severidade; }
    public void setSeveridade(Severidade severidade) { this.severidade = severidade; }

    public UUID getUsuarioRelacionado() { return usuarioRelacionado; }
    public void setUsuarioRelacionado(UUID usuarioRelacionado) { this.usuarioRelacionado = usuarioRelacionado; }

    public UUID getRegistroId() { return registroId; }
    public void setRegistroId(UUID registroId) { this.registroId = registroId; }

    public boolean isLido() { return lido; }
    public void setLido(boolean lido) { this.lido = lido; }

    public boolean isResolvido() { return resolvido; }
    public void setResolvido(boolean resolvido) { this.resolvido = resolvido; }

    public LocalDateTime getResolvidoEm() { return resolvidoEm; }
    public void setResolvidoEm(LocalDateTime resolvidoEm) { this.resolvidoEm = resolvidoEm; }

    public UUID getResolvidoPor() { return resolvidoPor; }
    public void setResolvidoPor(UUID resolvidoPor) { this.resolvidoPor = resolvidoPor; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
