package com.nanobanana.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class Usuario {

    public enum Role {
        DONO,       // 👑 Controle total: stock, preços, relatórios, usuários
        GERENTE,    // 👔 Vende + vê relatórios (NÃO toca stock)
        VENDEDOR    // 💰 APENAS vende
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String senha;

    @Column(length = 50)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @Column(nullable = false)
    private boolean ativo = true;

    private LocalDateTime ultimoLogin;

    @Column(nullable = false)
    private int tentativasFalhas = 0;

    @Column(nullable = false)
    private boolean bloqueado = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    private UUID criadoPor; // quem criou este usuário

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public LocalDateTime getUltimoLogin() { return ultimoLogin; }
    public void setUltimoLogin(LocalDateTime ultimoLogin) { this.ultimoLogin = ultimoLogin; }

    public int getTentativasFalhas() { return tentativasFalhas; }
    public void setTentativasFalhas(int tentativasFalhas) { this.tentativasFalhas = tentativasFalhas; }

    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public UUID getCriadoPor() { return criadoPor; }
    public void setCriadoPor(UUID criadoPor) { this.criadoPor = criadoPor; }

    // Métodos de permissão
    public boolean podeGerirStock() {
        return role == Role.DONO;
    }

    public boolean podeGerirPrecos() {
        return role == Role.DONO;
    }

    public boolean podeVerRelatorios() {
        return role == Role.DONO || role == Role.GERENTE;
    }

    public boolean podeCancelarVenda() {
        return role == Role.DONO || role == Role.GERENTE;
    }

    public boolean podeGerirUsuarios() {
        return role == Role.DONO;
    }

    public boolean podeVender() {
        return true; // Todos podem vender
    }
}
