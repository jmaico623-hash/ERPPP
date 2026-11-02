package com.nanobanana.config;

import com.nanobanana.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UsuarioRepository usuarioRepository;

    public SecurityConfig(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CSRF habilitado. Apenas o login/registro manual do AuthController é isento
        // (o formulário envia o token via Thymeleaf quando disponível).
        http.csrf(csrf -> csrf
                .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/login", "/registrar")
        );

        // Permite o H2 Console em dev (mesma origem)
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        http.authorizeHttpRequests(auth -> auth
                // Públicos
                .requestMatchers("/", "/login", "/registrar", "/css/**", "/js/**", "/images/**",
                        "/favicon.ico", "/error", "/h2-console/**").permitAll()
                // Health check público (Render/Docker)
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                // Painel do super admin: protegido pelo AdminAuthInterceptor (sessão própria)
                .requestMatchers("/admin/**").permitAll()
                // Rotas autenticadas de tenant
                .requestMatchers("/assinatura", "/dashboard", "/pdv/**", "/historico", "/vendas",
                        "/fiados", "/estoque", "/alertas", "/relatorio", "/api/**").authenticated()
                // Qualquer outra exige autenticação
                .anyRequest().authenticated()
        )
        // O login é feito manualmente pelo AuthController (sessão + Spring Security)
        .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
        )
        .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> usuarioRepository.findByEmail(email)
                .map(usuario -> org.springframework.security.core.userdetails.User.builder()
                        .username(usuario.getEmail())
                        .password(usuario.getSenha())
                        .roles(usuario.getRole().name())
                        .disabled(!usuario.isAtivo())
                        .accountLocked(usuario.isBloqueado())
                        .build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "Usuário não encontrado: " + email));
    }
}
