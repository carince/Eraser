# One setup, every device

The fix is not a sync tool. It's moving the source of truth out of your home folder and into a git repo.

Open the picture first: `visual.html`.

---

## The mental model

```
Device A (edit)  ──git push──▶  agent-config repo  ──git clone/pull──▶  Device B (already set up)
```

A device is a **cache** of the repo, not the owner of the config. Once you accept that:

- adding an MCP server on one machine and having it appear on the other is just `git pull`
- "setting up again" becomes `git clone` + one OAuth sign-in
- skills are shared by OpenCode, hermes, and Claude Code if you point them at one directory

---

## Repo layout

Make one private repo. This is the whole thing:

```
agent-config/
├── opencode.jsonc          # model, permissions, MCP servers, skills sources
├── skills/                 # <id>/SKILL.md folders (+ scripts/, references/)
├── agents/                 # subagent definitions
├── commands/               # slash commands
├── plugins/                # local plugins, if any
├── secrets.example         # names of required env vars, never values
├── .gitignore
└── bootstrap.ps1           # new-device one-liner
```

`.gitignore`:

```gitignore
service.json        # per-machine background-service registration
*.local.jsonc       # machine-specific overrides
secrets/
.env
```

**Never commit:** `service.json`, `~/.local/share/opencode` (sessions, db, logs), API keys, OAuth tokens.

---

## Option 1 — clone straight into the config dir (start here)

The repo *is* `~/.config/opencode`. Nothing to link, nothing to configure.

**Windows**

```powershell
# 1. save the one per-machine file
$dest = "$env:USERPROFILE\.config\opencode"
if (Test-Path "$dest\service.json") {
  Copy-Item "$dest\service.json" "$env:TEMP\opencode-service.json" -Force
}

# 2. clone into place (move the old dir aside first, don't delete blind)
if (Test-Path $dest) { Move-Item $dest "$dest.bak" }

git clone git@github.com:YOU/agent-config.git $dest

# 3. put the machine file back
Copy-Item "$env:TEMP\opencode-service.json" "$dest\service.json" -Force
```

**macOS / Linux**

```sh
git clone git@github.com:YOU/agent-config.git ~/.config/opencode
```

Daily use after that is just:

```sh
git -C ~/.config/opencode pull      # on the device you're sitting at
git -C ~/.config/opencode push      # after you change something
```

Config changes apply on save; a `pull` needs no restart.

---

## Option 2 — dotfiles repo + symlink

Better if you already sync a `~/dotfiles`, or if hermes and OpenCode should read the *same* skills folder.

```
~/dotfiles/
├── opencode/          → opencode.jsonc, agents/, commands/, plugins/
└── agent-skills/      → shared skills/, read by every harness
```

**Windows** (junction, no admin needed):

```powershell
New-Item -ItemType Junction `
  -Path   "$env:USERPROFILE\.config\opencode" `
  -Target "$env:USERPROFILE\dotfiles\opencode"
```

**macOS / Linux**

```sh
ln -s ~/dotfiles/opencode ~/.config/opencode
```

Then share the skills folder with everything else:

```powershell
# Windows
New-Item -ItemType Junction -Path "$env:USERPROFILE\.claude\skills" -Target "$env:USERPROFILE\dotfiles\agent-skills"
```

```sh
# macOS / Linux
ln -s ~/dotfiles/agent-skills ~/.claude/skills
```

OpenCode reads `~/.config/opencode/skills`, `~/.claude/skills`, and `~/.agents/skills` globally — so one linked folder serves all three harnesses.

---

## Option 3 — HTTP skill catalog (zero install)

Skills can be fetched from a URL. Put this on GitHub Pages / any static host:

```
https://you.github.io/agent-skills/
├── index.json
└── git-release/
    ├── SKILL.md
    └── references/release-policy.md
```

`index.json`:

```json
{
  "skills": [
    { "name": "git-release", "version": "3",
      "files": ["SKILL.md", "references/release-policy.md"] }
  ]
}
```

Then in `opencode.jsonc`:

```jsonc
{
  "$schema": "https://opencode.ai/config.json",
  "skills": ["https://you.github.io/agent-skills/"]
}
```

**Bump `version` whenever files change** or the device keeps its cached copy. This is the closest thing to "open it and it's already set up" — the machine needs no checkout at all, just the URL in the config.

You can mix all three:

```jsonc
{
  "skills": [
    "~/dotfiles/agent-skills",          // local, editable
    "https://you.github.io/agent-skills/" // remote, always current
  ]
}
```

---

## Secrets: keep them out of the config

OpenCode substitutes variables inside JSON strings, so the repo holds *names*, never values.

