package com.nanobanana.controller;

import com.nanobanana.model.*;
import com.nanobanana.repository.*;
import com.nanobanana.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * DASHBOARD
 * Dono: vê TUDO (vendas, stock, alertas, auditoria, vendedores)
 * Gerente: vê vendas e relatórios (NÃO vê stock)
 * Vendedor: vê APENAS suas vendas do período
 */
@Controller
public class DashboardController {

    @Autowired
    private AuthService authService;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private VendaItemRepository vendaItemRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private FiadoRepository fiadoRepository;

    @Autowired
    private AlertaSistemaRepository alertaSistemaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    @Autowired
    private DespesaRepository despesaRepository;

    @Autowired
    private DevolucaoRepository devolucaoRepository;

    @Autowired
    private DevolucaoItemRepository devolucaoItemRepository;

    @Autowired
    private CaixaRepository caixaRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model,
                            @RequestParam(required = false, defaultValue = "hoje") String periodo) {

        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();
        Periodo p = calcularPeriodo(periodo);

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        Caixa caixaAberto = caixaRepository
                .findFirstByTenantIdAndStatusOrderByDataAberturaDesc(tenantId, Caixa.Status.ABERTO)
                .orElse(null);
        Map<UUID, String> nomesUsuarios = nomesPorUsuario(tenantId);

        model.addAttribute("usuario", usuario);
        model.addAttribute("isDono", usuario.getRole() == Usuario.Role.DONO);
        model.addAttribute("isGerente", usuario.getRole() == Usuario.Role.GERENTE);
        model.addAttribute("isVendedor", usuario.getRole() == Usuario.Role.VENDEDOR);
        model.addAttribute("tenant", tenant);
        model.addAttribute("nomeLoja", tenant != null ? tenant.getNome() : "Minha loja");
        model.addAttribute("planoLabel", planoLabel(tenant));
        model.addAttribute("diasRestantes", tenant != null ? tenant.diasRestantes() : -1);
        model.addAttribute("periodo", p.id);
        model.addAttribute("periodoLabel", p.label);
        model.addAttribute("caixaAberto", caixaAberto);
        model.addAttribute("hoje", LocalDate.now());

        if (usuario.getRole() == Usuario.Role.VENDEDOR) {
            preencherDashboardVendedor(model, tenantId, usuario, p);
            return "dashboard-vendedor";
        }

        preencherDashboardGestor(model, tenantId, p, nomesUsuarios);

        if (usuario.getRole() == Usuario.Role.DONO) {
            preencherDadosDono(model, tenantId);
            return "dashboard-dono";
        }

        return "dashboard-gerente";
    }

    // ==================================================================
    // Dados partilhados por DONO e GERENTE
    // ==================================================================
    private void preencherDashboardGestor(Model model, UUID tenantId, Periodo p,
                                          Map<UUID, String> nomesUsuarios) {
        BigDecimal totalVendas = nvl(vendaRepository.somaVendasPeriodo(tenantId, p.inicio, p.fim));
        long quantidadeVendas = vendaRepository.countVendasPeriodo(tenantId, p.inicio, p.fim);
        BigDecimal ticketMedio = dividir(totalVendas, quantidadeVendas);

        BigDecimal totalDevolucoes = nvl(devolucaoRepository.somaPeriodo(tenantId, p.inicio, p.fim));
        BigDecimal vendasLiquidas = totalVendas.subtract(totalDevolucoes);
        if (vendasLiquidas.signum() < 0) vendasLiquidas = BigDecimal.ZERO;

        BigDecimal custoVendas = nvl(vendaItemRepository.custoVendasPeriodo(tenantId, p.inicio, p.fim));
        BigDecimal custoDevolvido = nvl(devolucaoItemRepository.custoDevolvidoPeriodo(tenantId, p.inicio, p.fim));
        BigDecimal cmv = custoVendas.subtract(custoDevolvido);
        if (cmv.signum() < 0) cmv = BigDecimal.ZERO;

        BigDecimal lucroBruto = vendasLiquidas.subtract(cmv);
        BigDecimal totalDespesas = nvl(despesaRepository.somaPeriodo(
                tenantId, p.inicio.toLocalDate(), LocalDate.now()));
        BigDecimal lucroLiquido = lucroBruto.subtract(totalDespesas);
        BigDecimal margem = vendasLiquidas.signum() == 0 ? BigDecimal.ZERO
                : lucroLiquido.multiply(BigDecimal.valueOf(100))
                    .divide(vendasLiquidas, 1, RoundingMode.HALF_UP);

        BigDecimal totalAnterior = nvl(vendaRepository.somaVendasPeriodo(
                tenantId, p.inicioAnterior, p.fimAnterior));
        long quantidadeAnterior = vendaRepository.countVendasPeriodo(
                tenantId, p.inicioAnterior, p.fimAnterior);

        List<Venda> vendasRecentes = vendaRepository
                .findByTenantIdAndStatusAndDataHoraBetweenOrderByDataHoraDesc(
                        tenantId, Venda.Status.CONCLUIDA, p.inicio, p.fim, PageRequest.of(0, 8))
                .getContent();

        model.addAttribute("totalVendas", totalVendas);
        model.addAttribute("quantidadeVendas", quantidadeVendas);
        model.addAttribute("ticketMedio", ticketMedio);
        model.addAttribute("totalDevolucoes", totalDevolucoes);
        model.addAttribute("vendasLiquidas", vendasLiquidas);
        model.addAttribute("cmv", cmv);
        model.addAttribute("lucroBruto", lucroBruto);
        model.addAttribute("totalDespesas", totalDespesas);
        model.addAttribute("lucroLiquido", lucroLiquido);
        model.addAttribute("margem", margem);
        model.addAttribute("variacaoVendas", variacao(totalVendas, totalAnterior));
        model.addAttribute("variacaoQuantidade", variacao(
                BigDecimal.valueOf(quantidadeVendas), BigDecimal.valueOf(quantidadeAnterior)));
        model.addAttribute("vendasRecentes", resumoVendas(vendasRecentes, nomesUsuarios));
        model.addAttribute("topProdutos", topProdutos(
                vendaItemRepository.topProdutosVendidos(tenantId, p.inicio, p.fim), 5));
        model.addAttribute("pagamentos", pagamentos(
                vendaRepository.resumoPorMetodoPagamento(tenantId, p.inicio, p.fim), totalVendas));
        model.addAttribute("rankingVendedores", ranking(
                vendaRepository.rankingVendedores(tenantId, p.inicio, p.fim), nomesUsuarios));

        BigDecimal totalFiado = nvl(fiadoRepository.totalFiadoNaoPago(tenantId));
        long fiadosNaoPagos = fiadoRepository.countByTenantIdAndPagoAndCanceladoFalse(tenantId, false);
        model.addAttribute("totalFiado", totalFiado);
        model.addAttribute("fiadosNaoPagos", fiadosNaoPagos);
    }

    // ==================================================================
    // Dados exclusivos do DONO
    // ==================================================================
    private void preencherDadosDono(Model model, UUID tenantId) {
        long totalProdutos = produtoRepository.countByTenantId(tenantId);
        List<Produto> stockBaixo = produtoRepository.findProdutosStockBaixo(tenantId);
        List<Produto> semStock = produtoRepository.findProdutosSemStock(tenantId);
        BigDecimal valorStock = produtoRepository.valorTotalStock(tenantId);

        long alertasNaoLidos = alertaSistemaRepository.countByTenantIdAndLidoFalse(tenantId);
        List<AlertaSistema> alertas = alertaSistemaRepository
                .findByTenantIdOrderByCriadoEmDesc(tenantId)
                .stream().limit(5).toList();

        model.addAttribute("totalProdutos", totalProdutos);
        model.addAttribute("produtosStockBaixo", stockBaixo.stream().limit(8).toList());
        model.addAttribute("qtdStockBaixo", stockBaixo.size());
        model.addAttribute("produtosSemStock", semStock);
        model.addAttribute("qtdSemStock", semStock.size());
        model.addAttribute("valorStock", nvl(valorStock));
        model.addAttribute("alertasNaoLidos", alertasNaoLidos);
        model.addAttribute("alertas", alertas);
    }

    // ==================================================================
    // Dashboard do VENDEDOR
    // ==================================================================
    private void preencherDashboardVendedor(Model model, UUID tenantId, Usuario usuario, Periodo p) {
        BigDecimal totalVendas = nvl(vendaRepository.somaVendasVendedorPeriodo(
                tenantId, usuario.getId(), p.inicio, p.fim));
        long quantidadeVendas = vendaRepository.countVendasVendedorPeriodo(
                tenantId, usuario.getId(), p.inicio, p.fim);
        BigDecimal ticketMedio = dividir(totalVendas, quantidadeVendas);

        BigDecimal totalAnterior = nvl(vendaRepository.somaVendasVendedorPeriodo(
                tenantId, usuario.getId(), p.inicioAnterior, p.fimAnterior));
        long quantidadeAnterior = vendaRepository.countVendasVendedorPeriodo(
                tenantId, usuario.getId(), p.inicioAnterior, p.fimAnterior);

        List<Venda> vendasRecentes = vendaRepository
                .findByTenantIdAndVendedorIdAndStatusAndDataHoraBetweenOrderByDataHoraDesc(
                        tenantId, usuario.getId(), Venda.Status.CONCLUIDA,
                        p.inicio, p.fim, PageRequest.of(0, 8))
                .getContent();

        List<Object[]> rankingRows = vendaRepository.rankingVendedores(tenantId, p.inicio, p.fim);
        List<Object[]> rankingOrdenado = new ArrayList<>(rankingRows);
        rankingOrdenado.sort((a, b) -> nvl((BigDecimal) b[2]).compareTo(nvl((BigDecimal) a[2])));

        int posicao = 0;
        for (int i = 0; i < rankingOrdenado.size(); i++) {
            if (usuario.getId().equals(rankingOrdenado.get(i)[0])) {
                posicao = i + 1;
                break;
            }
        }

        model.addAttribute("totalVendas", totalVendas);
        model.addAttribute("quantidadeVendas", quantidadeVendas);
        model.addAttribute("ticketMedio", ticketMedio);
        model.addAttribute("variacaoVendas", variacao(totalVendas, totalAnterior));
        model.addAttribute("variacaoQuantidade", variacao(
                BigDecimal.valueOf(quantidadeVendas), BigDecimal.valueOf(quantidadeAnterior)));
        model.addAttribute("vendasRecentes", resumoVendas(vendasRecentes, Map.of(usuario.getId(), usuario.getNome())));
        model.addAttribute("topProdutos", topProdutos(
                vendaItemRepository.topProdutosVendedor(tenantId, usuario.getId(), p.inicio, p.fim), 5));
        model.addAttribute("posicaoRanking", posicao);
        model.addAttribute("totalVendedoresRanking", rankingOrdenado.size());
    }

    // ==================================================================
    // Helpers de apresentação
    // ==================================================================
    private Map<UUID, String> nomesPorUsuario(UUID tenantId) {
        Map<UUID, String> nomes = new HashMap<>();
        for (Usuario u : usuarioRepository.findByTenantId(tenantId)) {
            nomes.put(u.getId(), u.getNome());
        }
        return nomes;
    }

    private List<Map<String, Object>> resumoVendas(List<Venda> vendas, Map<UUID, String> nomes) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Venda v : vendas) {
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("id", v.getId());
            linha.put("vendedor", nomes.getOrDefault(v.getVendedorId(), "—"));
            linha.put("total", nvl(v.getTotal()));
            linha.put("metodo", metodoLabel(v.getMetodoPagamento()));
            linha.put("metodoClasse", metodoClasse(v.getMetodoPagamento()));
            linha.put("dataHora", v.getDataHora());
            linha.put("fiado", v.getMetodoPagamento() == Venda.MetodoPagamento.FIADO);
            resultado.add(linha);
        }
        return resultado;
    }

    private List<Map<String, Object>> topProdutos(List<Object[]> rows, int limite) {
        BigDecimal maiorTotal = BigDecimal.ZERO;
        for (Object[] row : rows) {
            BigDecimal total = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
            if (total.compareTo(maiorTotal) > 0) maiorTotal = total;
        }

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Object[] row : rows.stream().limit(limite).toList()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nome", row[1] != null ? row[1] : "Produto removido");
            item.put("quantidade", row[2] != null ? ((Number) row[2]).longValue() : 0L);
            BigDecimal total = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
            item.put("total", total);
            item.put("percentual", percentual(total, maiorTotal));
            resultado.add(item);
        }
        return resultado;
    }

    private List<Map<String, Object>> pagamentos(List<Object[]> rows, BigDecimal totalVendas) {
        List<Object[]> ordenadas = new ArrayList<>(rows);
        ordenadas.sort((a, b) -> nvl((BigDecimal) b[2]).compareTo(nvl((BigDecimal) a[2])));

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Object[] row : ordenadas) {
            BigDecimal total = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("metodo", metodoLabel(row[0] != null ? row[0].toString() : null));
            item.put("metodoClasse", metodoClasse(row[0] != null ? row[0].toString() : null));
            item.put("quantidade", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            item.put("total", total);
            item.put("percentual", percentual(total, totalVendas));
            resultado.add(item);
        }
        return resultado;
    }

    private List<Map<String, Object>> ranking(List<Object[]> rows, Map<UUID, String> nomes) {
        List<Object[]> ordenadas = new ArrayList<>(rows);
        ordenadas.sort((a, b) -> nvl((BigDecimal) b[2]).compareTo(nvl((BigDecimal) a[2])));

        BigDecimal maiorTotal = BigDecimal.ZERO;
        for (Object[] row : ordenadas) {
            BigDecimal total = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            if (total.compareTo(maiorTotal) > 0) maiorTotal = total;
        }

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (int i = 0; i < ordenadas.size(); i++) {
            Object[] row = ordenadas.get(i);
            UUID vendedorId = (UUID) row[0];
            BigDecimal total = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("posicao", i + 1);
            item.put("nome", nomes.getOrDefault(vendedorId, "Vendedor removido"));
            item.put("quantidade", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            item.put("total", total);
            item.put("percentual", percentual(total, maiorTotal));
            resultado.add(item);
        }
        return resultado;
    }

    private static BigDecimal percentual(BigDecimal valor, BigDecimal base) {
        if (base == null || base.signum() == 0) return BigDecimal.ZERO;
        BigDecimal p = valor.multiply(BigDecimal.valueOf(100))
                .divide(base, 0, RoundingMode.HALF_UP);
        return p.min(BigDecimal.valueOf(100)).max(BigDecimal.ZERO);
    }

    private static BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() == 0) return null;
        return atual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal dividir(BigDecimal total, long quantidade) {
        if (quantidade <= 0) return BigDecimal.ZERO;
        return total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private static String metodoLabel(Venda.MetodoPagamento metodo) {
        return metodoLabel(metodo != null ? metodo.name() : null);
    }

    private static String metodoLabel(String metodo) {
        if (metodo == null) return "Outro";
        return switch (metodo.toUpperCase()) {
            case "CASH" -> "Dinheiro";
            case "MPESA" -> "M-Pesa";
            case "EMOLA" -> "e-Mola";
            case "FIADO" -> "Fiado";
            case "CARTAO" -> "Cartão";
            default -> metodo;
        };
    }

    private static String metodoClasse(Venda.MetodoPagamento metodo) {
        return metodoClasse(metodo != null ? metodo.name() : null);
    }

    private static String metodoClasse(String metodo) {
        if (metodo == null) return "outro";
        return switch (metodo.toUpperCase()) {
            case "CASH" -> "cash";
            case "MPESA" -> "mpesa";
            case "EMOLA" -> "emola";
            case "FIADO" -> "fiado";
            case "CARTAO" -> "cartao";
            default -> "outro";
        };
    }

    private static String planoLabel(Tenant tenant) {
        if (tenant == null || tenant.getPlano() == null || tenant.getPlano().isBlank()) return "Teste";
        return switch (tenant.getPlano().trim().toLowerCase()) {
            case "teste" -> "Teste";
            case "basico" -> "Básico";
            case "premium" -> "Premium";
            default -> tenant.getPlano();
        };
    }

    private Periodo calcularPeriodo(String periodo) {
        LocalDate hoje = LocalDate.now();
        LocalDateTime agora = LocalDateTime.now();
        String id = periodo != null ? periodo : "hoje";

        LocalDateTime inicio;
        String label;
        switch (id) {
            case "semana" -> {
                inicio = hoje.minusDays(6).atStartOfDay();
                label = "Últimos 7 dias";
            }
            case "mes" -> {
                inicio = hoje.minusDays(29).atStartOfDay();
                label = "Últimos 30 dias";
            }
            default -> {
                id = "hoje";
                inicio = hoje.atStartOfDay();
                label = "Hoje";
            }
        }

        Duration duracao = Duration.between(inicio, agora);
        LocalDateTime fimAnterior = inicio.minusSeconds(1);
        LocalDateTime inicioAnterior = fimAnterior.minus(duracao);
        return new Periodo(id, label, inicio, agora, inicioAnterior, fimAnterior);
    }

    private record Periodo(String id, String label, LocalDateTime inicio, LocalDateTime fim,
                           LocalDateTime inicioAnterior, LocalDateTime fimAnterior) {}

    // ==================================================================
    // Alertas
    // ==================================================================
    @GetMapping("/alertas")
    public String alertas(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();

        // APENAS DONO vê alertas
        if (usuario.getRole() != Usuario.Role.DONO) {
            return "redirect:/pdv";
        }

        UUID tenantId = authService.getTenantIdLogado();
        List<AlertaSistema> alertas = alertaSistemaRepository.findByTenantIdOrderByCriadoEmDesc(tenantId);

        model.addAttribute("usuario", usuario);
        model.addAttribute("alertas", alertas);

        return "alertas";
    }

    @PostMapping("/alertas/{id}/resolver")
    public String resolverAlerta(@PathVariable Long id) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        if (usuario.getRole() != Usuario.Role.DONO) {
            return "redirect:/pdv";
        }

        UUID tenantId = authService.getTenantIdLogado();
        AlertaSistema alerta = alertaSistemaRepository.findByIdAndTenantId(id, tenantId).orElse(null);
        if (alerta != null) {
            alerta.setResolvido(true);
            alerta.setResolvidoEm(java.time.LocalDateTime.now());
            alerta.setResolvidoPor(usuarioOpt.get().getId());
            alertaSistemaRepository.save(alerta);
        }

        return "redirect:/alertas";
    }
}
