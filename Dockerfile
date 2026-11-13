# NanoBanana ERP - Dockerfile
# Multi-stage build para imagem menor e mais segura

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia arquivos de build primeiro (cache de dependências)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Baixa dependências (cache layer)
RUN ./mvnw dependency:go-offline -B

# Copia código fonte
COPY src src

# Compila e empacota (skip tests para build rápido - CI roda tests separadamente)
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Usuário não-root para segurança
RUN addgroup -g 1000 -S nano && adduser -u 1000 -S nano -G nano

WORKDIR /app

# Copia JAR do stage anterior
COPY --from=builder /app/target/erp-1.0.0.jar app.jar

# Muda ownership para usuário não-root
RUN chown nano:nano app.jar

USER nano

# Perfil de produção por padrão
ENV SPRING_PROFILES_ACTIVE=prod

# Configurações JVM otimizadas para containers
ENV JAVA_OPTS="-Xms256m -Xmx768m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

# Porta exposta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]