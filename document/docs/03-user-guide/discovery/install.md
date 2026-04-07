---
title: 安装使用
sidebar_position: 2
description: 安装和使用技能
---

# 安装使用

## 通过 CLI 安装

### 安装到 OpenClaw（默认）

```bash
clawhub install my-skill
clawhub install team-name--my-skill
```

### 安装到 Claude Code

```bash
# 方式一：使用 --dir 参数
clawhub install my-skill --dir ~/.claude/skills

# 方式二：使用 CLAWHUB_WORKDIR 环境变量
CLAWHUB_WORKDIR=~/.claude/skills clawhub install my-skill
```

### 安装到自定义目录

```bash
clawhub install my-skill --dir /path/to/your/skills
```

### 安装指定版本

```bash
clawhub install my-skill@1.2.0
```

### 按标签安装

```bash
clawhub install my-skill@beta
```

## 安装目录

SkillHub 支持将技能安装到多个不同的客户端目录：

| 客户端 | 默认安装目录 | 说明 |
|--------|-------------|------|
| **OpenClaw** | `~/.openclaw/skills/` | 默认客户端 |
| **Claude Code** | `~/.claude/skills/` | Claude Code 官方技能目录 |
| **自定义** | 用户指定 | 任意兼容 OpenSkills 协议的客户端 |

### 客户端发现优先级

兼容 OpenSkills 协议的客户端按以下优先级发现技能：

| 优先级 | 路径 | 说明 |
|--------|------|------|
| 1 | `./.agent/skills/` | 项目级，universal 模式 |
| 2 | `~/.agent/skills/` | 全局级，universal 模式 |
| 3 | `./.claude/skills/` | 项目级，Claude 默认 |
| 4 | `~/.claude/skills/` | 全局级，Claude 默认 |

## 在 Claude Code 中使用

安装到 `~/.claude/skills/` 目录后，技能会被 Claude Code 自动发现和加载。

### 验证安装

```bash
# 查看已安装的 Claude Code 技能
ls ~/.claude/skills/

# 查看具体技能内容
ls ~/.claude/skills/my-skill/
```

### 在 Web UI 中获取安装命令

访问技能详情页，可以选择不同的客户端类型：

1. 访问技能详情页
2. 点击「Claude Code」选项卡
3. 复制显示的安装命令
4. 在终端中执行

## Windows 用户

### PowerShell

```powershell
# 安装到 Claude Code
clawhub install my-skill --dir "$env:USERPROFILE\.claude\skills"

# 或使用环境变量
$env:CLAWHUB_WORKDIR="$env:USERPROFILE\.claude\skills"
clawhub install my-skill
```

### CMD

```cmd
clawhub install my-skill --dir "%USERPROFILE%\.claude\skills"
```

## 下一步

- [评分与收藏](./ratings) - 反馈和收藏技能
