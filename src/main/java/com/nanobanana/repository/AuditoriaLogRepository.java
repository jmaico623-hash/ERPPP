package com.nanobanana.repository;

import com.nanobanana.model.AuditoriaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Long> {
    List<AuditoriaLog> findByTenantId(UUID tenantId);
    List<AuditoriaLog> findByTenantIdOrderByDataHoraDesc(UUID tenantId);
    List<AuditoriaLog> findByTenantIdAndUsuarioId(UUID tenantId, UUID usuarioId);
    List<AuditoriaLog> findByTenantIdAndAcao(UUID tenantId, AuditoriaLog.Acao acao);
    List<AuditoriaLog> findByTenantIdAndDataHoraBetween(UUID tenantId, LocalDateTime inicio, LocalDateTime fim);
}
