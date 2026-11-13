package com.nanobanana.controller;

import com.nanobanana.model.Usuario;
import com.nanobanana.model.Tenant;
import com.nanobanana.model.Pagamento;
import com.nanobanana.repository.UsuarioRepository;
import com.nanobanana.repository.TenantRepository;
import com.nanobanana.repository.PagamentoRepository;
import com.nanobanana.service.AuthService;
import com.nanobanana.util.TelefoneUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Pattern;

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

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Value("${app.subscription.price:350.00}")
    private BigDecimal subscriptionPrice;

    @Value("${app.subscription.mpesa-number:84 123 4567}")
    private String mpesaNumber;

    @Value("${app.subscription.emola-number:86 123 4567}")
    private String emolaNumber;

    @Value("${app.subscription.whatsapp-number:+258 84 123 4567}")
    private String whatsappNumber;

    private static final Pattern PAYMENT_REFERENCE_PATTERN = Pattern.compile("^[A-Za-z0-9 _.-]{4,100}$");

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
        boolean pedidoPendente = pagamentoRepository.existsByClienteIdAndConfirmadoFalse(tenant != null ? tenant.getId() : usuario.getTenantId());

        model.addAttribute("usuario", usuario);
        model.addAttribute("tenant", tenant);
        model.addAttribute("subscriptionPrice", subscriptionPrice);
        model.addAttribute("mpesaNumber", mpesaNumber);
        model.addAttribute("emolaNumber", emolaNumber);
        model.addAttribute("whatsappNumber", whatsappNumber);
        model.addAttribute("pedidoPendente", pedidoPendente);
        model.addAttribute("pagamentos", tenant != null ? pagamentoRepository.findTop3ByClienteIdOrderByDataDesc(tenant.getId()) : java.util.List.of());
        return "assinatura";
    }

    @PostMapping("/assinatura/solicitar")
    public String solicitarRenovacao(@RequestParam String metodo,
                                     @RequestParam String referencia,
                                     RedirectAttributes redirectAttrs) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        if (usuario.getRole() != Usuario.Role.DONO) {
            redirectAttrs.addFlashAttribute("erro", "APENAS o dono da loja pode solicitar a renovação da assinatura.");
            return "redirect:/assinatura";
        }

        UUID tenantId = usuario.getTenantId();
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            redirectAttrs.addFlashAttribute("erro", "Loja não encontrada.");
            return "redirect:/login";
        }

        String metodoLimpo = metodo != null ? metodo.trim().toLowerCase() : "";
        if (!java.util.Set.of("mpesa", "emola", "transferencia", "cash").contains(metodoLimpo)) {
            redirectAttrs.addFlashAttribute("erro", "Método de pagamento inválido.");
            return "redirect:/assinatura";
        }

        String refLimpa = referencia != null ? referencia.trim() : "";
        if (!PAYMENT_REFERENCE_PATTERN.matcher(refLimpa).matches()) {
            redirectAttrs.addFlashAttribute("erro", "Referência inválida. Use 4–100 caracteres (letras, números, espaço, _, . ou -).");
            return "redirect:/assinatura";
        }

        if (pagamentoRepository.existsByClienteIdAndConfirmadoFalse(tenantId)) {
            redirectAttrs.addFlashAttribute("erro", "Já existe um pedido de renovação pendente para esta loja.");
            return "redirect:/assinatura";
        }

        Pagamento pagamento = new Pagamento(tenantId, subscriptionPrice, metodoLimpo, refLimpa);
        pagamentoRepository.save(pagamento);
        redirectAttrs.addFlashAttribute("sucesso", "Pedido enviado! Assim que verificarmos o pagamento, a sua loja será ativada.");
        return "redirect:/assinatura";
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
    public String login(@RequestParam(name = "identificador", required = false) String identificador,
                        @RequestParam(name = "email", required = false) String email,
                        @RequestParam String senha,
                        HttpServletRequest request, HttpServletResponse response,
                        HttpSession session, Model model, RedirectAttributes redirectAttrs) {

        String loginId = identificador != null ? identificador.trim() : (email != null ? email.trim() : "");
        if (loginId.isEmpty()) {
            model.addAttribute("erro", "Informe email ou telefone e a senha.");
            return "login";
        }

        var usuarioOpt = authService.login(loginId, senha, session);

        if (usuarioOpt.isEmpty()) {
            model.addAttribute("erro", "Credenciais incorretas. Verifique o email/telefone e a senha.");
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
                    new UsernamePasswordAuthenticationToken(loginId, senha));
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
    @org.springframework.transaction.annotation.Transactional
    public String registrar(@RequestParam(name = "nomeCompleto", required = false) String nomeCompleto,
                           @RequestParam(name = "nome", required = false) String nomeLegacy,
                           @RequestParam(name = "nomeLoja", required = false) String nomeLoja,
                           @RequestParam(name = "telefone", required = false) String telefone,
                           @RequestParam(name = "email", required = false) String email,
                           @RequestParam(name = "nacionalidade", required = false) String nacionalidade,
                           @RequestParam(name = "provincia", required = false) String provincia,
                           @RequestParam String senha,
                           RedirectAttributes redirectAttrs) {

        String nomePessoa = nomeCompleto != null ? nomeCompleto.trim() : (nomeLegacy != null ? nomeLegacy.trim() : "");
        String nomeNegocio = nomeLoja != null ? nomeLoja.trim() : nomePessoa;
        String emailNormalizado = email != null ? email.trim().toLowerCase() : "";
        String telefoneNormalizado = TelefoneUtil.normalizar(telefone);
        String nacionalidadeNorm = nacionalidade != null ? nacionalidade.trim() : "";
        String provinciaNorm = provincia != null ? provincia.trim() : "";

        if (nomePessoa.isBlank()) {
            redirectAttrs.addFlashAttribute("erro", "Informe o seu nome completo.");
            return "redirect:/registrar";
        }
        if (nomeNegocio.isBlank()) {
            redirectAttrs.addFlashAttribute("erro", "Informe o nome da loja/negócio.");
            return "redirect:/registrar";
        }
        if (telefoneNormalizado.isBlank()) {
            redirectAttrs.addFlashAttribute("erro", "O número de telefone é obrigatório.");
            return "redirect:/registrar";
        }
        if (telefoneNormalizado.length() < 7 || telefoneNormalizado.length() > 15) {
            redirectAttrs.addFlashAttribute("erro", "Informe um número de telefone válido (com o indicativo do país, se necessário).");
            return "redirect:/registrar";
        }
        if (nacionalidadeNorm.isBlank()) {
            redirectAttrs.addFlashAttribute("erro", "Informe a nacionalidade/país.");
            return "redirect:/registrar";
        }
        if (provinciaNorm.isBlank()) {
            redirectAttrs.addFlashAttribute("erro", "Informe a província/estado/região.");
            return "redirect:/registrar";
        }
        if (senha == null || senha.length() < 8) {
            redirectAttrs.addFlashAttribute("erro", "Senha com pelo menos 8 caracteres.");
            return "redirect:/registrar";
        }

        String emailFinal = null;
        if (!emailNormalizado.isEmpty()) {
            if (!emailNormalizado.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                redirectAttrs.addFlashAttribute("erro", "Email inválido.");
                return "redirect:/registrar";
            }
            emailFinal = emailNormalizado;
            if (tenantRepository.existsByEmailIgnoreCase(emailFinal) || usuarioRepository.existsByEmailIgnoreCase(emailFinal)) {
                redirectAttrs.addFlashAttribute("erro", "Email já registrado.");
                return "redirect:/registrar";
            }
        }

        // Verifica telefone (identificador de acesso), aceitando as várias
        // formas de escrita: "84 555 0001", "+258 845550001", etc.
        var variantesTelefone = TelefoneUtil.variantes(telefoneNormalizado);
        if (usuarioRepository.existsByTelefoneIn(variantesTelefone)
                || tenantRepository.existsByTelefoneIn(variantesTelefone)) {
            redirectAttrs.addFlashAttribute("erro", "Este número de telefone já está registado. Use outro ou faça login.");
            return "redirect:/registrar";
        }

        try {
            // Cria Tenant (negócio)
            Tenant tenant = new Tenant();
            tenant.setNome(nomeNegocio);
            tenant.setEmail(emailFinal);
            tenant.setTelefone(telefoneNormalizado);
            tenant.setNacionalidade(nacionalidadeNorm);
            tenant.setProvincia(provinciaNorm);
            tenant.setDataInicio(LocalDate.now());
            tenant.setDataExpiracao(LocalDate.now().plusDays(15));
            tenant = tenantRepository.save(tenant);

            // Cria Usuário DONO (pessoa)
            Usuario dono = new Usuario();
            dono.setTenantId(tenant.getId());
            dono.setNome(nomePessoa);
            dono.setEmail(emailFinal);
            dono.setTelefone(telefoneNormalizado);
            dono.setSenha(passwordEncoder.encode(senha));
            dono.setRole(Usuario.Role.DONO);
            usuarioRepository.save(dono);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            redirectAttrs.addFlashAttribute("erro", "Não foi possível criar a conta. Email ou telefone já podem estar em uso.");
            return "redirect:/registrar";
        }

        redirectAttrs.addFlashAttribute("sucesso", "Conta criada! 15 dias grátis. Faça login com o seu telefone ou email.");
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
