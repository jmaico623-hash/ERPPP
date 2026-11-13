package com.nanobanana.service;

import com.nanobanana.model.Caixa;
import com.nanobanana.model.MovimentoCaixa;
import com.nanobanana.model.Usuario;
import com.nanobanana.model.Venda;
import com.nanobanana.repository.CaixaRepository;
import com.nanobanana.repository.MovimentoCaixaRepository;
import com.nanobanana.repository.VendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CaixaService {

    private final CaixaRepository caixaRepository;
    private final MovimentoCaixaRepository movimentoRepository;
    private final VendaRepository vendaRepository;

    public CaixaService(CaixaRepository caixaRepository,
                        MovimentoCaixaRepository movimentoRepository,
                        VendaRepository vendaRepository) {
        this.caixaRepository = caixaRepository;
        this.movimentoRepository = movimentoRepository;
        this.vendaRepository = vendaRepository;
    }

    public Optional<Caixa> getCaixaAberto(UUID tenantId) {
        return caixaRepository.findFirstByTenantIdAndStatusOrderByDataAberturaDesc(tenantId, Caixa.Status.ABERTO);
    }

    public List<Caixa> listarRecentes(UUID tenantId) {
        return caixaRepository.findTop20ByTenantIdOrderByDataAberturaDesc(tenantId);
    }

    public List<MovimentoCaixa> listarMovimentos(UUID caixaId) {
        return movimentoRepository.findByCaixaIdOrderByDataHoraDesc(caixaId);
    }

    @Transactional
    public Caixa abrirCaixa(UUID tenantId, Usuario usuario, BigDecimal valorAbertura, String observacao) {
        if (valorAbertura == null || valorAbertura.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor de abertura inválido.");
        }
        if (getCaixaAberto(tenantId).isPresent()) {
            throw new IllegalStateException("Já existe um caixa aberto. Feche o caixa atual antes de abrir outro.");
        }
        Caixa caixa = new Caixa();
        caixa.setTenantId(tenantId);
        caixa.setUsuarioAberturaId(usuario.getId());
        caixa.setValorAbertura(valorAbertura);
        caixa.setObservacaoAbertura(observacao != null && !observacao.trim().isEmpty() ? observacao.trim() : null);
        caixa.setStatus(Caixa.Status.ABERTO);
        caixa.setDataAbertura(LocalDateTime.now());
        return caixaRepository.save(caixa);
    }

    @Transactional
    public MovimentoCaixa registrarMovimento(UUID tenantId, Usuario usuario, MovimentoCaixa.Tipo tipo, BigDecimal valor, String motivo) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do movimento deve ser maior que zero.");
        }
        Caixa caixa = getCaixaAberto(tenantId)
                .orElseThrow(() -> new IllegalStateException("Não há caixa aberto. Abra o caixa antes de registar movimentos."));
        MovimentoCaixa mov = new MovimentoCaixa();
        mov.setCaixaId(caixa.getId());
        mov.setTenantId(tenantId);
        mov.setUsuarioId(usuario.getId());
        mov.setTipo(tipo);
        mov.setValor(valor);
        mov.setMotivo(motivo != null && !motivo.trim().isEmpty() ? motivo.trim() : null);
        mov.setDataHora(LocalDateTime.now());
        return movimentoRepository.save(mov);
    }

    @Transactional
    public Caixa fecharCaixa(UUID tenantId, Usuario usuario, BigDecimal valorInformado, String observacao) {
        if (valorInformado == null || valorInformado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor de fecho inválido.");
        }
        Caixa caixa = getCaixaAberto(tenantId)
                .orElseThrow(() -> new IllegalStateException("Não há caixa aberto para fechar."));
        LocalDateTime fim = LocalDateTime.now();
        ResumoCaixa resumo = calcularResumo(caixa, fim);

        caixa.setUsuarioFechoId(usuario.getId());
        caixa.setDataFecho(fim);
        caixa.setValorFechoInformado(valorInformado);
        caixa.setValorEsperado(resumo.esperadoEmCaixa());
        caixa.setDiferenca(valorInformado.subtract(resumo.esperadoEmCaixa()));
        caixa.setObservacaoFecho(observacao != null && !observacao.trim().isEmpty() ? observacao.trim() : null);
        caixa.setStatus(Caixa.Status.FECHADO);
        return caixaRepository.save(caixa);
    }

    public ResumoCaixa calcularResumo(Caixa caixa, LocalDateTime fim) {
        LocalDateTime inicio = caixa.getDataAbertura();
        LocalDateTime ate = fim != null ? fim : LocalDateTime.now();
        UUID tenantId = caixa.getTenantId();

        BigDecimal vendasCash = nz(vendaRepository.somaPorMetodoPeriodo(tenantId, Venda.MetodoPagamento.CASH, inicio, ate));
        BigDecimal vendasMpesa = nz(vendaRepository.somaPorMetodoPeriodo(tenantId, Venda.MetodoPagamento.MPESA, inicio, ate));
        BigDecimal vendasEmola = nz(vendaRepository.somaPorMetodoPeriodo(tenantId, Venda.MetodoPagamento.EMOLA, inicio, ate));
        BigDecimal vendasCartao = nz(vendaRepository.somaPorMetodoPeriodo(tenantId, Venda.MetodoPagamento.CARTAO, inicio, ate));
        BigDecimal vendasFiado = nz(vendaRepository.somaPorMetodoPeriodo(tenantId, Venda.MetodoPagamento.FIADO, inicio, ate));

        BigDecimal sangrias = nz(movimentoRepository.somaPorTipo(caixa.getId(), MovimentoCaixa.Tipo.SANGRIA));
        BigDecimal reforcos = nz(movimentoRepository.somaPorTipo(caixa.getId(), MovimentoCaixa.Tipo.REFORCO));

        BigDecimal esperadoCaixa = nz(caixa.getValorAbertura()).add(vendasCash).add(reforcos).subtract(sangrias);
        BigDecimal totalVendas = vendasCash.add(vendasMpesa).add(vendasEmola).add(vendasCartao).add(vendasFiado);

        return new ResumoCaixa(vendasCash, vendasMpesa, vendasEmola, vendasCartao, vendasFiado, totalVendas, sangrias, reforcos, esperadoCaixa);
    }

    public ResumoCaixa calcularResumoAtual(UUID tenantId) {
        return getCaixaAberto(tenantId).map(c -> calcularResumo(c, LocalDateTime.now())).orElse(null);
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    public record ResumoCaixa(
            BigDecimal vendasCash,
            BigDecimal vendasMpesa,
            BigDecimal vendasEmola,
            BigDecimal vendasCartao,
            BigDecimal vendasFiado,
            BigDecimal totalVendas,
            BigDecimal sangrias,
            BigDecimal reforcos,
            BigDecimal esperadoEmCaixa
    ) {}
}
