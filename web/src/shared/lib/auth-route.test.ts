import { afterEach, describe, expect, it, vi } from 'vitest'
import { isRedirect } from '@tanstack/react-router'
import { buildReturnTo, createRequireAuth } from './auth-route'

describe('auth-route', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('buildReturnTo preserves pathname search and hash', () => {
    expect(buildReturnTo({
      pathname: '/space/global/caldav-calendar',
      searchStr: '?tab=files',
      hash: '#readme',
    })).toBe('/space/global/caldav-calendar?tab=files#readme')
  })

  it('buildReturnTo keeps the active dev loopback origin instead of forcing localhost', () => {
    vi.stubGlobal('window', {
      location: {
        origin: 'http://127.0.0.1:3000',
        hostname: '127.0.0.1',
        port: '3000',
      },
    })

    expect(buildReturnTo({
      pathname: '/dashboard/publish',
      searchStr: '?draft=1',
      hash: '#summary',
    })).toBe('http://127.0.0.1:3000/dashboard/publish?draft=1#summary')
  })

  it('createRequireAuth redirects unauthenticated users to login with returnTo', async () => {
    const requireAuth = createRequireAuth(async () => null)

    await expect(requireAuth({
      location: {
        pathname: '/space/global/caldav-calendar',
        searchStr: '?tab=files',
        hash: '#readme',
      },
    })).rejects.toSatisfy((error: unknown) => {
      expect(isRedirect(error)).toBe(true)
      if (!isRedirect(error)) {
        return false
      }
      expect(error.options.to).toBe('/login')
      expect(error.options.search).toEqual({
        returnTo: '/space/global/caldav-calendar?tab=files#readme',
      })
      return true
    })
  })

  it('createRequireAuth returns the current user when authenticated', async () => {
    const user = { userId: 'user-1' }
    const getCurrentUser = vi.fn(async () => user)
    const requireAuth = createRequireAuth(getCurrentUser)

    await expect(requireAuth({
      location: { pathname: '/dashboard' },
    })).resolves.toEqual({ user })
    expect(getCurrentUser).toHaveBeenCalledTimes(1)
  })
})
