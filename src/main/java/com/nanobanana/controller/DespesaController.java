package com.nanobanana.controller;

import com.nanobanana.model.Despesa;
import com.nanobanana.model.Usuario;
import com.nanobanana.repository.DespesaRepository;
import com.nanobanana.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/despesas")
public class DespesaController {

    private final AuthService authService;
    private final DespesaRepository despesaRepository;

    public DespesaController(AuthService authService, DespesaRepository despesaRepository) {
        this.authService = authService;
        this.despesaRepository = despesaRepository;
    }

    @GetMapping
    public String listar(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        if (!usuario.podeVerRelatorios()) {
            return "redirect:/pdv";
        }

        UUID tenantId = authService.getTenantIdLogado();
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);

        model.addAttribute("usuario", usuario);
        model.addAttribute("despesas", despesaRepository.findTop50ByTenantIdOrderByDataDespesaDescCriadoEmDesc(tenantId));
        model.addAttribute("totalMes", despesaRepository.somaPeriodo(tenantId, inicioMes, hoje));
        model.addAttribute("categorias", Despesa.Categoria.values());
        model.addAttribute("hoje", hoje);
        return "despesas";
    }

    @PostMapping
    public String criar(@RequestParam String descricao,
                        @RequestParam String categoria,
                        @RequestParam BigDecimal valor,
                        @RequestParam(required = false) String dataDespesa,
                        @RequestParam(required = false) String metodoPagamento,
                        @RequestParam(required = false) String observacao,
                        RedirectAttributes redirectAttrs) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        if (!usuario.podeVerRelatorios()) {
            redirectAttrs.addFlashAttribute("erro", "Sem permissão para registar despesas.");
            return "redirect:/pdv";
        }

        try {
            if (descricao == null || descricao.trim().isEmpty()) {
                throw new IllegalArgumentException("Descrição é obrigatória.");
            }
            if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Valor deve ser maior que zero.");
            }
            Despesa d = new Despesa();
            d.setTenantId(authService.getTenantIdLogado());
            d.setUsuarioId(usuario.getId());
            d.setDescricao(descricao.trim());
            d.setCategoria(Despesa.Categoria.valueOf(categoria));
            d.setValor(valor);
            d.setDataDespesa(dataDespesa != null && !dataDespesa.isBlank() ? LocalDate.parse(dataDespesa) : LocalDate.now());
            d.setMetodoPagamento(metodoPagamento != null && !metodoPagamento.isBlank() ? metodoPagamento.trim() : null);
            d.setObservacao(observacao != null && !observacao.trim().isEmpty() ? observacao.trim() : null);
            despesaRepository.save(d);
            redirectAttrs.addFlashAttribute("sucesso", "Despesa registada.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage() != null ? e.getMessage() : "Erro ao registar despesa.");
        }
        return "redirect:/despesas";
    }
}
