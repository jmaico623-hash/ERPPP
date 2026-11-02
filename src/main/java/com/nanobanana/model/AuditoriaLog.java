package com.nanobanana.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * LOG DE AUDITORIA
 * TODA ação no sistema é registrada aqui.
 * Isso permite rastrear QUALQUER atividade suspeita.
 */
@Entity
@Table(name = "auditoria_logs")
public class AuditoriaLog {

    public enum Acao {
        CREATE, UPDATE, DELETE,
        CANCELAR_VENDA, AJUSTAR_STOCK,
        ALTERAR_PRECO, ADICIONAR_STOCK, BAIXAR_STOCK,
        LOGIN, LOGOUT,
        CRIAR_USUARIO, DESATIVAR_USUARIO,
        INICIAR_INVENTARIO, FINALIZAR_INVENTARIO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID tenantId;

    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private Acao acao;

    @Column(length = 100)
    private String tabelaAfetada;

    private UUID registroId;

    @Column(columnDefinition = "TEXT")
    private String dadosAntigos;

    @Column(columnDefinition = "TEXT")
    private String dadosNovos;

    @Column(length = 50)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }

    public Acao getAcao() { return acao; }
    public void setAcao(Acao acao) { this.acao = acao; }

    public String getTabelaAfetada() { return tabelaAfetada; }
    public void setTabelaAfetada(String tabelaAfetada) { this.tabelaAfetada = tabelaAfetada; }

    public UUID getRegistroId() { return registroId; }
    public void setRegistroId(UUID registroId) { this.registroId = registroId; }

    public String getDadosAntigos() { return dadosAntigos; }
    public void setDadosAntigos(String dadosAntigos) { this.dadosAntigos = dadosAntigos; }

    public String getDadosNovos() { return dadosNovos; }
    public void setDadosNovos(String dadosNovos) { this.dadosNovos = dadosNovos; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
