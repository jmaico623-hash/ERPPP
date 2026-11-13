-- NanoBanana ERP - V10
-- Registo de custo no momento da venda para cálculo de lucro (COGS)

ALTER TABLE venda_itens ADD COLUMN IF NOT EXISTS custo_unitario NUMERIC(10,2);
ALTER TABLE venda_itens ADD COLUMN IF NOT EXISTS custo_total NUMERIC(10,2);