```jsonc
{
  "providers": {
    "anthropic": { "settings": { "apiKey": "{env:ANTHROPIC_API_KEY}" } }
  },
  "mcp": {
    "servers": {
      "company-tools": {
        "type": "remote",
        "url": "https://mcp.example.com/mcp",
        "headers": { "Authorization": "Bearer {env:MCP_API_KEY}" }
      }
    }
  }
}
```

- `{env:NAME}` → environment variable (put them in your shell profile / Windows user env)
- `{file:~/.secrets/name}` → file contents, for things you'd rather not have in the environment
- unset variable → empty string, so a missing secret fails visibly, not silently wrong

Track the *list* of required names in `secrets.example` so a new device tells you what it's missing.

---

## MCP: what actually re-does itself

| Part | Travels? | New device action |
|---|---|---|
| `mcp.servers` definitions | ✅ in git | none |
| Local servers (`npx`, `uvx`, `bunx`) | ✅ definition | first run downloads the package |
| Remote servers (OAuth) | ✅ definition | **one** `/mcps` sign-in |
| Remote servers (API key) | ✅ definition | set the env var / secret file |
| OAuth tokens | ❌ by design | re-auth once, then cached |
| `disabled: true` servers | ✅ | stay off until you enable them |

OAuth credentials are stored outside project config on purpose. Don't try to sync them.

**Machine-specific servers** (something only installed on your work laptop): keep the entry in the shared config with `"disabled": true`, and enable it in a project-level or `*.local.jsonc` override. Note that a project config **replaces the whole server object** with the same name — repeat every required field if you override one.

---

## Bootstrap script (`bootstrap.ps1`)

Drop this in the repo root and the new-device ritual is one line.

```powershell
# bootstrap.ps1 — run on a fresh Windows machine
$ErrorActionPreference = "Stop"
$repo = "git@github.com:YOU/agent-config.git"
$dest = Join-Path $env:USERPROFILE ".config\opencode"

if (Get-Command opencode -ErrorAction SilentlyContinue) {
  Write-Host "opencode found: $((Get-Command opencode).Source)"
} else {
  Write-Warning "opencode is not on PATH yet - install it, then re-run."
}

New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null

$backup = $null
if (Test-Path "$dest\service.json") {
  $backup = Join-Path $env:TEMP "opencode-service.json"
  Copy-Item "$dest\service.json" $backup -Force
}

if (Test-Path $dest) { Move-Item $dest "$dest.bak-$(Get-Date -Format yyyyMMddHHmm)" }

git clone $repo $dest
if ($backup) { Copy-Item $backup "$dest\service.json" -Force }

Write-Host ""
Write-Host "Config in place. Remaining manual steps:" -ForegroundColor Cyan
Write-Host "  1. set the env vars listed in secrets.example"
Write-Host "  2. open opencode, run /mcps, sign in to each remote server"
Write-Host "  3. opencode mcp list   # expect: connected"
```

macOS/Linux equivalent is the same logic with `ln -s` and `~/.config/opencode`.

---

## Keeping devices honest

Pick one of these, in order of effort:

1. **Manual pull** — `git -C ~/.config/opencode pull` when you sit down. Fine for two devices.
2. **Shell alias** — `alias oc='git -C ~/.config/opencode pull -q; opencode "$@"'` so opening the tool syncs first.
3. **Scheduled pull** — a Task Scheduler job / `launchd` agent / cron running `git pull` every 15 min. Config reloads on change, so no restart.
4. **Bidirectional auto-commit** — a `post-save` watcher that commits and pushes. Powerful, but noisy; only worth it if you edit config often.

For two or three devices, #2 is the sweet spot.

---

## Gotchas

- **Don't sync `~/.local/share/opencode`.** That's sessions, database, and logs — device state, not config. Copying it across machines causes confusing session/state bugs.
- **`service.json` is per machine.** Back it up before replacing the config dir, restore after.
- **Project config beats global config.** If a repo has its own `opencode.jsonc`, it overrides your global settings there. That's a feature — but it's why "it works everywhere except this one project."
- **Skills are keyed by path, not frontmatter `name`.** `.opencode/skills/release/SKILL.md` has ID `release`.
- **Duplicate skill IDs:** the later source wins (`~/.config/opencode/skills` < project `.opencode/skills` < explicit `skills` entries).
- **HTTP catalogs cache.** Bump `version` in `index.json` on every change.
- **Secrets in git history are forever.** If you ever commit a key, rotate it — don't just delete the line.

---

## For hermes (and any other harness)

The same trick applies to any agent CLI that reads a config directory:

1. Put that tool's config dir in the same repo (or a sibling folder in `~/dotfiles`).
2. Link it into the location the tool expects on each device.
3. Point both harnesses at the same `agent-skills` folder.

If hermes supports a skills/config *path* setting, prefer pointing at a shared path over duplicating files — one copy, two consumers.
