package com.nanobanana.controller;

import com.nanobanana.model.*;
import com.nanobanana.repository.*;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminRepository adminRepo;

    @Autowired
    private TenantRepository tenantRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private PagamentoRepository pagamentoRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VendaRepository vendaRepo;

    @Autowired
    private ProdutoRepository produtoRepo;

    @Autowired
    private FiadoRepository fiadoRepo;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepo;

    @Autowired
    private CupomRepository cupomRepository;

    @Autowired
    private com.nanobanana.service.AdminService adminService;

    @Value("${app.subscription.price:350.00}")
    private BigDecimal subscriptionPrice;

    private static final Pattern CUPOM_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,50}$");

    // --- Login Admin ---

    @GetMapping("/login")
    public String loginAdmin() {
        return "admin-login";
    }

    @PostMapping("/login")
    public String loginAdminPost(
            @RequestParam String email,
            @RequestParam String senha,
            HttpSession session) {

        Admin admin = adminRepo.findByEmail(email).orElse(null);
        if (admin == null || !passwordEncoder.matches(senha, admin.getSenha())) {
            return "redirect:/admin/login?erro=credenciais";
        }

        session.setAttribute("adminId", admin.getId());
        session.setAttribute("adminNome", admin.getNome());
        session.setAttribute("admin", true);

        return "redirect:/admin";
    }

    @GetMapping("/logout")
    public String logoutAdmin(HttpSession session) {
        session.removeAttribute("adminId");
        session.removeAttribute("adminNome");
        session.removeAttribute("admin");
        return "redirect:/admin/login";
    }

    // --- Verificar se é admin ---
    private boolean isAdmin(HttpSession session) {
        Boolean admin = (Boolean) session.getAttribute("admin");
        return admin != null && admin;
    }

    // --- Dashboard Admin ---

    @GetMapping("")
    public String admin(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<Tenant> tenants = tenantRepo.findAll();
        List<Pagamento> todosPagamentos = pagamentoRepo.findAll();
        List<Pagamento> pendentes = todosPagamentos.stream()
                .filter(p -> !p.isConfirmado())
                .collect(Collectors.toList());

        // Estatísticas
        long totalClientes = tenants.size();
        long ativas = tenants.stream().filter(t -> t.isAtivo() && !t.expirado()).count();
        long expiradas = tenants.stream().filter(Tenant::expirado).count();
        long testes = tenants.stream().filter(t -> "teste".equals(t.getPlano())).count();
        long pagos = tenants.stream().filter(t -> "pago".equals(t.getPlano()) || "basico".equals(t.getPlano()) || "premium".equals(t.getPlano())).count();

        double receitaTotal = todosPagamentos.stream()
                .filter(Pagamento::isConfirmado)
                .mapToDouble(p -> p.getValor().doubleValue())
                .sum();

        // Receita por período
        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemana = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate inicioMes = hoje.withDayOfMonth(1);

        double receitaSemana = todosPagamentos.stream()
                .filter(p -> p.isConfirmado() && !p.getData().isBefore(inicioSemana))
                .mapToDouble(p -> p.getValor().doubleValue())
                .sum();

        double receitaMes = todosPagamentos.stream()
                .filter(p -> p.isConfirmado() && !p.getData().isBefore(inicioMes))
                .mapToDouble(p -> p.getValor().doubleValue())
                .sum();

        // Vendas globais hoje
        LocalDateTime inicioDia = hoje.atStartOfDay();
        LocalDateTime fimDia = hoje.plusDays(1).atStartOfDay();
        BigDecimal totalVendasHoje = vendaRepo.somaVendasGlobalPeriodo(inicioDia, fimDia);
        double vendasHoje = totalVendasHoje != null ? totalVendasHoje.doubleValue() : 0.0;

        // Total produtos em stock
        long totalProdutos = produtoRepo.count();

        model.addAttribute("clientes", tenants); // c mapped as Tenant in template
        model.addAttribute("pendentes", pendentes);
        model.addAttribute("receitaTotal", receitaTotal);
        model.addAttribute("receitaSemana", receitaSemana);
        model.addAttribute("receitaMes", receitaMes);
        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("ativas", ativas);
        model.addAttribute("expiradas", expiradas);
        model.addAttribute("testes", testes);
        model.addAttribute("pagos", pagos);
        model.addAttribute("vendasHoje", vendasHoje);
        model.addAttribute("totalProdutos", totalProdutos);

        // Mapa tenantId -> Tenant
        Map<UUID, Tenant> clienteAssinaturaMap = new HashMap<>();
        for (Tenant t : tenants) {
            clienteAssinaturaMap.put(t.getId(), t);
        }
        model.addAttribute("clienteAssinaturaMap", clienteAssinaturaMap);

        return "admin";
    }

    // --- Confirmar pagamento ---

    @PostMapping("/confirmar-pagamento/{id}")
    public String confirmarPagamento(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            adminService.confirmarPagamento(id);
            redirectAttrs.addFlashAttribute("sucesso", "Pagamento confirmado com sucesso.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage() != null ? e.getMessage() : "Pagamento inválido ou removido.");
        }
        return "redirect:/admin";
    }

    // --- Ativar/Desativar cliente ---

    @PostMapping("/cliente/{id}/ativar")
    public String ativarCliente(@PathVariable UUID id, RedirectAttributes redirectAttrs) {
        try {
            adminService.ativarCliente(id);
            redirectAttrs.addFlashAttribute("sucesso", "Cliente ativado com sucesso.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage() != null ? e.getMessage() : "Cliente inválido ou inexistente.");
        }
        return "redirect:/admin";
    }

    @PostMapping("/cliente/{id}/desativar")
    public String desativarCliente(@PathVariable UUID id, RedirectAttributes redirectAttrs) {
        try {
            adminService.desativarCliente(id);
            redirectAttrs.addFlashAttribute("sucesso", "Cliente desativado.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage() != null ? e.getMessage() : "Cliente inválido ou inexistente.");
        }
        return "redirect:/admin";
    }

    // --- Estender teste ---

    @PostMapping("/cliente/{id}/estender-teste")
    public String estenderTeste(@PathVariable UUID id, RedirectAttributes redirectAttrs) {
        try {
            adminService.estenderTeste(id, 15);
            redirectAttrs.addFlashAttribute("sucesso", "Teste estendido por 15 dias.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage() != null ? e.getMessage() : "Cliente inválido ou inexistente.");
        }
        return "redirect:/admin";
    }

    // --- Ver detalhes do cliente ---

    @GetMapping("/cliente/{id}")
    public String verCliente(@PathVariable UUID id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        Tenant tenant = tenantRepo.findById(id).orElse(null);
        if (tenant == null) {
            return "redirect:/admin";
        }

        List<Venda> vendas = vendaRepo.findByTenantId(id);
        List<Produto> produtos = produtoRepo.findByTenantId(id);
        List<Fiado> fiados = fiadoRepo.findByTenantId(id);

        double totalVendas = vendas.stream()
                .filter(v -> v.getStatus() == Venda.Status.CONCLUIDA)
                .mapToDouble(v -> v.getTotal().doubleValue())
                .sum();
        double totalFiado = fiados.stream().filter(f -> !f.isPago() && !f.isCancelado()).mapToDouble(f -> f.getValor().doubleValue()).sum();
        double valorStock = produtos.stream().mapToDouble(p -> p.getValorStock().doubleValue()).sum();

        model.addAttribute("cliente", tenant);
        model.addAttribute("assinatura", tenant); // Tenant acts as its own subscription in templates
        model.addAttribute("vendas", vendas);
        model.addAttribute("produtos", produtos);
        model.addAttribute("fiados", fiados);
        model.addAttribute("totalVendas", totalVendas);
        model.addAttribute("totalFiado", totalFiado);
        model.addAttribute("valorStock", valorStock);

        return "admin-cliente";
    }

    // ========================================
    // 🔥 URGENTE #1: Vendas Globais
    // ========================================

    @GetMapping("/vendas")
    public String vendasGlobais(
            HttpSession session,
            Model model,
            @RequestParam(required = false) String periodo) {

        if (!isAdmin(session)) return "redirect:/admin/login";

        List<Tenant> tenants = tenantRepo.findAll();
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicio = hoje.atStartOfDay();
        LocalDateTime fim = hoje.plusDays(1).atStartOfDay().minusNanos(1);
        String periodoLabel = "Hoje";

        if ("semana".equals(periodo)) {
            inicio = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            fim = hoje.plusDays(1).atStartOfDay().minusNanos(1);
            periodoLabel = "Esta Semana";
        } else if ("mes".equals(periodo)) {
            inicio = hoje.withDayOfMonth(1).atStartOfDay();
            fim = hoje.plusDays(1).atStartOfDay().minusNanos(1);
            periodoLabel = "Este Mês";
        } else if ("todas".equals(periodo)) {
            inicio = LocalDateTime.of(2020, 1, 1, 0, 0);
            fim = LocalDateTime.of(2099, 12, 31, 23, 59);
            periodoLabel = "Todas as Vendas";
        }

        List<Usuario> owners = usuarioRepo.findByRole(Usuario.Role.DONO);
        Map<UUID, String> ownerNames = owners.stream()
                .collect(Collectors.toMap(Usuario::getTenantId, Usuario::getNome, (a, b) -> a));

        List<Venda> vendasNoPeriodo = vendaRepo.findAllByDataHoraBetween(inicio, fim);

        Map<UUID, Tenant> tenantMap = tenants.stream().collect(Collectors.toMap(Tenant::getId, t -> t));

        List<Map<String, Object>> todasVendas = new ArrayList<>();
        double totalGeral = 0;

        for (Venda v : vendasNoPeriodo) {
            Map<String, Object> dados = new HashMap<>();
            Tenant t = tenantMap.get(v.getTenantId());
            if (t == null) {
                continue;
            }

            dados.put("venda", v);
            dados.put("clienteNome", ownerNames.getOrDefault(t.getId(), "Dono Desconhecido"));
            dados.put("clienteNegocio", t.getNome());
            todasVendas.add(dados);
            if (v.getStatus() == Venda.Status.CONCLUIDA) {
                totalGeral += v.getTotal().doubleValue();
            }
        }

        todasVendas.sort((a, b) -> {
            Venda va = (Venda) a.get("venda");
            Venda vb = (Venda) b.get("venda");
            return vb.getDataHora().compareTo(va.getDataHora());
        });

        model.addAttribute("todasVendas", todasVendas);
        model.addAttribute("totalGeral", totalGeral);
        model.addAttribute("periodoLabel", periodoLabel);
        model.addAttribute("periodo", periodo != null ? periodo : "hoje");
        return "admin-vendas";
    }

    // ========================================
    // 🔥 URGENTE #2: Ranking de Lojas
    // ========================================

    @GetMapping("/ranking")
    public String rankingLojas(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        List<Tenant> tenants = tenantRepo.findAll();
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioMes = hoje.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = hoje.plusDays(1).atStartOfDay().minusNanos(1);

        Map<UUID, Double> vendasMesPorTenant = new HashMap<>();
        for (Object[] row : vendaRepo.somaConcluidasPorTenantPeriodo(inicioMes, fimMes)) {
            vendasMesPorTenant.put((UUID) row[0], ((BigDecimal) row[1]).doubleValue());
        }
        Map<UUID, Long> qtdVendasPorTenant = new HashMap<>();
        for (Object[] row : vendaRepo.countVendasPorTenant()) {
            qtdVendasPorTenant.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        Map<UUID, Double> totalFiadoPorTenant = new HashMap<>();
        for (Object[] row : fiadoRepo.totalNaoPagoPorTenant()) {
            totalFiadoPorTenant.put((UUID) row[0], ((BigDecimal) row[1]).doubleValue());
        }

        List<Map<String, Object>> ranking = new ArrayList<>();

        for (Tenant t : tenants) {
            double vendasMes = vendasMesPorTenant.getOrDefault(t.getId(), 0.0);
            double totalFiado = totalFiadoPorTenant.getOrDefault(t.getId(), 0.0);
            long qtdVendas = qtdVendasPorTenant.getOrDefault(t.getId(), 0L);

            Map<String, Object> dados = new HashMap<>();
            dados.put("cliente", t);
            dados.put("vendasMes", vendasMes);
            dados.put("qtdVendas", qtdVendas);
            dados.put("totalFiado", totalFiado);
            ranking.add(dados);
        }

        ranking.sort((a, b) -> Double.compare((double) b.get("vendasMes"), (double) a.get("vendasMes")));

        model.addAttribute("ranking", ranking);
        return "admin-ranking";
    }

    // ========================================
    // 🔥 URGENTE #6: Apagar Cliente
    // ========================================

    @PostMapping("/cliente/{id}/apagar")
    public String apagarCliente(@PathVariable UUID id, RedirectAttributes redirectAttrs) {
        try {
            adminService.apagarCliente(id);
            redirectAttrs.addFlashAttribute("sucesso", "Cliente anonimizado e desativado com sucesso.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage() != null ? e.getMessage() : "Cliente inválido ou inexistente.");
        }
        return "redirect:/admin";
    }

    // ========================================
    // 🔥 URGENTE #7: Reset Senha
    // ========================================

    @PostMapping("/cliente/{id}/reset-senha")
    public String resetSenha(@PathVariable UUID id, @RequestParam String novaSenha, RedirectAttributes redirectAttrs) {
        try {
            adminService.resetSenhaDono(id, novaSenha);
            redirectAttrs.addFlashAttribute("sucesso", "Senha do DONO redefinida com sucesso.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage() != null ? e.getMessage() : "Não foi possível redefinir a senha.");
        }
        return "redirect:/admin";
    }

    // ========================================
    // 📅 #4: Configurar Preço da Assinatura
    // ========================================

    @GetMapping("/config")
    public String config(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        // Preço configurado no servidor via APP_SUBSCRIPTION_PRICE; a alteração exige deploy/reinício.
        model.addAttribute("precoAtual", subscriptionPrice);
        return "admin-config";
    }

    // ========================================
    // 📅 #5: Relatório Financeiro
    // ========================================

    @GetMapping("/financeiro")
    public String relatorioFinanceiro(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        List<Pagamento> pagamentos = pagamentoRepo.findAll().stream()
                .filter(Pagamento::isConfirmado).collect(Collectors.toList());

        // Receita por mês (últimos 12 meses)
        Map<String, Double> receitaMensal = new LinkedHashMap<>();
        LocalDate hoje = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM/yyyy");

        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.from(hoje).minusMonths(i);
            String chave = ym.format(fmt);
            double total = pagamentos.stream()
                    .filter(p -> YearMonth.from(p.getData()).equals(ym))
                    .mapToDouble(p -> p.getValor().doubleValue()).sum();
            receitaMensal.put(chave, total);
        }

        double receitaTotal = receitaMensal.values().stream().mapToDouble(Double::doubleValue).sum();
        double mediaMensal = receitaTotal / 12;

        model.addAttribute("receitaMensal", receitaMensal);
        model.addAttribute("receitaTotal", receitaTotal);
        model.addAttribute("mediaMensal", mediaMensal);
        model.addAttribute("pagamentos", pagamentos);
        return "admin-financeiro";
    }

    // ========================================
    // 📅 #9: Histórico de Pagamentos
    // ========================================

    @GetMapping("/pagamentos")
    public String historicoPagamentos(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        List<Pagamento> pagamentos = pagamentoRepo.findAll();
        List<Tenant> tenants = tenantRepo.findAll();

        Map<UUID, String> clienteNomeMap = new HashMap<>();
        for (Tenant t : tenants) {
            clienteNomeMap.put(t.getId(), t.getNome());
        }

        model.addAttribute("pagamentos", pagamentos);
        model.addAttribute("clienteNomeMap", clienteNomeMap);
        return "admin-pagamentos";
    }

    // ========================================
    // 🌟 #8: Log de Atividades
    // ========================================

    @GetMapping("/logs")
    public String logsAdmin(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        List<AuditoriaLog> logs = auditoriaLogRepo
                .findAll(Sort.by(Sort.Direction.DESC, "dataHora"))
                .stream()
                .limit(200)
                .toList();

        Map<UUID, String> clienteNomeMap = new HashMap<>();
        for (Tenant t : tenantRepo.findAll()) {
            clienteNomeMap.put(t.getId(), t.getNome());
        }

        model.addAttribute("logs", logs);
        model.addAttribute("clienteNomeMap", clienteNomeMap);
        return "admin-logs";
    }

    // ========================================
    // 🌟 #10: Cupão de Desconto
    // ========================================

    @GetMapping("/cupoes")
    public String cupoes(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("clientes", tenantRepo.findAll());
        model.addAttribute("cupoms", cupomRepository.findAllByOrderByCriadoEmDesc());
        return "admin-cupoes";
    }

    @PostMapping("/cupao/criar")
    public String criarCupao(
            @RequestParam String codigo,
            @RequestParam String descontoPercent,
            @RequestParam(required = false) String clienteId,
            HttpSession session,
            RedirectAttributes redirectAttrs) {
        if (!isAdmin(session)) {
            redirectAttrs.addFlashAttribute("erro", "Sessão de administrador inválida.");
            return "redirect:/admin/login";
        }

        String codigoLimpo = codigo != null ? codigo.trim().toUpperCase() : "";
        if (!CUPOM_CODE_PATTERN.matcher(codigoLimpo).matches()) {
            redirectAttrs.addFlashAttribute("erro", "Código inválido. Use 3–50 caracteres: letras, números, '-' ou '_'.");
            return "redirect:/admin/cupoes";
        }

        BigDecimal desconto;
        try {
            desconto = new BigDecimal(descontoPercent);
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", "Desconto inválido.");
            return "redirect:/admin/cupoes";
        }
        if (desconto.compareTo(BigDecimal.ZERO) <= 0 || desconto.compareTo(new BigDecimal("100")) > 0) {
            redirectAttrs.addFlashAttribute("erro", "O desconto deve ser maior que 0 e no máximo 100%.");
            return "redirect:/admin/cupoes";
        }

        UUID tenantId = null;
        String clienteIdTexto = clienteId != null ? clienteId.trim() : "";
        if (!clienteIdTexto.isEmpty()) {
            try {
                tenantId = UUID.fromString(clienteIdTexto);
            } catch (IllegalArgumentException e) {
                redirectAttrs.addFlashAttribute("erro", "Cliente inválido.");
                return "redirect:/admin/cupoes";
            }
            if (!tenantRepo.existsById(tenantId)) {
                redirectAttrs.addFlashAttribute("erro", "Cliente não encontrado.");
                return "redirect:/admin/cupoes";
            }
        }

        if (cupomRepository.findByCodigoIgnoreCase(codigoLimpo).isPresent()) {
            redirectAttrs.addFlashAttribute("erro", "Já existe um cupão com o código " + codigoLimpo + ".");
            return "redirect:/admin/cupoes";
        }

        Cupom cupom = new Cupom();
        cupom.setCodigo(codigoLimpo);
        cupom.setDescontoPercent(desconto);
        cupom.setTenantId(tenantId);
        cupomRepository.save(cupom);
        redirectAttrs.addFlashAttribute("sucesso", "Cupão " + cupom.getCodigo() + " criado.");

        return "redirect:/admin/cupoes";
    }
}
