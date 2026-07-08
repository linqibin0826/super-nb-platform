/**
 * 主题跨子域名共享的父域 cookie 契约（snb_theme）。四站消费，改必四边同步：
 *   fork utils/themeCookie.ts ↔ help .vitepress/{config.mts, theme/Layout.vue} ↔ activity activity.html
 * 语义：'dark'/'light'=显式选择；cookie 缺席=跟随系统。studio 是独立 origin，靠 cookie + 聚焦对账跟随主站/活动/指南。
 */
export type Theme = 'dark' | 'light'

const THEME_COOKIE_NAME = 'snb_theme'
const MAX_AGE = 365 * 24 * 3600

/** 生产（*.super-nb.me）种父域 cookie 全子域共享；本地开发落 host-only。与 auth cookie domainAttr 同款。 */
function domainAttr(): string {
  const h = location.hostname
  return h === 'super-nb.me' || h.endsWith('.super-nb.me') ? '; Domain=.super-nb.me' : ''
}

export function readThemeCookie(): Theme | null {
  const m = document.cookie.match(/(?:^|;\s*)snb_theme=([^;]*)/)
  if (!m) return null
  const v = decodeURIComponent(m[1])
  return v === 'dark' || v === 'light' ? v : null
}

export function writeThemeCookie(v: Theme): void {
  document.cookie = `${THEME_COOKIE_NAME}=${v}${domainAttr()}; Path=/; Secure; SameSite=Lax; Max-Age=${MAX_AGE}`
}

/** 回「跟随系统」：删 cookie。Domain 必须与写入一致否则删不掉。 */
export function clearThemeCookie(): void {
  document.cookie = `${THEME_COOKIE_NAME}=${domainAttr()}; Path=/; Secure; SameSite=Lax; Max-Age=0`
}

export function systemTheme(): Theme {
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/** 有效主题：cookie 显式值优先，否则跟随系统。 */
export function effectiveTheme(): Theme {
  return readThemeCookie() ?? systemTheme()
}

/**
 * 一次性迁移旧 localStorage 键 → cookie，随后无条件退役该键。
 * 无条件删除杜绝「删 cookie 回跟随系统后、下次启动又被旧键复活」的复活 bug。
 */
export function migrateLegacyThemeKey(legacyKey: string): void {
  try {
    const legacy = localStorage.getItem(legacyKey)
    if (!readThemeCookie() && (legacy === 'dark' || legacy === 'light')) writeThemeCookie(legacy)
    localStorage.removeItem(legacyKey)
  } catch {
    // localStorage 不可用时不迁移
  }
}
