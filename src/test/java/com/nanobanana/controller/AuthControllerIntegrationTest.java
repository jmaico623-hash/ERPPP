package com.nanobanana.controller;

import com.nanobanana.model.Usuario;
import com.nanobanana.model.Tenant;
import com.nanobanana.repository.UsuarioRepository;
import com.nanobanana.repository.TenantRepository;
import com.nanobanana.repository.PagamentoRepository;
import com.nanobanana.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(com.nanobanana.config.SecurityConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private TenantRepository tenantRepository;

    @MockBean
    private PagamentoRepository pagamentoRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void loginPageDeveRetornar200() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void registrarPageDeveRetornar200() throws Exception {
        mockMvc.perform(get("/registrar"))
                .andExpect(status().isOk())
                .andExpect(view().name("registrar"));
    }

    @Test
    void indexDeveRedirecionarParaLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void assinaturaRequerAutenticacao() throws Exception {
        // Sem usuario na sessao -> Spring Security retorna 403
        when(authService.getUsuarioLogado()).thenReturn(Optional.empty());

        mockMvc.perform(get("/assinatura"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void dashboardRequerAutenticacao() throws Exception {
        when(authService.getUsuarioLogado()).thenReturn(Optional.empty());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(roles = "DONO")
    void assinaturaComUsuarioLogadoDeveRetornar200() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setTenantId(UUID.randomUUID());
        usuario.setNome("Joao");
        usuario.setEmail("joao@email.com");
        usuario.setRole(Usuario.Role.DONO);
        usuario.setAtivo(true);

        Tenant tenant = new Tenant();
        tenant.setId(usuario.getTenantId());
        tenant.setNome("Mercearia Teste");
        tenant.setAtivo(true);

        when(authService.getUsuarioLogado()).thenReturn(Optional.of(usuario));
        when(tenantRepository.findById(usuario.getTenantId())).thenReturn(Optional.of(tenant));

        mockMvc.perform(get("/assinatura"))
                .andExpect(status().isOk())
                .andExpect(view().name("assinatura"));
    }

    @Test
    void registarDeveGuardarTelefoneNormalizado() throws Exception {
        when(tenantRepository.existsByTelefoneIn(any())).thenReturn(false);
        when(usuarioRepository.existsByTelefoneIn(any())).thenReturn(false);
        when(passwordEncoder.encode("teste1234")).thenReturn("$2a$10$hash");
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/registrar").with(csrf())
                        .param("nomeCompleto", "Maria Teste")
                        .param("nomeLoja", "Loja da Maria")
                        .param("telefone", "+258 84 555 0001")
                        .param("email", "")
                        .param("nacionalidade", "Moçambique")
                        .param("provincia", "Maputo")
                        .param("senha", "teste1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("sucesso"));

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        verify(usuarioRepository).save(usuarioCaptor.capture());

        assertThat(tenantCaptor.getValue().getTelefone()).isEqualTo("258845550001");
        assertThat(usuarioCaptor.getValue().getTelefone()).isEqualTo("258845550001");
        assertThat(usuarioCaptor.getValue().getEmail()).isNull();
    }

    @Test
    void registarComTelefoneJaExistenteNoutraFormatacaoDeveBloquear() throws Exception {
        when(tenantRepository.existsByTelefoneIn(any())).thenReturn(true);

        mockMvc.perform(post("/registrar").with(csrf())
                        .param("nomeCompleto", "Outra Pessoa")
                        .param("nomeLoja", "Outra Loja")
                        .param("telefone", "84 555 0001")
                        .param("email", "")
                        .param("nacionalidade", "Moçambique")
                        .param("provincia", "Maputo")
                        .param("senha", "teste1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrar"))
                .andExpect(flash().attributeExists("erro"));

        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
