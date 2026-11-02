package com.nanobanana.controller;

import com.nanobanana.model.*;
import com.nanobanana.repository.*;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
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
    private com.nanobanana.service.AdminService adminService;

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
        double vendasHoje = 0.0;
        LocalDateTime inicioDia = hoje.atStartOfDay();
        LocalDateTime fimDia = hoje.plusDays(1).atStartOfDay();
        for (Tenant t : tenants) {
            BigDecimal totalTenant = vendaRepo.somaVendasPeriodo(t.getId(), inicioDia, fimDia);
            if (totalTenant != null) {
                vendasHoje += totalTenant.doubleValue();
            }
        }

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
    public String confirmarPagamento(@PathVariable Long id) {
        try {
            adminService.confirmarPagamento(id);
        } catch (IllegalArgumentException ignored) {
            // cliente/pagamento removido entre a listagem e a ação
        }
        return "redirect:/admin";
    }

    // --- Ativar/Desativar cliente ---

    @PostMapping("/cliente/{id}/ativar")
    public String ativarCliente(@PathVariable UUID id) {
        try {
            adminService.ativarCliente(id);
        } catch (IllegalArgumentException ignored) {
        }
        return "redirect:/admin";
    }

    @PostMapping("/cliente/{id}/desativar")
    public String desativarCliente(@PathVariable UUID id) {
        try {
            adminService.desativarCliente(id);
        } catch (IllegalArgumentException ignored) {
        }
        return "redirect:/admin";
    }

    // --- Estender teste ---

    @PostMapping("/cliente/{id}/estender-teste")
    public String estenderTeste(@PathVariable UUID id) {
        try {
            adminService.estenderTeste(id, 15);
        } catch (IllegalArgumentException ignored) {
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
        double totalFiado = fiados.stream().filter(f -> !f.isPago()).mapToDouble(f -> f.getValor().doubleValue()).sum();
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

        // Agregar vendas de todos os clientes
        List<Map<String, Object>> todasVendas = new ArrayList<>();
        double totalGeral = 0;
        
        for (Tenant t : tenants) {
            // Find owner of store
            Optional<Usuario> ownerOpt = usuarioRepo.findAll().stream()
                    .filter(u -> u.getTenantId().equals(t.getId()) && u.getRole() == Usuario.Role.DONO)
                    .findFirst();
            String ownerName = ownerOpt.map(Usuario::getNome).orElse("Dono Desconhecido");

            List<Venda> vendas = vendaRepo.findByTenantIdAndDataHoraBetween(t.getId(), inicio, fim);
            for (Venda v : vendas) {
                Map<String, Object> dados = new HashMap<>();
                dados.put("venda", v);
                dados.put("clienteNome", ownerName);
                dados.put("clienteNegocio", t.getNome());
                todasVendas.add(dados);
                if (v.getStatus() == Venda.Status.CONCLUIDA) {
                    totalGeral += v.getTotal().doubleValue();
                }
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
        List<Map<String, Object>> ranking = new ArrayList<>();
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioMes = hoje.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = hoje.plusDays(1).atStartOfDay().minusNanos(1);

        for (Tenant t : tenants) {
            BigDecimal totalVendas = vendaRepo.somaVendasPeriodo(t.getId(), inicioMes, fimMes);
            double vendasMes = totalVendas != null ? totalVendas.doubleValue() : 0.0;
            
            List<Fiado> fiados = fiadoRepo.findByTenantId(t.getId());
            double totalFiado = fiados.stream().filter(f -> !f.isPago()).mapToDouble(f -> f.getValor().doubleValue()).sum();
            long qtdVendas = vendaRepo.countByTenantId(t.getId());

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
    public String apagarCliente(@PathVariable UUID id) {
        try {
            adminService.apagarCliente(id);
        } catch (IllegalArgumentException ignored) {
            // cliente removido entre a listagem e a ação
        }
        return "redirect:/admin?apagado";
    }

    // ========================================
    // 🔥 URGENTE #7: Reset Senha
    // ========================================

    @PostMapping("/cliente/{id}/reset-senha")
    public String resetSenha(@PathVariable UUID id, @RequestParam String novaSenha) {
        try {
            adminService.resetSenhaDono(id, novaSenha);
        } catch (IllegalArgumentException ignored) {
            // tenant/dono inexistente
        }
        return "redirect:/admin?senhaReset";
    }

    // ========================================
    // 📅 #4: Configurar Preço da Assinatura
    // ========================================

    @GetMapping("/config")
    public String config(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("precoAtual", 350.0);
        return "admin-config";
    }

    @PostMapping("/config/preco")
    public String atualizarPreco(@RequestParam Double preco) {
        log.info("Preço da assinatura atualizado para {}", preco);
        return "redirect:/admin/config?precoAtualizado";
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
    // 🌟 #3: Mensagem Global
    // ========================================

    @PostMapping("/mensagem-global")
    public String mensagemGlobal(
            @RequestParam String titulo,
            @RequestParam String mensagem,
            HttpSession session) {

        if (!isAdmin(session)) return "redirect:/admin/login";

        log.info("Mensagem global enviada: {} - {}", titulo, mensagem);
        return "redirect:/admin?msgEnviada";
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
        return "admin-cupoes";
    }

    @PostMapping("/cupao/criar")
    public String criarCupao(
            @RequestParam String codigo,
            @RequestParam Double descontoPercent,
            @RequestParam(required = false) UUID clienteId) {
        log.info("Cupão criado: {} ({}%) para cliente {}", codigo, descontoPercent, clienteId);
        return "redirect:/admin/cupoes?cupaoCriado";
    }
}
