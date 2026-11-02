package com.nanobanana.controller;

import com.nanobanana.model.Usuario;
import com.nanobanana.model.Tenant;
import com.nanobanana.repository.UsuarioRepository;
import com.nanobanana.repository.TenantRepository;
import com.nanobanana.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardRequerAutenticacao() throws Exception {
        when(authService.getUsuarioLogado()).thenReturn(Optional.empty());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isForbidden());
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
}
