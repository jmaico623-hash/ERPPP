package com.nanobanana.repository;

import com.nanobanana.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface ProdutoRepository extends JpaRepository<Produto, UUID> {
    List<Produto> findByTenantId(UUID tenantId);
    Optional<Produto> findByTenantIdAndId(UUID tenantId, UUID id);
    List<Produto> findByTenantIdAndStockAtualLessThanEqual(UUID tenantId, int stockMinimo);
    List<Produto> findByTenantIdAndStockAtualLessThanEqualOrderByStockAtualAsc(UUID tenantId, int stockMinimo);
    List<Produto> findByTenantIdAndCategoria(UUID tenantId, String categoria);
    List<Produto> findByTenantIdOrderByNome(UUID tenantId);
    boolean existsByTenantIdAndCodigoBarras(UUID tenantId, String codigoBarras);
    long countByTenantId(UUID tenantId);
    
    @Query("SELECT p FROM Produto p WHERE p.tenantId = :tenantId AND p.stockAtual <= p.stockMinimo")
    List<Produto> findProdutosStockBaixo(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT p FROM Produto p WHERE p.tenantId = :tenantId AND p.stockAtual = 0")
    List<Produto> findProdutosSemStock(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT SUM(p.stockAtual * COALESCE(p.precoCusto, p.precoVenda)) FROM Produto p WHERE p.tenantId = :tenantId")
    java.math.BigDecimal valorTotalStock(@Param("tenantId") UUID tenantId);

    /**
     * Busca produto com lock pessimista para operações de stock atómicas
     * Evita race condition: check stock -> update stock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Produto p WHERE p.tenantId = :tenantId AND p.id = :id")
    Optional<Produto> findByTenantIdAndIdWithLock(@Param("tenantId") UUID tenantId, @Param("id") UUID id);
}
