-- NanoBanana ERP - V13
-- Garante que um telefone (já normalizado pela V12) só pode pertencer a uma conta.
-- Isto evita que dois registos simultâneos criem contas com o mesmo número.
-- Índices únicos permitem vários NULL (funcionários sem telefone).

DROP INDEX IF EXISTS idx_usuarios_telefone;
DROP INDEX IF EXISTS idx_tenants_telefone;

CREATE UNIQUE INDEX IF NOT EXISTS uk_usuarios_telefone_unique ON usuarios (telefone);
CREATE UNIQUE INDEX IF NOT EXISTS uk_tenants_telefone_unique ON tenants (telefone);
