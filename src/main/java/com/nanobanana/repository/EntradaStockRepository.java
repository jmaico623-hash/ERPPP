package com.nanobanana.repository;

import com.nanobanana.model.EntradaStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EntradaStockRepository extends JpaRepository<EntradaStock, UUID> {
    List<EntradaStock> findByTenantId(UUID tenantId);
    List<EntradaStock> findByTenantIdOrderByDataEntradaDesc(UUID tenantId);
    List<EntradaStock> findByTenantIdAndProdutoId(UUID tenantId, UUID produtoId);
    List<EntradaStock> findByTenantIdAndDonoId(UUID tenantId, UUID donoId);
    List<EntradaStock> findByTenantIdAndDataEntradaBetween(UUID tenantId, LocalDateTime inicio, LocalDateTime fim);
    long countByTenantId(UUID tenantId);
    
    @Query("SELECT SUM(e.quantidade) FROM EntradaStock e WHERE e.tenantId = :tenantId AND e.produtoId = :produtoId")
    Integer somaQuantidadePorProduto(@Param("tenantId") UUID tenantId, @Param("produtoId") UUID produtoId);
}
