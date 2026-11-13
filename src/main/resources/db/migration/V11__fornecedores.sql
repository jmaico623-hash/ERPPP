-- NanoBanana ERP - V11
-- Cadastro de fornecedores

CREATE TABLE IF NOT EXISTS fornecedores (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    nome VARCHAR(255) NOT NULL,
    telefone VARCHAR(50),
    email VARCHAR(255),
    nuit_nif VARCHAR(100),
    endereco TEXT,
    ativo BOOLEAN NOT NULL DEFAULT true,
    criado_em TIMESTAMP NOT NULL,
    CONSTRAINT fk_fornecedor_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX IF NOT EXISTS idx_fornecedor_tenant ON fornecedores(tenant_id);
CREATE INDEX IF NOT EXISTS idx_fornecedor_nome ON fornecedores(tenant_id, nome);
