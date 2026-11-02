package com.nanobanana.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobanana.dto.VendaItemRequest;
import com.nanobanana.model.Produto;
import com.nanobanana.model.Usuario;
import com.nanobanana.model.Venda;
import com.nanobanana.repository.ProdutoRepository;
import com.nanobanana.repository.VendaRepository;
import com.nanobanana.service.AuthService;
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
    private final VendaService vendaService;
    private final ObjectMapper objectMapper;

    public PDVController(AuthService authService,
                         ProdutoRepository produtoRepository,
                         VendaRepository vendaRepository,
                         VendaService vendaService,
                         ObjectMapper objectMapper) {
        this.authService = authService;
        this.produtoRepository = produtoRepository;
        this.vendaRepository = vendaRepository;
        this.vendaService = vendaService;
        this.objectMapper = objectMapper;
    }

    /**
     * PDV - Tela de vendas
     */
    @GetMapping("/pdv")
    public String pdv(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

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
            List<VendaItemRequest> itens = parseItensJson(itensJson);
            Venda venda = vendaService.finalizarVenda(
                    tenantId,
                    usuario.getId(),
                    itens,
                    desconto,
                    Venda.MetodoPagamento.valueOf(metodoPagamento.toUpperCase()),
                    valorPago,
                    nomeClienteFiado,
                    telefoneClienteFiado);

            redirectAttrs.addFlashAttribute("sucesso", "Venda registrada! Total: " + venda.getTotal() + " MZN");
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
