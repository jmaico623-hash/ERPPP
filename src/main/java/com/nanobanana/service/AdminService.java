package com.nanobanana.service;

import com.nanobanana.model.Pagamento;
import com.nanobanana.model.Tenant;
import com.nanobanana.model.Usuario;
import com.nanobanana.model.Venda;
import com.nanobanana.model.Fiado;
import com.nanobanana.model.AuditoriaLog;
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

        boolean jaConfirmado = pagamento.isConfirmado();
        if (jaConfirmado) {
            return tenantRepository.findById(pagamento.getClienteId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente do pagamento não encontrado"));
        }

        pagamento.confirmar();
        pagamentoRepository.save(pagamento);

        Tenant tenant = tenantRepository.findById(pagamento.getClienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente do pagamento não encontrado"));

        if (!"premium".equals(tenant.getPlano())) {
            tenant.setPlano("pago");
        }
        tenant.setAtivo(true);
        tenant.setDataExpiracao(LocalDate.now().plusMonths(1));
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant ativarCliente(UUID tenantId) {
        Tenant tenant = buscarTenant(tenantId);
        tenant.setAtivo(true);
        if (!"premium".equals(tenant.getPlano())) {
            tenant.setPlano("pago");
        }
        if (tenant.getDataExpiracao() == null || tenant.expirado()) {
            tenant.setDataExpiracao(LocalDate.now().plusMonths(1));
        }
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant desativarCliente(UUID tenantId) {
        Tenant tenant = buscarTenant(tenantId);
        tenant.setDataExpiracao(LocalDate.now().minusDays(1));
        tenant.setAtivo(false);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant estenderTeste(UUID tenantId, int dias) {
        Tenant tenant = buscarTenant(tenantId);
        tenant.setAtivo(true);
        tenant.setDataExpiracao(LocalDate.now().plusDays(dias));
        return tenantRepository.save(tenant);
    }

    /**
     * Redefine a senha do usuário DONO do tenant.
     */
    @Transactional
    public void resetSenhaDono(UUID tenantId, String novaSenha) {
        if (novaSenha == null || novaSenha.length() < 8) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos 8 caracteres");
        }

        Usuario dono = usuarioRepository.findByTenantIdAndRole(tenantId, Usuario.Role.DONO)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cliente sem usuário DONO"));

        dono.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(dono);
    }

    /**
     * Remove os dados pessoais do cliente sem destruir o histórico financeiro.
     * O tenant e os registos ligados a ele são anonimizados e desativados.
     */
    @Transactional
    public void apagarCliente(UUID tenantId) {
        Tenant tenant = buscarTenant(tenantId);
        String deletedEmail = "apagado-" + UUID.randomUUID() + "@deleted.local";

        List<Venda> vendas = vendaRepository.findByTenantId(tenantId);
        for (Venda venda : vendas) {
            if (venda.getNomeClienteFiado() != null) {
                venda.setNomeClienteFiado("Cliente removido");
            }
            venda.setTelefoneClienteFiado(null);
        }
        if (!vendas.isEmpty()) {
            vendaRepository.saveAll(vendas);
        }

        List<Fiado> fiados = fiadoRepository.findByTenantId(tenantId);
        for (Fiado fiado : fiados) {
            fiado.setNomeCliente("Cliente removido");
            fiado.setTelefoneCliente(null);
        }
        if (!fiados.isEmpty()) {
            fiadoRepository.saveAll(fiados);
        }

        List<Usuario> usuarios = usuarioRepository.findByTenantId(tenantId);
        for (Usuario usuario : usuarios) {
            usuario.setNome("Usuário removido");
            usuario.setEmail("apagado-" + UUID.randomUUID() + "@deleted.local");
            usuario.setTelefone(null);
            usuario.setSenha(passwordEncoder.encode(UUID.randomUUID().toString()));
            usuario.setAtivo(false);
            usuario.setBloqueado(true);
        }
        if (!usuarios.isEmpty()) {
            usuarioRepository.saveAll(usuarios);
        }

        List<Pagamento> pagamentos = pagamentoRepository.findByClienteId(tenantId);
        for (Pagamento pagamento : pagamentos) {
            pagamento.setReferencia(null);
        }
        if (!pagamentos.isEmpty()) {
            pagamentoRepository.saveAll(pagamentos);
        }

        tenant.setNome("Cliente removido");
        tenant.setEmail(deletedEmail);
        tenant.setNui(null);
        tenant.setTelefone(null);
        tenant.setEndereco(null);
        tenant.setPlano("expirado");
        tenant.setAtivo(false);
        tenant.setDataExpiracao(LocalDate.now().minusDays(1));
        tenant.setAlertasAtivos(false);
        tenant.setAuditoriaStock(false);
        tenantRepository.save(tenant);

        AuditoriaLog log = new AuditoriaLog();
        log.setTenantId(tenantId);
        log.setAcao(AuditoriaLog.Acao.DELETE);
        log.setTabelaAfetada("tenants");
        log.setRegistroId(tenantId);
        log.setDadosNovos("Cliente anonimizado e desativado");
        auditoriaLogRepository.save(log);
    }

    private Tenant buscarTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
    }
}
