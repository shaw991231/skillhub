type DevProxyConfig = {
  target: string
  changeOrigin: boolean
}

const DEFAULT_PROXY_TARGET = 'http://localhost:8080'

export function createDevServerProxy(target = DEFAULT_PROXY_TARGET) {
  const proxyConfig: DevProxyConfig = {
    target,
    // Preserve the frontend origin so OAuth providers can return through localhost:3000 in development.
    changeOrigin: false,
  }

  return {
    '/api': proxyConfig,
    '/oauth2': proxyConfig,
    '/login/oauth2': proxyConfig,
  }
}
