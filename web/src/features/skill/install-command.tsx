import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Check, Copy, Terminal, Code } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { useCopyToClipboard } from '@/shared/lib/clipboard'

interface InstallCommandProps {
  namespace: string
  slug: string
  version?: string
}

// 客户端类型定义
export type ClientType = 'openclaw' | 'claude-code' | 'custom'

// 客户端配置
export interface ClientConfig {
  id: ClientType
  name: string
  defaultDir: string
  supportsCustomDir: boolean
  icon: typeof Terminal
}

// 支持的客户端列表
export const CLIENTS: ClientConfig[] = [
  {
    id: 'openclaw',
    name: 'OpenClaw',
    defaultDir: '~/.openclaw/skills',
    supportsCustomDir: true,
    icon: Terminal,
  },
  {
    id: 'claude-code',
    name: 'Claude Code',
    defaultDir: '~/.claude/skills',
    supportsCustomDir: true,
    icon: Code,
  },
]

export function buildInstallTarget(namespace: string, slug: string): string {
  return namespace === 'global' ? slug : `${namespace}--${slug}`
}

export function getBaseUrl(): string {
  if (typeof window === 'undefined') {
    return ''
  }
  const runtimeConfig = window.__SKILLHUB_RUNTIME_CONFIG__
  const configuredUrl = runtimeConfig?.appBaseUrl
  // Use configured URL only if it's set and not localhost
  if (configuredUrl && !configuredUrl.includes('localhost')) {
    return configuredUrl
  }
  // Fallback to current page origin
  return `${window.location.protocol}//${window.location.host}`
}

function detectPlatform(): string {
  if (typeof navigator !== 'undefined' && /Win/i.test(navigator.userAgent)) return 'windows'
  return ''
}

export function buildInstallCommand(
  namespace: string,
  slug: string,
  baseUrl: string,
  clientType: ClientType = 'openclaw',
  customDir?: string,
  platform?: string,
): string {
  const installTarget = buildInstallTarget(namespace, slug)
  const client = CLIENTS.find((c) => c.id === clientType)
  const installDir = customDir || client?.defaultDir
  const base = `npx clawhub install ${installTarget} --registry ${baseUrl}`

  // Windows: generate multi-step PowerShell command to create skills dir first
  if (platform === 'windows') {
    const unixDir = installDir || '~/.claude/skills'
    const psDir = unixDir.startsWith('~') ? `$env:USERPROFILE${unixDir.slice(1)}` : unixDir
    return `$env:CHDIR="${psDir}"; New-Item -ItemType Directory -Force -Path $env:CHDIR | Out-Null; cd $env:CHDIR; ${base} --dir $env:CHDIR`
  }

  // OpenClaw 使用默认目录时不显示 --dir 参数
  if (clientType === 'openclaw' && !customDir) {
    return base
  }

  // Claude Code 或自定义目录时显示 --dir 参数
  return `${base} --dir ${installDir}`
}

export function InstallCommand({ namespace, slug }: InstallCommandProps) {
  const { t } = useTranslation()
  const [copied, copy] = useCopyToClipboard()
  const [selectedClient, setSelectedClient] = useState<ClientType>('openclaw')
  const [showCustomDir, setShowCustomDir] = useState(false)
  const [customDir, setCustomDir] = useState('')

  const baseUrl = useMemo(() => getBaseUrl(), [])
  const platform = useMemo(() => detectPlatform(), [])

  const command = useMemo(
    () => buildInstallCommand(namespace, slug, baseUrl, selectedClient, customDir || undefined, platform),
    [baseUrl, namespace, slug, selectedClient, customDir, platform]
  )

  const handleCopy = async () => {
    try {
      await copy(command)
    } catch (err) {
      console.error('Failed to copy:', err)
    }
  }

  const handleClientChange = (clientType: ClientType) => {
    setSelectedClient(clientType)
    // 如果选择的是 Claude Code，自动显示自定义目录输入
    if (clientType === 'claude-code') {
      setShowCustomDir(true)
    }
  }

  const currentClient = CLIENTS.find((c) => c.id === selectedClient)

  return (
    <div className="space-y-3">
      {/* 客户端选择器 */}
      <div className="flex items-center gap-2">
        {CLIENTS.map((client) => {
          const ClientIcon = client.icon
          return (
            <Button
              key={client.id}
              type="button"
              variant={selectedClient === client.id ? 'default' : 'outline'}
              size="sm"
              onClick={() => handleClientChange(client.id)}
              className="flex items-center gap-2"
            >
              <ClientIcon className="h-4 w-4" />
              {client.name}
            </Button>
          )
        })}
      </div>

      {/* 安装命令显示区域 */}
      <div className="relative overflow-hidden rounded-xl border border-border/60 bg-muted/50">
        <Button
          type="button"
          variant="ghost"
          size="icon"
          onClick={handleCopy}
          title={copied ? t('copyButton.copied') : t('copyButton.copy')}
          aria-label={copied ? t('copyButton.copied') : t('copyButton.copy')}
          className="absolute right-2 top-2 z-10 h-8 w-8 rounded-md bg-background/80 backdrop-blur hover:bg-background"
        >
          {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
        </Button>
        <pre className="px-4 py-3 pr-14 whitespace-pre-wrap break-all">
          <code className="font-mono text-[13px] leading-relaxed text-foreground whitespace-pre-wrap break-all sm:text-sm">
            {command}
          </code>
        </pre>
      </div>

      {/* 自定义目录输入（仅 Claude Code 或高级选项） */}
      {showCustomDir && (
        <div className="flex items-center gap-2">
          <input
            type="text"
            placeholder={currentClient?.defaultDir}
            value={customDir}
            onChange={(e) => setCustomDir(e.target.value)}
            className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
          />
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => setShowCustomDir(false)}
          >
            {t('cancel')}
          </Button>
        </div>
      )}
    </div>
  )
}
