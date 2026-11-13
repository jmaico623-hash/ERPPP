package com.nanobanana.repository;

import com.nanobanana.model.DevolucaoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DevolucaoItemRepository extends JpaRepository<DevolucaoItem, UUID> {
    List<DevolucaoItem> findByDevolucaoId(UUID devolucaoId);

    @Query("SELECT COALESCE(SUM(di.quantidade), 0) FROM DevolucaoItem di WHERE di.vendaItemId = :vendaItemId")
    Integer somaQuantidadeDevolvidaPorVendaItem(@Param("vendaItemId") UUID vendaItemId);

    @Query("SELECT COALESCE(SUM(di.quantidade * COALESCE(vi.custoUnitario, 0)), 0) FROM DevolucaoItem di, VendaItem vi, Devolucao d " +
           "WHERE di.devolucaoId = d.id AND di.vendaItemId = vi.id AND d.tenantId = :tenantId AND d.dataHora BETWEEN :inicio AND :fim")
    java.math.BigDecimal custoDevolvidoPeriodo(@Param("tenantId") UUID tenantId,
                                               @Param("inicio") java.time.LocalDateTime inicio,
                                               @Param("fim") java.time.LocalDateTime fim);
}
