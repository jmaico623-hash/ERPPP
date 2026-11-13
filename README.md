# 🍌 NanoBanana ERP SaaS

> **ERP por assinatura** — 350 MZN/mês com 15 dias de teste grátis.

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy)

## 🚀 Stack

- **Spring Boot 3.5.16** + **Java 21**
- **PostgreSQL** (produção) / **H2 persistente** (desenvolvimento)
- **Flyway** para versionamento do schema
- **Thymeleaf** + CSS responsivo (tema claro/escuro)
- **Spring Security** (BCrypt, CSRF, sessões, bloqueio por tentativas)
- **Maven Wrapper** (`./mvnw`) — não precisa instalar Maven

## 🏃 Rodar localmente

Pré-requisitos: **JDK 21**.

```bash
./mvnw spring-boot:run           # perfil dev (H2 em ./data, dados demo)
# ou
./mvnw clean package -DskipTests
java -jar target/erp-1.0.0.jar    # perfil dev por padrão
```

Acesse: <http://localhost:8080>

| Recurso | URL |
|---|---|
| Login | `/login` |
| PDV | `/pdv` |
| Dashboard | `/dashboard` |
| Painel super admin | `/admin/login` |
| Health check | `/actuator/health` |
| API JSON | `/api/produtos`, `/api/vendas`, `/api/resumo` |
| Exportações CSV | `/export/vendas.csv`, `/export/stock.csv`, `/export/fiados.csv` |

As listagens de vendas e fiados são paginadas (25 registos por página).

### Credenciais de desenvolvimento

Criadas apenas com `app.seed.enabled=true` (perfil **dev**):

| Perfil | Email | Senha |
|---|---|---|
| Dono (Mercearia) | `joao@email.com` | `1234` |
| Gerente | `ana@email.com` | `1234` |
| Vendedor | `carlos@email.com` | `1234` |
| Dono (Farmácia) | `maria.farma@email.com` | `1234` |
| Super admin | `admin@nanobanana.co.mz` | `admin123` (ou `ADMIN_PASSWORD`) |

> ⚠️ Em produção `app.seed.enabled=false`: nenhum dado demo é criado.
> O super admin só existe se `ADMIN_PASSWORD` estiver definida.

## ⚙️ Perfis e configuração

| Perfil | Banco | Seed | H2 Console |
|---|---|---|---|
| `dev` (padrão) | H2 em arquivo (`./data`) | ✅ | ✅ `/h2-console` |
| `prod` | PostgreSQL via env vars | ❌ | ❌ |
| `test` | H2 em memória | ❌ | ❌ |

### Variáveis de ambiente (produção)

| Variável | Descrição |
|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | Ativa o perfil de produção |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Conexão PostgreSQL |
| `DB_USER`, `DB_PASS` | Credenciais do banco |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Super administrador (obrigatório) |
| `SESSION_COOKIE_SECURE` | `true` atrás de HTTPS (padrão em prod) |
| `PORT` | Porta HTTP (padrão 8080) |

| `APP_MPESA_NUMBER` | Número M-Pesa mostrado na renovação |
| `APP_EMOLA_NUMBER` | Número e-Mola mostrado na renovação |
| `APP_WHATSAPP_NUMBER` | WhatsApp de suporte para comprovativos |
| `APP_SUBSCRIPTION_PRICE` | Preço mensal exibido no app/admin (padrão: `350.00`) |

Copie `.env.example` para `.env` ao usar Docker Compose.

## 🐳 Docker

```bash
cp .env.example .env
# edite DB_PASS e ADMIN_PASSWORD
docker compose up -d --build
docker compose --profile tools up -d   # inclui pgAdmin em :5050
```

## ☁️ Deploy no Render

1. Faça push do projeto para o GitHub.
2. Em **render.com** → **New +** → **Blueprint** → selecione o repositório.
3. O `render.yaml` cria o Web Service + PostgreSQL e gera `ADMIN_PASSWORD`.
4. Após o deploy, veja a senha gerada em **Environment** no painel do Render.

## 🧪 Testes

```bash
./mvnw test
```

Cobertura inclui autenticação/bloqueio de login, PDV (venda/cancelamento),
stock (entrada/baixa), administração de assinaturas e controllers.

## 📁 Estrutura

```
src/main/java/com/nanobanana/
├── config/       SecurityConfig, AdminAuthInterceptor, WebConfig, initializers
├── controller/   Auth, PDV, Stock, Fiado, Dashboard, Funcionario, Admin,
│                 Export (CSV) e Api (JSON)
├── service/      AuthService, StockService, VendaService, AdminService
├── model/        Entidades JPA
├── repository/   Spring Data JPA
└── dto/          Requests validados

src/main/resources/
├── application.properties         ← base
├── application-dev.properties     ← H2 + seed
├── application-prod.properties    ← PostgreSQL
├── db/migration/                  ← Flyway (V1, V2)
├── templates/                     ← páginas Thymeleaf
└── static/css|js/                 ← tema global (theme.css/theme.js)
```

## 🔐 Segurança

- Senhas com **BCrypt**; bloqueio após 5 tentativas (configurável).
- **CSRF** ativo em todos os formulários (Thymeleaf injeta o token).
- Painel `/admin/**` isolado por sessão própria (`AdminAuthInterceptor`).
- Credenciais e segredos vêm do ambiente — nada hardcoded em produção.
- Cookies `HttpOnly` + `SameSite=Lax` (e `Secure` em produção).

## 💰 Modelo de negócio

- **15 dias grátis** — registo sem cartão
- **350,00 MZN/mês** após o teste
- Pagamento: M-Pesa, e-Mola, Cash
- Bloqueio automático após expiração
- Renovação manual: o dono solicita pelo app em `/assinatura`; o super admin aprova em **Admin → Pagamentos pendentes**
