import { describe, expect, it } from 'vitest'
import { createDevServerProxy } from './dev-proxy-config'

describe('createDevServerProxy', () => {
  it('proxies api and oauth routes through the backend without rewriting the host header', () => {
    const proxy = createDevServerProxy()

    expect(proxy['/api']).toMatchObject({
      target: 'http://localhost:8080',
      changeOrigin: false,
    })
    expect(proxy['/oauth2']).toMatchObject({
      target: 'http://localhost:8080',
      changeOrigin: false,
    })
    expect(proxy['/login/oauth2']).toMatchObject({
      target: 'http://localhost:8080',
      changeOrigin: false,
    })
  })
})
