package com.nanobanana.controller;

import com.nanobanana.model.Produto;
import com.nanobanana.model.Usuario;
import com.nanobanana.repository.ProdutoRepository;
import com.nanobanana.repository.VendaItemRepository;
import com.nanobanana.service.AuthService;
import com.nanobanana.service.StockService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Inventário e produtos estagnados.
 */
@Controller
public class InventoryController {

    private static final long DIAS_ESTAGNADO = 30;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AuthService authService;
    private final ProdutoRepository produtoRepository;
    private final VendaItemRepository vendaItemRepository;
    private final StockService stockService;

    public InventoryController(AuthService authService,
                               ProdutoRepository produtoRepository,
                               VendaItemRepository vendaItemRepository,
                               StockService stockService) {
        this.authService = authService;
        this.produtoRepository = produtoRepository;
        this.vendaItemRepository = vendaItemRepository;
        this.stockService = stockService;
    }

    @GetMapping("/inventario")
    public String inventario(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        if (!usuario.podeVerRelatorios()) {
            return "redirect:/pdv";
        }

        UUID tenantId = authService.getTenantIdLogado();
        Map<UUID, LocalDateTime> ultimaVenda = ultimaVendaPorProduto(tenantId);

        List<ProdutoInventario> produtos = produtoRepository.findByTenantIdOrderByNome(tenantId)
                .stream()
                .map(produto -> toInventario(produto, ultimaVenda.get(produto.getId())))
                .sorted(Comparator
                        .comparing(ProdutoInventario::estaEstagnado).reversed()
                        .thenComparing(ProdutoInventario::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        model.addAttribute("usuario", usuario);
        model.addAttribute("produtos", produtos);
        return "inventario";
    }

    @PostMapping("/inventario/adicionar")
    public String adicionarProduto(@RequestParam String nome,
                                   @RequestParam BigDecimal preco,
                                   @RequestParam int stock,
                                   RedirectAttributes redirectAttrs) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        if (!usuario.podeGerirStock()) {
            redirectAttrs.addFlashAttribute("erro", "APENAS o dono pode adicionar produtos.");
            return "redirect:/inventario";
        }

        if (nome == null || nome.isBlank() || preco == null || preco.compareTo(BigDecimal.ZERO) <= 0 || stock < 0) {
            redirectAttrs.addFlashAttribute("erro", "Nome, preço maior que zero e stock não negativo são obrigatórios.");
            return "redirect:/inventario";
        }

        UUID tenantId = authService.getTenantIdLogado();
        Produto produto = new Produto();
        produto.setTenantId(tenantId);
        produto.setNome(nome.trim());
        produto.setPrecoVenda(preco.setScale(2, RoundingMode.HALF_UP));
        produto.setStockAtual(0);
        produto.setStockMinimo(5);
        produtoRepository.save(produto);

        if (stock > 0) {
            stockService.adicionarStock(tenantId, produto.getId(), stock, usuario.getId(),
                    null, null, null, "Stock inicial");
        }

        redirectAttrs.addFlashAttribute("sucesso", "Produto adicionado!");
        return "redirect:/inventario";
    }

    @PostMapping("/inventario/{id}/reabastecer")
    public String reabastecer(@PathVariable UUID id,
                              @RequestParam int qtd,
                              RedirectAttributes redirectAttrs) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        if (!usuario.podeGerirStock()) {
            redirectAttrs.addFlashAttribute("erro", "APENAS o dono pode reabastecer stock.");
            return "redirect:/inventario";
        }

        if (qtd <= 0) {
            redirectAttrs.addFlashAttribute("erro", "A quantidade deve ser maior que zero.");
            return "redirect:/inventario";
        }

        try {
            stockService.adicionarStock(authService.getTenantIdLogado(), id, qtd, usuario.getId(),
                    null, null, null, "Reabastecimento inventário");
            redirectAttrs.addFlashAttribute("sucesso", "Stock reabastecido!");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/inventario";
    }

    @GetMapping("/estagnados")
    public String estagnados(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        if (!usuario.podeVerRelatorios()) {
            return "redirect:/pdv";
        }

        UUID tenantId = authService.getTenantIdLogado();
        Map<UUID, LocalDateTime> ultimaVenda = ultimaVendaPorProduto(tenantId);
        LocalDate hoje = LocalDate.now();

        List<ProdutoEstagnado> produtos = produtoRepository.findByTenantIdOrderByNome(tenantId)
                .stream()
                .filter(p -> p.getStockAtual() > 0)
                .map(p -> toEstagnado(p, ultimaVenda.get(p.getId()), hoje))
                .filter(ProdutoEstagnado::estagnado)
                .sorted(Comparator.comparingLong(ProdutoEstagnado::diasSemVenda).reversed())
                .toList();

        BigDecimal totalParado = produtos.stream()
                .map(ProdutoEstagnado::valorStock)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("usuario", usuario);
        model.addAttribute("estagnados", produtos);
        model.addAttribute("totalParado", totalParado);
        return "estagnados";
    }

    private Map<UUID, LocalDateTime> ultimaVendaPorProduto(UUID tenantId) {
        Map<UUID, LocalDateTime> map = new HashMap<>();
        for (Object[] row : vendaItemRepository.ultimaVendaPorProduto(tenantId)) {
            map.put((UUID) row[0], (LocalDateTime) row[1]);
        }
        return map;
    }

    private ProdutoInventario toInventario(Produto produto, LocalDateTime ultimaVenda) {
        BigDecimal unitCost = produto.getPrecoCusto() != null ? produto.getPrecoCusto() : produto.getPrecoVenda();
        BigDecimal valorStock = unitCost.multiply(BigDecimal.valueOf(produto.getStockAtual()));
        long dias = diasSemVenda(ultimaVenda, LocalDate.now());
        return new ProdutoInventario(
                produto.getId(),
                produto.getNome(),
                produto.getPrecoVenda(),
                produto.getStockAtual(),
                valorStock,
                dias > DIAS_ESTAGNADO);
    }

    private ProdutoEstagnado toEstagnado(Produto produto, LocalDateTime ultimaVenda, LocalDate hoje) {
        BigDecimal unitCost = produto.getPrecoCusto() != null ? produto.getPrecoCusto() : produto.getPrecoVenda();
        BigDecimal valorStock = unitCost.multiply(BigDecimal.valueOf(produto.getStockAtual()));
        long dias = diasSemVenda(ultimaVenda, hoje);
        return new ProdutoEstagnado(
                produto.getNome(),
                ultimaVenda != null ? ultimaVenda.format(DATA) : "Nunca",
                dias,
                valorStock);
    }

    private long diasSemVenda(LocalDateTime ultimaVenda, LocalDate hoje) {
        if (ultimaVenda == null) return 9999;
        return Math.max(0, ChronoUnit.DAYS.between(ultimaVenda.toLocalDate(), hoje));
    }

    public static class ProdutoInventario {
        private final UUID id;
        private final String nome;
        private final BigDecimal preco;
        private final int stock;
        private final BigDecimal valorStock;
        private final boolean estagnado;

        public ProdutoInventario(UUID id, String nome, BigDecimal preco, int stock, BigDecimal valorStock, boolean estagnado) {
            this.id = id;
            this.nome = nome;
            this.preco = preco;
            this.stock = stock;
            this.valorStock = valorStock;
            this.estagnado = estagnado;
        }

        public UUID getId() { return id; }
        public String getNome() { return nome; }
        public BigDecimal getPreco() { return preco; }
        public int getStock() { return stock; }
        public BigDecimal getValorStock() { return valorStock; }
        public boolean estaEstagnado() { return estagnado; }
    }

    public static class ProdutoEstagnado {
        private final String nome;
        private final String ultimaVenda;
        private final long diasSemVenda;
        private final BigDecimal valorStock;

        public ProdutoEstagnado(String nome, String ultimaVenda, long diasSemVenda, BigDecimal valorStock) {
            this.nome = nome;
            this.ultimaVenda = ultimaVenda;
            this.diasSemVenda = diasSemVenda;
            this.valorStock = valorStock;
        }

        public String getNome() { return nome; }
        public String getUltimaVenda() { return ultimaVenda; }
        public long diasSemVenda() { return diasSemVenda; }
        public BigDecimal valorStock() { return valorStock; }
        public BigDecimal getValorStock() { return valorStock; }
        public boolean estagnado() { return diasSemVenda > DIAS_ESTAGNADO; }
    }
}
