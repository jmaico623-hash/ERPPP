package com.nanobanana.service;

import com.nanobanana.model.*;
import com.nanobanana.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StockService {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EntradaStockRepository entradaStockRepository;

    @Autowired
    private BaixaStockRepository baixaStockRepository;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    @Autowired
    private AlertaSistemaRepository alertaSistemaRepository;

    /**
     * ADICIONAR STOCK - APENAS DONO PODE
     * Registra entrada e atualiza stock do produto
     * Atomic com lock pessimista
     */
    @Transactional
    public Produto adicionarStock(UUID tenantId, UUID produtoId, int quantidade, UUID donoId,
                                  String fornecedor, String notaFiscal, BigDecimal precoCusto, String motivo) {
        
        if (quantidade <= 0) {
            throw new RuntimeException("A quantidade deve ser maior que zero");
        }

        Produto produto = produtoRepository.findByTenantIdAndIdWithLock(tenantId, produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        // Registra entrada
        EntradaStock entrada = new EntradaStock();
        entrada.setTenantId(tenantId);
        entrada.setProdutoId(produtoId);
        entrada.setQuantidade(quantidade);
        entrada.setDonoId(donoId);
        entrada.setFornecedor(fornecedor);
        entrada.setNotaFiscal(notaFiscal);
        entrada.setPrecoCusto(precoCusto);
        entrada.setMotivo(motivo);
        entradaStockRepository.save(entrada);

        // Atualiza stock
        int stockAntigo = produto.getStockAtual();
        produto.setStockAtual(produto.getStockAtual() + quantidade);
        produtoRepository.save(produto);

        // Log de auditoria
        logAuditoria(tenantId, donoId, AuditoriaLog.Acao.ADICIONAR_STOCK, "produtos", produtoId,
                "Stock: " + stockAntigo + " -> " + produto.getStockAtual(),
                "Entrada: +" + quantidade + " unidades");

        return produto;
    }

    /**
     * BAIXAR STOCK - APENAS DONO PODE
     * Registra baixa por perda, validade, quebra, roubo ou ajuste
     * Atomic com lock pessimista
     */
    @Transactional
    public Produto baixarStock(UUID tenantId, UUID produtoId, int quantidade, UUID donoId,
                               BaixaStock.TipoBaixa tipoBaixa, String motivo) {
        
        if (quantidade <= 0) {
            throw new RuntimeException("A quantidade deve ser maior que zero");
        }

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new RuntimeException("Motivo da baixa é OBRIGATÓRIO");
        }

        Produto produto = produtoRepository.findByTenantIdAndIdWithLock(tenantId, produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        if (produto.getStockAtual() < quantidade) {
            throw new RuntimeException("Stock insuficiente. Disponível: " + produto.getStockAtual());
        }

        // Registra baixa
        BaixaStock baixa = new BaixaStock();
        baixa.setTenantId(tenantId);
        baixa.setProdutoId(produtoId);
        baixa.setQuantidade(quantidade);
        baixa.setDonoId(donoId);
        baixa.setTipoBaixa(tipoBaixa);
        baixa.setMotivo(motivo);
        baixaStockRepository.save(baixa);

        // Atualiza stock
        int stockAntigo = produto.getStockAtual();
        produto.setStockAtual(produto.getStockAtual() - quantidade);
        produtoRepository.save(produto);

        // Log de auditoria
        logAuditoria(tenantId, donoId, AuditoriaLog.Acao.BAIXAR_STOCK, "produtos", produtoId,
                "Stock: " + stockAntigo + " -> " + produto.getStockAtual(),
                "Baixa: -" + quantidade + " (" + tipoBaixa + ") - Motivo: " + motivo);

        // Alerta se baixa significativa
        if (quantidade > 10) {
            criarAlerta(tenantId, AlertaSistema.Tipo.AJUSTE_STOCK,
                    "Baixa de " + quantidade + " unidades do produto " + produto.getNome(),
                    AlertaSistema.Severidade.ALTA, donoId, produtoId);
        }

        return produto;
    }

    /**
     * VENDER - Baixa stock automaticamente na venda
     * Usado pelo PDV quando finaliza uma venda
     * Atomic: check + update com lock pessimista
     */
    @Transactional
    public void baixarStockVenda(UUID tenantId, UUID produtoId, int quantidade) {
        Produto produto = produtoRepository.findByTenantIdAndIdWithLock(tenantId, produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        if (produto.getStockAtual() < quantidade) {
            throw new RuntimeException("Stock insuficiente para " + produto.getNome() + 
                    ". Disponível: " + produto.getStockAtual());
        }

        produto.setStockAtual(produto.getStockAtual() - quantidade);
        produtoRepository.save(produto);

        // Alerta se stock ficou baixo
        if (produto.stockBaixo()) {
            criarAlerta(tenantId, AlertaSistema.Tipo.STOCK_BAIXO,
                    produto.getNome() + " com stock baixo: " + produto.getStockAtual() + 
                    " (mínimo: " + produto.getStockMinimo() + ")",
                    AlertaSistema.Severidade.MEDIA, null, produtoId);
        }

        // Alerta se stock negativo (não deveria acontecer)
        if (produto.getStockAtual() < 0) {
            criarAlerta(tenantId, AlertaSistema.Tipo.ESTOQUE_NEGATIVO,
                    produto.getNome() + " com stock NEGATIVO: " + produto.getStockAtual(),
                    AlertaSistema.Severidade.CRITICA, null, produtoId);
        }
    }

    /**
     * Restaura stock quando venda é cancelada (APENAS DONO/GERENTE)
     * Atomic com lock pessimista e registra auditoria/entrada de stock
     */
    @Transactional
    public void restaurarStockVenda(UUID tenantId, UUID produtoId, int quantidade, UUID usuarioId, UUID vendaId) {
        if (quantidade <= 0) {
            throw new RuntimeException("A quantidade deve ser maior que zero");
        }

        Produto produto = produtoRepository.findByTenantIdAndIdWithLock(tenantId, produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        int stockAntigo = produto.getStockAtual();
        produto.setStockAtual(produto.getStockAtual() + quantidade);
        produtoRepository.save(produto);

        EntradaStock entrada = new EntradaStock();
        entrada.setTenantId(tenantId);
        entrada.setProdutoId(produtoId);
        entrada.setQuantidade(quantidade);
        entrada.setDonoId(usuarioId);
        entrada.setMotivo("Cancelamento da venda " + vendaId);
        entradaStockRepository.save(entrada);

        logAuditoria(tenantId, usuarioId, AuditoriaLog.Acao.ADICIONAR_STOCK, "produtos", produtoId,
                "Stock: " + stockAntigo + " -> " + produto.getStockAtual(),
                "Restauro por cancelamento da venda " + vendaId + ": +" + quantidade + " unidades");
    }

    /**
     * Lista produtos com stock baixo
     */
    public List<Produto> getProdutosStockBaixo(UUID tenantId) {
        return produtoRepository.findProdutosStockBaixo(tenantId);
    }

    /**
     * Lista entradas de stock
     */
    public List<EntradaStock> getEntradasStock(UUID tenantId) {
        return entradaStockRepository.findByTenantIdOrderByDataEntradaDesc(tenantId);
    }

    /**
     * Lista baixas de stock
     */
    public List<BaixaStock> getBaixasStock(UUID tenantId) {
        return baixaStockRepository.findByTenantIdOrderByDataBaixaDesc(tenantId);
    }

    // ============ MÉTODOS AUXILIARES ============

    private void logAuditoria(UUID tenantId, UUID usuarioId, AuditoriaLog.Acao acao,
                              String tabela, UUID registroId, String dadosAntigos, String dadosNovos) {
        AuditoriaLog log = new AuditoriaLog();
        log.setTenantId(tenantId);
        log.setUsuarioId(usuarioId);
        log.setAcao(acao);
        log.setTabelaAfetada(tabela);
        log.setRegistroId(registroId);
        log.setDadosAntigos(dadosAntigos);
        log.setDadosNovos(dadosNovos);
        auditoriaLogRepository.save(log);
    }

    private void criarAlerta(UUID tenantId, AlertaSistema.Tipo tipo, String descricao,
                             AlertaSistema.Severidade severidade, UUID usuarioId, UUID registroId) {
        AlertaSistema alerta = new AlertaSistema();
        alerta.setTenantId(tenantId);
        alerta.setTipo(tipo);
        alerta.setDescricao(descricao);
        alerta.setSeveridade(severidade);
        alerta.setUsuarioRelacionado(usuarioId);
        alerta.setRegistroId(registroId);
        alertaSistemaRepository.save(alerta);
    }
}
