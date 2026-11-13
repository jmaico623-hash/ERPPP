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
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.nanobanana.util.TelefoneUtil;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts = 5;

    /**
     * Faz login de um usuário por email ou telefone
     * Transacional com lock pessimista para evitar race condition nas tentativas de login
     */
    @Transactional
    public Optional<Usuario> login(String identificador, String senha, HttpSession session) {
        if (identificador == null || identificador.trim().isEmpty()) {
            return Optional.empty();
        }
        String id = identificador.trim();
        // Email e telefone são identificadores distintos: o email é comparado
        // sem distinção de maiúsculas/minúsculas; o telefone é comparado apenas
        // pelos dígitos, aceitando espaços, +258, 00258, etc.
        Optional<Usuario> usuarioOpt;
        if (id.contains("@")) {
            usuarioOpt = usuarioRepository.findByIdentificadorWithLock(id.toLowerCase(Locale.ROOT));
        } else {
            var telefones = TelefoneUtil.variantes(id);
            usuarioOpt = telefones.isEmpty()
                    ? Optional.empty()
                    : usuarioRepository.findFirstByTelefoneIn(telefones);
        }
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
            
            // Bloqueia após o limite configurado (default: 5)
            if (usuario.getTentativasFalhas() >= maxLoginAttempts) {
                usuario.setBloqueado(true);
            }
            usuarioRepository.save(usuario);
            return Optional.empty();
        }

        // Login sucesso - verifica se o tenant está ativo antes de criar a sessão
        Tenant tenant = tenantRepository.findById(usuario.getTenantId()).orElse(null);
        if (tenant == null || !tenant.isAtivo()) {
            return Optional.empty();
        }

        // Reseta tentativas
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
        session.setAttribute("usuarioTelefone", usuario.getTelefone());
        session.setAttribute("usuarioRole", usuario.getRole().name());
        session.setAttribute("tenantId", usuario.getTenantId());
    }

    /**
     * Obtém usuário logado da sessão
     * Revalida ativo/bloqueado e tenant ativo a cada acesso.
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

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = usuarioOpt.get();
        if (!usuario.isAtivo() || usuario.isBloqueado()) {
            session.invalidate();
            return Optional.empty();
        }

        Tenant tenant = tenantRepository.findById(usuario.getTenantId()).orElse(null);
        if (tenant == null || !tenant.isAtivo()) {
            session.invalidate();
            return Optional.empty();
        }

        return Optional.of(usuario);
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
    public Usuario criarUsuario(UUID tenantId, String nome, String email, String telefone, String senha,
                                Usuario.Role role, UUID criadoPor) {
        String emailNorm = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String telefoneNorm = TelefoneUtil.normalizar(telefone);

        // Verifica se email já existe globalmente (double-check)
        if (!emailNorm.isEmpty() && usuarioRepository.existsByEmailIgnoreCase(emailNorm)) {
            throw new IllegalArgumentException("Email já registrado.");
        }
        if (!telefoneNorm.isEmpty()) {
            var telefones = TelefoneUtil.variantes(telefoneNorm);
            if (usuarioRepository.existsByTelefoneIn(telefones)
                    || tenantRepository.existsByTelefoneIn(telefones)) {
                throw new IllegalArgumentException("Telefone já registrado.");
            }
        }

        Usuario usuario = new Usuario();
        usuario.setTenantId(tenantId);
        usuario.setNome(nome);
        usuario.setEmail(emailNorm.isEmpty() ? null : emailNorm);
        usuario.setTelefone(telefoneNorm.isEmpty() ? null : telefoneNorm);
        usuario.setSenha(passwordEncoder.encode(senha)); // Hash com BCrypt
        usuario.setRole(role);
        usuario.setCriadoPor(criadoPor);
        return usuarioRepository.save(usuario);
    }
}
