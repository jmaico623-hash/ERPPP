-- NanoBanana ERP - V4
-- Permite cancelar fiados associados a vendas canceladas sem apagar histórico

ALTER TABLE fiados
    ADD COLUMN IF NOT EXISTS cancelado BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_fiado_tenant_cancelado ON fiados(tenant_id, cancelado);
