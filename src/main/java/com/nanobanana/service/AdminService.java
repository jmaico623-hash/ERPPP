package com.nanobanana.service;

import com.nanobanana.model.Pagamento;
import com.nanobanana.model.Tenant;
import com.nanobanana.model.Usuario;
import com.nanobanana.model.Venda;
import com.nanobanana.repository.AlertaSistemaRepository;
import com.nanobanana.repository.AuditoriaLogRepository;
import com.nanobanana.repository.BaixaStockRepository;
import com.nanobanana.repository.EntradaStockRepository;
import com.nanobanana.repository.FiadoRepository;
import com.nanobanana.repository.PagamentoRepository;
import com.nanobanana.repository.ProdutoRepository;
import com.nanobanana.repository.TenantRepository;
import com.nanobanana.repository.UsuarioRepository;
import com.nanobanana.repository.VendaItemRepository;
import com.nanobanana.repository.VendaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Operações administrativas de assinatura/clientes do super admin.
 */
@Service
public class AdminService {

    private final TenantRepository tenantRepository;
    private final PagamentoRepository pagamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VendaRepository vendaRepository;
    private final VendaItemRepository vendaItemRepository;
    private final FiadoRepository fiadoRepository;
    private final ProdutoRepository produtoRepository;
    private final EntradaStockRepository entradaStockRepository;
    private final BaixaStockRepository baixaStockRepository;
    private final AlertaSistemaRepository alertaSistemaRepository;
    private final AuditoriaLogRepository auditoriaLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(TenantRepository tenantRepository,
                        PagamentoRepository pagamentoRepository,
                        UsuarioRepository usuarioRepository,
                        VendaRepository vendaRepository,
                        VendaItemRepository vendaItemRepository,
                        FiadoRepository fiadoRepository,
                        ProdutoRepository produtoRepository,
                        EntradaStockRepository entradaStockRepository,
                        BaixaStockRepository baixaStockRepository,
                        AlertaSistemaRepository alertaSistemaRepository,
                        AuditoriaLogRepository auditoriaLogRepository,
                        PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.vendaRepository = vendaRepository;
        this.vendaItemRepository = vendaItemRepository;
        this.fiadoRepository = fiadoRepository;
        this.produtoRepository = produtoRepository;
        this.entradaStockRepository = entradaStockRepository;
        this.baixaStockRepository = baixaStockRepository;
        this.alertaSistemaRepository = alertaSistemaRepository;
        this.auditoriaLogRepository = auditoriaLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Tenant confirmarPagamento(Long pagamentoId) {
        Pagamento pagamento = pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado"));

        pagamento.confirmar();
        pagamentoRepository.save(pagamento);

        Tenant tenant = tenantRepository.findById(pagamento.getClienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente do pagamento não encontrado"));

        tenant.setPlano("pago");
        tenant.setAtivo(true);
        tenant.setDataExpiracao(LocalDate.now().plusMonths(1));
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant ativarCliente(UUID tenantId) {
        Tenant tenant = buscarTenant(tenantId);
        tenant.setPlano("pago");
        tenant.setAtivo(true);
        tenant.setDataExpiracao(LocalDate.now().plusMonths(1));
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant desativarCliente(UUID tenantId) {
        Tenant tenant = buscarTenant(tenantId);
        tenant.setPlano("expirado");
        tenant.setDataExpiracao(LocalDate.now().minusDays(1));
        tenant.setAtivo(false);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant estenderTeste(UUID tenantId, int dias) {
        Tenant tenant = buscarTenant(tenantId);
        tenant.setPlano("teste");
        tenant.setAtivo(true);
        tenant.setDataExpiracao(LocalDate.now().plusDays(dias));
        return tenantRepository.save(tenant);
    }

    /**
     * Redefine a senha do usuário DONO do tenant.
     */
    @Transactional
    public void resetSenhaDono(UUID tenantId, String novaSenha) {
        if (novaSenha == null || novaSenha.isBlank()) {
            throw new IllegalArgumentException("A nova senha é obrigatória");
        }

        Usuario dono = usuarioRepository.findByTenantIdAndRole(tenantId, Usuario.Role.DONO)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cliente sem usuário DONO"));

        dono.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(dono);
    }

    /**
     * Remove um cliente e todos os dados relacionados, respeitando as FKs.
     */
    @Transactional
    public void apagarCliente(UUID tenantId) {
        Tenant tenant = buscarTenant(tenantId);

        List<Venda> vendas = vendaRepository.findByTenantId(tenantId);
        if (!vendas.isEmpty()) {
            List<UUID> vendaIds = vendas.stream().map(Venda::getId).toList();
            vendaItemRepository.deleteAll(vendaItemRepository.findByVendaIdIn(vendaIds));
        }

        fiadoRepository.deleteAll(fiadoRepository.findByTenantId(tenantId));
        vendaRepository.deleteAll(vendas);

        baixaStockRepository.deleteAll(baixaStockRepository.findByTenantId(tenantId));
        entradaStockRepository.deleteAll(entradaStockRepository.findByTenantId(tenantId));
        alertaSistemaRepository.deleteAll(alertaSistemaRepository.findByTenantId(tenantId));
        auditoriaLogRepository.deleteAll(auditoriaLogRepository.findByTenantId(tenantId));
        produtoRepository.deleteAll(produtoRepository.findByTenantId(tenantId));

        List<Pagamento> pagamentos = pagamentoRepository.findAll()
                .stream()
                .filter(p -> tenantId.equals(p.getClienteId()))
                .toList();
        pagamentoRepository.deleteAll(pagamentos);

        usuarioRepository.deleteAll(usuarioRepository.findByTenantId(tenantId));

        tenantRepository.delete(tenant);
    }

    private Tenant buscarTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
    }
}
