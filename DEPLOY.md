# 🚀 Guia de Deploy - NanoBanana ERP

Deploy em produção usando **Render.com** (recomendado) ou **Docker**.

## ✅ Pré-requisitos

- Repositório no GitHub
- Conta gratuita em [render.com](https://render.com)
- JDK 21 apenas para rodar localmente (o Render compila com o `mvnw` do projeto)

## 1️⃣ Enviar o código para o GitHub

```bash
git init
git add .
git commit -m "NanoBanana ERP"
git branch -M master
git remote add origin https://github.com/SEU_USER/nanobanana-erp.git
git push -u origin master
```

## 2️⃣ Deploy no Render (Blueprint)

1. Acesse **render.com** → **New +** → **Blueprint**.
2. Selecione o repositório `nanobanana-erp`.
3. O Render lê o `render.yaml` e cria:
   - **Web Service Docker** (build pelo `Dockerfile`, plano `1c-2g`)
   - **PostgreSQL** pago (`nanobanana-db`, plano `0.1c-256mb`, 10 GB)
   - Variáveis de conexão (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`)
   - `ADMIN_PASSWORD` gerada automaticamente
4. Clique em **Apply** e aguarde o build (~3–5 min).
5. Faça push para `master`; o Render publica automaticamente **somente após o CI ficar verde** (`autoDeployTrigger: checksPass`).

### Variáveis de ambiente

| Variável | Descrição |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Conexão PostgreSQL |
| `DB_USER`, `DB_PASS` | Credenciais do banco |
| `ADMIN_EMAIL` | Email do super admin |
| `ADMIN_PASSWORD` | Senha do super admin (**obrigatória em produção**) |
| `SESSION_COOKIE_SECURE` | `true` atrás de HTTPS |
| `PORT` | Porta HTTP (o Render injeta automaticamente) |

> A senha do super admin aparece em **Environment** no painel do Render.
> Guarde-a: ela não é exibida novamente.

### Deploy manual (sem Blueprint)

- **Build**: `./mvnw clean package -DskipTests`
- **Start**: `java -jar target/erp-1.0.0.jar`
- **Health check**: `/actuator/health`
- Crie um **PostgreSQL** e preencha as variáveis `DB_*` acima.

## 3️⃣ Push-to-Deploy

```bash
git add .
git commit -m "minha alteracao"
git push
```

O push na `master` inicia o CI (`GitHub Actions`). Se os checks passarem, o Render publica automaticamente porque está configurado com `autoDeployTrigger: checksPass`.

## 🐳 Deploy com Docker

```bash
cp .env.example .env
# edite DB_PASS, ADMIN_PASSWORD e (opcional) SESSION_COOKIE_SECURE
docker compose up -d --build
```

- ERP: <http://localhost:8080>
- pgAdmin (opcional): `docker compose --profile tools up -d` → <http://localhost:5050>

O container usa o perfil `prod`, PostgreSQL e Flyway (schema aplicado no start).

## 🔗 URLs após o deploy

| Recurso | URL |
|---|---|
| ERP | `https://SEU-SERVICO.onrender.com` |
| Login | `/login` |
| Painel super admin | `/admin/login` |
| Health check | `/actuator/health` |
| API JSON | `/api/produtos`, `/api/vendas`, `/api/resumo` |
| Exportações | `/export/vendas.csv`, `/export/stock.csv`, `/export/fiados.csv` |

## 🔑 Credenciais

- **Super admin**: `ADMIN_EMAIL` / `ADMIN_PASSWORD` definidos no ambiente.
- **Dados demo**: existem apenas no perfil `dev` (`app.seed.enabled=true`).
- Em produção **não há senhas padrão**.

## 🧪 Verificação pós-deploy

```bash
curl -s https://SEU-SERVICO.onrender.com/actuator/health
# {"status":"UP"}
```

Depois acesse `/admin/login` com as credenciais de super admin e confirme que o
painel abre. Se o login falhar, revise `ADMIN_EMAIL`/`ADMIN_PASSWORD` e os logs
do serviço.

## 🛠️ Troubleshooting

| Problema | Solução |
|---|---|
| Build falha | Veja os logs em **Events / Logs** no Render |
| 502 Bad Gateway | Aguarde 1–2 min (primeiro start é mais lento) |
| Erro de banco | Confira `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS` |
| Login admin falha | Confirme `ADMIN_PASSWORD`; o admin só é criado se ela existir |
| Sessão cai após login | Em HTTPS, use `SESSION_COOKIE_SECURE=true` |
| Dados somem | Em produção use PostgreSQL (`prod`), nunca H2 |
| Push rejeitado | `git config --global credential.helper store` |

## 📌 Observações

- O schema é versionado por **Flyway** (`src/main/resources/db/migration`).
  Nunca use `ddl-auto=update` em produção.
- O PostgreSQL gratuito do Render expira após 90 dias; faça backup/upgrade antes.
- Faça backup do banco antes de atualizações grandes:
  `pg_dump -h HOST -U USER -d nanobanana > backup.sql`
