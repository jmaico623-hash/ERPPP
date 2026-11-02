package com.nanobanana.controller;

import com.nanobanana.model.Usuario;
import com.nanobanana.repository.UsuarioRepository;
import com.nanobanana.repository.TenantRepository;
import com.nanobanana.repository.*;
import com.nanobanana.service.AuthService;
import com.nanobanana.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PDVController.class)
@Import(com.nanobanana.config.SecurityConfig.class)
class PDVControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private TenantRepository tenantRepository;

    @MockBean
    private ProdutoRepository produtoRepository;

    @MockBean
    private VendaRepository vendaRepository;

    @MockBean
    private VendaItemRepository vendaItemRepository;

    @MockBean
    private FiadoRepository fiadoRepository;

    @MockBean
    private StockService stockService;

    @MockBean
    private AuditoriaLogRepository auditoriaLogRepository;

    @MockBean
    private AlertaSistemaRepository alertaSistemaRepository;

    @Test
    void pdvRequerAutenticacao() throws Exception {
        when(authService.getUsuarioLogado()).thenReturn(Optional.empty());

        mockMvc.perform(get("/pdv"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void pdvComVendedorDeveRetornar200() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setTenantId(UUID.randomUUID());
        usuario.setNome("Vendedor Teste");
        usuario.setEmail("vendedor@test.com");
        usuario.setRole(Usuario.Role.VENDEDOR);
        usuario.setAtivo(true);

        when(authService.getUsuarioLogado()).thenReturn(Optional.of(usuario));
        when(authService.getTenantIdLogado()).thenReturn(usuario.getTenantId());
        when(produtoRepository.findByTenantIdOrderByNome(usuario.getTenantId()))
                .thenReturn(List.of());

        mockMvc.perform(get("/pdv"))
                .andExpect(status().isOk())
                .andExpect(view().name("pdv"));
    }

    @Test
    @WithMockUser(roles = "DONO")
    void pdvComDonoDeveRetornar200() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setTenantId(UUID.randomUUID());
        usuario.setNome("Dono Teste");
        usuario.setEmail("dono@test.com");
        usuario.setRole(Usuario.Role.DONO);
        usuario.setAtivo(true);

        when(authService.getUsuarioLogado()).thenReturn(Optional.of(usuario));
        when(authService.getTenantIdLogado()).thenReturn(usuario.getTenantId());
        when(produtoRepository.findByTenantIdOrderByNome(usuario.getTenantId()))
                .thenReturn(List.of());

        mockMvc.perform(get("/pdv"))
                .andExpect(status().isOk())
                .andExpect(view().name("pdv"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void pdvComGerenteDeveRetornar200() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setTenantId(UUID.randomUUID());
        usuario.setNome("Gerente Teste");
        usuario.setEmail("gerente@test.com");
        usuario.setRole(Usuario.Role.GERENTE);
        usuario.setAtivo(true);

        when(authService.getUsuarioLogado()).thenReturn(Optional.of(usuario));
        when(authService.getTenantIdLogado()).thenReturn(usuario.getTenantId());
        when(produtoRepository.findByTenantIdOrderByNome(usuario.getTenantId()))
                .thenReturn(List.of());

        mockMvc.perform(get("/pdv"))
                .andExpect(status().isOk())
                .andExpect(view().name("pdv"));
    }
}
