package com.nanobanana.service;

import com.nanobanana.dto.VendaItemRequest;
import com.nanobanana.model.AlertaSistema;
import com.nanobanana.model.Fiado;
import com.nanobanana.model.Produto;
import com.nanobanana.model.Usuario;
import com.nanobanana.model.Venda;
import com.nanobanana.model.VendaItem;
import com.nanobanana.repository.AlertaSistemaRepository;
import com.nanobanana.repository.FiadoRepository;
import com.nanobanana.repository.ProdutoRepository;
import com.nanobanana.repository.VendaItemRepository;
import com.nanobanana.repository.VendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Regras de negócio de vendas (PDV).
 *
 * Mantém o controller responsável apenas por HTTP/sessão, e concentra
 * aqui as validações e a transação de finalização/cancelamento.
 */
@Service
public class VendaService {

    private final ProdutoRepository produtoRepository;
    private final VendaRepository vendaRepository;
    private final VendaItemRepository vendaItemRepository;
    private final FiadoRepository fiadoRepository;
    private final StockService stockService;
    private final AlertaSistemaRepository alertaSistemaRepository;

    public VendaService(ProdutoRepository produtoRepository,
                        VendaRepository vendaRepository,
                        VendaItemRepository vendaItemRepository,
                        FiadoRepository fiadoRepository,
                        StockService stockService,
                        AlertaSistemaRepository alertaSistemaRepository) {
        this.produtoRepository = produtoRepository;
        this.vendaRepository = vendaRepository;
        this.vendaItemRepository = vendaItemRepository;
        this.fiadoRepository = fiadoRepository;
        this.stockService = stockService;
        this.alertaSistemaRepository = alertaSistemaRepository;
    }

    /**
     * Finaliza uma venda: valida stock, calcula totais, salva venda/itens,
     * baixa o stock e registra fiado quando necessário.
     */
    @Transactional
    public Venda finalizarVenda(UUID tenantId,
                                UUID vendedorId,
                                List<VendaItemRequest> itens,
                                BigDecimal desconto,
                                Venda.MetodoPagamento metodoPagamento,
                                BigDecimal valorPago,
                                String nomeClienteFiado,
                                String telefoneClienteFiado) {

        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Adicione pelo menos 1 produto");
        }

        Venda venda = new Venda();
        venda.setTenantId(tenantId);
        venda.setVendedorId(vendedorId);
        venda.setMetodoPagamento(metodoPagamento);

        BigDecimal subtotal = BigDecimal.ZERO;
        List<VendaItem> vendaItens = new ArrayList<>();

        for (VendaItemRequest itemData : itens) {
            if (itemData.quantidade() == null || itemData.quantidade() <= 0) {
                throw new IllegalArgumentException("Quantidade inválida para o produto " + itemData.produtoId());
            }

            UUID produtoId = itemData.produtoId();
            int quantidade = itemData.quantidade();

            Produto produto = produtoRepository.findByTenantIdAndId(tenantId, produtoId)
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + produtoId));

            if (produto.getStockAtual() < quantidade) {
                throw new IllegalArgumentException("Stock insuficiente para " + produto.getNome()
                        + ". Disponível: " + produto.getStockAtual());
            }

            BigDecimal precoUnitario = produto.getPrecoVenda();
            BigDecimal itemSubtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));

            VendaItem item = new VendaItem();
            item.setProdutoId(produtoId);
            item.setNomeProduto(produto.getNome());
            item.setQuantidade(quantidade);
            item.setPrecoUnitario(precoUnitario);
            item.setSubtotal(itemSubtotal);
            vendaItens.add(item);

            subtotal = subtotal.add(itemSubtotal);
        }

        BigDecimal descontoFinal = desconto != null ? desconto : BigDecimal.ZERO;
        if (descontoFinal.signum() < 0) {
            throw new IllegalArgumentException("O desconto não pode ser negativo");
        }
        BigDecimal total = subtotal.subtract(descontoFinal);
        if (total.signum() < 0) {
            throw new IllegalArgumentException("O desconto não pode ser maior que o subtotal");
        }

        venda.setSubtotal(subtotal);
        venda.setDesconto(descontoFinal);
        venda.setTotal(total);

        if (metodoPagamento == Venda.MetodoPagamento.FIADO) {
            if (nomeClienteFiado == null || nomeClienteFiado.isBlank()) {
                throw new IllegalArgumentException("Informe o nome do cliente para venda fiada");
            }
            venda.setNomeClienteFiado(nomeClienteFiado);
            venda.setTelefoneClienteFiado(telefoneClienteFiado);
        }

        if (metodoPagamento == Venda.MetodoPagamento.CASH) {
            if (valorPago == null) {
                venda.setValorPago(total);
                venda.setTroco(BigDecimal.ZERO);
            } else {
                venda.setValorPago(valorPago);
                venda.setTroco(valorPago.compareTo(total) >= 0
                        ? valorPago.subtract(total)
                        : BigDecimal.ZERO);
            }
        }

        venda = vendaRepository.save(venda);

        for (VendaItem item : vendaItens) {
            item.setVendaId(venda.getId());
            vendaItemRepository.save(item);
            stockService.baixarStockVenda(tenantId, item.getProdutoId(), item.getQuantidade());
        }

        if (metodoPagamento == Venda.MetodoPagamento.FIADO) {
            Fiado fiado = new Fiado();
            fiado.setTenantId(tenantId);
            fiado.setVendaId(venda.getId());
            fiado.setNomeCliente(nomeClienteFiado);
            fiado.setTelefoneCliente(telefoneClienteFiado);
            fiado.setValor(total);
            fiadoRepository.save(fiado);
        }

        return venda;
    }

    /**
     * Cancela uma venda, restaura o stock e gera alerta.
     * Apenas DONO/GERENTE podem cancelar.
     */
    @Transactional
    public Venda cancelarVenda(UUID tenantId, UUID vendaId, Usuario usuario, String motivo) {
        if (usuario == null || !usuario.podeCancelarVenda()) {
            throw new IllegalStateException("Você NÃO tem permissão para cancelar vendas.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("O motivo do cancelamento é obrigatório");
        }

        Venda venda = vendaRepository.findById(vendaId)
                .orElseThrow(() -> new IllegalArgumentException("Venda não encontrada"));

        if (!venda.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Venda não pertence a esta loja");
        }
        if (venda.getStatus() == Venda.Status.CANCELADA) {
            throw new IllegalStateException("Venda já está cancelada");
        }

        venda.setStatus(Venda.Status.CANCELADA);
        venda.setCanceladaPor(usuario.getId());
        venda.setMotivoCancelamento(motivo);
        vendaRepository.save(venda);

        List<VendaItem> itens = vendaItemRepository.findByVendaId(venda.getId());
        for (VendaItem item : itens) {
            stockService.restaurarStockVenda(venda.getTenantId(), item.getProdutoId(), item.getQuantidade());
        }

        AlertaSistema alerta = new AlertaSistema();
        alerta.setTenantId(venda.getTenantId());
        alerta.setTipo(AlertaSistema.Tipo.VENDA_CANCELADA);
        alerta.setDescricao("Venda #" + vendaId.toString().substring(0, 8) + " cancelada por "
                + usuario.getNome() + ". Motivo: " + motivo + ". Valor: " + venda.getTotal() + " MZN");
        alerta.setSeveridade(AlertaSistema.Severidade.MEDIA);
        alerta.setUsuarioRelacionado(usuario.getId());
        alerta.setRegistroId(venda.getId());
        alertaSistemaRepository.save(alerta);

        return venda;
    }
}
