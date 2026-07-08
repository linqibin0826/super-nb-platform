import { clearTokens, getToken, isExpiringSoon } from './tokens'
import { refreshTokens } from './refresh'

const API_BASE = '/api/v1'

export class ApiError extends Error {
  readonly status: number
  readonly code: number

  constructor(status: number, code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export function loginUrl(): string {
  // 子域名部署（studio.super-nb.me）：登录在主站完成，不做跨域回跳——
  // 登录后切回本站，聚焦对账（tokens.ts reconcileFromCookie）自动接上会话
  const h = location.hostname
  if ((h === 'super-nb.me' || h.endsWith('.super-nb.me')) && h !== 'super-nb.me') {
    return 'https://super-nb.me/login'
  }
  // 同源路径部署：带回跳（/studio 在 fork 登录白名单里）
  return '/login?redirect=' + encodeURIComponent(import.meta.env.BASE_URL)
}

export function keysUrl(): string {
  // 子域名部署下 /keys 是本站相对路径，会被 studio 自己的 Caddy try_files 兜底成
  // 本站 SPA（无此路由）而不是控制台真正的 Keys 页——和 loginUrl 一样必须给绝对地址
  const h = location.hostname
  if ((h === 'super-nb.me' || h.endsWith('.super-nb.me')) && h !== 'super-nb.me') {
    return 'https://super-nb.me/keys'
  }
  return '/keys'
}

export async function apiFetch<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
  // 临期先刷（刷失败也继续发——老 token 可能仍活着，401 分支兜底）
  if (!retried && isExpiringSoon()) await refreshTokens()

  const token = getToken()
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init.headers ?? {}),
    },
  })

  if (res.status === 401) {
    if (!retried && (await refreshTokens())) return apiFetch<T>(path, init, true)
    clearTokens()
    throw new ApiError(401, -1, 'unauthorized')
  }

  const body = (await res.json().catch(() => null)) as
    | { code?: number; message?: string; data?: T }
    | null
  if (!res.ok) throw new ApiError(res.status, body?.code ?? -1, body?.message ?? res.statusText)
  if (body && typeof body === 'object' && typeof body.code === 'number') {
    if (body.code === 0) return body.data as T
    throw new ApiError(res.status, body.code, body.message ?? 'api error')
  }
  return body as unknown as T
}
