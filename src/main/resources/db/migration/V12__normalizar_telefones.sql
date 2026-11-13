-- NanoBanana ERP - V12
-- Normaliza telefones já existentes para que o login funcione independentemente
-- da formatação usada no registo: "84 555 0001", "+258 84 555 0001", "00258...".
-- O número passa a ser guardado apenas com dígitos (o indicativo do país é mantido).

UPDATE usuarios
SET telefone = REGEXP_REPLACE(telefone, '[^0-9]', '')
WHERE telefone IS NOT NULL AND telefone <> '';

UPDATE tenants
SET telefone = REGEXP_REPLACE(telefone, '[^0-9]', '')
WHERE telefone IS NOT NULL AND telefone <> '';

-- Remove o prefixo internacional "00" (ex.: 00258845550001 -> 258845550001)
UPDATE usuarios
SET telefone = SUBSTRING(telefone, 3)
WHERE telefone LIKE '00%' AND LENGTH(telefone) > 2;

UPDATE tenants
SET telefone = SUBSTRING(telefone, 3)
WHERE telefone LIKE '00%' AND LENGTH(telefone) > 2;

-- Telefones que ficaram sem dígitos deixam de existir
UPDATE usuarios SET telefone = NULL WHERE telefone = '';
UPDATE tenants SET telefone = NULL WHERE telefone = '';
