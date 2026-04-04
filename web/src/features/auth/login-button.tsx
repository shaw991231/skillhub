import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/ui/button'
import { getAuthProvidersRuntimeConfig } from '@/api/client'
import { useAuthMethods } from './use-auth-methods'

interface LoginButtonProps {
  returnTo?: string
}

function ProviderIcon({ provider }: { provider: string }) {
  switch (provider) {
    case 'feishu':
      // Feishu logo (simplified bird shape) in Feishu brand blue #3370ff
      return (
        <svg className="w-5 h-5 mr-3" viewBox="0 0 24 24" fill="#3370ff">
          <path d="M3.5 7.4C5.2 4 8.9 2 12.8 2.3c2.5.2 4.7 1.5 6.1 3.5.7 1 1.1 2.2 1.2 3.4.1 1.3-.2 2.5-.8 3.6-.4.7-.9 1.3-1.5 1.8-.5.4-1.1.7-1.7.9-.4.1-.8.2-1.2.2h-.5c-.3 0-.6-.1-.8-.3-.3-.2-.4-.5-.4-.8 0-.4.2-.7.5-.9.2-.1.5-.2.7-.2.3 0 .5 0 .7-.1.4-.1.7-.3 1-.6.4-.4.7-.8.9-1.3.3-.7.4-1.4.3-2.1-.1-.8-.4-1.5-.9-2.1-1-1.4-2.6-2.4-4.3-2.6-2.9-.3-5.6 1.2-7 3.8-.4.8-.6 1.7-.6 2.6 0 1 .3 1.9.8 2.7.4.6.9 1.1 1.5 1.4.3.2.5.4.6.7.1.3.1.6-.1.9-.2.3-.5.5-.8.5-.2 0-.3 0-.5-.1-.9-.5-1.7-1.2-2.3-2C3.2 12 2.7 10.3 3 8.5c.1-.4.3-.8.5-1.1z" />
        </svg>
      )
    case 'github':
      // GitHub mark
      return (
        <svg className="w-5 h-5 mr-3" fill="currentColor" viewBox="0 0 24 24">
          <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
        </svg>
      )
    default:
      // Generic circle icon for unknown providers
      return (
        <svg className="w-5 h-5 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <circle cx="12" cy="12" r="10" />
        </svg>
      )
  }
}

/**
 * Renders OAuth login buttons from the auth-method catalog returned by the backend.
 */
export function LoginButton({ returnTo }: LoginButtonProps) {
  const { t } = useTranslation()
  const { data, isLoading } = useAuthMethods(returnTo)
  const authProvidersConfig = getAuthProvidersRuntimeConfig()

  const providers = (data ?? [])
    .filter((method) => method.methodType === 'OAUTH_REDIRECT')
    .filter((method) => !method.hidden)
    .filter((method) => {
      const providerKey = method.provider.toLowerCase()
      if (providerKey === 'feishu') {
        return authProvidersConfig.feishu.visible
      }
      if (providerKey === 'github') {
        return authProvidersConfig.github.visible
      }
      return true
    })

  if (isLoading) {
    return (
      <div className="space-y-3">
        <Button className="w-full h-12" disabled>
          <div className="w-5 h-5 rounded-full animate-shimmer mr-3" />
          {t('loginButton.loading')}
        </Button>
      </div>
    )
  }

  if (providers.length === 0) {
    return null
  }

  return (
    <div className="space-y-3">
      {providers.map((provider) => (
        <Button
          key={provider.id}
          className="w-full h-12 text-base"
          variant="outline"
          onClick={() => {
            window.location.href = provider.actionUrl
          }}
        >
          <ProviderIcon provider={provider.provider} />
          {t('loginButton.loginWith', { name: provider.displayName })}
        </Button>
      ))}
    </div>
  )
}
