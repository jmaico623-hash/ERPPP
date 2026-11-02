# 🍌 NanoBanana ERP SaaS

> **ERP Revolucionário por Assinatura** — 350 MZN/mês com 15 dias de teste grátis

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy)

## 🚀 Deploy em 2 Minutos (Grátis)

### Passo 1: Push para o GitHub

```bash
cd /home/tiih/nanobanana-web
git init
git add .
git commit -m "🍌 NanoBanana ERP SaaS - Initial commit"
git branch -M main
git remote add origin https://github.com/SEU_USER/nanobanana-erp.git
git push -u origin main
```

### Passo 2: Deploy no Render.com

1. Acesse **[render.com](https://render.com)** e crie conta grátis
2. Clique em **"New +" → "Blueprint"**
3. Conecte seu repositório GitHub `nanobanana-erp`
4. Render detecta o `render.yaml` automaticamente
5. Clique em **"Apply"**
6. Pronto! Seu ERP estará em: `https://nanobanana-erp.onrender.com`

**Alternativa manual:** New+ → Web Service → Conecte o repo → Build: `mvn clean package -DskipTests` → Start: `java -jar target/erp-1.0.0.jar`

---

## 🔑 Credenciais

| Perfil | Email | Senha |
|---|---|---|
| **Cliente Demo (Pago)** | junior@email.com | 1234 |
| **Teste Expirado** | maria@email.com | 1234 |
| **Super Admin** | admin@nanobanana.co.mz | nano2026 |

## 💰 Modelo de Negócio

- **15 dias grátis** — registo sem cartão
- **350,00 MZN/mês** — após período de teste
- **Pagamento**: M-Pesa, e-Mola, Cash
- **Bloqueio automático** após expiração

## 📋 Funcionalidades

| Módulo | Descrição |
|---|---|
| 📊 Dashboard | Vendas do dia, fiados, valor stock |
| 🛒 PDV | Ponto de venda com busca de produtos |
| 📖 Fiado | Caderno de fiado com cobrança |
| 📦 Inventário | CRUD produtos, reabastecimento |
| ⏳ Estagnados | Produtos sem venda há +30 dias |
| 💰 Assinatura | Gestão de assinatura do cliente |
| 🛡️ Admin Panel | Painel super-admin completo |

## 🏗️ Stack

- **Spring Boot 3.2.4** + **Java 21**
- **PostgreSQL** (produção) / **H2** (dev)
- **Thymeleaf** + **CSS responsivo**
- **Maven** para build

## 📁 Estrutura

```
nanobanana-web/
├── pom.xml                          ← Maven config
├── render.yaml                      ← Deploy automático
├── schema.sql                       ← DB schema
├── deploy.sh                        ← Script local
├── src/
│   ├── java/com/nanobanana/
│   │   ├── NanoBananaApplication.java
│   │   ├── model/          ← 6 entidades JPA
│   │   ├── repository/     ← 6 repositories
│   │   ├── controller/     ← Auth, ERP, Admin
│   │   └── config/         ← DataInitializer
│   └── resources/
│       ├── application.properties   ← Multi-perfil (dev/prod)
│       ├── templates/      ← 9 páginas HTML
│       └── static/css/     ← Style responsivo
└── .gitignore
```

## 🔧 Dev Local

```bash
./deploy.sh build   # Compila
./deploy.sh start   # Inicia http://localhost:8080
./deploy.sh status  # Verifica estado
./deploy.sh stop    # Para servidor
```

## 🔄 Push-to-Deploy Workflow

```bash
# Após fazer alterações no código:
git add .
git commit -m "descrição da mudança"
git push

# Render detecta o push e faz deploy automático!
# Seu site atualiza em ~2 minutos 🚀
```
