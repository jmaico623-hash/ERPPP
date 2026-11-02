package com.nanobanana.controller;

import com.nanobanana.model.Usuario;
import com.nanobanana.model.Tenant;
import com.nanobanana.repository.UsuarioRepository;
import com.nanobanana.repository.TenantRepository;
import com.nanobanana.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/assinatura")
    public String assinaturaPage(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";
        
        Usuario usuario = usuarioOpt.get();
        Tenant tenant = tenantRepository.findById(usuario.getTenantId()).orElse(null);
        
        model.addAttribute("usuario", usuario);
        model.addAttribute("tenant", tenant);
        return "assinatura";
    }

    @GetMapping("/login")
    public String loginPage(Model model, HttpSession session) {
        // Verifica se já está logado acessando a sessão diretamente
        try {
            Object uid = session.getAttribute("usuarioId");
            if (uid != null) {
                return "redirect:/dashboard";
            }
        } catch (Exception e) {}
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String senha,
                        HttpServletRequest request, HttpServletResponse response,
                        HttpSession session, Model model, RedirectAttributes redirectAttrs) {
        
        var usuarioOpt = authService.login(email, senha, session);
        
        if (usuarioOpt.isEmpty()) {
            model.addAttribute("erro", "Email ou senha incorretos");
            return "login";
        }

        Usuario usuario = usuarioOpt.get();

        // Verifica se tenant está ativo
        Tenant tenant = tenantRepository.findById(usuario.getTenantId()).orElse(null);
        if (tenant == null || !tenant.isAtivo()) {
            model.addAttribute("erro", "Conta desativada. Contacte o suporte.");
            return "login";
        }

        // Verifica expiração
        if (tenant.expirado()) {
            return "redirect:/assinatura";
        }

        // Autentica no Spring Security para proteger rotas
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, senha));
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Salva o contexto de segurança na sessão HTTP para persistir entre requisições
            new HttpSessionSecurityContextRepository().saveContext(
                    SecurityContextHolder.getContext(), request, response);
        } catch (Exception e) {
            // Se falhar aqui (não deveria), continua com sessão manual
        }

        // Redireciona conforme role
        if (usuario.getRole() == Usuario.Role.DONO) {
            return "redirect:/dashboard";
        } else if (usuario.getRole() == Usuario.Role.GERENTE) {
            return "redirect:/dashboard";
        } else {
            // VENDEDOR vai direto para PDV
            return "redirect:/pdv";
        }
    }

    @GetMapping("/registrar")
    public String registrarPage(Model model) {
        return "registrar";
    }

    @PostMapping("/registrar")
    public String registrar(@RequestParam String nome, @RequestParam String email,
                           @RequestParam String senha, @RequestParam String telefone,
                           @RequestParam String nui, RedirectAttributes redirectAttrs) {
        
        // Verifica se email já existe
        if (usuarioRepository.findByEmail(email).isPresent()) {
            redirectAttrs.addFlashAttribute("erro", "Email já registrado");
            return "redirect:/registrar";
        }

        // Cria Tenant
        Tenant tenant = new Tenant();
        tenant.setNome(nome);
        tenant.setEmail(email);
        tenant.setTelefone(telefone);
        tenant.setNui(nui);
        tenant.setDataInicio(LocalDate.now());
        tenant.setDataExpiracao(LocalDate.now().plusDays(15)); // 15 dias teste
        tenant = tenantRepository.save(tenant);

        // Cria Usuário DONO
        Usuario dono = new Usuario();
        dono.setTenantId(tenant.getId());
        dono.setNome(nome);
        dono.setEmail(email);
        dono.setSenha(passwordEncoder.encode(senha)); // Hash com BCrypt
        dono.setRole(Usuario.Role.DONO);
        usuarioRepository.save(dono);

        redirectAttrs.addFlashAttribute("sucesso", "Conta criada! 15 dias grátis. Faça login.");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        authService.logout();
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout=true";
    }
}
