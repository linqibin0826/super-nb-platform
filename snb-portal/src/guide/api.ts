/** 通用引导已读 API(同源 /guide/v1,生产由 Caddy 反代 snb-platform)。
 * 鉴权照 invoiceFetch:Bearer + 临期先刷 + 401 刷新重试一次;未登录抛 GuideAuthError 由 hook 降级。 */
import { getToken, isExpiringSoon } from '../auth/tokens'
import { refreshTokens } from '../auth/refresh'

const BASE = '/guide/v1'

export class GuideAuthError extends Error {
  constructor() {
    super('未登录或登录已过期')
    this.name = 'GuideAuthError'
  }
}

async function authed(path: string, init: RequestInit = {}, retried = false): Promise<Response> {
  if (!retried && isExpiringSoon()) await refreshTokens()
  const token = getToken()
  if (!token) throw new GuideAuthError()
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: { Authorization: `Bearer ${token}`, ...(init.headers ?? {}) },
  })
  if (res.status === 401) {
    if (!retried && (await refreshTokens())) return authed(path, init, true)
    throw new GuideAuthError()
  }
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res
}

/** 我的已读 key 集合 */
export async function getGuideAcks(): Promise<string[]> {
  const res = await authed('/acks')
  const body = (await res.json()) as { keys?: string[] }
  return body.keys ?? []
}

/** 幂等标记已读 */
export async function postGuideAck(key: string): Promise<void> {
  await authed(`/acks/${encodeURIComponent(key)}`, { method: 'POST' })
}
