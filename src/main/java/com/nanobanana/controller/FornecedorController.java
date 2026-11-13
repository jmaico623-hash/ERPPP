package com.nanobanana.controller;

import com.nanobanana.model.Fornecedor;
import com.nanobanana.model.Usuario;
import com.nanobanana.repository.FornecedorRepository;
import com.nanobanana.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/fornecedores")
public class FornecedorController {

    private final AuthService authService;
    private final FornecedorRepository fornecedorRepository;

    public FornecedorController(AuthService authService, FornecedorRepository fornecedorRepository) {
        this.authService = authService;
        this.fornecedorRepository = fornecedorRepository;
    }

    @GetMapping
    public String listar(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";
        Usuario usuario = usuarioOpt.get();
        if (usuario.getRole() == Usuario.Role.VENDEDOR) return "redirect:/pdv";

        UUID tenantId = authService.getTenantIdLogado();
        model.addAttribute("usuario", usuario);
        model.addAttribute("fornecedores", fornecedorRepository.findByTenantIdOrderByNomeAsc(tenantId));
        return "fornecedores";
    }

    @PostMapping
    public String criar(@RequestParam String nome,
                        @RequestParam(required = false) String telefone,
                        @RequestParam(required = false) String email,
                        @RequestParam(required = false) String nuitNif,
                        @RequestParam(required = false) String endereco,
                        RedirectAttributes redirectAttrs) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";
        Usuario usuario = usuarioOpt.get();
        if (!usuario.podeGerirStock()) {
            redirectAttrs.addFlashAttribute("erro", "Apenas o DONO pode gerir fornecedores.");
            return "redirect:/fornecedores";
        }
        if (nome == null || nome.trim().isEmpty()) {
            redirectAttrs.addFlashAttribute("erro", "Nome do fornecedor é obrigatório.");
            return "redirect:/fornecedores";
        }
        Fornecedor f = new Fornecedor();
        f.setTenantId(authService.getTenantIdLogado());
        f.setNome(nome.trim());
        f.setTelefone(blankToNull(telefone));
        f.setEmail(blankToNull(email));
        f.setNuitNif(blankToNull(nuitNif));
        f.setEndereco(blankToNull(endereco));
        f.setAtivo(true);
        fornecedorRepository.save(f);
        redirectAttrs.addFlashAttribute("sucesso", "Fornecedor registado.");
        return "redirect:/fornecedores";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable UUID id, RedirectAttributes redirectAttrs) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";
        Usuario usuario = usuarioOpt.get();
        if (!usuario.podeGerirStock()) {
            redirectAttrs.addFlashAttribute("erro", "Apenas o DONO pode gerir fornecedores.");
            return "redirect:/fornecedores";
        }
        var fOpt = fornecedorRepository.findByIdAndTenantId(id, authService.getTenantIdLogado());
        if (fOpt.isPresent()) {
            Fornecedor f = fOpt.get();
            f.setAtivo(!f.isAtivo());
            fornecedorRepository.save(f);
            redirectAttrs.addFlashAttribute("sucesso", "Estado do fornecedor atualizado.");
        }
        return "redirect:/fornecedores";
    }

    private static String blankToNull(String v) {
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }
}
