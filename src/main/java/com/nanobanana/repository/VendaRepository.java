package com.nanobanana.repository;

import com.nanobanana.model.Venda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendaRepository extends JpaRepository<Venda, UUID> {
    List<Venda> findByTenantId(UUID tenantId);
    List<Venda> findByTenantIdOrderByDataHoraDesc(UUID tenantId);

    // Versões paginadas para listagens grandes
    @Query("SELECT SUM(v.total) FROM Venda v WHERE v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim")
    BigDecimal somaVendasGlobalPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT v.tenantId, COUNT(v) FROM Venda v GROUP BY v.tenantId")
    List<Object[]> countVendasPorTenant();

    @Query("SELECT v.tenantId, SUM(v.total) FROM Venda v WHERE v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim GROUP BY v.tenantId")
    List<Object[]> somaConcluidasPorTenantPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT v.tenantId, COUNT(v) FROM Venda v WHERE v.dataHora BETWEEN :inicio AND :fim GROUP BY v.tenantId")
    List<Object[]> countVendasPorTenantPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(v) FROM Venda v WHERE v.tenantId = :tenantId AND v.metodoPagamento = 'FIADO' AND v.dataHora BETWEEN :inicio AND :fim")
    long countVendasFiadoPeriodo(@Param("tenantId") UUID tenantId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT v FROM Venda v WHERE v.dataHora BETWEEN :inicio AND :fim")
    List<Venda> findAllByDataHoraBetween(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    Page<Venda> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Venda> findByTenantIdAndVendedorId(UUID tenantId, UUID vendedorId, Pageable pageable);
    List<Venda> findByTenantIdAndVendedorId(UUID tenantId, UUID vendedorId);
    List<Venda> findByTenantIdAndStatus(UUID tenantId, Venda.Status status);
    List<Venda> findByTenantIdAndDataHoraBetween(UUID tenantId, LocalDateTime inicio, LocalDateTime fim);
    Optional<Venda> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Busca a venda com lock pessimista para evitar cancelamento concorrente
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Venda v WHERE v.id = :id AND v.tenantId = :tenantId")
    Optional<Venda> findByIdAndTenantIdWithLock(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    Page<Venda> findByTenantIdAndStatusOrderByDataHoraDesc(UUID tenantId, Venda.Status status, Pageable pageable);
    Page<Venda> findByTenantIdAndVendedorIdAndStatusOrderByDataHoraDesc(UUID tenantId, UUID vendedorId, Venda.Status status, Pageable pageable);

    Page<Venda> findByTenantIdAndStatusAndDataHoraBetweenOrderByDataHoraDesc(
            UUID tenantId, Venda.Status status, LocalDateTime inicio, LocalDateTime fim, Pageable pageable);

    Page<Venda> findByTenantIdAndVendedorIdAndStatusAndDataHoraBetweenOrderByDataHoraDesc(
            UUID tenantId, UUID vendedorId, Venda.Status status, LocalDateTime inicio, LocalDateTime fim, Pageable pageable);

    List<Venda> findByTenantIdAndStatusOrderByDataHoraDesc(UUID tenantId, Venda.Status status);
    long countByTenantId(UUID tenantId);
    
    @Query("SELECT SUM(v.total) FROM Venda v WHERE v.tenantId = :tenantId AND v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim")
    BigDecimal somaVendasPeriodo(@Param("tenantId") UUID tenantId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
    
    @Query("SELECT COUNT(v) FROM Venda v WHERE v.tenantId = :tenantId AND v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim")
    long countVendasPeriodo(@Param("tenantId") UUID tenantId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT SUM(v.total) FROM Venda v WHERE v.tenantId = :tenantId AND v.vendedorId = :vendedorId AND v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim")
    BigDecimal somaVendasVendedorPeriodo(@Param("tenantId") UUID tenantId, @Param("vendedorId") UUID vendedorId,
                                         @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(v) FROM Venda v WHERE v.tenantId = :tenantId AND v.vendedorId = :vendedorId AND v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim")
    long countVendasVendedorPeriodo(@Param("tenantId") UUID tenantId, @Param("vendedorId") UUID vendedorId,
                                    @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
    
    @Query("SELECT v FROM Venda v WHERE v.tenantId = :tenantId AND v.status = 'CANCELADA' ORDER BY v.dataHora DESC")
    List<Venda> findVendasCanceladas(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT v.vendedorId, COUNT(v), SUM(v.total) FROM Venda v WHERE v.tenantId = :tenantId AND v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim GROUP BY v.vendedorId")
    List<Object[]> rankingVendedores(@Param("tenantId") UUID tenantId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
    
    @Query("SELECT v.metodoPagamento, COUNT(v), SUM(v.total) FROM Venda v WHERE v.tenantId = :tenantId AND v.status = 'CONCLUIDA' AND v.dataHora BETWEEN :inicio AND :fim GROUP BY v.metodoPagamento")
    List<Object[]> resumoPorMetodoPagamento(@Param("tenantId") UUID tenantId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venda v WHERE v.tenantId = :tenantId AND v.status = 'CONCLUIDA' AND v.metodoPagamento = :metodo AND v.dataHora >= :inicio AND v.dataHora <= :fim")
    BigDecimal somaPorMetodoPeriodo(@Param("tenantId") UUID tenantId, @Param("metodo") Venda.MetodoPagamento metodo, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
