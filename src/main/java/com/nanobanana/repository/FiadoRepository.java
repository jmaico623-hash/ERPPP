package com.nanobanana.repository;

import com.nanobanana.model.Fiado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface FiadoRepository extends JpaRepository<Fiado, UUID> {
    List<Fiado> findByTenantId(UUID tenantId);
    List<Fiado> findByTenantIdAndPago(UUID tenantId, boolean pago);
    long countByTenantId(UUID tenantId);
    long countByTenantIdAndPago(UUID tenantId, boolean pago);
    List<Fiado> findByTenantIdOrderByDataCriacaoDesc(UUID tenantId);
    List<Fiado> findByTenantIdOrderByDataPagamentoDesc(UUID tenantId);
    
    @Query("SELECT SUM(f.valor) FROM Fiado f WHERE f.tenantId = :tenantId AND f.pago = false")
    BigDecimal totalFiadoNaoPago(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT f.nomeCliente, SUM(f.valor), COUNT(f) FROM Fiado f WHERE f.tenantId = :tenantId AND f.pago = false GROUP BY f.nomeCliente ORDER BY SUM(f.valor) DESC")
    List<Object[]> rankingDevedores(@Param("tenantId") UUID tenantId);
}
