package com.nanobanana.repository;

import com.nanobanana.model.VendaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface VendaItemRepository extends JpaRepository<VendaItem, UUID> {
    List<VendaItem> findByVendaId(UUID vendaId);
    List<VendaItem> findByVendaIdIn(List<UUID> vendaIds);
    
    @Query("SELECT vi.produtoId, vi.nomeProduto, SUM(vi.quantidade), SUM(vi.subtotal) FROM VendaItem vi WHERE vi.vendaId IN (SELECT v.id FROM Venda v WHERE v.tenantId = :tenantId AND v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim) GROUP BY vi.produtoId, vi.nomeProduto ORDER BY SUM(vi.quantidade) DESC")
    List<Object[]> topProdutosVendidos(@Param("tenantId") UUID tenantId, @Param("inicio") java.time.LocalDateTime inicio, @Param("fim") java.time.LocalDateTime fim);

    @Query("SELECT vi.produtoId, vi.nomeProduto, SUM(vi.quantidade), SUM(vi.subtotal) FROM VendaItem vi WHERE vi.vendaId IN (SELECT v.id FROM Venda v WHERE v.tenantId = :tenantId AND v.vendedorId = :vendedorId AND v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim) GROUP BY vi.produtoId, vi.nomeProduto ORDER BY SUM(vi.quantidade) DESC")
    List<Object[]> topProdutosVendedor(@Param("tenantId") UUID tenantId, @Param("vendedorId") UUID vendedorId,
                                       @Param("inicio") java.time.LocalDateTime inicio, @Param("fim") java.time.LocalDateTime fim);

    @Query("SELECT vi.produtoId, MAX(v.dataHora) FROM VendaItem vi JOIN Venda v ON v.id = vi.vendaId WHERE v.tenantId = :tenantId AND v.status = 'CONCLUIDA' GROUP BY vi.produtoId")
    List<Object[]> ultimaVendaPorProduto(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(COALESCE(vi.custoTotal, 0)), 0) FROM VendaItem vi JOIN Venda v ON v.id = vi.vendaId WHERE v.tenantId = :tenantId AND v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim")
    java.math.BigDecimal custoVendasPeriodo(@Param("tenantId") UUID tenantId, @Param("inicio") java.time.LocalDateTime inicio, @Param("fim") java.time.LocalDateTime fim);
}
