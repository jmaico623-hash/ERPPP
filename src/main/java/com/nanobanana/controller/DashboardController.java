package com.nanobanana.controller;

import com.nanobanana.model.*;
import com.nanobanana.repository.*;
import com.nanobanana.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * DASHBOARD
 * Dono: vê TUDO (vendas, stock, alertas, auditoria, vendedores)
 * Gerente: vê vendas e relatórios (NÃO vê stock)
 * Vendedor: vê APENAS suas vendas do dia
 */
@Controller
public class DashboardController {

    @Autowired
    private AuthService authService;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private VendaItemRepository vendaItemRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private FiadoRepository fiadoRepository;

    @Autowired
    private AlertaSistemaRepository alertaSistemaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model,
                           @RequestParam(required = false, defaultValue = "hoje") String periodo) {
        
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        // Define período
        LocalDateTime inicio, fim = LocalDateTime.now();
        if ("hoje".equals(periodo)) {
            inicio = LocalDate.now().atStartOfDay();
        } else if ("semana".equals(periodo)) {
            inicio = LocalDate.now().minusWeeks(1).atStartOfDay();
        } else if ("mes".equals(periodo)) {
            inicio = LocalDate.now().minusMonths(1).atStartOfDay();
        } else {
            inicio = LocalDate.now().atStartOfDay();
        }

        // ============ DADOS COMUNS (todos podem ver suas vendas) ============
        List<Venda> vendas;
        BigDecimal totalVendas;
        long quantidadeVendas;

        if (usuario.getRole() == Usuario.Role.VENDEDOR) {
            // Vendedor vê APENAS suas vendas
            vendas = vendaRepository.findByTenantIdAndVendedorId(tenantId, usuario.getId())
                    .stream()
                    .filter(v -> v.getStatus() == Venda.Status.CONCLUIDA && 
                            v.getDataHora().isAfter(inicio))
                    .toList();
            totalVendas = vendas.stream().map(Venda::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            quantidadeVendas = vendas.size();
        } else {
            // Dono/Gerente vê TODAS as vendas
            vendas = vendaRepository.findByTenantIdAndStatusOrderByDataHoraDesc(tenantId, Venda.Status.CONCLUIDA)
                    .stream()
                    .filter(v -> v.getDataHora().isAfter(inicio))
                    .limit(50)
                    .toList();
            totalVendas = vendaRepository.somaVendasPeriodo(tenantId, inicio, fim);
            quantidadeVendas = vendaRepository.countVendasPeriodo(tenantId, inicio, fim);
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("isDono", usuario.getRole() == Usuario.Role.DONO);
        model.addAttribute("isGerente", usuario.getRole() == Usuario.Role.GERENTE);
        model.addAttribute("isVendedor", usuario.getRole() == Usuario.Role.VENDEDOR);
        model.addAttribute("vendas", vendas);
        model.addAttribute("totalVendas", totalVendas != null ? totalVendas : BigDecimal.ZERO);
        model.addAttribute("quantidadeVendas", quantidadeVendas);

        // ============ DADOS DO DONO ============
        if (usuario.getRole() == Usuario.Role.DONO) {
            // Stock
            long totalProdutos = produtoRepository.countByTenantId(tenantId);
            List<Produto> produtosStockBaixo = produtoRepository.findProdutosStockBaixo(tenantId);
            List<Produto> produtosSemStock = produtoRepository.findProdutosSemStock(tenantId);
            BigDecimal valorStock = produtoRepository.valorTotalStock(tenantId);

            // Fiados
            BigDecimal totalFiado = fiadoRepository.totalFiadoNaoPago(tenantId);
            long fiadosNaoPagos = fiadoRepository.countByTenantIdAndPago(tenantId, false);

            // Alertas
            long alertasNaoLidos = alertaSistemaRepository.countByTenantIdAndLidoFalse(tenantId);
            List<AlertaSistema> alertas = alertaSistemaRepository.findByTenantIdOrderByCriadoEmDesc(tenantId)
                    .stream().limit(10).toList();

            // Vendedores ativos
            List<Usuario> vendedores = usuarioRepository.findByTenantIdAndRole(tenantId, Usuario.Role.VENDEDOR);

            // Ranking vendedores do dia
            LocalDateTime inicioHoje = LocalDate.now().atStartOfDay();
            List<Object[]> rankingVendedores = vendaRepository.rankingVendedores(tenantId, inicioHoje, fim);

            model.addAttribute("totalProdutos", totalProdutos);
            model.addAttribute("produtosStockBaixo", produtosStockBaixo);
            model.addAttribute("produtosSemStock", produtosSemStock);
            model.addAttribute("valorStock", valorStock != null ? valorStock : BigDecimal.ZERO);
            model.addAttribute("totalFiado", totalFiado != null ? totalFiado : BigDecimal.ZERO);
            model.addAttribute("fiadosNaoPagos", fiadosNaoPagos);
            model.addAttribute("alertasNaoLidos", alertasNaoLidos);
            model.addAttribute("alertas", alertas);
            model.addAttribute("vendedores", vendedores);
            model.addAttribute("rankingVendedores", rankingVendedores);

            return "dashboard-dono";
        }

        // ============ DADOS DO GERENTE ============
        if (usuario.getRole() == Usuario.Role.GERENTE) {
            // Gerente vê relatórios mas NÃO vê stock detalhado
            model.addAttribute("periodo", periodo);
            return "dashboard-gerente";
        }

        // ============ DADOS DO VENDEDOR ============
        return "dashboard-vendedor";
    }

    @GetMapping("/alertas")
    public String alertas(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        
        // APENAS DONO vê alertas
        if (usuario.getRole() != Usuario.Role.DONO) {
            return "redirect:/pdv";
        }

        UUID tenantId = authService.getTenantIdLogado();
        List<AlertaSistema> alertas = alertaSistemaRepository.findByTenantIdOrderByCriadoEmDesc(tenantId);

        model.addAttribute("usuario", usuario);
        model.addAttribute("alertas", alertas);
        
        return "alertas";
    }

    @PostMapping("/alertas/{id}/resolver")
    public String resolverAlerta(@PathVariable Long id) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        AlertaSistema alerta = alertaSistemaRepository.findById(id).orElse(null);
        if (alerta != null) {
            alerta.setResolvido(true);
            alerta.setResolvidoEm(java.time.LocalDateTime.now());
            alerta.setResolvidoPor(usuarioOpt.get().getId());
            alertaSistemaRepository.save(alerta);
        }

        return "redirect:/alertas";
    }
}
