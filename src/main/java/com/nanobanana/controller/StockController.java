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

/**
 * CONTROLLER DE GESTÃO DE STOCK - APENAS DONO
 * Vendedores NÃO têm acesso a este controller
 */
@Controller
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    private EntradaStockRepository entradaStockRepository;

    @Autowired
    private BaixaStockRepository baixaStockRepository;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    /**
     * GESTÃO DE STOCK - APENAS DONO
     */
    @GetMapping("/gestao")
    public String gestaoStock(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        
        // VERIFICA SE É DONO
        if (usuario.getRole() != Usuario.Role.DONO) {
            return "redirect:/pdv"; // Vendedor não acessa gestão de stock
        }

        UUID tenantId = authService.getTenantIdLogado();

        List<Produto> produtos = produtoRepository.findByTenantIdOrderByNome(tenantId);
        List<EntradaStock> entradas = entradaStockRepository.findByTenantIdOrderByDataEntradaDesc(tenantId)
                .stream().limit(20).toList();
        List<BaixaStock> baixas = baixaStockRepository.findByTenantIdOrderByDataBaixaDesc(tenantId)
                .stream().limit(20).toList();

        model.addAttribute("usuario", usuario);
        model.addAttribute("produtos", produtos);
        model.addAttribute("entradas", entradas);
        model.addAttribute("baixas", baixas);
        model.addAttribute("produtosStockBaixo", stockService.getProdutosStockBaixo(tenantId));
        
        return "stock-gestao";
    }

    /**
     * ADICIONAR STOCK - APENAS DONO
     */
    @PostMapping("/adicionar")
    public String adicionarStock(@RequestParam UUID produtoId,
                                 @RequestParam int quantidade,
                                 @RequestParam(required = false) String fornecedor,
                                 @RequestParam(required = false) String notaFiscal,
                                 @RequestParam(required = false) BigDecimal precoCusto,
                                 @RequestParam(required = false) String motivo,
                                 RedirectAttributes redirectAttrs) {
        
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        
        // APENAS DONO pode adicionar stock
        if (!usuario.podeGerirStock()) {
            redirectAttrs.addFlashAttribute("erro", "APENAS o dono pode adicionar stock.");
            return "redirect:/pdv";
        }

        UUID tenantId = authService.getTenantIdLogado();

        try {
            stockService.adicionarStock(tenantId, produtoId, quantidade, usuario.getId(),
                    fornecedor, notaFiscal, precoCusto, motivo);
            redirectAttrs.addFlashAttribute("sucesso", "Stock adicionado com sucesso!");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }

        return "redirect:/stock/gestao";
    }

    /**
     * BAIXAR STOCK - APENAS DONO
     */
    @PostMapping("/baixar")
    public String baixarStock(@RequestParam UUID produtoId,
                              @RequestParam int quantidade,
                              @RequestParam String tipoBaixa,
                              @RequestParam String motivo,
                              RedirectAttributes redirectAttrs) {
        
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        
        // APENAS DONO pode baixar stock
        if (!usuario.podeGerirStock()) {
            redirectAttrs.addFlashAttribute("erro", "APENAS o dono pode baixar stock.");
            return "redirect:/pdv";
        }

        UUID tenantId = authService.getTenantIdLogado();

        try {
            BaixaStock.TipoBaixa tipo = BaixaStock.TipoBaixa.valueOf(tipoBaixa.toUpperCase());
            stockService.baixarStock(tenantId, produtoId, quantidade, usuario.getId(), tipo, motivo);
            redirectAttrs.addFlashAttribute("sucesso", "Baixa registrada com sucesso!");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }

        return "redirect:/stock/gestao";
    }

    /**
     * ADICIONAR NOVO PRODUTO - APENAS DONO
     */
    @PostMapping("/produto/adicionar")
    public String adicionarProduto(@RequestParam String nome,
                                   @RequestParam(required = false) String codigoBarras,
                                   @RequestParam(required = false) String categoria,
                                   @RequestParam BigDecimal precoCusto,
                                   @RequestParam BigDecimal precoVenda,
                                   @RequestParam(required = false, defaultValue = "5") int stockMinimo,
                                   @RequestParam(required = false, defaultValue = "0") int stockInicial,
                                   RedirectAttributes redirectAttrs) {
        
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        
        // APENAS DONO pode adicionar produtos
        if (!usuario.podeGerirStock()) {
            redirectAttrs.addFlashAttribute("erro", "APENAS o dono pode adicionar produtos.");
            return "redirect:/pdv";
        }

        UUID tenantId = authService.getTenantIdLogado();

        Produto produto = new Produto();
        produto.setTenantId(tenantId);
        produto.setNome(nome);
        produto.setCodigoBarras(codigoBarras);
        produto.setCategoria(categoria);
        produto.setPrecoCusto(precoCusto);
        produto.setPrecoVenda(precoVenda);
        produto.setStockMinimo(stockMinimo);
        produto.setStockAtual(stockInicial);
        produtoRepository.save(produto);

        // Se tem stock inicial, registra entrada
        if (stockInicial > 0) {
            stockService.adicionarStock(tenantId, produto.getId(), stockInicial, usuario.getId(),
                    null, null, precoCusto, "Stock inicial");
        }

        redirectAttrs.addFlashAttribute("sucesso", "Produto adicionado!");
        return "redirect:/stock/gestao";
    }

    /**
     * EDITAR PREÇO - APENAS DONO
     */
    @PostMapping("/produto/{id}/editar-preco")
    public String editarPreco(@PathVariable UUID id,
                              @RequestParam BigDecimal precoVenda,
                              @RequestParam(required = false) BigDecimal precoCusto,
                              RedirectAttributes redirectAttrs) {
        
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        // APENAS DONO pode editar preços
        if (!usuario.podeGerirPrecos()) {
            redirectAttrs.addFlashAttribute("erro", "APENAS o dono pode editar preços.");
            return "redirect:/pdv";
        }

        Produto produto = produtoRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        BigDecimal precoAntigo = produto.getPrecoVenda();
        produto.setPrecoVenda(precoVenda);
        if (precoCusto != null) {
            produto.setPrecoCusto(precoCusto);
        }
        produtoRepository.save(produto);

        redirectAttrs.addFlashAttribute("sucesso", "Preço atualizado!");
        return "redirect:/stock/gestao";
    }
}
