package com.nanobanana.controller;

import com.nanobanana.model.*;
import com.nanobanana.repository.*;
import com.nanobanana.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * Lista todos os fiados
     */
    @GetMapping
    public String listarFiados(Model model) {
        var usuarioOpt = authService.getUsuarioLogado();
        if (usuarioOpt.isEmpty()) return "redirect:/login";

        Usuario usuario = usuarioOpt.get();
        UUID tenantId = authService.getTenantIdLogado();

        List<Fiado> fiados = fiadoRepository.findByTenantIdOrderByDataCriacaoDesc(tenantId);
        BigDecimal totalFiado = fiadoRepository.totalFiadoNaoPago(tenantId);
        long fiadosNaoPagos = fiadoRepository.countByTenantIdAndPago(tenantId, false);

        model.addAttribute("usuario", usuario);
        model.addAttribute("fiados", fiados);
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
            fiado.setPago(true);
            fiado.setDataPagamento(LocalDateTime.now());
            fiadoRepository.save(fiado);

            // Atualizar venda associada se existir
            vendaRepository.findById(fiado.getVendaId()).ifPresent(venda -> {
                // Fiado pago - registamos que foi quitado
            });

            redirectAttrs.addFlashAttribute("sucesso", "Fiado de " + fiado.getNomeCliente() + " marcado como pago!");
        }

        return "redirect:/fiados";
    }
}
