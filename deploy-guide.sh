#!/bin/bash
# 🍌 NanoBanana ERP - Guia Interativo de Deploy
echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║   🍌 NanoBanana ERP - Deploy para Producao (GRATIS)     ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""
echo "  Este script vai guiar voce no deploy do seu ERP."
echo "  Serao necessários:"
echo "    1. Uma conta no GitHub"
echo "    2. Uma conta no Render.com"
echo ""
echo "════════════════════════════════════════════════════════════"
echo ""

echo "📌 PASSO 1: Criar repositório no GitHub"
echo ""
echo "  a) Acesse: https://github.com/new"
echo "  b) Nome do repo: nanobanana-erp"
echo "  c) Deixe como Public"
echo "  d) NAO inicialize com README, .gitignore, ou license"
echo "  e) Clique em 'Create repository'"
echo ""
echo "  Depois copie a URL do repositorio."
echo "  Deve ser algo como:"
echo "  https://github.com/SEU_USERNAME/nanobanana-erp.git"
echo ""
read -p "  Cole aqui a URL do GitHub: " REPO_URL

if [ -z "$REPO_URL" ]; then
    echo "  ❌ Nenhuma URL fornecida. A cancelar."
    exit 1
fi

echo ""
echo "📌 PASSO 2: Push do código para o GitHub"
echo ""

cd /home/tiih/nanobanana-web

git remote remove origin 2>/dev/null
git remote add origin "$REPO_URL"

echo "  A fazer push..."
git push -u origin main

if [ $? -eq 0 ]; then
    echo "  ✅ Push concluído com sucesso!"
    echo "  📂 Veja: $REPO_URL"
else
    echo ""
    echo "  ⚠️  O push falhou. Possiveis causas:"
    echo "  - Credenciais GitHub nao configuradas"
    echo "  - Faca: git config --global credential.helper store"
    echo "  - Depois tente: git push -u origin main"
    echo ""
    echo "  Ou use um token:"
    echo "  https://github.com/settings/tokens"
    exit 1
fi

echo ""
echo "════════════════════════════════════════════════════════════"
echo "📌 PASSO 3: Deploy no Render.com (GRATIS)"
echo ""
echo "  a) Acesse: https://render.com"
echo "  b) Clique em 'Get Started for Free'"
echo "  c) Registe-se com GitHub (recomendado)"
echo "  d) No dashboard, clique em 'New +'"
echo "  e) Escolha 'Blueprint'"
echo "  f) Selecione o repositorio 'nanobanana-erp'"
echo "  g) Clique em 'Apply'"
echo ""
echo "  O Render vai:"
echo "    ✅ Criar banco de dados PostgreSQL gratis"
echo "    ✅ Compilar o projeto com Maven"
echo "    ✅ Iniciar o servidor automaticamente"
echo "    ✅ Configurar HTTPS (SSL)"
echo ""
echo "  URL do seu ERP sera algo como:"
echo "  https://nanobanana-erp.onrender.com"
echo ""
echo "════════════════════════════════════════════════════════════"
echo ""
echo "🔄 FUTURE UPDATES (Push-to-Deploy):"
echo ""
echo "  Sempre que fizer alteracoes no codigo:"
echo ""
echo "    cd /home/tiih/nanobanana-web"
echo "    git add ."
echo "    git commit -m 'descricao'"
echo "    git push"
echo ""
echo "  O Render detecta o push e faz deploy automatico! 🚀"
echo ""
echo "════════════════════════════════════════════════════════════"
echo ""
echo "  ✅ Pronto! O seu NanoBanana ERP esta em producao!"
echo "  🍌 Boa sorte com os seus clientes!"
echo ""
