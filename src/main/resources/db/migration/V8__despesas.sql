-- NanoBanana ERP - V8
-- Registo de despesas operacionais (fluxo de caixa/lucro operacional)

CREATE TABLE IF NOT EXISTS despesas (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    valor NUMERIC(12,2) NOT NULL,
    data_despesa DATE NOT NULL,
    metodo_pagamento VARCHAR(30),
    observacao TEXT,
    criado_em TIMESTAMP NOT NULL,
    CONSTRAINT fk_despesa_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_despesa_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX IF NOT EXISTS idx_despesa_tenant_data ON despesas(tenant_id, data_despesa);
CREATE INDEX IF NOT EXISTS idx_despesa_tenant_categoria ON despesas(tenant_id, categoria);
