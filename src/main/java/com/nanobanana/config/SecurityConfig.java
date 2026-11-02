package com.nanobanana.config;

import com.nanobanana.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
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
        // CSRF com token exposto via atributo para Thymeleaf (_csrf)
        // Desabilitado para /login e /registrar (login manual do AuthController)
        http.csrf(csrf -> csrf
                .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/login", "/registrar")
        );

        // Permitir frames (H2 Console em dev)
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        http.authorizeHttpRequests(auth -> auth
                // Públicos (incluindo POST /login e POST /registrar do controller manual)
                .requestMatchers("/login", "/registrar", "/css/**", "/js/**", "/images/**", "/", "/h2-console/**").permitAll()
                // Rotas autenticadas
                .requestMatchers("/assinatura", "/dashboard", "/pdv/**", "/historico", "/vendas",
                        "/fiados", "/estoque", "/alertas", "/relatorio").authenticated()
                // Admin apenas DONO
                .requestMatchers("/admin/**").hasRole("DONO")
                // Qualquer outra exige autenticação
                .anyRequest().authenticated()
        )
        // NÃO usar .formLogin() aqui — o AuthController faz login manual via sessão
        // Spring Security só protege as rotas; o login continua no AuthController
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
                .map(usuario -> User.builder()
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
