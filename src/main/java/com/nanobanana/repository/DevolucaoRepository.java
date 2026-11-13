package com.nanobanana.repository;

import com.nanobanana.model.Devolucao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DevolucaoRepository extends JpaRepository<Devolucao, UUID> {
    List<Devolucao> findByTenantIdAndVendaIdOrderByDataHoraDesc(UUID tenantId, UUID vendaId);
    List<Devolucao> findTop50ByTenantIdOrderByDataHoraDesc(UUID tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(d.valorTotal), 0) FROM Devolucao d WHERE d.tenantId = :tenantId AND d.dataHora BETWEEN :inicio AND :fim")
    java.math.BigDecimal somaPeriodo(@org.springframework.data.repository.query.Param("tenantId") UUID tenantId,
                                     @org.springframework.data.repository.query.Param("inicio") java.time.LocalDateTime inicio,
                                     @org.springframework.data.repository.query.Param("fim") java.time.LocalDateTime fim);
}
