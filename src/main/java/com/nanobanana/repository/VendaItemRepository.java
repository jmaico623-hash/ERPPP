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
}
