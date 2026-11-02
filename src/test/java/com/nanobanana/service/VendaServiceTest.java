package com.nanobanana.service;

import com.nanobanana.dto.VendaItemRequest;
import com.nanobanana.model.AlertaSistema;
import com.nanobanana.model.Produto;
import com.nanobanana.model.Usuario;
import com.nanobanana.model.Venda;
import com.nanobanana.model.VendaItem;
import com.nanobanana.repository.AlertaSistemaRepository;
import com.nanobanana.repository.FiadoRepository;
import com.nanobanana.repository.ProdutoRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VendaServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private VendaRepository vendaRepository;
    @Mock private VendaItemRepository vendaItemRepository;
    @Mock private FiadoRepository fiadoRepository;
    @Mock private StockService stockService;
    @Mock private AlertaSistemaRepository alertaSistemaRepository;

    @InjectMocks
    private VendaService vendaService;

    private UUID tenantId;
    private UUID vendedorId;
    private UUID produtoId;
    private Produto produto;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        vendedorId = UUID.randomUUID();
        produtoId = UUID.randomUUID();

        produto = new Produto();
        produto.setId(produtoId);
        produto.setTenantId(tenantId);
        produto.setNome("Arroz 5kg");
        produto.setPrecoVenda(new BigDecimal("350.00"));
        produto.setStockAtual(10);

        when(vendaRepository.save(any(Venda.class))).thenAnswer(inv -> {
            Venda v = inv.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });
    }

    @Test
    void finalizarVendaCalculaTotalEBaixaStock() {
        when(produtoRepository.findByTenantIdAndId(tenantId, produtoId)).thenReturn(Optional.of(produto));

        Venda venda = vendaService.finalizarVenda(
                tenantId, vendedorId,
                List.of(new VendaItemRequest(produtoId, 2)),
                BigDecimal.ZERO, Venda.MetodoPagamento.CASH, null, null, null);

        assertThat(venda.getSubtotal()).isEqualByComparingTo("700.00");
        assertThat(venda.getTotal()).isEqualByComparingTo("700.00");
        assertThat(venda.getValorPago()).isEqualByComparingTo("700.00");
        assertThat(venda.getTroco()).isEqualByComparingTo("0");

        verify(vendaItemRepository).save(any(VendaItem.class));
        verify(stockService).baixarStockVenda(tenantId, produtoId, 2);
        verify(fiadoRepository, never()).save(any());
    }

    @Test
    void finalizarVendaSemItensFalha() {
        assertThatThrownBy(() -> vendaService.finalizarVenda(
                tenantId, vendedorId, List.of(), BigDecimal.ZERO,
                Venda.MetodoPagamento.CASH, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pelo menos 1 produto");
    }

    @Test
    void finalizarVendaComStockInsuficienteFalha() {
        produto.setStockAtual(1);
        when(produtoRepository.findByTenantIdAndId(tenantId, produtoId)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> vendaService.finalizarVenda(
                tenantId, vendedorId,
                List.of(new VendaItemRequest(produtoId, 5)),
                BigDecimal.ZERO, Venda.MetodoPagamento.CASH, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");

        verify(stockService, never()).baixarStockVenda(any(), any(), anyInt());
    }

    @Test
    void finalizarVendaComDescontoMaiorQueSubtotalFalha() {
        when(produtoRepository.findByTenantIdAndId(tenantId, produtoId)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> vendaService.finalizarVenda(
                tenantId, vendedorId,
                List.of(new VendaItemRequest(produtoId, 1)),
                new BigDecimal("999.00"), Venda.MetodoPagamento.CASH, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("desconto");
    }

    @Test
    void vendedorNaoPodeCancelarVenda() {
        Usuario vendedor = new Usuario();
        vendedor.setId(UUID.randomUUID());
        vendedor.setRole(Usuario.Role.VENDEDOR);

        assertThatThrownBy(() -> vendaService.cancelarVenda(tenantId, UUID.randomUUID(), vendedor, "teste"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("permissão");
    }

    @Test
    void cancelarVendaRestauraStockEGeraAlerta() {
        Usuario dono = new Usuario();
        dono.setId(UUID.randomUUID());
        dono.setNome("Dono");
        dono.setRole(Usuario.Role.DONO);

        Venda venda = new Venda();
        venda.setId(UUID.randomUUID());
        venda.setTenantId(tenantId);
        venda.setTotal(new BigDecimal("350.00"));
        venda.setStatus(Venda.Status.CONCLUIDA);

        VendaItem item = new VendaItem();
        item.setProdutoId(produtoId);
        item.setQuantidade(2);

        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(vendaItemRepository.findByVendaId(venda.getId())).thenReturn(List.of(item));

        vendaService.cancelarVenda(tenantId, venda.getId(), dono, "cliente desistiu");

        assertThat(venda.getStatus()).isEqualTo(Venda.Status.CANCELADA);
        assertThat(venda.getCanceladaPor()).isEqualTo(dono.getId());
        verify(stockService).restaurarStockVenda(tenantId, produtoId, 2);
        verify(alertaSistemaRepository).save(any(AlertaSistema.class));
        verify(vendaRepository).save(venda);
    }
}
