package com.nanobanana.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobanana.dto.VendaItemRequest;
import com.nanobanana.model.Produto;
import com.nanobanana.model.Usuario;
import com.nanobanana.model.Venda;
import com.nanobanana.repository.ProdutoRepository;
import com.nanobanana.repository.TenantRepository;
import com.nanobanana.repository.UsuarioRepository;
import com.nanobanana.repository.VendaItemRepository;
import com.nanobanana.repository.VendaRepository;
import com.nanobanana.service.AuthService;
import com.nanobanana.service.DevolucaoService;
import com.nanobanana.service.VendaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * CONTROLLER DO PONTO DE VENDA (PDV)
 * Acessível por TODOS os usuários (DONO, GERENTE, VENDEDOR)
 * Vendedores APENAS vendem - não podem alterar stock ou preços.
 */
@Controller
public class PDVController {

    private final AuthService authService;
    private final ProdutoRepository produtoRepository;
    private final VendaRepository vendaRepository;
    private final VendaItemRepository vendaItemRepository;
    private final VendaService vendaService;
    private final DevolucaoService devolucaoService;
    private final TenantRepository tenantRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public PDVController(AuthService authService,
                         ProdutoRepository produtoRepository,
                         VendaRepository vendaRepository,
                         VendaItemRepository vendaItemRepository,
                         VendaService vendaService,
                         DevolucaoService devolucaoService,
                         TenantRepository tenantRepository,
                         UsuarioRepository usuarioRepository,
                         ObjectMapper objectMapper) {
        this.authService = authService;
        this.produtoRepository = produtoRepository;
        this.vendaRepository = vendaRepository;
        this.vendaItemRepository = vendaItemRepository;
        this.vendaService = vendaService;
        this.devolucaoService = devolucaoService;
        this.tenantRepository = tenantRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * PDV - Tela de vendas
     */
    @GetMapping("/pdv")
    public String pdv(Model model, HttpSession session) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        List<Produto> produtos = produtoRepository.findByTenantIdOrderByNome(tenantId);
        String checkoutToken = UUID.randomUUID().toString();
        session.setAttribute("pdvCheckoutToken", checkoutToken);
        model.addAttribute("checkoutToken", checkoutToken);
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
                                 @RequestParam String checkoutToken,
                                 @RequestParam(required = false) BigDecimal desconto,
                                 @RequestParam String metodoPagamento,
                                 @RequestParam(required = false) BigDecimal valorPago,
                                 @RequestParam(required = false) String nomeClienteFiado,
                                 @RequestParam(required = false) String telefoneClienteFiado,
                                 RedirectAttributes redirectAttrs,
                                 HttpSession session) {

        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        String expectedToken = (String) session.getAttribute("pdvCheckoutToken");
        if (expectedToken == null || !expectedToken.equals(checkoutToken)) {
            redirectAttrs.addFlashAttribute("erro", "Venda já processada ou formulário expirado. Tente novamente.");
            return "redirect:/pdv";
        }
        session.removeAttribute("pdvCheckoutToken");

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        try {
            List<VendaItemRequest> itens = parseItensJson(itensJson);
            Venda venda = vendaService.finalizarVenda(
                    tenantId,
                    usuario,
                    itens,
                    desconto,
                    Venda.MetodoPagamento.valueOf(metodoPagamento.toUpperCase()),
                    valorPago,
                    nomeClienteFiado,
                    telefoneClienteFiado);

            redirectAttrs.addFlashAttribute("sucesso", "Venda registrada! Total: " + venda.getTotal() + " MZN");
            return "redirect:/pdv/recibo/" + venda.getId();
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("erro", "Erro ao finalizar venda: " + e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", "Erro ao finalizar venda. Tente novamente.");
        }

        return "redirect:/pdv";
    }

    /**
     * CANCELAR VENDA - APENAS DONO/GERENTE
     * Vendedores NÃO podem cancelar.
     */
    @PostMapping("/pdv/cancelar/{id}")
    public String cancelarVenda(@PathVariable UUID id,
                                @RequestParam String motivo,
                                RedirectAttributes redirectAttrs) {

        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        try {
            vendaService.cancelarVenda(authService.getTenantIdLogado(), id, usuarioOpt.get(), motivo);
            redirectAttrs.addFlashAttribute("sucesso", "Venda cancelada. Stock restaurado.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }

        return "redirect:/pdv";
    }

    /**
     * RECIBO/COMPROVATIVO DE VENDA (imprimível)
     */
    @GetMapping("/pdv/recibo/{id}")
    public String recibo(@PathVariable UUID id, Model model, RedirectAttributes redirectAttrs) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        var vendaOpt = vendaRepository.findByIdAndTenantId(id, tenantId);
        if (vendaOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("erro", "Venda não encontrada.");
            return "redirect:/pdv/historico";
        }

        Venda venda = vendaOpt.get();
        // Vendedor só pode ver recibos das próprias vendas
        if (usuario.getRole() == Usuario.Role.VENDEDOR && !venda.getVendedorId().equals(usuario.getId())) {
            redirectAttrs.addFlashAttribute("erro", "Acesso negado a este recibo.");
            return "redirect:/pdv/historico";
        }

        var itens = vendaItemRepository.findByVendaId(venda.getId());
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        var vendedor = usuarioRepository.findById(venda.getVendedorId()).orElse(null);

        model.addAttribute("venda", venda);
        model.addAttribute("itens", itens);
        model.addAttribute("tenant", tenant);
        model.addAttribute("vendedor", vendedor);
        model.addAttribute("usuario", usuario);
        return "recibo";
    }

    /**
     * FORMULÁRIO DE DEVOLUÇÃO (parcial/total)
     */
    @GetMapping("/pdv/devolucao/{id}")
    public String devolucaoForm(@PathVariable("id") UUID vendaId, Model model, RedirectAttributes redirectAttrs) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        if (!usuario.podeCancelarVenda()) {
            redirectAttrs.addFlashAttribute("erro", "Apenas DONO/GERENTE podem registar devoluções.");
            return "redirect:/pdv/historico";
        }

        UUID tenantId = authService.getTenantIdLogado();
        var vendaOpt = vendaRepository.findByIdAndTenantId(vendaId, tenantId);
        if (vendaOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("erro", "Venda não encontrada.");
            return "redirect:/pdv/historico";
        }
        Venda venda = vendaOpt.get();
        if (venda.getStatus() != Venda.Status.CONCLUIDA) {
            redirectAttrs.addFlashAttribute("erro", "Só é possível devolver itens de vendas concluídas.");
            return "redirect:/pdv/historico";
        }

        var itens = vendaItemRepository.findByVendaId(vendaId);
        java.util.Map<UUID, Integer> jaDevolvido = new java.util.HashMap<>();
        java.util.Map<UUID, Integer> disponivel = new java.util.HashMap<>();
        for (var item : itens) {
            int jd = devolucaoService.quantidadeJaDevolvida(item.getId());
            jaDevolvido.put(item.getId(), jd);
            disponivel.put(item.getId(), Math.max(0, item.getQuantidade() - jd));
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("venda", venda);
        model.addAttribute("itens", itens);
        model.addAttribute("jaDevolvido", jaDevolvido);
        model.addAttribute("disponivel", disponivel);
        model.addAttribute("devolucoes", devolucaoService.listarPorVenda(tenantId, vendaId));
        return "devolucao";
    }

    /**
     * REGISTAR DEVOLUÇÃO
     */
    @PostMapping("/pdv/devolucao/{id}")
    public String registrarDevolucao(@PathVariable("id") UUID vendaId,
                                     @RequestParam String motivo,
                                     @RequestParam(required = false) String reporStock,
                                     @RequestParam java.util.Map<String, String> params,
                                     RedirectAttributes redirectAttrs) {
        boolean repor = reporStock != null; // checkbox presente => repor, ausente => não repor
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        try {
            java.util.Map<UUID, Integer> quantidades = new java.util.HashMap<>();
            for (var e : params.entrySet()) {
                if (e.getKey().startsWith("qtd_") && e.getValue() != null && !e.getValue().isBlank()) {
                    try {
                        UUID itemId = UUID.fromString(e.getKey().substring(4));
                        int q = Integer.parseInt(e.getValue());
                        if (q > 0) quantidades.put(itemId, q);
                    } catch (Exception ignored) {}
                }
            }
            var dev = devolucaoService.registrarDevolucao(tenantId, vendaId, usuario, quantidades, repor, motivo);
            redirectAttrs.addFlashAttribute("sucesso", "Devolução registada. Valor: " + dev.getValorTotal() + " MZN");
            return "redirect:/pdv/recibo/" + vendaId;
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage() != null ? e.getMessage() : "Erro ao registar devolução.");
            return "redirect:/pdv/devolucao/" + vendaId;
        }
    }

    /**
     * HISTÓRICO DE VENDAS
     * Dono e Gerente vêem tudo. Vendedor vê apenas as suas.
     * Listagem paginada (25 por página).
     */
    @GetMapping("/pdv/historico")
    public String historico(@RequestParam(defaultValue = "0") int page, Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        PageRequest pageable = PageRequest.of(Math.max(page, 0), 25,
                Sort.by(Sort.Direction.DESC, "dataHora"));

        Page<Venda> vendas = usuario.getRole() == Usuario.Role.VENDEDOR
                ? vendaRepository.findByTenantIdAndVendedorId(tenantId, usuario.getId(), pageable)
                : vendaRepository.findByTenantId(tenantId, pageable);

        model.addAttribute("usuario", usuario);
        model.addAttribute("vendas", vendas.getContent());
        model.addAttribute("pagina", vendas);
        model.addAttribute("isDono", usuario.getRole() == Usuario.Role.DONO);
        model.addAttribute("isGerente", usuario.getRole() == Usuario.Role.GERENTE);
        model.addAttribute("isVendedor", usuario.getRole() == Usuario.Role.VENDEDOR);

        return "pdv-historico";
    }

    private List<VendaItemRequest> parseItensJson(String itensJson) {
        if (itensJson == null || itensJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(itensJson, new TypeReference<List<VendaItemRequest>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Itens da venda inválidos");
        }
    }
}
