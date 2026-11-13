package com.nanobanana.service;

import com.nanobanana.model.Usuario;
import com.nanobanana.model.Tenant;
import com.nanobanana.repository.UsuarioRepository;
import com.nanobanana.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpSession session;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setTenantId(UUID.randomUUID());
        usuario.setNome("Teste");
        usuario.setEmail("teste@email.com");
        usuario.setSenha("$2a$10$hash simulado");
        usuario.setRole(Usuario.Role.VENDEDOR);
        usuario.setAtivo(true);
        usuario.setBloqueado(false);
        usuario.setTentativasFalhas(0);

        Tenant tenant = new Tenant();
        tenant.setId(usuario.getTenantId());
        tenant.setNome("Loja Teste");
        tenant.setEmail("loja@email.com");
        tenant.setAtivo(true);
        when(tenantRepository.findById(usuario.getTenantId())).thenReturn(Optional.of(tenant));

        // Mock do RequestContextHolder para sessão
        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
        when(request.getSession(false)).thenReturn(session);
        RequestContextHolder.setRequestAttributes(attrs);

        // Mock dos métodos com lock (email ou telefone)
        when(usuarioRepository.findByIdentificadorWithLock("teste@email.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByIdentificadorWithLock("naoexiste@email.com")).thenReturn(Optional.empty());
        // Compatibilidade
        when(usuarioRepository.findByEmailWithLock("teste@email.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmailWithLock("naoexiste@email.com")).thenReturn(Optional.empty());
    }

    @Test
    void loginComSenhaCorretaDeveRetornarUsuario() {
        when(passwordEncoder.matches("senha123", usuario.getSenha())).thenReturn(true);

        Optional<Usuario> resultado = authService.login("teste@email.com", "senha123", session);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getTentativasFalhas()).isZero();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void loginComSenhaIncorretaDeveIncrementarTentativas() {
        when(passwordEncoder.matches("senha_errada", usuario.getSenha())).thenReturn(false);

        Optional<Usuario> resultado = authService.login("teste@email.com", "senha_errada", session);

        assertThat(resultado).isEmpty();
        assertThat(usuario.getTentativasFalhas()).isEqualTo(1);
        assertThat(usuario.isBloqueado()).isFalse();
    }

    @Test
    void loginAposCincoTentativasFalhasDeveBloquearUsuario() {
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        for (int i = 0; i < 5; i++) {
            authService.login("teste@email.com", "senha_errada", session);
        }

        assertThat(usuario.isBloqueado()).isTrue();
        assertThat(usuario.getTentativasFalhas()).isEqualTo(5);
    }

    @Test
    void loginUsuarioBloqueadoDeveRetornarVazio() {
        usuario.setBloqueado(true);
        Optional<Usuario> resultado = authService.login("teste@email.com", "qualquer", session);

        assertThat(resultado).isEmpty();
    }

    @Test
    void loginUsuarioInativoDeveRetornarVazio() {
        usuario.setAtivo(false);
        when(passwordEncoder.matches("senha123", usuario.getSenha())).thenReturn(true);

        Optional<Usuario> resultado = authService.login("teste@email.com", "senha123", session);

        assertThat(resultado).isEmpty();
    }

    @Test
    void loginEmailNaoExistenteDeveRetornarVazio() {
        Optional<Usuario> resultado = authService.login("naoexiste@email.com", "senha", session);

        assertThat(resultado).isEmpty();
    }

    @Test
    void loginComTelefoneFormatadoDeveEncontrarUsuario() {
        usuario.setTelefone("845550001");
        when(passwordEncoder.matches("senha123", usuario.getSenha())).thenReturn(true);
        when(usuarioRepository.findFirstByTelefoneIn(any())).thenReturn(Optional.of(usuario));

        Optional<Usuario> resultado = authService.login("+258 84 555 0001", "senha123", session);

        assertThat(resultado).isPresent();
        assertThat(resultado.get()).isSameAs(usuario);
    }

    @Test
    void loginComEmailEmMaiusculasDeveFuncionar() {
        when(passwordEncoder.matches("senha123", usuario.getSenha())).thenReturn(true);

        Optional<Usuario> resultado = authService.login("TESTE@EMAIL.COM", "senha123", session);

        assertThat(resultado).isPresent();
        verify(usuarioRepository).findByIdentificadorWithLock("teste@email.com");
    }
}
