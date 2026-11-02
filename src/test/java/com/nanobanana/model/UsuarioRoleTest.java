package com.nanobanana.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioRoleTest {

    @Test
    void donoDeveTerTodasPermissoes() {
        Usuario dono = new Usuario();
        dono.setRole(Usuario.Role.DONO);

        assertThat(dono.podeGerirStock()).isTrue();
        assertThat(dono.podeGerirPrecos()).isTrue();
        assertThat(dono.podeVerRelatorios()).isTrue();
        assertThat(dono.podeCancelarVenda()).isTrue();
        assertThat(dono.podeGerirUsuarios()).isTrue();
        assertThat(dono.podeVender()).isTrue();
    }

    @Test
    void gerenteDeveTerPermissoesLimitadas() {
        Usuario gerente = new Usuario();
        gerente.setRole(Usuario.Role.GERENTE);

        assertThat(gerente.podeGerirStock()).isFalse();
        assertThat(gerente.podeGerirPrecos()).isFalse();
        assertThat(gerente.podeVerRelatorios()).isTrue();
        assertThat(gerente.podeCancelarVenda()).isTrue();
        assertThat(gerente.podeGerirUsuarios()).isFalse();
        assertThat(gerente.podeVender()).isTrue();
    }

    @Test
    void vendedorDeveApenasVender() {
        Usuario vendedor = new Usuario();
        vendedor.setRole(Usuario.Role.VENDEDOR);

        assertThat(vendedor.podeGerirStock()).isFalse();
        assertThat(vendedor.podeGerirPrecos()).isFalse();
        assertThat(vendedor.podeVerRelatorios()).isFalse();
        assertThat(vendedor.podeCancelarVenda()).isFalse();
        assertThat(vendedor.podeGerirUsuarios()).isFalse();
        assertThat(vendedor.podeVender()).isTrue();
    }
}
