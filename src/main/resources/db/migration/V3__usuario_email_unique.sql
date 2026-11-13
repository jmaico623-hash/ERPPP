-- NanoBanana ERP - V3
-- Garante unicidade global de email de usuário (necessária para login seguro e estável)
-- ATENÇÃO: falha se já existirem emails duplicados; resolva-os manualmente antes de migrar.

CREATE UNIQUE INDEX IF NOT EXISTS uk_usuarios_email ON usuarios(email);
