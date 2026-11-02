package com.nanobanana.config;

import com.nanobanana.model.*;
import com.nanobanana.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (adminRepository.count() == 0) {
            Admin adminObj = new Admin("admin@nanobanana.co.mz", passwordEncoder.encode("nano2026"), "Super Admin");
            adminRepository.save(adminObj);
        }

        // Só inicializa se não houver dados
        if (usuarioRepository.count() > 0) {
            return;
        }

        log.info("🍌 Inicializando NanoBanana ERP Anti-Desvio...");

        // ============ TENANT 1: Mercearia do João ============
        Tenant tenant1 = new Tenant();
        tenant1.setNome("Mercearia do João");
        tenant1.setEmail("joao@email.com");
        tenant1.setTelefone("841234567");
        tenant1.setNui("123456789");
        tenant1.setPlano("teste");
        tenant1.setDataInicio(LocalDate.now());
        tenant1.setDataExpiracao(LocalDate.now().plusDays(15));
        tenant1 = tenantRepository.save(tenant1);

        // DONO - João
        Usuario dono1 = new Usuario();
        dono1.setTenantId(tenant1.getId());
        dono1.setNome("João Silva");
        dono1.setEmail("joao@email.com");
        dono1.setSenha(passwordEncoder.encode("1234"));
        dono1.setRole(Usuario.Role.DONO);
        dono1 = usuarioRepository.save(dono1);

        // GERENTE - Ana
        Usuario gerente1 = new Usuario();
        gerente1.setTenantId(tenant1.getId());
        gerente1.setNome("Ana Santos");
        gerente1.setEmail("ana@email.com");
        gerente1.setSenha(passwordEncoder.encode("1234"));
        gerente1.setRole(Usuario.Role.GERENTE);
        gerente1.setCriadoPor(dono1.getId());
        gerente1 = usuarioRepository.save(gerente1);

        // VENDEDOR 1 - Carlos
        Usuario vendedor1 = new Usuario();
        vendedor1.setTenantId(tenant1.getId());
        vendedor1.setNome("Carlos Pereira");
        vendedor1.setEmail("carlos@email.com");
        vendedor1.setSenha(passwordEncoder.encode("1234"));
        vendedor1.setRole(Usuario.Role.VENDEDOR);
        vendedor1.setCriadoPor(dono1.getId());
        vendedor1 = usuarioRepository.save(vendedor1);

        // VENDEDOR 2 - Maria
        Usuario vendedor2 = new Usuario();
        vendedor2.setTenantId(tenant1.getId());
        vendedor2.setNome("Maria Costa");
        vendedor2.setEmail("maria@email.com");
        vendedor2.setSenha(passwordEncoder.encode("1234"));
        vendedor2.setRole(Usuario.Role.VENDEDOR);
        vendedor2.setCriadoPor(dono1.getId());
        vendedor2 = usuarioRepository.save(vendedor2);

        // Produtos da Mercearia do João (com stock inicial)
        criarProduto(tenant1.getId(), "Cerveja 2L", "Bebidas", "7891234567890",
                new BigDecimal("80"), new BigDecimal("120"), 10, 50);
        criarProduto(tenant1.getId(), "Arroz 5kg", "Alimentos", "7891234567891",
                new BigDecimal("250"), new BigDecimal("350"), 20, 60);
        criarProduto(tenant1.getId(), "Óleo Alimentar 1L", "Alimentos", "7891234567892",
                new BigDecimal("120"), new BigDecimal("180"), 10, 35);
        criarProduto(tenant1.getId(), "Farinha de Trigo 2kg", "Alimentos", "7891234567893",
                new BigDecimal("90"), new BigDecimal("140"), 10, 25);
        criarProduto(tenant1.getId(), "Leite 1L", "Laticínios", "7891234567894",
                new BigDecimal("60"), new BigDecimal("90"), 10, 40);
        criarProduto(tenant1.getId(), "Açúcar 1kg", "Alimentos", "7891234567895",
                new BigDecimal("70"), new BigDecimal("110"), 15, 30);

        // ============ TENANT 2: Farmácia da Maria ============
        Tenant tenant2 = new Tenant();
        tenant2.setNome("Farmácia da Maria");
        tenant2.setEmail("maria.farma@email.com");
        tenant2.setTelefone("842345678");
        tenant2.setNui("987654321");
        tenant2.setPlano("teste");
        tenant2.setDataInicio(LocalDate.now());
        tenant2.setDataExpiracao(LocalDate.now().plusDays(15));
        tenant2 = tenantRepository.save(tenant2);

        // DONO - Maria
        Usuario dono2 = new Usuario();
        dono2.setTenantId(tenant2.getId());
        dono2.setNome("Maria Farmácia");
        dono2.setEmail("maria.farma@email.com");
        dono2.setSenha(passwordEncoder.encode("1234"));
        dono2.setRole(Usuario.Role.DONO);
        dono2 = usuarioRepository.save(dono2);

        // VENDEDOR - Pedro
        Usuario vendedor3 = new Usuario();
        vendedor3.setTenantId(tenant2.getId());
        vendedor3.setNome("Pedro Costa");
        vendedor3.setEmail("pedro@email.com");
        vendedor3.setSenha(passwordEncoder.encode("1234"));
        vendedor3.setRole(Usuario.Role.VENDEDOR);
        vendedor3.setCriadoPor(dono2.getId());
        vendedor3 = usuarioRepository.save(vendedor3);

        // Produtos da Farmácia
        criarProduto(tenant2.getId(), "Paracetamol 500mg", "Medicamentos", "7892234567890",
                new BigDecimal("30"), new BigDecimal("50"), 20, 100);
        criarProduto(tenant2.getId(), "Ibuprofeno 400mg", "Medicamentos", "7892234567891",
                new BigDecimal("50"), new BigDecimal("80"), 15, 80);
        criarProduto(tenant2.getId(), "Vitamina C 1g", "Suplementos", "7892234567892",
                new BigDecimal("40"), new BigDecimal("70"), 20, 60);

        log.info("✅ Dados iniciais criados com sucesso!");
        log.info("📊 Tenants: 2");
        log.info("👥 Usuários: 7");
        log.info("📦 Produtos: 9");
        log.info("\n🔐 Credenciais de teste:");
        log.info("   Tenant 1 (Mercearia):");
        log.info("   👑 Dono: joao@email.com / 1234");
        log.info("   👔 Gerente: ana@email.com / 1234");
        log.info("   💰 Vendedor: carlos@email.com / 1234");
        log.info("   💰 Vendedor: maria@email.com / 1234");
        log.info("   Tenant 2 (Farmácia):");
        log.info("   👑 Dono: maria.farma@email.com / 1234");
        log.info("   💰 Vendedor: pedro@email.com / 1234");
    }

    private void criarProduto(UUID tenantId, String nome, String categoria, String codigoBarras,
                              BigDecimal precoCusto, BigDecimal precoVenda, int stockMinimo, int stockInicial) {
        Produto produto = new Produto();
        produto.setTenantId(tenantId);
        produto.setNome(nome);
        produto.setCategoria(categoria);
        produto.setCodigoBarras(codigoBarras);
        produto.setPrecoCusto(precoCusto);
        produto.setPrecoVenda(precoVenda);
        produto.setStockMinimo(stockMinimo);
        produto.setStockAtual(stockInicial);
        produtoRepository.save(produto);
    }
}
