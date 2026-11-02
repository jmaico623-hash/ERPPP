#!/bin/bash
# NanoBanana ERP - Script de Deploy
# Uso: ./deploy.sh

JAR="target/erp-1.0.0.jar"
PORT=8080

echo "🍌 NanoBanana ERP - Deploy"
echo "=========================="

if [ "$1" = "build" ]; then
    echo "📦 A compilar..."
    ./mvnw clean package -DskipTests
    echo "✅ Build concluído!"
    exit 0
fi

if [ "$1" = "start" ]; then
    if [ -f "$JAR" ]; then
        echo "🚀 A iniciar servidor na porta $PORT..."
        nohup java -jar $JAR --server.port=$PORT --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev} > nanobanana.log 2>&1 &
        echo "PID: $!"
        echo "📋 Ver logs: tail -f nanobanana.log"
        echo "🌐 Acessar: http://localhost:$PORT"
    else
        echo "❌ JAR não encontrado! Execute: ./deploy.sh build"
    fi
    exit 0
fi

if [ "$1" = "stop" ]; then
    pkill -f "erp-1.0.0.jar"
    echo "🛑 Servidor parado"
    exit 0
fi

if [ "$1" = "status" ]; then
    if pgrep -f "erp-1.0.0.jar" > /dev/null; then
        echo "✅ Servidor está a correr"
        curl -s -o /dev/null -w "   HTTP: %{http_code}\n" http://localhost:$PORT/login
    else
        echo "❌ Servidor parado"
    fi
    exit 0
fi

echo "Uso: ./deploy.sh {build|start|stop|status}"
echo ""
echo "  build   - Compila o projeto"
echo "  start   - Inicia o servidor"
echo "  stop    - Para o servidor"
echo "  status  - Verifica estado"
