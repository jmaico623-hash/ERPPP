package com.nanobanana.config;

import com.nanobanana.model.Admin;
import com.nanobanana.repository.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o super administrador a partir das variáveis de ambiente.
 *
 * Em produção é obrigatório definir ADMIN_PASSWORD. Sem essa variável
 * nenhum administrador é criado (evita credenciais padrão).
 */
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@nanobanana.co.mz}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.nome:Super Admin}")
    private String adminNome;

    public AdminInitializer(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_PASSWORD não definido: super administrador não foi criado.");
            return;
        }

        var existente = adminRepository.findByEmail(adminEmail);
        if (existente.isPresent()) {
            Admin admin = existente.get();
            if (!passwordEncoder.matches(adminPassword, admin.getSenha())) {
                admin.setSenha(passwordEncoder.encode(adminPassword));
                adminRepository.save(admin);
                log.info("Senha do super administrador sincronizada a partir de ADMIN_PASSWORD para {}", adminEmail);
            }
            return;
        }

        if (adminRepository.count() > 0) {
            log.warn("Já existe um administrador com outro email; nenhum administrador foi criado para {}", adminEmail);
            return;
        }

        Admin admin = new Admin(adminEmail, passwordEncoder.encode(adminPassword), adminNome);
        adminRepository.save(admin);
        log.info("Super administrador criado para {}", adminEmail);

        if ("admin123".equals(adminPassword)) {
            log.warn("ADMIN_PASSWORD está usando o valor padrão de desenvolvimento. Troque em produção!");
        }
    }
}
