-- NanoBanana ERP - V6
-- Suporte a registo internacional (países lusófonos): email opcional, telefone/nacionalidade/província

-- Tenant: novos campos e email opcional
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS nacionalidade VARCHAR(100);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS provincia VARCHAR(100);
ALTER TABLE tenants ALTER COLUMN email DROP NOT NULL;

-- Remover unicidade rígida de email do tenant (se existir) para permitir NULLs
ALTER TABLE tenants DROP CONSTRAINT IF EXISTS tenants_email_key;
DROP INDEX IF EXISTS uk_tenants_email;
DROP INDEX IF EXISTS uk_tenants_email_not_null;
CREATE UNIQUE INDEX IF NOT EXISTS uk_tenants_email_unique ON tenants (email);

-- Usuario: email opcional
ALTER TABLE usuarios ALTER COLUMN email DROP NOT NULL;

-- Substituir unicidade global (permite múltiplos NULLs em H2/PostgreSQL)
DROP INDEX IF EXISTS uk_usuarios_email;
DROP INDEX IF EXISTS uk_usuarios_email_not_null;
CREATE UNIQUE INDEX IF NOT EXISTS uk_usuarios_email_unique ON usuarios (email);

-- Índices para login por telefone
CREATE INDEX IF NOT EXISTS idx_usuarios_telefone ON usuarios (telefone);
CREATE INDEX IF NOT EXISTS idx_tenants_telefone ON tenants (telefone);
