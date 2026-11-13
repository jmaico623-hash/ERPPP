package com.nanobanana.controller;

import com.nanobanana.model.Fiado;
import com.nanobanana.model.Produto;
import com.nanobanana.model.Usuario;
import com.nanobanana.model.Venda;
import com.nanobanana.repository.FiadoRepository;
import com.nanobanana.repository.ProdutoRepository;
import com.nanobanana.repository.VendaRepository;
import com.nanobanana.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Exportações CSV compatíveis com Excel/LibreOffice (UTF-8 + BOM, separador ';').
 */
@Controller
@RequestMapping("/export")
public class ExportController {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AuthService authService;
    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;
    private final FiadoRepository fiadoRepository;

    public ExportController(AuthService authService,
                            VendaRepository vendaRepository,
                            ProdutoRepository produtoRepository,
                            FiadoRepository fiadoRepository) {
        this.authService = authService;
        this.vendaRepository = vendaRepository;
        this.produtoRepository = produtoRepository;
        this.fiadoRepository = fiadoRepository;
    }

    @GetMapping("/vendas.csv")
    public void exportarVendas(HttpServletResponse response) throws IOException {
        Usuario usuario = usuarioLogado();
        if (usuario == null) {
            response.sendRedirect("/login");
            return;
        }

        UUID tenantId = usuario.getTenantId();
        List<Venda> vendas = usuario.getRole() == Usuario.Role.VENDEDOR
                ? vendaRepository.findByTenantIdAndVendedorId(tenantId, usuario.getId())
                : vendaRepository.findByTenantIdOrderByDataHoraDesc(tenantId);

        prepararResposta(response, "vendas.csv");
        PrintWriter out = response.getWriter();
        out.write('\uFEFF');
        linha(out, "ID", "Data/Hora", "Status", "Metodo", "Subtotal", "Desconto", "Total",
                "Valor Pago", "Troco", "Cliente Fiado", "Motivo Cancelamento");

        for (Venda v : vendas) {
            linha(out,
                    v.getId() != null ? v.getId().toString() : "",
                    v.getDataHora() != null ? v.getDataHora().format(DATA_HORA) : "",
                    v.getStatus() != null ? v.getStatus().name() : "",
                    v.getMetodoPagamento() != null ? v.getMetodoPagamento().name() : "",
                    money(v.getSubtotal()),
                    money(v.getDesconto()),
                    money(v.getTotal()),
                    money(v.getValorPago()),
                    money(v.getTroco()),
                    nulo(v.getNomeClienteFiado()),
                    nulo(v.getMotivoCancelamento()));
        }
        out.flush();
    }

    @GetMapping("/stock.csv")
    public void exportarStock(HttpServletResponse response) throws IOException {
        Usuario usuario = usuarioLogado();
        if (usuario == null) {
            response.sendRedirect("/login");
            return;
        }
        if (!usuario.podeVerRelatorios()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        List<Produto> produtos = produtoRepository.findByTenantIdOrderByNome(usuario.getTenantId());

        prepararResposta(response, "stock.csv");
        PrintWriter out = response.getWriter();
        out.write('\uFEFF');
        linha(out, "ID", "Nome", "Categoria", "Codigo de Barras", "Preco Custo",
                "Preco Venda", "Stock Atual", "Stock Minimo", "Valor em Stock");

        for (Produto p : produtos) {
            linha(out,
                    p.getId() != null ? p.getId().toString() : "",
                    p.getNome(),
                    nulo(p.getCategoria()),
                    nulo(p.getCodigoBarras()),
                    money(p.getPrecoCusto()),
                    money(p.getPrecoVenda()),
                    String.valueOf(p.getStockAtual()),
                    String.valueOf(p.getStockMinimo()),
                    money(p.getValorStock()));
        }
        out.flush();
    }

    @GetMapping("/fiados.csv")
    public void exportarFiados(HttpServletResponse response) throws IOException {
        Usuario usuario = usuarioLogado();
        if (usuario == null) {
            response.sendRedirect("/login");
            return;
        }
        if (!usuario.podeVerRelatorios()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        List<Fiado> fiados = fiadoRepository.findByTenantIdAndCanceladoFalseOrderByDataCriacaoDesc(usuario.getTenantId());

        prepararResposta(response, "fiados.csv");
        PrintWriter out = response.getWriter();
        out.write('\uFEFF');
        linha(out, "ID", "Cliente", "Telefone", "Valor", "Pago", "Data Criacao", "Data Pagamento");

        for (Fiado f : fiados) {
            linha(out,
                    f.getId() != null ? f.getId().toString() : "",
                    f.getNomeCliente(),
                    nulo(f.getTelefoneCliente()),
                    money(f.getValor()),
                    f.isPago() ? "Sim" : "Nao",
                    f.getDataCriacao() != null ? f.getDataCriacao().format(DATA_HORA) : "",
                    f.getDataPagamento() != null ? f.getDataPagamento().format(DATA) : "");
        }
        out.flush();
    }

    private Usuario usuarioLogado() {
        return authService.getUsuarioLogado().orElse(null);
    }

    private void prepararResposta(HttpServletResponse response, String nomeArquivo) {
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + nomeArquivo + "\"");
        response.setHeader("Cache-Control", "no-store");
    }

    private void linha(PrintWriter out, String... campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) sb.append(';');
            sb.append(escapar(campos[i]));
        }
        out.println(sb);
    }

    private String escapar(String valor) {
        if (valor == null) return "";
        String limpo = valor.replace("\r", " ").replace("\n", " ");
        // Neutraliza fórmulas abertas pelo Excel/LibreOffice
        if (limpo.startsWith("=") || limpo.startsWith("+") || limpo.startsWith("-") || limpo.startsWith("@")) {
            limpo = "'" + limpo;
        }
        if (limpo.contains(";") || limpo.contains("\"")) {
            limpo = "\"" + limpo.replace("\"", "\"\"") + "\"";
        }
        return limpo;
    }

    private String money(BigDecimal valor) {
        return valor != null ? valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "";
    }

    private String nulo(String valor) {
        return valor != null ? valor : "";
    }
}
