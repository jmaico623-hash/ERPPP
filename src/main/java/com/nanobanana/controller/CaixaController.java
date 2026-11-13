package com.nanobanana.controller;

import com.nanobanana.model.Caixa;
import com.nanobanana.model.MovimentoCaixa;
import com.nanobanana.model.Usuario;
import com.nanobanana.service.AuthService;
import com.nanobanana.service.CaixaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/caixa")
public class CaixaController {

    private final AuthService authService;
    private final CaixaService caixaService;

    public CaixaController(AuthService authService, CaixaService caixaService) {
        this.authService = authService;
        this.caixaService = caixaService;
    }

    @GetMapping
    public String caixa(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        var caixaAberto = caixaService.getCaixaAberto(tenantId);
        model.addAttribute("usuario", usuario);
        model.addAttribute("caixaAberto", caixaAberto.orElse(null));
        model.addAttribute("historico", caixaService.listarRecentes(tenantId));
        if (caixaAberto.isPresent()) {
            Caixa c = caixaAberto.get();
            model.addAttribute("movimentos", caixaService.listarMovimentos(c.getId()));
            model.addAttribute("resumo", caixaService.calcularResumo(c, LocalDateTime.now()));
        } else {
            model.addAttribute("movimentos", java.util.List.of());
            model.addAttribute("resumo", null);
        }
        return "caixa";
    }

    @PostMapping("/abrir")
    public String abrir(@RequestParam BigDecimal valorAbertura,
                        @RequestParam(required = false) String observacao,
                        RedirectAttributes redirectAttrs) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        // Dono e Gerente abrem caixa; vendedor também pode operar se permitido - permitir todos autenticados do tenant
        try {
            caixaService.abrirCaixa(authService.getTenantIdLogado(), usuario, valorAbertura, observacao);
            redirectAttrs.addFlashAttribute("sucesso", "Caixa aberto com sucesso.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/caixa";
    }

    @PostMapping("/movimento")
    public String movimento(@RequestParam String tipo,
                            @RequestParam BigDecimal valor,
                            @RequestParam(required = false) String motivo,
                            RedirectAttributes redirectAttrs) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        try {
            MovimentoCaixa.Tipo t = MovimentoCaixa.Tipo.valueOf(tipo.toUpperCase());
            caixaService.registrarMovimento(authService.getTenantIdLogado(), usuario, t, valor, motivo);
            redirectAttrs.addFlashAttribute("sucesso", (t == MovimentoCaixa.Tipo.SANGRIA ? "Sangria" : "Reforço") + " registado.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage() != null ? e.getMessage() : "Erro ao registar movimento.");
        }
        return "redirect:/caixa";
    }

    @PostMapping("/fechar")
    public String fechar(@RequestParam BigDecimal valorInformado,
                         @RequestParam(required = false) String observacao,
                         RedirectAttributes redirectAttrs) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        try {
            Caixa fechado = caixaService.fecharCaixa(authService.getTenantIdLogado(), usuario, valorInformado, observacao);
            redirectAttrs.addFlashAttribute("sucesso", "Caixa fechado. Esperado: " + fechado.getValorEsperado() + " | Informado: " + fechado.getValorFechoInformado() + " | Diferença: " + fechado.getDiferenca());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/caixa";
    }
}
