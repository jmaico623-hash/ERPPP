package com.nanobanana.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 100)
    private String nui; // Número de Identificação Fiscal

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String telefone;

    @Column(length = 100)
    private String nacionalidade;

    @Column(length = 100)
    private String provincia;

    @Column(columnDefinition = "TEXT")
    private String endereco;

    @Column(length = 50, nullable = false)
    private String plano = "teste"; // teste, basico, premium

    @Column(nullable = false)
    private LocalDate dataInicio = LocalDate.now();

    private LocalDate dataExpiracao;

    @Column(nullable = false)
    private boolean ativo = true;

    // Configurações anti-desvio
    @Column(nullable = false)
    private boolean alertasAtivos = true;

    @Column(nullable = false)
    private boolean auditoriaStock = true;

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

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getNui() { return nui; }
    public void setNui(String nui) { this.nui = nui; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getNacionalidade() { return nacionalidade; }
    public void setNacionalidade(String nacionalidade) { this.nacionalidade = nacionalidade; }

    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getPlano() { return plano; }
    public void setPlano(String plano) { this.plano = plano; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataExpiracao() { return dataExpiracao; }
    public void setDataExpiracao(LocalDate dataExpiracao) { this.dataExpiracao = dataExpiracao; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public boolean isAlertasAtivos() { return alertasAtivos; }
    public void setAlertasAtivos(boolean alertasAtivos) { this.alertasAtivos = alertasAtivos; }

    public boolean isAuditoriaStock() { return auditoriaStock; }
    public void setAuditoriaStock(boolean auditoriaStock) { this.auditoriaStock = auditoriaStock; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    public int diasRestantes() {
        if (dataExpiracao == null) return -1;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dataExpiracao);
    }

    public boolean expirado() {
        return dataExpiracao != null && LocalDate.now().isAfter(dataExpiracao);
    }

    public String getNomeNegocio() {
        return nome;
    }
}
