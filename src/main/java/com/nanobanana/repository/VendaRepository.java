package com.nanobanana.repository;

import com.nanobanana.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface VendaRepository extends JpaRepository<Venda, UUID> {
    List<Venda> findByTenantId(UUID tenantId);
    List<Venda> findByTenantIdOrderByDataHoraDesc(UUID tenantId);
    List<Venda> findByTenantIdAndVendedorId(UUID tenantId, UUID vendedorId);
    List<Venda> findByTenantIdAndStatus(UUID tenantId, Venda.Status status);
    List<Venda> findByTenantIdAndDataHoraBetween(UUID tenantId, LocalDateTime inicio, LocalDateTime fim);
    List<Venda> findByTenantIdAndStatusOrderByDataHoraDesc(UUID tenantId, Venda.Status status);
    long countByTenantId(UUID tenantId);
    
    @Query("SELECT SUM(v.total) FROM Venda v WHERE v.tenantId = :tenantId AND v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim")
    BigDecimal somaVendasPeriodo(@Param("tenantId") UUID tenantId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
    
    @Query("SELECT COUNT(v) FROM Venda v WHERE v.tenantId = :tenantId AND v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim")
    long countVendasPeriodo(@Param("tenantId") UUID tenantId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
    
    @Query("SELECT v FROM Venda v WHERE v.tenantId = :tenantId AND v.status = 'CANCELADA' ORDER BY v.dataHora DESC")
    List<Venda> findVendasCanceladas(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT v.vendedorId, COUNT(v), SUM(v.total) FROM Venda v WHERE v.tenantId = :tenantId AND v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim GROUP BY v.vendedorId")
    List<Object[]> rankingVendedores(@Param("tenantId") UUID tenantId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
    
    @Query("SELECT v.metodoPagamento, COUNT(v), SUM(v.total) FROM Venda v WHERE v.tenantId = :tenantId AND v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim GROUP BY v.metodoPagamento")
    List<Object[]> resumoPorMetodoPagamento(@Param("tenantId") UUID tenantId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
