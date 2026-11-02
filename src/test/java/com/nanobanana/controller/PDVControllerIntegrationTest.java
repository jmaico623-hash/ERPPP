package com.nanobanana.controller;

import com.nanobanana.model.Usuario;
import com.nanobanana.model.Venda;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private com.nanobanana.service.VendaService vendaService;

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

    @Test
    @WithMockUser(roles = "DONO")
    void historicoDeveSerPaginado() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setTenantId(UUID.randomUUID());
        usuario.setNome("Dono Teste");
        usuario.setEmail("dono@test.com");
        usuario.setRole(Usuario.Role.DONO);
        usuario.setAtivo(true);

        Venda venda = new Venda();
        venda.setId(UUID.randomUUID());
        venda.setTenantId(usuario.getTenantId());
        venda.setVendedorId(usuario.getId());
        venda.setTotal(new BigDecimal("100.00"));
        venda.setDataHora(LocalDateTime.now());
        venda.setStatus(Venda.Status.CONCLUIDA);

        when(authService.getUsuarioLogado()).thenReturn(Optional.of(usuario));
        when(authService.getTenantIdLogado()).thenReturn(usuario.getTenantId());
        when(vendaRepository.findByTenantId(eq(usuario.getTenantId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(venda), PageRequest.of(0, 25), 30));

        mockMvc.perform(get("/pdv/historico").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("pdv-historico"))
                .andExpect(model().attribute("vendas", List.of(venda)))
                .andExpect(model().attribute("pagina",
                        org.hamcrest.Matchers.hasProperty("totalPages", org.hamcrest.Matchers.is(2))));
    }
}
