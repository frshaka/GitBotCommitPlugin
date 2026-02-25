# GitBotCommit Plugin - Memory

## Projeto
Plugin IntelliJ IDEA que analisa o diff do Git e gera mensagens de commit via IA (OpenRouter).

## Arquivos principais
- `src/main/kotlin/com/frshaka/gitbot/actions/GenerateCommitAction.kt` — lógica principal
- `src/main/kotlin/com/frshaka/gitbot/ai/OpenRouterClient.kt` — cliente HTTP para OpenRouter
- `src/main/kotlin/com/frshaka/gitbot/settings/` — configurações (API key, model, prompt, language)
- `build.gradle.kts` — alvo: IntelliJ IDEA 2025.2.4, since build 252.25557

## Arquitetura do diff
O plugin roda `git diff` via `ProcessBuilder` (não usa IntelliJ VCS API para o diff em si).

### Ordem de tentativa do diff:
1. `git diff --cached` — staging area ativado no IntelliJ
2. `git diff HEAD` — modo padrão do IntelliJ (sem staging, arquivos não staged)

### Filtro de arquivos:
- `Refreshable.PANEL_KEY as? CheckinProjectPanel` → `.selectedChanges` = **arquivos com checkbox marcado** (fonte correta)
- Fallback: `VcsDataKeys.SELECTED_CHANGES` = arquivos highlighted/focados no momento
- Se nenhum → diff sem filtro (todos os changes)

## Lições aprendidas sobre VcsDataKeys
| Data Key | O que retorna |
|---|---|
| `VcsDataKeys.CHANGES` | TODOS os arquivos do change list (checked + unchecked) — NÃO usar para filtrar |
| `VcsDataKeys.SELECTED_CHANGES` | Arquivos highlighted (clicou e ficou azul na lista) |
| `CheckinProjectPanel.selectedChanges` | Apenas arquivos com checkbox marcado ✓ |

## Armadilhas conhecidas
- `mutableListOf(...) + list` retorna `List` imutável — usar `addAll()` para manter mutabilidade
- O linter/IDE reverte funções para versões anteriores ocasionalmente — verificar assinatura após edições
- IntelliJ modo não-staging: `git diff --cached` retorna vazio; usar `git diff HEAD` como fallback
