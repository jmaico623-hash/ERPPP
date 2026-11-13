package com.nanobanana.repository;

import com.nanobanana.model.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FornecedorRepository extends JpaRepository<Fornecedor, UUID> {
    List<Fornecedor> findByTenantIdOrderByNomeAsc(UUID tenantId);
    List<Fornecedor> findByTenantIdAndAtivoTrueOrderByNomeAsc(UUID tenantId);
    Optional<Fornecedor> findByIdAndTenantId(UUID id, UUID tenantId);
}
