# GitBot Commit

**GitBot Commit** is an IntelliJ IDEA plugin that generates AI-powered commit messages directly from your staged changes, integrated into the native Git Commit workflow.

It reads your `git diff --cached`, sends it to an AI model via [OpenRouter](https://openrouter.ai), and writes a structured commit message following the **Conventional Commits** standard with emojis.

---

## Requirements

- IntelliJ IDEA (2022.1 or later)
- Git enabled in the project
- An [OpenRouter](https://openrouter.ai) account and API key
- At least one staged change (`git add`) before running the plugin

---

## Installation

1. Open IntelliJ IDEA
2. Go to **Settings → Plugins → Marketplace**
3. Search for **GitBot Commit**
4. Click **Install** and restart the IDE

---

## Configuration

Before using the plugin, you need to configure it through the IDE settings.

### Step 1 — Open Settings

Go to **Settings** (or **Preferences** on macOS) → **GitBot Commit**

### Step 2 — Set your OpenRouter API Key

- Paste your OpenRouter API key in the **OpenRouter API Key** field
- The key is stored securely using the IDE's built-in credential store (not in plain text)

> To get an API key, sign up at [openrouter.ai](https://openrouter.ai) and generate a key from your dashboard.

### Step 3 — Choose a Model

- In the **Model** field, enter the model ID you want to use
- Default: `anthropic/claude-3.5-sonnet`
- You can use any model available on OpenRouter. Examples:
  - `anthropic/claude-3.5-sonnet`
  - `openai/gpt-4o`
  - `google/gemini-pro`
  - `meta-llama/llama-3-70b-instruct`

> Browse available models at [openrouter.ai/models](https://openrouter.ai/models)

### Step 4 — Select the Commit Language

- Use the **Commit language** dropdown to choose the output language
- Available options:
  - `PT_BR` — Brazilian Portuguese
  - `EN` — English

> Each language has its own independent prompt template.

### Step 5 — Review the Prompt Template (optional)

- The **Prompt Template** field shows the system prompt sent to the AI for the selected language
- It comes pre-configured with a Conventional Commits-oriented prompt
- You can edit it freely to adjust tone, format, scope conventions, or any other behavior
- Click **Reset to default** to restore the original prompt for the selected language at any time

### Step 6 — Apply

Click **Apply** or **OK** to save your settings.

---

## Usage

### Step 1 — Stage your changes

In your terminal or IntelliJ's Git panel, stage the files you want to include in the commit:

```bash
git add <file>
# or stage all changes
git add .
```

> The plugin only reads staged changes (`git diff --cached`). Unstaged changes are ignored.

### Step 2 — Open the Commit panel

Open IntelliJ's Git Commit panel using one of the following:
- **Keyboard shortcut:** `Ctrl+K` (Windows/Linux) or `Cmd+K` (macOS)
- **Menu:** Git → Commit

### Step 3 — Run the plugin

In the Commit panel, locate the **⚡ AI Commit** action. It appears in the toolbar at the top of the commit message area.

Click **⚡ AI Commit** to start the generation.

> A background progress indicator will appear at the bottom of the IDE while the model processes your diff.
> You can cancel the generation at any time by clicking the **X** button on the progress bar.

### Step 4 — Review the generated message

A preview dialog will open displaying the generated commit message. From here you can:

| Button | Action |
|--------|--------|
| **Edit** | Makes the text area editable so you can adjust the message before applying |
| **Copy** | Copies the message to your clipboard |
| **Apply** | Inserts the message into IntelliJ's commit message field and closes the dialog |
| **Cancel** | Discards the generated message |

### Step 5 — Commit

After clicking **Apply**, the generated message will appear in the Commit panel's message field.

Review it, make any final edits if needed, and click **Commit** (or **Commit and Push**).

> After a successful commit, the message field is automatically cleared and ready for the next commit.

---

## Commit Message Format

The plugin generates messages following the [Conventional Commits](https://www.conventionalcommits.org) specification with emojis:

```
<emoji><type>[optional scope]: <description>

<body explaining the changes>
```

### Types and emojis

| Type       | Emoji | When to use                        |
|------------|-------|------------------------------------|
| `feat`     | ✨    | New feature                        |
| `fix`      | 🐛    | Bug fix                            |
| `refactor` | ♻️    | Code restructuring without behavior change |
| `docs`     | 📝    | Documentation changes              |
| `chore`    | 🔧    | Build, config, or tooling changes  |
| `test`     | 🧪    | Adding or updating tests           |
| `style`    | 🎨    | Formatting, whitespace, code style |

### Priority rule

When a diff contains multiple change types, the plugin selects the highest-impact type:

```
fix > feat > refactor > chore > docs > test > style
```

### Example output

```
✨feat(auth): add OAuth2 login support

Implement Google OAuth2 flow using the existing session manager.
Add callback endpoint and token exchange logic.
Update user model to store provider and external ID.
```

---

## Selecting specific files

By default, the plugin uses the full staged diff. If you want to generate a commit message based on a **subset of staged files**, select them in the Commit panel's file list before clicking **⚡ AI Commit**. The plugin will restrict the diff to only those files.

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "No Git repository found" | Make sure the project has a Git repository initialized (`git init`) |
| "No staged changes found" | Stage your changes with `git add` before running the plugin |
| "Configure your OpenRouter API key" | Go to **Settings → GitBot Commit** and enter your API key |
| "Configure the model" | Go to **Settings → GitBot Commit** and enter a valid model ID |
| OpenRouter error message | Check your API key, account credits, and the model ID at openrouter.ai |
| Message not applied after clicking Apply | If `setCommitMessage` fails silently, the message is copied to your clipboard as a fallback |

---

## Security

- The API key is stored using IntelliJ's native **credential store** (OS keychain / IDE secrets), never written in plain text to any config file
- The diff content is sent directly to OpenRouter's API over HTTPS and is subject to their [privacy policy](https://openrouter.ai/privacy)

---

## License

MIT — see [LICENSE](LICENSE) for details.

**Source code:** [github.com/frshaka/GitBotCommitPlugin](https://github.com/frshaka/GitBotCommitPlugin)

---

## Release Notes

### 1.0.6 — Multi-provider support

- **New provider: Ollama (local).** Run commit generation 100% offline against models pulled with `ollama pull`. Requires Ollama 0.1.27+ for OpenAI-compatible `/v1/chat/completions` support.
- **Provider selector in Settings.** Switch between **OpenRouter** (cloud) and **Ollama** (local). The settings panel was migrated to the modern Kotlin UI DSL, showing only the relevant fields per provider.
- **Test Connection for Ollama.** Verifies the server is reachable and reports the number of installed models in one click.
- **User-friendly error handling.** Distinct messages for unreachable providers, invalid API key, missing model (with `ollama pull` hint), and rate limits.
- **Per-provider model memory.** Switching providers preserves the previously selected model on each side, so you don't lose your selection.

### 1.0.5 — Bug fixes

- **Fixed 400 error when generating commits with any OpenRouter model.** Request and response message DTOs are now separate, so no extra fields are sent.
- **Fixed infinite 400 loop with Chain-of-Thought reasoning models.** A user continuation message is now appended after each reasoning step.

---

---

# GitBot Commit — Português do Brasil

**GitBot Commit** é um plugin para IntelliJ IDEA que gera mensagens de commit com inteligência artificial diretamente a partir das suas alterações staged, integrado ao fluxo nativo de Git Commit da IDE.

Ele lê o seu `git diff --cached`, envia para um modelo de IA via [OpenRouter](https://openrouter.ai) e escreve uma mensagem de commit estruturada seguindo o padrão **Conventional Commits** com emojis.

---

## Requisitos

- IntelliJ IDEA (2022.1 ou superior)
- Git habilitado no projeto
- Uma conta e chave de API no [OpenRouter](https://openrouter.ai)
- Ao menos uma alteração staged (`git add`) antes de executar o plugin

---

## Instalação

1. Abra o IntelliJ IDEA
2. Acesse **Settings → Plugins → Marketplace**
3. Pesquise por **GitBot Commit**
4. Clique em **Install** e reinicie a IDE

---

## Configuração

Antes de usar o plugin, é necessário configurá-lo nas preferências da IDE.

### Passo 1 — Abrir as Configurações

Acesse **Settings** (ou **Preferences** no macOS) → **GitBot Commit**

### Passo 2 — Definir a chave de API do OpenRouter

- Cole sua chave de API do OpenRouter no campo **OpenRouter API Key**
- A chave é armazenada com segurança usando o armazenamento de credenciais nativo da IDE (não em texto simples)

> Para obter uma chave, crie uma conta em [openrouter.ai](https://openrouter.ai) e gere uma chave no seu painel.

### Passo 3 — Escolher um Modelo

- No campo **Model**, informe o ID do modelo que deseja usar
- Padrão: `anthropic/claude-3.5-sonnet`
- Você pode usar qualquer modelo disponível no OpenRouter. Exemplos:
  - `anthropic/claude-3.5-sonnet`
  - `openai/gpt-4o`
  - `google/gemini-pro`
  - `meta-llama/llama-3-70b-instruct`

> Consulte os modelos disponíveis em [openrouter.ai/models](https://openrouter.ai/models)

### Passo 4 — Selecionar o Idioma do Commit

- Use o seletor **Commit language** para escolher o idioma de saída
- Opções disponíveis:
  - `PT_BR` — Português do Brasil
  - `EN` — Inglês

> Cada idioma possui seu próprio template de prompt independente.

### Passo 5 — Revisar o Template de Prompt (opcional)

- O campo **Prompt Template** exibe o prompt de sistema enviado à IA para o idioma selecionado
- Ele vem pré-configurado com um prompt orientado ao Conventional Commits
- Você pode editá-lo livremente para ajustar tom, formato, convenções de escopo ou qualquer outro comportamento
- Clique em **Reset to default** para restaurar o prompt original do idioma selecionado a qualquer momento

### Passo 6 — Aplicar

Clique em **Apply** ou **OK** para salvar as configurações.

---

## Uso

### Passo 1 — Faça o stage das suas alterações

No terminal ou no painel Git do IntelliJ, adicione ao stage os arquivos que deseja incluir no commit:

```bash
git add <arquivo>
# ou adicionar todas as alterações
git add .
```

> O plugin lê apenas as alterações staged (`git diff --cached`). Alterações não staged são ignoradas.

### Passo 2 — Abrir o painel de Commit

Abra o painel de Commit do IntelliJ usando uma das opções abaixo:
- **Atalho de teclado:** `Ctrl+K` (Windows/Linux) ou `Cmd+K` (macOS)
- **Menu:** Git → Commit

### Passo 3 — Executar o plugin

No painel de Commit, localize a ação **⚡ AI Commit** na barra de ferramentas acima do campo de mensagem.

Clique em **⚡ AI Commit** para iniciar a geração.

> Um indicador de progresso aparecerá na parte inferior da IDE enquanto o modelo processa o diff.
> Você pode cancelar a geração a qualquer momento clicando no botão **X** na barra de progresso.

### Passo 4 — Revisar a mensagem gerada

Um diálogo de pré-visualização será aberto com a mensagem gerada. A partir dele você pode:

| Botão | Ação |
|-------|------|
| **Edit** | Torna o campo de texto editável para ajustar a mensagem antes de aplicar |
| **Copy** | Copia a mensagem para a área de transferência |
| **Apply** | Insere a mensagem no campo de commit do IntelliJ e fecha o diálogo |
| **Cancel** | Descarta a mensagem gerada |

### Passo 5 — Commit

Após clicar em **Apply**, a mensagem gerada aparecerá no campo de mensagem do painel de Commit.

Revise, faça ajustes finais se necessário, e clique em **Commit** (ou **Commit and Push**).

> Após um commit bem-sucedido, o campo de mensagem é limpo automaticamente e fica pronto para o próximo commit.

---

## Formato da Mensagem de Commit

O plugin gera mensagens seguindo a especificação [Conventional Commits](https://www.conventionalcommits.org) com emojis:

```
<emoji><tipo>[escopo opcional]: <descrição>

<corpo explicando as alterações>
```

### Tipos e emojis

| Tipo       | Emoji | Quando usar                               |
|------------|-------|-------------------------------------------|
| `feat`     | ✨    | Nova funcionalidade                       |
| `fix`      | 🐛    | Correção de bug                           |
| `refactor` | ♻️    | Reestruturação de código sem mudança de comportamento |
| `docs`     | 📝    | Alterações na documentação               |
| `chore`    | 🔧    | Alterações de build, config ou ferramentas |
| `test`     | 🧪    | Adição ou atualização de testes           |
| `style`    | 🎨    | Formatação, espaços em branco, estilo de código |

### Regra de prioridade

Quando um diff contém múltiplos tipos de alteração, o plugin seleciona o tipo de maior impacto:

```
fix > feat > refactor > chore > docs > test > style
```

### Exemplo de saída

```
✨feat(auth): adiciona suporte a login OAuth2

Implementa o fluxo OAuth2 do Google usando o gerenciador de sessão existente.
Adiciona endpoint de callback e lógica de troca de token.
Atualiza o modelo de usuário para armazenar o provedor e o ID externo.
```

---

## Selecionando arquivos específicos

Por padrão, o plugin utiliza o diff completo de todos os arquivos staged. Se quiser gerar uma mensagem de commit com base em um **subconjunto de arquivos**, selecione-os na lista de arquivos do painel de Commit antes de clicar em **⚡ AI Commit**. O plugin restringirá o diff apenas aos arquivos selecionados.

---

## Solução de Problemas

| Problema | Solução |
|----------|---------|
| "No Git repository found" | Verifique se o projeto possui um repositório Git inicializado (`git init`) |
| "No staged changes found" | Faça o stage das suas alterações com `git add` antes de usar o plugin |
| "Configure your OpenRouter API key" | Acesse **Settings → GitBot Commit** e informe sua chave de API |
| "Configure the model" | Acesse **Settings → GitBot Commit** e informe um ID de modelo válido |
| Mensagem de erro do OpenRouter | Verifique sua chave de API, créditos da conta e o ID do modelo em openrouter.ai |
| Mensagem não aplicada após clicar em Apply | Se `setCommitMessage` falhar silenciosamente, a mensagem é copiada para a área de transferência como fallback |

---

## Segurança

- A chave de API é armazenada usando o **armazenamento de credenciais nativo** do IntelliJ (keychain do SO / segredos da IDE), nunca gravada em texto simples em nenhum arquivo de configuração
- O conteúdo do diff é enviado diretamente à API do OpenRouter via HTTPS e está sujeito à [política de privacidade](https://openrouter.ai/privacy) deles

---

## Licença

MIT — veja [LICENSE](LICENSE) para detalhes.

**Código-fonte:** [github.com/frshaka/GitBotCommitPlugin](https://github.com/frshaka/GitBotCommitPlugin)

---

## Notas de Versão

### 1.0.6 — Suporte multi-provider

- **Novo provider: Ollama (local).** Gere mensagens de commit 100% offline com modelos baixados via `ollama pull`. Requer Ollama 0.1.27+ para suporte ao endpoint compatível com OpenAI `/v1/chat/completions`.
- **Seletor de provider nas Configurações.** Alterne entre **OpenRouter** (nuvem) e **Ollama** (local). O painel de configurações foi migrado para o Kotlin UI DSL moderno, exibindo apenas os campos relevantes para cada provider.
- **Test Connection para Ollama.** Verifica se o servidor está acessível e exibe o número de modelos instalados com um clique.
- **Tratamento de erros amigável.** Mensagens distintas para provider inacessível, chave de API inválida, modelo ausente (com dica de `ollama pull`) e limite de taxa.
- **Memória de modelo por provider.** Trocar de provider preserva o modelo selecionado anteriormente em cada lado, sem perder a configuração.

### 1.0.5 — Correções de bugs

- **Corrigido erro 400 ao gerar commits com qualquer modelo do OpenRouter.** Os DTOs de mensagem de requisição e resposta agora são separados, eliminando campos extras nas requisições.
- **Corrigido loop infinito de erro 400 com modelos de raciocínio Chain-of-Thought.** Uma mensagem de continuação do usuário é adicionada após cada etapa de raciocínio.
