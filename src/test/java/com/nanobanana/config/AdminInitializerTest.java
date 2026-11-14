package com.nanobanana.config;

import com.nanobanana.model.Admin;
import com.nanobanana.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new AdminInitializer(adminRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "adminEmail", "admin@nanobanana.co.mz");
        ReflectionTestUtils.setField(initializer, "adminPassword", "nova-senha-forte");
        ReflectionTestUtils.setField(initializer, "adminNome", "Super Admin");
    }

    @Test
    void deveCriarAdminQuandoNaoExisteNenhum() {
        when(adminRepository.findByEmail("admin@nanobanana.co.mz")).thenReturn(Optional.empty());
        when(adminRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("nova-senha-forte")).thenReturn("hash-novo");

        initializer.run();

        ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@nanobanana.co.mz");
        assertThat(captor.getValue().getSenha()).isEqualTo("hash-novo");
    }

    @Test
    void deveSincronizarSenhaQuandoEnvMudou() {
        Admin admin = new Admin("admin@nanobanana.co.mz", "hash-antigo", "Super Admin");
        when(adminRepository.findByEmail("admin@nanobanana.co.mz")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("nova-senha-forte", "hash-antigo")).thenReturn(false);
        when(passwordEncoder.encode("nova-senha-forte")).thenReturn("hash-novo");

        initializer.run();

        verify(adminRepository).save(admin);
        assertThat(admin.getSenha()).isEqualTo("hash-novo");
    }

    @Test
    void naoDeveSalvarQuandoSenhaJaConfere() {
        Admin admin = new Admin("admin@nanobanana.co.mz", "hash-atual", "Super Admin");
        when(adminRepository.findByEmail("admin@nanobanana.co.mz")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("nova-senha-forte", "hash-atual")).thenReturn(true);

        initializer.run();

        verify(adminRepository, never()).save(any());
    }

    @Test
    void naoDeveFazerNadaSemAdminPassword() {
        ReflectionTestUtils.setField(initializer, "adminPassword", "  ");

        initializer.run();

        verifyNoInteractions(adminRepository);
    }
}
