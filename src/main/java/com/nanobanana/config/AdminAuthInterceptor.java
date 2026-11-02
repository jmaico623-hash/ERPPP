package com.nanobanana.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Protege todas as rotas /admin/** exigindo uma sessão de super administrador.
 *
 * O super admin NÃO é um usuário de tenant, portanto não passa pelo
 * UserDetailsService do Spring Security. Esta barreira garante que apenas
 * quem autenticou em /admin/login acesse o painel.
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        HttpSession session = request.getSession(false);
        boolean isAdmin = session != null && Boolean.TRUE.equals(session.getAttribute("admin"));

        if (!isAdmin) {
            response.sendRedirect(request.getContextPath() + "/admin/login");
            return false;
        }
        return true;
    }
}
