package com.nanobanana.repository;

import com.nanobanana.model.Fiado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface FiadoRepository extends JpaRepository<Fiado, UUID> {
    List<Fiado> findByTenantId(UUID tenantId);
    Page<Fiado> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Fiado> findByTenantIdAndCanceladoFalse(UUID tenantId, Pageable pageable);
    List<Fiado> findByTenantIdAndPago(UUID tenantId, boolean pago);
    List<Fiado> findByTenantIdAndVendaId(UUID tenantId, UUID vendaId);
    List<Fiado> findByTenantIdOrderByDataCriacaoDesc(UUID tenantId);
    List<Fiado> findByTenantIdAndCanceladoFalseOrderByDataCriacaoDesc(UUID tenantId);
    long countByTenantId(UUID tenantId);
    long countByTenantIdAndPago(UUID tenantId, boolean pago);
    long countByTenantIdAndPagoAndCanceladoFalse(UUID tenantId, boolean pago);
    List<Fiado> findByTenantIdOrderByDataPagamentoDesc(UUID tenantId);
    
    @Query("SELECT SUM(f.valor) FROM Fiado f WHERE f.tenantId = :tenantId AND f.pago = false AND f.cancelado = false")
    BigDecimal totalFiadoNaoPago(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT f.nomeCliente, SUM(f.valor), COUNT(f) FROM Fiado f WHERE f.tenantId = :tenantId AND f.pago = false AND f.cancelado = false GROUP BY f.nomeCliente ORDER BY SUM(f.valor) DESC")
    List<Object[]> rankingDevedores(@Param("tenantId") UUID tenantId);

    @Query("SELECT SUM(f.valor) FROM Fiado f, Venda v WHERE f.vendaId = v.id AND f.tenantId = :tenantId AND v.tenantId = :tenantId AND f.pago = false AND f.cancelado = false AND v.dataHora BETWEEN :inicio AND :fim")
    BigDecimal totalFiadoNaoPagoPeriodo(@Param("tenantId") UUID tenantId, @Param("inicio") java.time.LocalDateTime inicio, @Param("fim") java.time.LocalDateTime fim);

    @Query("SELECT f.tenantId, SUM(f.valor) FROM Fiado f WHERE f.pago = false AND f.cancelado = false GROUP BY f.tenantId")
    List<Object[]> totalNaoPagoPorTenant();
}
