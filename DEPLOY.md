# 🚀 Guia de Deploy - NanoBanana ERP

## Deploy em 3 Passos

### 1️⃣ Push para GitHub

```bash
cd /home/tiih/nanobanana-web

# Crie o repo em github.com/new com nome: nanobanana-erp

git remote add origin https://github.com/SEU_USER/nanobanana-erp.git
git push -u origin main
```

### 2️⃣ Deploy no Render.com

**Método BluePrint (automático):**
1. Acesse **render.com** → conta grátis
2. **New+** → **Blueprint**
3. Selecione o repo `nanobanana-erp`
4. Clique **Apply** → Pronto!

**Método Manual:**
1. **New+** → **Web Service**
2. Conecte o repo `nanobanana-erp`
3. Configurações:
   - **Build**: `mvn clean package -DskipTests`
   - **Start**: `java -jar target/erp-1.0.0.jar`
   - **Environment**: `SPRING_PROFILES_ACTIVE=prod`
4. Adicione PostgreSQL: **New+** → **PostgreSQL** → Plano Free
5. Copie a connection string para a variável `DATABASE_URL`
6. Deploy!

### 3️⃣ Push-to-Deploy

Daqui em diante, cada push atualiza o site:

```bash
git add .
git commit -m "minha alteracao"
git push
# Render atualiza automaticamente em ~2 min 🚀
```

## URLs Após Deploy

| Recurso | URL |
|---|---|
| **ERP** | `https://nanobanana-erp.onrender.com` |
| **Login** | `https://nanobanana-erp.onrender.com/login` |
| **Admin** | `https://nanobanana-erp.onrender.com/admin` |
| **H2 (dev)** | `http://localhost:8080/h2-console` |

## Credenciais

| Perfil | Email | Senha |
|---|---|---|
| Super Admin | admin@nanobanana.co.mz | nano2026 |
| Demo Pago | junior@email.com | 1234 |
| Teste Expirado | maria@email.com | 1234 |

## Troubleshooting

| Problema | Solução |
|---|---|
| Build falha no Render | Verifique logs em "Events" no dashboard |
| 502 Bad Gateway | Aguarde 1-2 min para o servidor iniciar |
| Erro de banco | Verifique `DATABASE_URL` nas env vars |
| Push rejeitado | `git config --global credential.helper store` |
