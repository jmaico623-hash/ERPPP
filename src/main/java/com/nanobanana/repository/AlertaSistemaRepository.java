package com.nanobanana.repository;

import com.nanobanana.model.AlertaSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaSistemaRepository extends JpaRepository<AlertaSistema, Long> {
    List<AlertaSistema> findByTenantId(UUID tenantId);
    List<AlertaSistema> findByTenantIdAndLidoFalse(UUID tenantId);
    List<AlertaSistema> findByTenantIdAndResolvidoFalse(UUID tenantId);
    List<AlertaSistema> findByTenantIdOrderByCriadoEmDesc(UUID tenantId);
    Optional<AlertaSistema> findByIdAndTenantId(Long id, UUID tenantId);
    long countByTenantIdAndLidoFalse(UUID tenantId);
    long countByTenantIdAndResolvidoFalse(UUID tenantId);
    long countByTenantIdAndSeveridade(UUID tenantId, AlertaSistema.Severidade severidade);
}
