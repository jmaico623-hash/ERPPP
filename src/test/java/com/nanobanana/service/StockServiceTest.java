package com.nanobanana.service;

import com.nanobanana.model.BaixaStock;
import com.nanobanana.model.Produto;
import com.nanobanana.repository.AlertaSistemaRepository;
import com.nanobanana.repository.AuditoriaLogRepository;
import com.nanobanana.repository.BaixaStockRepository;
import com.nanobanana.repository.EntradaStockRepository;
import com.nanobanana.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private EntradaStockRepository entradaStockRepository;
    @Mock private BaixaStockRepository baixaStockRepository;
    @Mock private AuditoriaLogRepository auditoriaLogRepository;
    @Mock private AlertaSistemaRepository alertaSistemaRepository;

    @InjectMocks
    private StockService stockService;

    private UUID tenantId;
    private UUID produtoId;
    private UUID donoId;
    private Produto produto;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        produtoId = UUID.randomUUID();
        donoId = UUID.randomUUID();

        produto = new Produto();
        produto.setId(produtoId);
        produto.setTenantId(tenantId);
        produto.setNome("Óleo 1L");
        produto.setPrecoVenda(new BigDecimal("180.00"));
        produto.setStockAtual(10);
    }

    @Test
    void adicionarStockIncrementaQuantidade() {
        when(produtoRepository.findByTenantIdAndIdWithLock(tenantId, produtoId))
                .thenReturn(Optional.of(produto));

        Produto resultado = stockService.adicionarStock(tenantId, produtoId, 5, donoId,
                "Fornecedor", "NF-1", new BigDecimal("120.00"), "compra");

        assertThat(resultado.getStockAtual()).isEqualTo(15);
        verify(entradaStockRepository).save(any());
        verify(produtoRepository).save(produto);
    }

    @Test
    void baixarStockSemMotivoFalha() {
        assertThatThrownBy(() -> stockService.baixarStock(tenantId, produtoId, 1, donoId,
                BaixaStock.TipoBaixa.PERDA, "  "))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Motivo");
    }

    @Test
    void baixarStockInsuficienteFalha() {
        when(produtoRepository.findByTenantIdAndIdWithLock(tenantId, produtoId))
                .thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> stockService.baixarStock(tenantId, produtoId, 50, donoId,
                BaixaStock.TipoBaixa.PERDA, "quebra"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("insuficiente");

        verify(baixaStockRepository, never()).save(any());
    }

    @Test
    void baixarStockValidoDebitaQuantidade() {
        when(produtoRepository.findByTenantIdAndIdWithLock(tenantId, produtoId))
                .thenReturn(Optional.of(produto));

        Produto resultado = stockService.baixarStock(tenantId, produtoId, 4, donoId,
                BaixaStock.TipoBaixa.VALIDADE, "produto vencido");

        assertThat(resultado.getStockAtual()).isEqualTo(6);
        verify(baixaStockRepository).save(any());
        verify(produtoRepository).save(produto);
    }
}
