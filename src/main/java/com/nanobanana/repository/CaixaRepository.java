package com.nanobanana.repository;

import com.nanobanana.model.Caixa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaixaRepository extends JpaRepository<Caixa, UUID> {
    Optional<Caixa> findFirstByTenantIdAndStatusOrderByDataAberturaDesc(UUID tenantId, Caixa.Status status);
    Optional<Caixa> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Caixa> findTop20ByTenantIdOrderByDataAberturaDesc(UUID tenantId);
}
