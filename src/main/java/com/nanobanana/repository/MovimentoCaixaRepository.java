package com.nanobanana.repository;

import com.nanobanana.model.MovimentoCaixa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface MovimentoCaixaRepository extends JpaRepository<MovimentoCaixa, UUID> {
    List<MovimentoCaixa> findByCaixaIdOrderByDataHoraDesc(UUID caixaId);

    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM MovimentoCaixa m WHERE m.caixaId = :caixaId AND m.tipo = :tipo")
    BigDecimal somaPorTipo(@Param("caixaId") UUID caixaId, @Param("tipo") MovimentoCaixa.Tipo tipo);
}
