package com.nanobanana.model;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProdutoStockTest {

    @Test
    void produtoComStockAbaixoDoMinimoDeveSerStockBaixo() {
        Produto produto = new Produto();
        produto.setStockAtual(3);
        produto.setStockMinimo(5);

        assertThat(produto.stockBaixo()).isTrue();
    }

    @Test
    void produtoComStockIgualAoMinimoDeveSerStockBaixo() {
        Produto produto = new Produto();
        produto.setStockAtual(5);
        produto.setStockMinimo(5);

        assertThat(produto.stockBaixo()).isTrue();
    }

    @Test
    void produtoComStockAcimaDoMinimoNaoDeveSerStockBaixo() {
        Produto produto = new Produto();
        produto.setStockAtual(10);
        produto.setStockMinimo(5);

        assertThat(produto.stockBaixo()).isFalse();
    }

    @Test
    void produtoComStockZeroDeveSerSemStock() {
        Produto produto = new Produto();
        produto.setStockAtual(0);
        produto.setStockMinimo(5);

        assertThat(produto.semStock()).isTrue();
    }

    @Test
    void produtoComStockPositivoNaoDeveSerSemStock() {
        Produto produto = new Produto();
        produto.setStockAtual(1);
        produto.setStockMinimo(5);

        assertThat(produto.semStock()).isFalse();
    }

    @Test
    void vendaAbaixoDoCustoDeveSerDetectada() {
        Produto produto = new Produto();
        produto.setPrecoCusto(new BigDecimal("100.00"));

        assertThat(produto.abaixoDeCusto(new BigDecimal("90.00"))).isTrue();
        assertThat(produto.abaixoDeCusto(new BigDecimal("100.00"))).isFalse();
        assertThat(produto.abaixoDeCusto(new BigDecimal("110.00"))).isFalse();
    }

    @Test
    void produtoSemPrecoCustoNaoDeveSerAbaixoDeCusto() {
        Produto produto = new Produto();
        produto.setPrecoCusto(null);

        assertThat(produto.abaixoDeCusto(new BigDecimal("10.00"))).isFalse();
    }

    @Test
    void valorStockDeveMultiplicarPrecoVendaPeloStock() {
        Produto produto = new Produto();
        produto.setPrecoVenda(new BigDecimal("50.00"));
        produto.setStockAtual(10);

        assertThat(produto.getValorStock()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void valorStockComPrecoNuloDeveRetornarZero() {
        Produto produto = new Produto();
        produto.setPrecoVenda(null);
        produto.setStockAtual(10);

        assertThat(produto.getValorStock()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
