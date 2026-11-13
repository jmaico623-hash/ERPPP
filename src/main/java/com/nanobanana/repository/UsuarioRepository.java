package com.nanobanana.repository;

import com.nanobanana.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByTenantIdAndEmail(UUID tenantId, String email);
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);
    Optional<Usuario> findByTelefone(String telefone);
    Optional<Usuario> findByEmailOrTelefone(String email, String telefone);
    List<Usuario> findByTenantId(UUID tenantId);
    List<Usuario> findByRole(Usuario.Role role);
    List<Usuario> findByTenantIdAndRole(UUID tenantId, Usuario.Role role);
    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByTelefone(String telefone);
    boolean existsByTelefoneIn(Collection<String> telefones);
    long countByTenantId(UUID tenantId);
    List<Usuario> findByTenantIdAndRoleOrderByNome(UUID tenantId, Usuario.Role role);

    /** Pesquisa por telefone aceitando as várias formas de escrita do número. */
    Optional<Usuario> findFirstByTelefoneInOrderByCriadoEmAsc(Collection<String> telefones);

    /**
     * Busca usuário com lock pessimista para evitar race condition no login
     * Usado quando vai atualizar tentativasFalhas/bloqueado
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.email = :email")
    Optional<Usuario> findByEmailWithLock(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.telefone = :telefone")
    Optional<Usuario> findByTelefoneWithLock(@Param("telefone") String telefone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Usuario> findFirstByTelefoneIn(Collection<String> telefones);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE (LOWER(u.email) = :identificador) OR (u.telefone = :identificador)")
    Optional<Usuario> findByIdentificadorWithLock(@Param("identificador") String identificador);
}
