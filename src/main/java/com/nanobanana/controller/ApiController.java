package com.nanobanana.controller;

import com.nanobanana.model.Fiado;
import com.nanobanana.model.Produto;
import com.nanobanana.model.Usuario;
import com.nanobanana.model.Venda;
import com.nanobanana.repository.FiadoRepository;
import com.nanobanana.repository.ProdutoRepository;
import com.nanobanana.repository.VendaRepository;
import com.nanobanana.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * API JSON somente leitura para integrações e futuras telas.
 * Todas as rotas exigem sessão autenticada e respeitam o tenant do usuário.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final AuthService authService;
    private final ProdutoRepository produtoRepository;
    private final VendaRepository vendaRepository;
    private final FiadoRepository fiadoRepository;

    public ApiController(AuthService authService,
                         ProdutoRepository produtoRepository,
                         VendaRepository vendaRepository,
                         FiadoRepository fiadoRepository) {
        this.authService = authService;
        this.produtoRepository = produtoRepository;
        this.vendaRepository = vendaRepository;
        this.fiadoRepository = fiadoRepository;
    }

    @GetMapping("/produtos")
    public ResponseEntity<List<ProdutoDto>> produtos() {
        Usuario usuario = usuario();
        if (usuario == null) return ResponseEntity.status(401).build();

        List<ProdutoDto> produtos = produtoRepository
                .findByTenantIdOrderByNome(usuario.getTenantId())
                .stream()
                .map(p -> new ProdutoDto(p.getId(), p.getNome(), p.getCategoria(),
                        p.getPrecoVenda(), p.getStockAtual(), p.getStockMinimo()))
                .toList();

        return ResponseEntity.ok(produtos);
    }

    @GetMapping("/vendas")
    public ResponseEntity<List<VendaDto>> vendas() {
        Usuario usuario = usuario();
        if (usuario == null) return ResponseEntity.status(401).build();

        List<Venda> vendas = usuario.getRole() == Usuario.Role.VENDEDOR
                ? vendaRepository.findByTenantIdAndVendedorIdAndStatusOrderByDataHoraDesc(
                        usuario.getTenantId(), usuario.getId(), Venda.Status.CONCLUIDA,
                        org.springframework.data.domain.PageRequest.of(0, 50)).getContent()
                : vendaRepository.findByTenantIdAndStatusOrderByDataHoraDesc(
                        usuario.getTenantId(), Venda.Status.CONCLUIDA,
                        org.springframework.data.domain.PageRequest.of(0, 50)).getContent();

        List<VendaDto> dtos = vendas.stream()
                .map(v -> new VendaDto(v.getId(), v.getDataHora(), v.getStatus().name(),
                        v.getMetodoPagamento() != null ? v.getMetodoPagamento().name() : null,
                        v.getTotal()))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/resumo")
    public ResponseEntity<ResumoDto> resumo() {
        Usuario usuario = usuario();
        if (usuario == null) return ResponseEntity.status(401).build();

        if (!usuario.podeVerRelatorios()) {
            return ResponseEntity.status(403).build();
        }

        UUID tenantId = usuario.getTenantId();
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fim = LocalDate.now().plusDays(1).atStartOfDay();

        BigDecimal vendasHoje = vendaRepository.somaVendasPeriodo(tenantId, inicio, fim);
        long qtdVendasHoje = vendaRepository.countVendasPeriodo(tenantId, inicio, fim);
        BigDecimal totalFiado = fiadoRepository.totalFiadoNaoPago(tenantId);
        long stockBaixo = produtoRepository.findProdutosStockBaixo(tenantId).size();

        return ResponseEntity.ok(new ResumoDto(
                vendasHoje != null ? vendasHoje : BigDecimal.ZERO,
                qtdVendasHoje,
                totalFiado != null ? totalFiado : BigDecimal.ZERO,
                stockBaixo));
    }

    private Usuario usuario() {
        return authService.getUsuarioLogado().orElse(null);
    }

    public record ProdutoDto(UUID id, String nome, String categoria, BigDecimal precoVenda,
                             int stockAtual, int stockMinimo) {}

    public record VendaDto(UUID id, LocalDateTime dataHora, String status, String metodoPagamento,
                           BigDecimal total) {}

    public record ResumoDto(BigDecimal vendasHoje, long quantidadeVendasHoje,
                            BigDecimal totalFiado, long produtosStockBaixo) {}
}
