package com.nanobanana.service;

import com.nanobanana.model.*;
import com.nanobanana.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DevolucaoService {

    private final VendaRepository vendaRepository;
    private final VendaItemRepository vendaItemRepository;
    private final DevolucaoRepository devolucaoRepository;
    private final DevolucaoItemRepository devolucaoItemRepository;
    private final StockService stockService;
    private final AuditoriaLogRepository auditoriaLogRepository;

    public DevolucaoService(VendaRepository vendaRepository,
                            VendaItemRepository vendaItemRepository,
                            DevolucaoRepository devolucaoRepository,
                            DevolucaoItemRepository devolucaoItemRepository,
                            StockService stockService,
                            AuditoriaLogRepository auditoriaLogRepository) {
        this.vendaRepository = vendaRepository;
        this.vendaItemRepository = vendaItemRepository;
        this.devolucaoRepository = devolucaoRepository;
        this.devolucaoItemRepository = devolucaoItemRepository;
        this.stockService = stockService;
        this.auditoriaLogRepository = auditoriaLogRepository;
    }

    @Transactional
    public Devolucao registrarDevolucao(UUID tenantId, UUID vendaId, Usuario usuario, Map<UUID, Integer> quantidadesPorItem, boolean reporStock, String motivo) {
        if (usuario == null || !usuario.podeCancelarVenda()) {
            throw new IllegalStateException("Você NÃO tem permissão para registar devoluções.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("O motivo da devolução é obrigatório.");
        }
        Venda venda = vendaRepository.findByIdAndTenantId(vendaId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Venda não encontrada."));
        if (venda.getStatus() != Venda.Status.CONCLUIDA) {
            throw new IllegalStateException("Só é possível devolver itens de vendas concluídas.");
        }

        List<VendaItem> itensVenda = vendaItemRepository.findByVendaId(vendaId);
        Map<UUID, VendaItem> itensPorId = new HashMap<>();
        for (VendaItem vi : itensVenda) itensPorId.put(vi.getId(), vi);

        if (quantidadesPorItem == null || quantidadesPorItem.isEmpty()) {
            throw new IllegalArgumentException("Informe pelo menos um item para devolução.");
        }

        Devolucao devolucao = new Devolucao();
        devolucao.setTenantId(tenantId);
        devolucao.setVendaId(vendaId);
        devolucao.setUsuarioId(usuario.getId());
        devolucao.setReporStock(reporStock);
        devolucao.setMotivo(motivo.trim());
        devolucao.setDataHora(LocalDateTime.now());
        devolucao.setValorTotal(BigDecimal.ZERO);
        devolucao = devolucaoRepository.save(devolucao);

        BigDecimal total = BigDecimal.ZERO;
        int totalItens = 0;

        for (Map.Entry<UUID, Integer> e : quantidadesPorItem.entrySet()) {
            UUID vendaItemId = e.getKey();
            int qtd = e.getValue() == null ? 0 : e.getValue();
            if (qtd <= 0) continue;

            VendaItem vi = itensPorId.get(vendaItemId);
            if (vi == null) {
                throw new IllegalArgumentException("Item de venda inválido: " + vendaItemId);
            }
            int jaDevolvido = Optional.ofNullable(devolucaoItemRepository.somaQuantidadeDevolvidaPorVendaItem(vendaItemId)).orElse(0);
            int disponivel = vi.getQuantidade() - jaDevolvido;
            if (qtd > disponivel) {
                throw new IllegalArgumentException("Quantidade a devolver excede o disponível para " + vi.getNomeProduto() + " (disponível: " + disponivel + ").");
            }

            BigDecimal subtotal = vi.getPrecoUnitario().multiply(BigDecimal.valueOf(qtd));
            DevolucaoItem di = new DevolucaoItem();
            di.setDevolucaoId(devolucao.getId());
            di.setVendaItemId(vendaItemId);
            di.setProdutoId(vi.getProdutoId());
            di.setNomeProduto(vi.getNomeProduto());
            di.setQuantidade(qtd);
            di.setPrecoUnitario(vi.getPrecoUnitario());
            di.setSubtotal(subtotal);
            devolucaoItemRepository.save(di);

            if (reporStock) {
                stockService.restaurarStockVenda(tenantId, vi.getProdutoId(), qtd, usuario.getId(), vendaId);
            }

            total = total.add(subtotal);
            totalItens += qtd;
        }

        if (totalItens == 0) {
            throw new IllegalArgumentException("Informe pelo menos um item com quantidade maior que zero.");
        }

        devolucao.setValorTotal(total);
        devolucao = devolucaoRepository.save(devolucao);

        AuditoriaLog log = new AuditoriaLog();
        log.setTenantId(tenantId);
        log.setUsuarioId(usuario.getId());
        log.setAcao(AuditoriaLog.Acao.UPDATE);
        log.setTabelaAfetada("devolucoes");
        log.setRegistroId(devolucao.getId());
        log.setDadosNovos("Devolução venda " + vendaId + ", itens=" + totalItens + ", valor=" + total + ", reporStock=" + reporStock + ", motivo=" + motivo.trim());
        auditoriaLogRepository.save(log);

        return devolucao;
    }

    public List<Devolucao> listarPorVenda(UUID tenantId, UUID vendaId) {
        return devolucaoRepository.findByTenantIdAndVendaIdOrderByDataHoraDesc(tenantId, vendaId);
    }

    public List<DevolucaoItem> listarItens(UUID devolucaoId) {
        return devolucaoItemRepository.findByDevolucaoId(devolucaoId);
    }

    public int quantidadeJaDevolvida(UUID vendaItemId) {
        return Optional.ofNullable(devolucaoItemRepository.somaQuantidadeDevolvidaPorVendaItem(vendaItemId)).orElse(0);
    }
}
