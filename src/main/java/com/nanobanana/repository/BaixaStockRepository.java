package com.nanobanana.repository;

import com.nanobanana.model.BaixaStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BaixaStockRepository extends JpaRepository<BaixaStock, UUID> {
    List<BaixaStock> findByTenantId(UUID tenantId);
    List<BaixaStock> findByTenantIdOrderByDataBaixaDesc(UUID tenantId);
    List<BaixaStock> findByTenantIdAndProdutoId(UUID tenantId, UUID produtoId);
    List<BaixaStock> findByTenantIdAndTipoBaixa(UUID tenantId, BaixaStock.TipoBaixa tipoBaixa);
    List<BaixaStock> findByTenantIdAndDataBaixaBetween(UUID tenantId, LocalDateTime inicio, LocalDateTime fim);
    long countByTenantId(UUID tenantId);
    
    @Query("SELECT SUM(b.quantidade) FROM BaixaStock b WHERE b.tenantId = :tenantId AND b.produtoId = :produtoId")
    Integer somaQuantidadePorProduto(@Param("tenantId") UUID tenantId, @Param("produtoId") UUID produtoId);
    
    @Query("SELECT b FROM BaixaStock b WHERE b.tenantId = :tenantId ORDER BY b.dataBaixa DESC LIMIT 10")
    List<BaixaStock> findUltimasBaixas(@Param("tenantId") UUID tenantId);
}
