package com.nanobanana.controller;

import com.nanobanana.model.Usuario;
import com.nanobanana.model.Venda;
import com.nanobanana.repository.DespesaRepository;
import com.nanobanana.repository.DevolucaoItemRepository;
import com.nanobanana.repository.DevolucaoRepository;
import com.nanobanana.repository.FiadoRepository;
import com.nanobanana.repository.VendaItemRepository;
import com.nanobanana.repository.VendaRepository;
import com.nanobanana.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Relatórios de vendas para DONO/GERENTE.
 */
@Controller
public class ReportController {

    private final AuthService authService;
    private final VendaRepository vendaRepository;
    private final VendaItemRepository vendaItemRepository;
    private final FiadoRepository fiadoRepository;
    private final DespesaRepository despesaRepository;
    private final DevolucaoRepository devolucaoRepository;
    private final DevolucaoItemRepository devolucaoItemRepository;

    public ReportController(AuthService authService,
                            VendaRepository vendaRepository,
                            VendaItemRepository vendaItemRepository,
                            FiadoRepository fiadoRepository,
                            DespesaRepository despesaRepository,
                            DevolucaoRepository devolucaoRepository,
                            DevolucaoItemRepository devolucaoItemRepository) {
        this.authService = authService;
        this.vendaRepository = vendaRepository;
        this.vendaItemRepository = vendaItemRepository;
        this.fiadoRepository = fiadoRepository;
        this.despesaRepository = despesaRepository;
        this.devolucaoRepository = devolucaoRepository;
        this.devolucaoItemRepository = devolucaoItemRepository;
    }

    @GetMapping("/relatorio")
    public String relatorio(Model model,
                            @RequestParam(required = false, defaultValue = "hoje") String periodo) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        if (!usuario.podeVerRelatorios()) {
            return "redirect:/pdv";
        }

        UUID tenantId = authService.getTenantIdLogado();
        Periodo p = calcularPeriodo(periodo != null ? periodo : "hoje");

        LocalDateTime agora = LocalDateTime.now();
        BigDecimal totalVendas = nvl(vendaRepository.somaVendasPeriodo(tenantId, p.inicio, agora));
        long totalVendasCount = vendaRepository.countVendasPeriodo(tenantId, p.inicio, agora);
        long vendasFiado = vendaRepository.countVendasFiadoPeriodo(tenantId, p.inicio, agora);
        BigDecimal mediaVenda = totalVendasCount == 0 ? BigDecimal.ZERO
                : totalVendas.divide(BigDecimal.valueOf(totalVendasCount), 2, RoundingMode.HALF_UP);

        Map<String, Long> porFormaPagamento = new HashMap<>();
        Map<String, BigDecimal> valorPorFormaPagamento = new HashMap<>();
        for (Object[] row : vendaRepository.resumoPorMetodoPagamento(tenantId, p.inicio, agora)) {
            String key = row[0] != null ? row[0].toString().toLowerCase() : "outro";
            porFormaPagamento.put(key, ((Number) row[1]).longValue());
            valorPorFormaPagamento.put(key, row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO);
        }

        Map<String, Long> topProdutos = new LinkedHashMap<>();
        List<Object[]> rows = vendaItemRepository.topProdutosVendidos(tenantId, p.inicio, agora);
        rows.stream()
                .limit(10)
                .forEach(row -> topProdutos.put((String) row[1], ((Number) row[2]).longValue()));

        BigDecimal totalFiado = nvl(fiadoRepository.totalFiadoNaoPagoPeriodo(tenantId, p.inicio, agora));
        LocalDate dataInicio = p.inicio.toLocalDate();
        LocalDate dataFim = LocalDate.now();
        BigDecimal totalDespesas = nvl(despesaRepository.somaPeriodo(tenantId, dataInicio, dataFim));

        BigDecimal totalDevolucoes = nvl(devolucaoRepository.somaPeriodo(tenantId, p.inicio, agora));
        BigDecimal vendasLiquidas = totalVendas.subtract(totalDevolucoes);
        if (vendasLiquidas.signum() < 0) vendasLiquidas = BigDecimal.ZERO;

        BigDecimal custoVendas = nvl(vendaItemRepository.custoVendasPeriodo(tenantId, p.inicio, agora));
        BigDecimal custoDevolvido = nvl(devolucaoItemRepository.custoDevolvidoPeriodo(tenantId, p.inicio, agora));
        BigDecimal custoMercadoriaVendida = custoVendas.subtract(custoDevolvido);
        if (custoMercadoriaVendida.signum() < 0) custoMercadoriaVendida = BigDecimal.ZERO;

        BigDecimal lucroBruto = vendasLiquidas.subtract(custoMercadoriaVendida);
        BigDecimal lucroLiquido = lucroBruto.subtract(totalDespesas);
        // compat: resultado operacional como lucro líquido (com custos quando disponíveis)
        BigDecimal resultadoOperacional = lucroLiquido;

        model.addAttribute("usuario", usuario);
        model.addAttribute("periodo", p.id);
        model.addAttribute("periodoLabel", p.label);
        model.addAttribute("totalVendas", totalVendas);
        model.addAttribute("totalVendasCount", totalVendasCount);
        model.addAttribute("mediaVenda", mediaVenda);
        model.addAttribute("vendasFiado", vendasFiado);
        model.addAttribute("porFormaPagamento", porFormaPagamento);
        model.addAttribute("valorPorFormaPagamento", valorPorFormaPagamento);
        model.addAttribute("topProdutos", topProdutos);
        model.addAttribute("totalFiado", totalFiado);
        model.addAttribute("totalDespesas", totalDespesas);
        model.addAttribute("resultadoOperacional", resultadoOperacional);
        model.addAttribute("totalDevolucoes", totalDevolucoes);
        model.addAttribute("vendasLiquidas", vendasLiquidas);
        model.addAttribute("custoMercadoriaVendida", custoMercadoriaVendida);
        model.addAttribute("lucroBruto", lucroBruto);
        model.addAttribute("lucroLiquido", lucroLiquido);

        return "relatorio";
    }

    private Periodo calcularPeriodo(String periodo) {
        LocalDate hoje = LocalDate.now();
        return switch (periodo) {
            case "semana" -> new Periodo("semana", "Esta Semana", hoje.minusWeeks(1).atStartOfDay());
            case "mes" -> new Periodo("mes", "Este Mês", hoje.minusMonths(1).atStartOfDay());
            case "hoje" -> new Periodo("hoje", "Hoje", hoje.atStartOfDay());
            default -> new Periodo("hoje", "Hoje", hoje.atStartOfDay());
        };
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record Periodo(String id, String label, LocalDateTime inicio) {}
}
