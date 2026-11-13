package com.nanobanana.repository;

import com.nanobanana.model.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DespesaRepository extends JpaRepository<Despesa, UUID> {
    List<Despesa> findTop50ByTenantIdOrderByDataDespesaDescCriadoEmDesc(UUID tenantId);
    List<Despesa> findByTenantIdAndDataDespesaBetweenOrderByDataDespesaDesc(UUID tenantId, LocalDate inicio, LocalDate fim);

    @Query("SELECT COALESCE(SUM(d.valor), 0) FROM Despesa d WHERE d.tenantId = :tenantId AND d.dataDespesa BETWEEN :inicio AND :fim")
    BigDecimal somaPeriodo(@Param("tenantId") UUID tenantId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
