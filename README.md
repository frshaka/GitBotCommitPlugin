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
