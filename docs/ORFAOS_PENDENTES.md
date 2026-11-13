# Templates órfãos — resolvido

Este arquivo registra a limpeza dos templates legados. As páginas implementadas já têm rota e passam pelos testes existentes.

## Páginas implementadas

| Página | Rota | Acesso |
|---|---|---|
| Inventário | `/inventario` | DONO/GERENTE vê; apenas DONO adiciona/reabastece |
| Produtos estagnados | `/estagnados` | DONO/GERENTE |
| Relatório de vendas | `/relatorio` | DONO/GERENTE |
| Stock rápido | `/stock/rapido` | DONO |

Links antigos foram ajustados:

| Link antigo | Rota atual |
|---|---|
| `/vendas` | `/pdv` |
| `/vendas/historico` | `/pdv/historico` |
| `/vendas/relatorio` | `/relatorio` |

## Templates removidos

| Template | Motivo | Substituto |
|---|---|---|
| `historico.html` | Histórico antigo, sem rota mapeada e com CSRF/rota inválidos | `pdv-historico.html` |
| `vendas.html` | PDV antigo, sem rota mapeada | `pdv.html` |
| `setup-wizard.html` | Onboarding não ligado e sem rotas | Removido sem substituta |

Não havia `@GetMapping`/`@PostMapping` nem `return "<template>"` para esses arquivos no código antes da remoção.

## Templates que existem e têm finalidade

- `inventario.html` — rota `/inventario`
- `estagnados.html` — rota `/estagnados`
- `relatorio.html` — rota `/relatorio`
- `stock-rapido.html` — rota `/stock/rapido`
- `error/403.html` e `error/404.html` — páginas de erro
- `pdv-historico.html` — `/pdv/historico`
- `stock-gestao.html` — `/stock/gestao`

## Regra para não voltar a quebrar

Antes de criar/alterar um link ou template:

1. Existe `@GetMapping`/`@PostMapping` para a rota?
2. Existe `return "nome-do-template"` no controller?
3. A navegação aponta para rotas existentes em todos os templates?
4. Templates sem uso e sem funcionalidade devem ser removidos após confirmação.

## Estado final

Não há templates órfãos pendentes conhecidos.
