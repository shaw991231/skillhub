---
title: Install & Use
sidebar_position: 2
description: Install and use skills
---

# Install & Use

## Install via CLI

### Install to OpenClaw (Default)

```bash
clawhub install my-skill
clawhub install team-name--my-skill
```

### Install to Claude Code

```bash
# Method 1: Using --dir flag
clawhub install my-skill --dir ~/.claude/skills

# Method 2: Using CLAWHUB_WORKDIR environment variable
CLAWHUB_WORKDIR=~/.claude/skills clawhub install my-skill
```

### Install to Custom Directory

```bash
clawhub install my-skill --dir /path/to/your/skills
```

### Install Specific Version

```bash
clawhub install my-skill@1.2.0
```

### Install by Tag

```bash
clawhub install my-skill@beta
```

## Installation Directory

SkillHub supports installing skills to multiple client directories:

| Client | Default Directory | Description |
|--------|------------------|-------------|
| **OpenClaw** | `~/.openclaw/skills/` | Default client |
| **Claude Code** | `~/.claude/skills/` | Claude Code official skill directory |
| **Custom** | User specified | Any OpenSkills-compatible client |

### Client Discovery Priority

OpenSkills-compatible clients discover skills in the following priority order:

| Priority | Path | Description |
|----------|------|-------------|
| 1 | `./.agent/skills/` | Project level, universal mode |
| 2 | `~/.agent/skills/` | Global level, universal mode |
| 3 | `./.claude/skills/` | Project level, Claude default |
| 4 | `~/.claude/skills/` | Global level, Claude default |

## Use in Claude Code

After installing to `~/.claude/skills/`, skills are automatically discovered and loaded by Claude Code.

### Verify Installation

```bash
# List installed Claude Code skills
ls ~/.claude/skills/

# View specific skill contents
ls ~/.claude/skills/my-skill/
```

### Get Install Command from Web UI

Visit the skill detail page to select different client types:

1. Navigate to the skill detail page
2. Click the "Claude Code" tab
3. Copy the displayed install command
4. Execute in your terminal

## Windows Users

### PowerShell

```powershell
# Install to Claude Code
clawhub install my-skill --dir "$env:USERPROFILE\.claude\skills"

# Or use environment variable
$env:CLAWHUB_WORKDIR="$env:USERPROFILE\.claude\skills"
clawhub install my-skill
```

### CMD

```cmd
clawhub install my-skill --dir "%USERPROFILE%\.claude\skills"
```

## Next Steps

- [Ratings & Stars](./ratings) - Feedback and favorite skills
