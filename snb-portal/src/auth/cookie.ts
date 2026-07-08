// 父域 cookie 登录态契约（A 方案 SSO）：主站 fork 与 studio 共用，两边字段/属性必须一致。
// fork 侧对应 frontend/src/utils/authCookie.ts——改契约必须两边同步改。
//
// 为什么 cookie 是「唯一真源」：sub2api 的 refresh token 严格一次性轮换（旧 token 用后立删），
// 主站和 studio 各自持有一份就会互相顶掉对方会话。所以谁刷新了 token 都要回写 cookie，
// 读方在启动/聚焦/刷新前跟 cookie 对账（reconcile），刷新失败先看 cookie 是否已被别家换新再清会话。
//
// 墓碑（{out:true}）：登出时不能只删 cookie——「cookie 不存在」和「从没写过 cookie 的存量登录态」
// 无法区分，后者要做 localStorage→cookie 的迁移镜像，会把刚登出的会话复活。
// 服务端撤销 refresh token 才是真登出，墓碑只负责让各源的 UI 尽快跟上。

import { PARENT_DOMAIN, isParentDomainHost } from '../config'

export interface AuthCookieSession {
  v: 1
  /** access token（JWT） */
  at: string
  /** refresh token */
  rt: string
  /** access token 过期时间戳（ms），对应 localStorage 的 token_expires_at */
  exp: number
  /** 最小用户信息，够 studio 渲染登录态即可 */
  user: { id: number; email: string }
}

export interface AuthCookieTombstone {
  v: 1
  out: true
}

export type AuthCookieValue = AuthCookieSession | AuthCookieTombstone

export const AUTH_COOKIE_NAME = '__Secure-snb_auth'
// 30 天信封：只是运输层上限，token 真实有效性由服务端判（refresh TTL 是后端配置）
const SESSION_MAX_AGE = 30 * 24 * 3600
const TOMBSTONE_MAX_AGE = 7 * 24 * 3600

export function isTombstone(v: AuthCookieValue): v is AuthCookieTombstone {
  return 'out' in v && v.out === true
}

/** 生产（*.super-nb.me）种父域 cookie 全子域共享；本地开发/测试落 host-only */
function domainAttr(): string {
  return isParentDomainHost(location.hostname) ? `; Domain=.${PARENT_DOMAIN}` : ''
}

export function readAuthCookie(): AuthCookieValue | null {
  const match = document.cookie.match(
    new RegExp(`(?:^|;\\s*)${AUTH_COOKIE_NAME.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}=([^;]*)`)
  )
  if (!match) return null
  try {
    const parsed = JSON.parse(decodeURIComponent(match[1])) as Partial<AuthCookieSession> &
      Partial<AuthCookieTombstone>
    if (parsed?.v !== 1) return null
    if (parsed.out === true) return { v: 1, out: true }
    if (
      typeof parsed.at === 'string' &&
      typeof parsed.rt === 'string' &&
      typeof parsed.exp === 'number' &&
      typeof parsed.user?.id === 'number' &&
      typeof parsed.user?.email === 'string'
    ) {
      return { v: 1, at: parsed.at, rt: parsed.rt, exp: parsed.exp, user: parsed.user }
    }
  } catch {
    // 脏值当不存在，下一次写入会覆盖
  }
  return null
}

export function writeAuthCookie(session: Omit<AuthCookieSession, 'v'>): void {
  setCookie(JSON.stringify({ v: 1, ...session } satisfies AuthCookieSession), SESSION_MAX_AGE)
}

export function writeAuthTombstone(): void {
  setCookie(JSON.stringify({ v: 1, out: true } satisfies AuthCookieTombstone), TOMBSTONE_MAX_AGE)
}

function setCookie(json: string, maxAge: number): void {
  document.cookie =
    `${AUTH_COOKIE_NAME}=${encodeURIComponent(json)}${domainAttr()}` +
    `; Path=/; Secure; SameSite=Lax; Max-Age=${maxAge}`
}
