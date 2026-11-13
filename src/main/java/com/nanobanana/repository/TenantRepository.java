package com.nanobanana.repository;

import com.nanobanana.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByEmail(String email);
    Optional<Tenant> findByEmailIgnoreCase(String email);
    Optional<Tenant> findByTelefone(String telefone);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByTelefone(String telefone);
    boolean existsByTelefoneIn(Collection<String> telefones);
}
