package com.nanobanana.controller;

import com.nanobanana.model.*;
import com.nanobanana.repository.*;
import com.nanobanana.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CONTROLLER DE FIADOS
 * Vendedores podem registrar e cobrar fiados
 * Dono vê todos os fiados da loja
 */
@Controller
@RequestMapping("/fiados")
public class FiadoController {

    @Autowired
    private AuthService authService;

    @Autowired
    private FiadoRepository fiadoRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    /**
     * Lista todos os fiados (paginado, 25 por página).
     */
    @GetMapping
    public String listarFiados(@RequestParam(defaultValue = "0") int page, Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        Page<Fiado> fiados = fiadoRepository.findByTenantIdAndCanceladoFalse(tenantId,
                PageRequest.of(Math.max(page, 0), 25, Sort.by(Sort.Direction.DESC, "dataCriacao")));
        BigDecimal totalFiado = fiadoRepository.totalFiadoNaoPago(tenantId);
        long fiadosNaoPagos = fiadoRepository.countByTenantIdAndPagoAndCanceladoFalse(tenantId, false);

        model.addAttribute("usuario", usuario);
        model.addAttribute("fiados", fiados.getContent());
        model.addAttribute("pagina", fiados);
        model.addAttribute("totalFiado", totalFiado != null ? totalFiado : BigDecimal.ZERO);
        model.addAttribute("fiadosNaoPagos", fiadosNaoPagos);

        return "fiados";
    }

    /**
     * Marcar fiado como pago
     */
    @PostMapping("/{id}/pagar")
    public String pagarFiado(@PathVariable UUID id, RedirectAttributes redirectAttrs) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        Optional<Fiado> fiadoOpt = fiadoRepository.findById(id);
        if (fiadoOpt.isPresent() && fiadoOpt.get().getTenantId().equals(tenantId)) {
            Fiado fiado = fiadoOpt.get();
            if (fiado.isCancelado()) {
                redirectAttrs.addFlashAttribute("erro", "Este fiado foi cancelado.");
                return "redirect:/fiados";
            }
            if (fiado.isPago()) {
                redirectAttrs.addFlashAttribute("sucesso", "Este fiado já estava marcado como pago.");
                return "redirect:/fiados";
            }
            fiado.setPago(true);
            fiado.setDataPagamento(LocalDateTime.now());
            fiadoRepository.save(fiado);

            AuditoriaLog log = new AuditoriaLog();
            log.setTenantId(tenantId);
            log.setUsuarioId(usuario.getId());
            log.setAcao(AuditoriaLog.Acao.UPDATE);
            log.setTabelaAfetada("fiados");
            log.setRegistroId(fiado.getId());
            log.setDadosNovos("Fiado marcado como pago. Valor: " + fiado.getValor() + " MZN");
            auditoriaLogRepository.save(log);

            redirectAttrs.addFlashAttribute("sucesso", "Fiado de " + fiado.getNomeCliente() + " marcado como pago!");
        }

        return "redirect:/fiados";
    }
}
