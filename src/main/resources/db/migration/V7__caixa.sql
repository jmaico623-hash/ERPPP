-- NanoBanana ERP - V7
-- Gestão de caixa: abertura/fecho e movimentos (sangria/reforço)

CREATE TABLE IF NOT EXISTS caixas (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    usuario_abertura_id UUID NOT NULL,
    usuario_fecho_id UUID,
    valor_abertura NUMERIC(12,2) NOT NULL,
    valor_fecho_informado NUMERIC(12,2),
    valor_esperado NUMERIC(12,2),
    diferenca NUMERIC(12,2),
    status VARCHAR(20) NOT NULL,
    data_abertura TIMESTAMP NOT NULL,
    data_fecho TIMESTAMP,
    observacao_abertura TEXT,
    observacao_fecho TEXT,
    CONSTRAINT fk_caixa_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_caixa_usuario_abertura FOREIGN KEY (usuario_abertura_id) REFERENCES usuarios(id),
    CONSTRAINT fk_caixa_usuario_fecho FOREIGN KEY (usuario_fecho_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS movimentos_caixa (
    id UUID PRIMARY KEY,
    caixa_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    valor NUMERIC(12,2) NOT NULL,
    motivo TEXT,
    data_hora TIMESTAMP NOT NULL,
    CONSTRAINT fk_mov_caixa FOREIGN KEY (caixa_id) REFERENCES caixas(id),
    CONSTRAINT fk_mov_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_mov_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX IF NOT EXISTS idx_caixa_tenant_status ON caixas(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_caixa_tenant_data ON caixas(tenant_id, data_abertura);
CREATE INDEX IF NOT EXISTS idx_mov_caixa ON movimentos_caixa(caixa_id);
CREATE INDEX IF NOT EXISTS idx_mov_tenant ON movimentos_caixa(tenant_id);
