import {
  isTombstone,
  readAuthCookie,
  writeAuthCookie,
  writeAuthTombstone,
} from './cookie'

// localStorage 契约与主站 stores/auth.ts:11-14 完全一致，key 名不得改。
// A 方案 SSO 后 localStorage 退居「本源工作副本」，跨源真源是父域 cookie（见 cookie.ts），
// 启动/聚焦/刷新前都要 reconcileFromCookie 对账。
export const AUTH_TOKEN_KEY = 'auth_token'
export const AUTH_USER_KEY = 'auth_user'
export const REFRESH_TOKEN_KEY = 'refresh_token'
export const TOKEN_EXPIRES_AT_KEY = 'token_expires_at'

export interface AuthUser {
  id: number
  email: string
}

const AUTH_EVENT = 'snb-studio-auth'

export function getToken(): string | null {
  return localStorage.getItem(AUTH_TOKEN_KEY)
}

let cachedRaw: string | null = null
let cachedUser: AuthUser | null = null

// useSyncExternalStore 的 getSnapshot：raw 未变必须返回同一引用，否则无限重渲染
export function getUserSnapshot(): AuthUser | null {
  const raw = localStorage.getItem(AUTH_USER_KEY)
  if (raw === cachedRaw) return cachedUser
  cachedRaw = raw
  try {
    cachedUser = raw ? (JSON.parse(raw) as AuthUser) : null
  } catch {
    cachedUser = null
  }
  return cachedUser
}

export function isLoggedIn(): boolean {
  return getToken() !== null && getUserSnapshot() !== null
}

export function isExpiringSoon(withinMs = 60_000): boolean {
  const at = Number(localStorage.getItem(TOKEN_EXPIRES_AT_KEY))
  return Number.isFinite(at) && at > 0 && Date.now() > at - withinMs
}

export function setTokens(access: string, refresh: string, expiresInSec: number): void {
  localStorage.setItem(AUTH_TOKEN_KEY, access)
  localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
  localStorage.setItem(TOKEN_EXPIRES_AT_KEY, String(Date.now() + expiresInSec * 1000))
  mirrorCookieFromLocal()
  notify()
}

export function clearTokens(): void {
  clearLocal()
  // 会话终结（登出/刷新彻底失败）→ 落墓碑，让主站等其他源尽快跟上；
  // 服务端撤销 refresh token 才是真登出，这里只管 UI 收敛
  writeAuthTombstone()
  notify()
}

/**
 * 与父域 cookie 对账（幂等，变更才 notify）：
 * - cookie 会话且 token 与本地不同：谁的 exp 新收养谁（对方刚刷新过 → 收养；本地更新 → 镜像出去）
 * - cookie 墓碑：本地有会话则清（别的源登出了）
 * - cookie 不存在：本地有会话则镜像出去（存量登录态迁移）
 */
export function reconcileFromCookie(): void {
  const cookie = readAuthCookie()
  const localToken = localStorage.getItem(AUTH_TOKEN_KEY)

  if (!cookie) {
    if (localToken) mirrorCookieFromLocal()
    return
  }

  if (isTombstone(cookie)) {
    if (localToken) {
      clearLocal()
      notify()
    }
    return
  }

  if (cookie.at === localToken) return

  const localExp = Number(localStorage.getItem(TOKEN_EXPIRES_AT_KEY)) || 0
  if (cookie.exp > localExp) {
    localStorage.setItem(AUTH_TOKEN_KEY, cookie.at)
    localStorage.setItem(REFRESH_TOKEN_KEY, cookie.rt)
    localStorage.setItem(TOKEN_EXPIRES_AT_KEY, String(cookie.exp))
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(cookie.user))
    notify()
  } else {
    mirrorCookieFromLocal()
  }
}

/** 本地四件套 → cookie。user 缺失时不写（半截会话镜像出去只会害别的源） */
function mirrorCookieFromLocal(): void {
  const at = localStorage.getItem(AUTH_TOKEN_KEY)
  const rt = localStorage.getItem(REFRESH_TOKEN_KEY)
  const user = getUserSnapshot()
  if (!at || !rt || !user) return
  writeAuthCookie({
    at,
    rt,
    exp: Number(localStorage.getItem(TOKEN_EXPIRES_AT_KEY)) || 0,
    user: { id: user.id, email: user.email },
  })
}

function clearLocal(): void {
  for (const key of [AUTH_TOKEN_KEY, AUTH_USER_KEY, REFRESH_TOKEN_KEY, TOKEN_EXPIRES_AT_KEY]) {
    localStorage.removeItem(key)
  }
}

export function subscribeAuth(cb: () => void): () => void {
  const onStorage = (e: StorageEvent) => {
    if (e.key === null || e.key === AUTH_TOKEN_KEY || e.key === AUTH_USER_KEY) cb()
  }
  // cookie 变更不产生事件：切回页面时对账一次（主站登录/登出/轮换都靠这里接上）
  const onFocus = () => reconcileFromCookie()
  window.addEventListener('storage', onStorage)
  window.addEventListener(AUTH_EVENT, cb)
  window.addEventListener('focus', onFocus)
  document.addEventListener('visibilitychange', onFocus)
  return () => {
    window.removeEventListener('storage', onStorage)
    window.removeEventListener(AUTH_EVENT, cb)
    window.removeEventListener('focus', onFocus)
    document.removeEventListener('visibilitychange', onFocus)
  }
}

function notify(): void {
  window.dispatchEvent(new Event(AUTH_EVENT))
}
