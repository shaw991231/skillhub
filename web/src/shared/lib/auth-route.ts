import { redirect } from '@tanstack/react-router'

export type RouteLocationLike = {
  pathname: string
  searchStr?: string
  hash?: string
}

function isDevLoopbackLocation(location: Pick<Location, 'hostname' | 'port'>) {
  return location.port === '3000'
    && (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
}

/**
 * 构建 OAuth 授权后返回的 URL。
 * 在开发环境（localhost:3000）下，使用完整 URL 以确保 OAuth 回调后跳转到前端而非后端。
 */
export function buildReturnTo(location: RouteLocationLike) {
  const path = `${location.pathname}${location.searchStr ?? ''}${location.hash ?? ''}`

  // 开发环境：返回完整的前端 URL
  if (typeof window !== 'undefined' && isDevLoopbackLocation(window.location)) {
    return `${window.location.origin}${path}`
  }

  // 生产环境：使用相对路径
  return path
}

export function createRequireAuth(getCurrentUser: () => Promise<unknown>) {
  return async function requireAuth({ location }: { location: RouteLocationLike }) {
    const user = await getCurrentUser()
    if (!user) {
      throw redirect({
        to: '/login',
        search: { returnTo: buildReturnTo(location) },
      })
    }
    return { user }
  }
}
