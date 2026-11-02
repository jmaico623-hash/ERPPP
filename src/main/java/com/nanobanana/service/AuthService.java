package com.nanobanana.service;

import com.nanobanana.model.Usuario;
import com.nanobanana.model.Tenant;
import com.nanobanana.repository.UsuarioRepository;
import com.nanobanana.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Faz login de um usuário
     * Transacional com lock pessimista para evitar race condition nas tentativas de login
     */
    @Transactional
    public Optional<Usuario> login(String email, String senha, HttpSession session) {
        // Busca usuário por email COM LOCK PESSIMISTA
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailWithLock(email);
        
        if (usuarioOpt.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = usuarioOpt.get();

        // Verifica se está bloqueado
        if (usuario.isBloqueado()) {
            return Optional.empty();
        }

        // Verifica se está ativo
        if (!usuario.isAtivo()) {
            return Optional.empty();
        }

        // Verifica senha com BCrypt
        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            // Incrementa tentativas falhas (atomic com lock)
            usuario.setTentativasFalhas(usuario.getTentativasFalhas() + 1);
            
            // Bloqueia após 5 tentativas
            if (usuario.getTentativasFalhas() >= 5) {
                usuario.setBloqueado(true);
            }
            usuarioRepository.save(usuario);
            return Optional.empty();
        }

        // Login sucesso - reseta tentativas
        usuario.setTentativasFalhas(0);
        usuario.setUltimoLogin(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // Salva na sessão
        salvarNaSessao(usuario, session);

        return Optional.of(usuario);
    }

    /**
     * Salva dados do usuário na sessão
     */
    public void salvarNaSessao(Usuario usuario, HttpSession session) {
        session.setAttribute("usuarioId", usuario.getId());
        session.setAttribute("usuarioNome", usuario.getNome());
        session.setAttribute("usuarioEmail", usuario.getEmail());
        session.setAttribute("usuarioRole", usuario.getRole().name());
        session.setAttribute("tenantId", usuario.getTenantId());
    }

    /**
     * Obtém usuário logado da sessão
     */
    public Optional<Usuario> getUsuarioLogado() {
        HttpSession session = getSession();
        if (session == null) {
            return Optional.empty();
        }
        UUID usuarioId = (UUID) session.getAttribute("usuarioId");
        
        if (usuarioId == null) {
            return Optional.empty();
        }

        return usuarioRepository.findById(usuarioId);
    }

    /**
     * Obtém tenantId do usuário logado
     */
    public UUID getTenantIdLogado() {
        HttpSession session = getSession();
        if (session == null) {
            return null;
        }
        try {
            return (UUID) session.getAttribute("tenantId");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verifica se usuário está logado
     */
    public boolean estaLogado() {
        try {
            return getTenantIdLogado() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica se usuário é DONO
     */
    public boolean isDono() {
        try {
            return getUsuarioLogado().map(Usuario::podeGerirStock).orElse(false);
        } catch (Exception e) { return false; }
    }

    /**
     * Verifica se usuário é DONO ou GERENTE
     */
    public boolean podeVerRelatorios() {
        try {
            return getUsuarioLogado().map(Usuario::podeVerRelatorios).orElse(false);
        } catch (Exception e) { return false; }
    }

    /**
     * Logout
     */
    public void logout() {
        HttpSession session = getSession();
        session.invalidate();
    }

    /**
     * Obtém a sessão HTTP atual
     */
    private HttpSession getSession() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            return attrs.getRequest().getSession(false);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Cria um novo usuário (apenas DONO pode)
     */
    @Transactional
    public Usuario criarUsuario(UUID tenantId, String nome, String email, String senha, Usuario.Role role, UUID criadoPor) {
        // Verifica se email já existe no tenant (double-check)
        if (usuarioRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new IllegalArgumentException("Email já registrado nesta loja.");
        }
        Usuario usuario = new Usuario();
        usuario.setTenantId(tenantId);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(senha)); // Hash com BCrypt
        usuario.setRole(role);
        usuario.setCriadoPor(criadoPor);
        return usuarioRepository.save(usuario);
    }
}
