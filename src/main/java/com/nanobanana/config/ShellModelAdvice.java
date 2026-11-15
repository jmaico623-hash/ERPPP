package com.nanobanana.config;

import com.nanobanana.model.Caixa;
import com.nanobanana.model.Tenant;
import com.nanobanana.model.Usuario;
import com.nanobanana.repository.AlertaSistemaRepository;
import com.nanobanana.repository.CaixaRepository;
import com.nanobanana.repository.TenantRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import java.util.UUID;

/**
 * Disponibiliza os dados da "moldura" da aplicação (loja, plano, caixa,
 * alertas e utilizador) para todas as páginas autenticadas.
 *
 * Os atributos usam o prefixo {@code nav} para não colidirem com os
 * atributos específicos de cada página (ex.: {@code usuario}, {@code tenant}).
 */
@ControllerAdvice
public class ShellModelAdvice {

    private final TenantRepository tenantRepository;
    private final CaixaRepository caixaRepository;
    private final AlertaSistemaRepository alertaSistemaRepository;

    public ShellModelAdvice(TenantRepository tenantRepository,
                            CaixaRepository caixaRepository,
                            AlertaSistemaRepository alertaSistemaRepository) {
        this.tenantRepository = tenantRepository;
        this.caixaRepository = caixaRepository;
        this.alertaSistemaRepository = alertaSistemaRepository;
    }

    @ModelAttribute
    public void adicionarDadosDaMoldura(Model model, HttpSession session) {
        Object tenantIdAttr = session.getAttribute("tenantId");
        if (!(tenantIdAttr instanceof UUID tenantId)) {
            return;
        }
        // Páginas do painel administrativo têm a sua própria moldura.
        if (Boolean.TRUE.equals(session.getAttribute("admin"))) {
            return;
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            return;
        }

        model.addAttribute("navNomeLoja", tenant.getNome());
        model.addAttribute("navPlanoLabel", planoLabel(tenant));
        model.addAttribute("navDiasRestantes", tenant.diasRestantes());
        model.addAttribute("navTemExpiracao", tenant.getDataExpiracao() != null);
        model.addAttribute("navCaixaAberto", caixaRepository
                .findFirstByTenantIdAndStatusOrderByDataAberturaDesc(tenantId, Caixa.Status.ABERTO)
                .isPresent());
        model.addAttribute("navAlertas", alertaSistemaRepository.countByTenantIdAndLidoFalse(tenantId));

        Object role = session.getAttribute("usuarioRole");
        model.addAttribute("navRole", role != null ? role.toString() : "");
        model.addAttribute("navRoleLabel", roleLabel(role));
        Object nome = session.getAttribute("usuarioNome");
        model.addAttribute("navUsuarioNome", nome);
        model.addAttribute("navIniciais", iniciais(nome));
    }

    private static String iniciais(Object nome) {
        if (nome == null) return "U";
        String texto = nome.toString().trim();
        if (texto.isEmpty()) return "U";
        return texto.substring(0, 1).toUpperCase();
    }

    private static String roleLabel(Object role) {
        if (role == null) return "Utilizador";
        return switch (role.toString()) {
            case "DONO" -> "Proprietário";
            case "GERENTE" -> "Gerente";
            case "VENDEDOR" -> "Vendedor";
            default -> "Utilizador";
        };
    }

    private static String planoLabel(Tenant tenant) {
        if (tenant == null || tenant.getPlano() == null || tenant.getPlano().isBlank()) return "Teste";
        return switch (tenant.getPlano().trim().toLowerCase()) {
            case "teste" -> "Teste";
            case "basico" -> "Básico";
            case "premium" -> "Premium";
            default -> tenant.getPlano();
        };
    }
}
