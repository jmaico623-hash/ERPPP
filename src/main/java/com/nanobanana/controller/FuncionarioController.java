package com.nanobanana.controller;

import com.nanobanana.model.*;
import com.nanobanana.repository.*;
import com.nanobanana.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * GESTÃO DE FUNCIONÁRIOS - APENAS DONO
 * O dono cria, edita, ativa/desativa funcionários
 */
@Controller
@RequestMapping("/funcionarios")
public class FuncionarioController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Lista todos os funcionários - APENAS DONO
     */
    @GetMapping
    public String listarFuncionarios(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();

        // APENAS DONO pode gerir funcionários
        if (!usuario.podeGerirUsuarios()) {
            return "redirect:/pdv";
        }

        UUID tenantId = authService.getTenantIdLogado();

        List<Usuario> funcionarios = usuarioRepository.findByTenantId(tenantId);
        
        // Conta vendas de cada funcionário hoje
        LocalDateTime inicioHoje = LocalDate.now().atStartOfDay();
        LocalDateTime agora = LocalDateTime.now();

        model.addAttribute("usuario", usuario);
        model.addAttribute("funcionarios", funcionarios);
        
        return "funcionarios";
    }

    /**
     * CRIAR FUNCIONÁRIO - APENAS DONO
     */
    @PostMapping("/criar")
    public String criarFuncionario(@RequestParam String nome,
                                   @RequestParam String email,
                                   @RequestParam String senha,
                                   @RequestParam(required = false) String telefone,
                                   @RequestParam String role,
                                   RedirectAttributes redirectAttrs) {
        
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario dono = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        // APENAS DONO pode criar funcionários
        if (!dono.podeGerirUsuarios()) {
            redirectAttrs.addFlashAttribute("erro", "APENAS o dono pode criar funcionários.");
            return "redirect:/funcionarios";
        }

        // Verifica se email já existe neste tenant
        if (usuarioRepository.existsByTenantIdAndEmail(tenantId, email)) {
            redirectAttrs.addFlashAttribute("erro", "Email já registrado nesta loja.");
            return "redirect:/funcionarios";
        }

        // Cria funcionário
        Usuario.Role roleEnum;
        try {
            roleEnum = Usuario.Role.valueOf(role.toUpperCase());
            
            // Vendedor não pode criar outro funcionário como DONO
            if (roleEnum == Usuario.Role.DONO) {
                redirectAttrs.addFlashAttribute("erro", "Não pode criar outro DONO.");
                return "redirect:/funcionarios";
            }
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("erro", "Função inválida.");
            return "redirect:/funcionarios";
        }

        Usuario funcionario = authService.criarUsuario(
                tenantId, nome, email, senha, roleEnum, dono.getId());

        // Log de auditoria
        AuditoriaLog log = new AuditoriaLog();
        log.setTenantId(tenantId);
        log.setUsuarioId(dono.getId());
        log.setAcao(AuditoriaLog.Acao.CRIAR_USUARIO);
        log.setTabelaAfetada("usuarios");
        log.setRegistroId(funcionario.getId());
        log.setDadosNovos("Nome: " + nome + ", Role: " + role + ", Email: " + email);
        auditoriaLogRepository.save(log);

        redirectAttrs.addFlashAttribute("sucesso", "Funcionário criado com sucesso!");
        return "redirect:/funcionarios";
    }

    /**
     * DESATIVAR FUNCIONÁRIO - APENAS DONO
     */
    @PostMapping("/{id}/desativar")
    public String desativarFuncionario(@PathVariable UUID id,
                                       RedirectAttributes redirectAttrs) {
        
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario dono = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        if (!dono.podeGerirUsuarios()) {
            redirectAttrs.addFlashAttribute("erro", "APENAS o dono pode gerir funcionários.");
            return "redirect:/funcionarios";
        }

        Optional<Usuario> funcOpt = usuarioRepository.findById(id);
        if (funcOpt.isEmpty() || !funcOpt.get().getTenantId().equals(tenantId)) {
            redirectAttrs.addFlashAttribute("erro", "Funcionário não encontrado.");
            return "redirect:/funcionarios";
        }

        Usuario funcionario = funcOpt.get();
        funcionario.setAtivo(false);
        usuarioRepository.save(funcionario);

        // Log
        AuditoriaLog log = new AuditoriaLog();
        log.setTenantId(tenantId);
        log.setUsuarioId(dono.getId());
        log.setAcao(AuditoriaLog.Acao.DESATIVAR_USUARIO);
        log.setTabelaAfetada("usuarios");
        log.setRegistroId(funcionario.getId());
        log.setDadosNovos("Funcionário desativado: " + funcionario.getNome());
        auditoriaLogRepository.save(log);

        redirectAttrs.addFlashAttribute("sucesso", "Funcionário desativado.");
        return "redirect:/funcionarios";
    }

    /**
     * REATIVAR FUNCIONÁRIO - APENAS DONO
     */
    @PostMapping("/{id}/reativar")
    public String reativarFuncionario(@PathVariable UUID id,
                                       RedirectAttributes redirectAttrs) {
        
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario dono = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        if (!dono.podeGerirUsuarios()) {
            redirectAttrs.addFlashAttribute("erro", "APENAS o dono pode gerir funcionários.");
            return "redirect:/funcionarios";
        }

        Optional<Usuario> funcOpt = usuarioRepository.findById(id);
        if (funcOpt.isEmpty() || !funcOpt.get().getTenantId().equals(tenantId)) {
            redirectAttrs.addFlashAttribute("erro", "Funcionário não encontrado.");
            return "redirect:/funcionarios";
        }

        Usuario funcionario = funcOpt.get();
        funcionario.setAtivo(true);
        funcionario.setBloqueado(false);
        funcionario.setTentativasFalhas(0);
        usuarioRepository.save(funcionario);

        redirectAttrs.addFlashAttribute("sucesso", "Funcionário reativado.");
        return "redirect:/funcionarios";
    }

    /**
     * RESET SENHA FUNCIONÁRIO - APENAS DONO
     */
    @PostMapping("/{id}/reset-senha")
    public String resetSenha(@PathVariable UUID id,
                             @RequestParam String novaSenha,
                             RedirectAttributes redirectAttrs) {
        
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario dono = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        if (!dono.podeGerirUsuarios()) {
            redirectAttrs.addFlashAttribute("erro", "APENAS o dono pode resetar senhas.");
            return "redirect:/funcionarios";
        }

        Optional<Usuario> funcOpt = usuarioRepository.findById(id);
        if (funcOpt.isEmpty() || !funcOpt.get().getTenantId().equals(tenantId)) {
            redirectAttrs.addFlashAttribute("erro", "Funcionário não encontrado.");
            return "redirect:/funcionarios";
        }

        Usuario funcionario = funcOpt.get();
        funcionario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(funcionario);

        redirectAttrs.addFlashAttribute("sucesso", "Senha resetada com sucesso.");
        return "redirect:/funcionarios";
    }
}
