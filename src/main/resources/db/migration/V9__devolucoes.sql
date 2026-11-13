-- NanoBanana ERP - V9
-- Devoluções de vendas (parcial/total) com reposição opcional de stock

CREATE TABLE IF NOT EXISTS devolucoes (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    venda_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    valor_total NUMERIC(12,2) NOT NULL,
    repor_stock BOOLEAN NOT NULL DEFAULT true,
    motivo TEXT NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    CONSTRAINT fk_devolucao_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_devolucao_venda FOREIGN KEY (venda_id) REFERENCES vendas(id),
    CONSTRAINT fk_devolucao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS devolucao_itens (
    id UUID PRIMARY KEY,
    devolucao_id UUID NOT NULL,
    venda_item_id UUID NOT NULL,
    produto_id UUID NOT NULL,
    nome_produto VARCHAR(255) NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(10,2) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    CONSTRAINT fk_dev_item_devolucao FOREIGN KEY (devolucao_id) REFERENCES devolucoes(id),
    CONSTRAINT fk_dev_item_venda_item FOREIGN KEY (venda_item_id) REFERENCES venda_itens(id),
    CONSTRAINT fk_dev_item_produto FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE INDEX IF NOT EXISTS idx_devolucao_tenant ON devolucoes(tenant_id);
CREATE INDEX IF NOT EXISTS idx_devolucao_venda ON devolucoes(venda_id);
CREATE INDEX IF NOT EXISTS idx_dev_item_devolucao ON devolucao_itens(devolucao_id);
CREATE INDEX IF NOT EXISTS idx_dev_item_venda_item ON devolucao_itens(venda_item_id);
