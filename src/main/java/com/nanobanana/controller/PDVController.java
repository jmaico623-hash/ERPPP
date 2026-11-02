package com.nanobanana.controller;

import com.nanobanana.model.*;
import com.nanobanana.repository.*;
import com.nanobanana.service.AuthService;
import com.nanobanana.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CONTROLLER DO PONTO DE VENDA (PDV)
 * Acessível por TODOS os usuários (DONO, GERENTE, VENDEDOR)
 * Vendedores APENAS vendem - não podem alterar stock ou preços
 */
@Controller
public class PDVController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private VendaItemRepository vendaItemRepository;

    @Autowired
    private FiadoRepository fiadoRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    @Autowired
    private AlertaSistemaRepository alertaSistemaRepository;

    /**
     * PDV - Tela de vendas
     */
    @GetMapping("/pdv")
    public String pdv(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        // Lista produtos disponíveis
        List<Produto> produtos = produtoRepository.findByTenantIdOrderByNome(tenantId);
        model.addAttribute("usuario", usuario);
        model.addAttribute("isDono", usuario.getRole() == Usuario.Role.DONO);
        model.addAttribute("produtos", produtos);
        
        return "pdv";
    }

    /**
     * FINALIZAR VENDA
     * Todos podem vender. Stock baixa automaticamente.
     */
    @PostMapping("/pdv/finalizar")
    public String finalizarVenda(@RequestParam String itensJson,
                                 @RequestParam(required = false) BigDecimal desconto,
                                 @RequestParam String metodoPagamento,
                                 @RequestParam(required = false) BigDecimal valorPago,
                                 @RequestParam(required = false) String nomeClienteFiado,
                                 @RequestParam(required = false) String telefoneClienteFiado,
                                 RedirectAttributes redirectAttrs) {
        
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        try {
            // Parse itens JSON
            List<Map<String, Object>> itens = parseItensJson(itensJson);
            if (itens.isEmpty()) {
                redirectAttrs.addFlashAttribute("erro", "Adicione pelo menos 1 produto");
                return "redirect:/pdv";
            }

            // Cria venda
            Venda venda = new Venda();
            venda.setTenantId(tenantId);
            venda.setVendedorId(usuario.getId());
            venda.setMetodoPagamento(Venda.MetodoPagamento.valueOf(metodoPagamento.toUpperCase()));
            
            // Calcula totais
            BigDecimal subtotal = BigDecimal.ZERO;
            List<VendaItem> vendaItens = new ArrayList<>();

            for (Map<String, Object> itemData : itens) {
                UUID produtoId = UUID.fromString((String) itemData.get("produtoId"));
                int quantidade = (int) itemData.get("quantidade");

                Produto produto = produtoRepository.findByTenantIdAndId(tenantId, produtoId)
                        .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + produtoId));

                // Verifica stock
                if (produto.getStockAtual() < quantidade) {
                    throw new RuntimeException("Stock insuficiente para " + produto.getNome() + 
                            ". Disponível: " + produto.getStockAtual());
                }

                BigDecimal precoUnitario = produto.getPrecoVenda();
                BigDecimal itemSubtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));

                VendaItem item = new VendaItem();
                item.setVendaId(null); // será setado após salvar venda
                item.setProdutoId(produtoId);
                item.setNomeProduto(produto.getNome());
                item.setQuantidade(quantidade);
                item.setPrecoUnitario(precoUnitario);
                item.setSubtotal(itemSubtotal);
                vendaItens.add(item);

                subtotal = subtotal.add(itemSubtotal);
            }

            // Aplica desconto
            BigDecimal descontoFinal = desconto != null ? desconto : BigDecimal.ZERO;
            BigDecimal total = subtotal.subtract(descontoFinal);

            venda.setSubtotal(subtotal);
            venda.setDesconto(descontoFinal);
            venda.setTotal(total);

            // Fiado
            if (venda.getMetodoPagamento() == Venda.MetodoPagamento.FIADO) {
                venda.setNomeClienteFiado(nomeClienteFiado);
                venda.setTelefoneClienteFiado(telefoneClienteFiado);
            }

            // Pagamento cash - cálculo de troco
            if (venda.getMetodoPagamento() == Venda.MetodoPagamento.CASH) {
                if (valorPago != null && valorPago.compareTo(total) >= 0) {
                    venda.setValorPago(valorPago);
                    venda.setTroco(valorPago.subtract(total));
                } else if (valorPago != null && valorPago.compareTo(total) < 0) {
                    // Valor pago menor que total - registra mesmo assim
                    venda.setValorPago(valorPago);
                    venda.setTroco(BigDecimal.ZERO);
                } else {
                    // Sem valor pago - assume exacto
                    venda.setValorPago(total);
                    venda.setTroco(BigDecimal.ZERO);
                }
            }

            // Salva venda
            venda = vendaRepository.save(venda);

            // Salva itens e baixa stock
            for (VendaItem item : vendaItens) {
                item.setVendaId(venda.getId());
                vendaItemRepository.save(item);

                // Baixa stock automaticamente
                stockService.baixarStockVenda(tenantId, item.getProdutoId(), item.getQuantidade());
            }

            // Registra fiado se aplicável
            if (venda.getMetodoPagamento() == Venda.MetodoPagamento.FIADO) {
                Fiado fiado = new Fiado();
                fiado.setTenantId(tenantId);
                fiado.setVendaId(venda.getId());
                fiado.setNomeCliente(nomeClienteFiado);
                fiado.setTelefoneCliente(telefoneClienteFiado);
                fiado.setValor(total);
                fiadoRepository.save(fiado);
            }

            redirectAttrs.addFlashAttribute("sucesso", "Venda registrada! Total: " + total + " MZN");
            return "redirect:/pdv";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", "Erro ao finalizar venda: " + e.getMessage());
            return "redirect:/pdv";
        }
    }

    /**
     * CANCELAR VENDA - APENAS DONO/GERENTE
     * Vendedores NÃO podem cancelar
     */
    @PostMapping("/pdv/cancelar/{id}")
    public String cancelarVenda(@PathVariable UUID id,
                                @RequestParam String motivo,
                                RedirectAttributes redirectAttrs) {
        
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();

        // VERIFICA PERMISSÃO: Apenas DONO/GERENTE
        if (!usuario.podeCancelarVenda()) {
            redirectAttrs.addFlashAttribute("erro", "Você NÃO tem permissão para cancelar vendas.");
            return "redirect:/pdv";
        }

        Venda venda = vendaRepository.findById(id).orElse(null);
        if (venda == null) {
            redirectAttrs.addFlashAttribute("erro", "Venda não encontrada");
            return "redirect:/pdv";
        }

        if (venda.getStatus() == Venda.Status.CANCELADA) {
            redirectAttrs.addFlashAttribute("erro", "Venda já está cancelada");
            return "redirect:/pdv";
        }

        // Cancela venda
        venda.setStatus(Venda.Status.CANCELADA);
        venda.setCanceladaPor(usuario.getId());
        venda.setMotivoCancelamento(motivo);
        vendaRepository.save(venda);

        // Restaura stock
        List<VendaItem> itens = vendaItemRepository.findByVendaId(venda.getId());
        for (VendaItem item : itens) {
            stockService.restaurarStockVenda(venda.getTenantId(), item.getProdutoId(), item.getQuantidade());
        }

        // Alerta de cancelamento
        AlertaSistema alerta = new AlertaSistema();
        alerta.setTenantId(venda.getTenantId());
        alerta.setTipo(AlertaSistema.Tipo.VENDA_CANCELADA);
        alerta.setDescricao("Venda #" + id.toString().substring(0, 8) + " cancelada por " + 
                usuario.getNome() + ". Motivo: " + motivo + ". Valor: " + venda.getTotal() + " MZN");
        alerta.setSeveridade(AlertaSistema.Severidade.MEDIA);
        alerta.setUsuarioRelacionado(usuario.getId());
        alerta.setRegistroId(venda.getId());
        alertaSistemaRepository.save(alerta);

        redirectAttrs.addFlashAttribute("sucesso", "Venda cancelada. Stock restaurado.");
        return "redirect:/pdv";
    }

    /**
     * HISTÓRICO DE VENDAS
     * Dono e Gerente vêem tudo. Vendedor vê apenas as suas.
     */
    @GetMapping("/pdv/historico")
    public String historico(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        List<Venda> vendas;
        if (usuario.getRole() == Usuario.Role.VENDEDOR) {
            // Vendedor vê APENAS as suas vendas
            vendas = vendaRepository.findByTenantIdAndVendedorId(tenantId, usuario.getId())
                    .stream().limit(100).collect(Collectors.toList());
        } else {
            // Dono/Gerente vê TODAS as vendas
            vendas = vendaRepository.findByTenantIdOrderByDataHoraDesc(tenantId)
                    .stream().limit(100).collect(Collectors.toList());
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("vendas", vendas);
        model.addAttribute("isDono", usuario.getRole() == Usuario.Role.DONO);
        model.addAttribute("isGerente", usuario.getRole() == Usuario.Role.GERENTE);
        model.addAttribute("isVendedor", usuario.getRole() == Usuario.Role.VENDEDOR);

        return "pdv-historico";
    }

    // ============ MÉTODOS AUXILIARES ============

    private List<Map<String, Object>> parseItensJson(String itensJson) {
        List<Map<String, Object>> itens = new ArrayList<>();

        if (itensJson == null || itensJson.isBlank()) return itens;

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            itens = mapper.readValue(itensJson, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            // Se falhar, retorna lista vazia
        }

        return itens;
    }
}
