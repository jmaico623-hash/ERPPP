package com.nanobanana.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TelefoneUtilTest {

    @Test
    void normalizarRemoveFormatacao() {
        assertThat(TelefoneUtil.normalizar("84 555 0001")).isEqualTo("845550001");
        assertThat(TelefoneUtil.normalizar("+258 84 555 0001")).isEqualTo("258845550001");
        assertThat(TelefoneUtil.normalizar("(84) 555-0001")).isEqualTo("845550001");
        assertThat(TelefoneUtil.normalizar("00258 845550001")).isEqualTo("258845550001");
        assertThat(TelefoneUtil.normalizar(null)).isEmpty();
        assertThat(TelefoneUtil.normalizar("   ")).isEmpty();
    }

    @Test
    void variantesAceitamNumeroLocalEComIndicativo() {
        Set<String> locais = TelefoneUtil.variantes("84 555 0001");
        assertThat(locais).contains("845550001", "258845550001", "258" + "845550001");

        Set<String> internacionais = TelefoneUtil.variantes("+258 84 555 0001");
        assertThat(internacionais).contains("258845550001", "845550001");
    }

    @Test
    void variantesNaoRemovemCodigoDeNumeroNacionalBrasileiro() {
        // DDD 55 (Brasil) + número local: não deve ser confundido com o indicativo +55
        Set<String> variantes = TelefoneUtil.variantes("55 98765-4321");
        assertThat(variantes).contains("55987654321");
        assertThat(variantes).doesNotContain("987654321");
    }
}
