package com.nanobanana.service;

import com.nanobanana.model.Pagamento;
import com.nanobanana.model.Tenant;
import com.nanobanana.model.Usuario;
import com.nanobanana.repository.AlertaSistemaRepository;
import com.nanobanana.repository.AuditoriaLogRepository;
import com.nanobanana.repository.BaixaStockRepository;
import com.nanobanana.repository.EntradaStockRepository;
import com.nanobanana.repository.FiadoRepository;
import com.nanobanana.repository.PagamentoRepository;
import com.nanobanana.repository.ProdutoRepository;
import com.nanobanana.repository.TenantRepository;
import com.nanobanana.repository.UsuarioRepository;
import com.nanobanana.repository.VendaItemRepository;
import com.nanobanana.repository.VendaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private PagamentoRepository pagamentoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private VendaRepository vendaRepository;
    @Mock private VendaItemRepository vendaItemRepository;
    @Mock private FiadoRepository fiadoRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private EntradaStockRepository entradaStockRepository;
    @Mock private BaixaStockRepository baixaStockRepository;
    @Mock private AlertaSistemaRepository alertaSistemaRepository;
    @Mock private AuditoriaLogRepository auditoriaLogRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setNome("Mercearia Teste");
        tenant.setPlano("teste");
        tenant.setDataExpiracao(LocalDate.now());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void confirmarPagamentoAtivaClientePorUmMes() {
        Pagamento pagamento = new Pagamento(tenant.getId(), new BigDecimal("350.00"), "mpesa", "REF1");
        pagamento.setId(1L);

        when(pagamentoRepository.findById(1L)).thenReturn(Optional.of(pagamento));
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));

        Tenant resultado = adminService.confirmarPagamento(1L);

        assertThat(pagamento.isConfirmado()).isTrue();
        assertThat(resultado.getPlano()).isEqualTo("pago");
        assertThat(resultado.isAtivo()).isTrue();
        assertThat(resultado.getDataExpiracao()).isAfter(LocalDate.now());
        verify(pagamentoRepository).save(pagamento);
    }

    @Test
    void confirmarPagamentoInexistenteFalha() {
        when(pagamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.confirmarPagamento(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pagamento");
    }

    @Test
    void desativarClienteMarcaComoExpirado() {
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));

        Tenant resultado = adminService.desativarCliente(tenant.getId());

        assertThat(resultado.getPlano()).isEqualTo("teste");
        assertThat(resultado.isAtivo()).isFalse();
        assertThat(resultado.expirado()).isTrue();
    }

    @Test
    void estenderTesteDefineNovaData() {
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));

        Tenant resultado = adminService.estenderTeste(tenant.getId(), 15);

        assertThat(resultado.getPlano()).isEqualTo("teste");
        assertThat(resultado.getDataExpiracao()).isEqualTo(LocalDate.now().plusDays(15));
    }

    @Test
    void resetSenhaDonoUsaHashBCrypt() {
        Usuario dono = new Usuario();
        dono.setId(UUID.randomUUID());
        dono.setTenantId(tenant.getId());
        dono.setRole(Usuario.Role.DONO);
        dono.setSenha("antiga");

        when(usuarioRepository.findByTenantIdAndRole(tenant.getId(), Usuario.Role.DONO))
                .thenReturn(List.of(dono));
        when(passwordEncoder.encode("nova-senha")).thenReturn("hash-bcrypt");

        adminService.resetSenhaDono(tenant.getId(), "nova-senha");

        assertThat(dono.getSenha()).isEqualTo("hash-bcrypt");
        verify(usuarioRepository).save(dono);
    }

    @Test
    void resetSenhaSemDonoFalha() {
        when(usuarioRepository.findByTenantIdAndRole(tenant.getId(), Usuario.Role.DONO))
                .thenReturn(List.of());

        assertThatThrownBy(() -> adminService.resetSenhaDono(tenant.getId(), "senha-forte"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DONO");
    }

    @Test
    void apagarClienteRemoveDadosRelacionados() {
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(vendaRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(fiadoRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(produtoRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(usuarioRepository.findByTenantId(tenant.getId())).thenReturn(List.of());
        when(pagamentoRepository.findAll()).thenReturn(List.of());

        adminService.apagarCliente(tenant.getId());

        assertThat(tenant.isAtivo()).isFalse();
        assertThat(tenant.expirado()).isTrue();
        verify(tenantRepository).save(tenant);
        verify(usuarioRepository, never()).deleteAll(anyList());
        verify(vendaRepository, never()).deleteAll(anyList());
        verify(produtoRepository, never()).deleteAll(anyList());
        verify(pagamentoRepository, never()).deleteAll(anyList());
    }
}
