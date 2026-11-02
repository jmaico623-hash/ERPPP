package com.nanobanana.repository;

import com.nanobanana.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findByClienteId(UUID clienteId);
}
