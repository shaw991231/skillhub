import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { InstallCommand, buildInstallCommand, buildInstallTarget, getBaseUrl, CLIENTS } from './install-command'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

describe('install-command', () => {
  const originalWindow = globalThis.window

  function setMockWindow(appBaseUrl?: string) {
    const location = {
      protocol: 'https:',
      host: 'fallback.example.com',
    } satisfies Pick<Location, 'protocol' | 'host'>

    Object.defineProperty(globalThis, 'window', {
      configurable: true,
      writable: true,
      value: {
        __SKILLHUB_RUNTIME_CONFIG__: {
          appBaseUrl,
        },
        location,
      } satisfies {
        location: Pick<Location, 'protocol' | 'host'>
      } & {
        __SKILLHUB_RUNTIME_CONFIG__: {
          appBaseUrl?: string
        }
      },
    })
  }

  afterEach(() => {
    if (originalWindow) {
      Object.defineProperty(globalThis, 'window', {
        configurable: true,
        writable: true,
        value: originalWindow,
      })
      return
    }
    Reflect.deleteProperty(globalThis, 'window')
  })

  describe('buildInstallTarget', () => {
    it('uses the plain slug for the global namespace', () => {
      expect(buildInstallTarget('global', 'my-skill')).toBe('my-skill')
    })

    it('prefixes non-global namespaces in the install target', () => {
      expect(buildInstallTarget('team-alpha', 'my-skill')).toBe('team-alpha--my-skill')
    })
  })

  describe('buildInstallCommand', () => {
    const baseUrl = 'https://skill.xfyun.cn'

    it('generates OpenClaw command without --dir by default', () => {
      expect(buildInstallCommand('global', 'my-skill', baseUrl, 'openclaw')).toBe(
        'npx clawhub install my-skill --registry https://skill.xfyun.cn',
      )
    })

    it('generates Claude Code command with --dir', () => {
      expect(buildInstallCommand('global', 'my-skill', baseUrl, 'claude-code')).toBe(
        'npx clawhub install my-skill --registry https://skill.xfyun.cn --dir ~/.claude/skills',
      )
    })

    it('generates command with custom directory', () => {
      expect(buildInstallCommand('global', 'my-skill', baseUrl, 'openclaw', '~/.custom/skills')).toBe(
        'npx clawhub install my-skill --registry https://skill.xfyun.cn --dir ~/.custom/skills',
      )
    })

    it('handles namespace prefix for team skills', () => {
      expect(buildInstallCommand('team-alpha', 'my-skill', baseUrl, 'claude-code')).toBe(
        'npx clawhub install team-alpha--my-skill --registry https://skill.xfyun.cn --dir ~/.claude/skills',
      )
    })

    it('generates PS-native multi-step command on Windows', () => {
      const cmd = buildInstallCommand('global', 'my-skill', baseUrl, 'claude-code', undefined, 'windows')
      expect(cmd).toContain('$env:USERPROFILE/.claude/skills')
      expect(cmd).toContain('New-Item -ItemType Directory -Force')
      expect(cmd).toContain('npx clawhub install my-skill --registry https://skill.xfyun.cn')
    })
  })

  describe('getBaseUrl', () => {
    it('uses the runtime app base url when available', () => {
      setMockWindow('https://app.example.com')
      expect(getBaseUrl()).toBe('https://app.example.com')
    })

    it('falls back to the browser origin when the app base url is missing', () => {
      setMockWindow()
      expect(getBaseUrl()).toBe('https://fallback.example.com')
    })

    it('falls back to browser origin when app base url is localhost', () => {
      setMockWindow('http://localhost')
      expect(getBaseUrl()).toBe('https://fallback.example.com')
    })

    it('falls back to browser origin when app base url contains localhost', () => {
      setMockWindow('http://localhost:8080')
      expect(getBaseUrl()).toBe('https://fallback.example.com')
    })
  })

  describe('CLIENTS', () => {
    it('defines OpenClaw client', () => {
      const openclaw = CLIENTS.find((c) => c.id === 'openclaw')
      expect(openclaw).toBeDefined()
      expect(openclaw?.defaultDir).toBe('~/.openclaw/skills')
    })

    it('defines Claude Code client', () => {
      const claudeCode = CLIENTS.find((c) => c.id === 'claude-code')
      expect(claudeCode).toBeDefined()
      expect(claudeCode?.defaultDir).toBe('~/.claude/skills')
    })
  })

  describe('InstallCommand component', () => {
    it('renders the install command in a more compact code block', () => {
      setMockWindow('http://localhost:3000')

      const html = renderToStaticMarkup(
        createElement(InstallCommand, { namespace: 'global', slug: 'meeting-minutes-generator' })
      )

      expect(html).toContain('px-4 py-3')
      expect(html).toContain('leading-relaxed')
      expect(html).toContain('break-all')
    })

    it('renders client selector buttons', () => {
      setMockWindow('http://localhost:3000')

      const html = renderToStaticMarkup(
        createElement(InstallCommand, { namespace: 'global', slug: 'my-skill' })
      )

      expect(html).toContain('OpenClaw')
      expect(html).toContain('Claude Code')
    })
  })
})
