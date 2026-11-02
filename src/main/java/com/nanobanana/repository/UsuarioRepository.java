package com.nanobanana.repository;

import com.nanobanana.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByTenantIdAndEmail(UUID tenantId, String email);
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findByTenantId(UUID tenantId);
    List<Usuario> findByTenantIdAndRole(UUID tenantId, Usuario.Role role);
    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
    long countByTenantId(UUID tenantId);
    List<Usuario> findByTenantIdAndRoleOrderByNome(UUID tenantId, Usuario.Role role);

    /**
     * Busca usuário com lock pessimista para evitar race condition no login
     * Usado quando vai atualizar tentativasFalhas/bloqueado
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.email = :email")
    Optional<Usuario> findByEmailWithLock(@Param("email") String email);
}
