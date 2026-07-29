/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  SUPER·NB 明暗主题契约 —— 跨子域名唯一真源（canonical reference implementation）
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * 【为什么这个文件存在】
 * 历史上这套契约是「fork `utils/themeCookie.ts` ↔ snb-portal `src/themeCookie.ts` ↔
 * help `.vitepress/`」三边手抄、「改必三边同步」，活动静态页接入后变成四边。手抄的代价
 * 是每次改都要人肉对齐，漏一边就是「切了一个站，另一个站没跟上」。
 * 2026-07-29 起改成**单一真源**：逻辑只写在这里，四个消费方照抄或直接 vendor。
 *
 * 【分期说明，别读成方向反复】
 * 双档一直在计划内。2026-07-26 的「全站恒暗」是**分期上线的第一步**（先把网吧化改造
 * 整车推上线），浅色皮肤排在第二步；当时把 fork/portal 的 themeCookie 写入口摘掉、
 * help 的三边契约拆掉，是分期期间防回潮的**临时护栏**，注释语气偏终局，但那不是产品
 * 意图。本文件是按原计划补全第二步，把契约装回来。
 *
 * ───────────────────────────────────────────────────────────────────────────
 * 【契约本体】改必**四边同步**（本文件是真源，四边照它对齐）：
 *   ① sub2api fork  —— `frontend/src/utils/themeCookie.ts`（Vue 3）
 *   ② snb-portal    —— `snb-portal/src/themeCookie.ts`（React，也可直接 vendor 本文件）
 *   ③ 活动静态页 ×N —— 抄 `templates/snb-theme.js`（IIFE 版，无模块无框架）
 *   ④ super-nb-learn—— `.vitepress/config.mts`（首帧 boot）+ `.vitepress/theme/Layout.vue`（对账）
 *
 * 【七条硬规则】
 *  1. **父域 cookie `snb_theme` 是跨子域名唯一真源**：
 *     `Domain=.super-nb.me; Path=/; Secure; SameSite=Lax; Max-Age=1年; 非 HttpOnly`。
 *     值 `dark` / `light` = 用户显式选择；**cookie 缺席 = 跟随系统**（prefers-color-scheme）。
 *  2. **切到与系统一致的值 → 删 cookie**，回到「跟随系统」，而不是写一个同值 cookie。
 *     （写同值 cookie 会把用户永久钉死在当前值上，系统换档时站点不跟——这是行为差异不是优化。）
 *  3. **跨源没有 storage 事件**：切一个站，其余站靠**聚焦对账**（`focus` /
 *     `visibilitychange`）重读 cookie 跟上。别指望 storage / BroadcastChannel。
 *  4. **首帧防闪**：`<head>` 里必须内联一段 boot 脚本（在 CSS 之前跑），
 *     否则会先渲染一帧错档再跳（见 `THEME_BOOT_SNIPPET`）。
 *  5. **本地开发退化成 host-only cookie**（非 `*.super-nb.me` 时不带 Domain）。
 *     ⚠️ **删除时 Domain 必须与写入时一致**——Domain 对不上删不掉，是 cookie 删除
 *     最常见的失手处（会表现为「点了跟随系统但没生效」）。
 *  6. **旧 localStorage 键一次性迁移后无条件退役**：fork `theme` / studio
 *     `snb-studio-theme` / help `snb-help-theme`。不退役的话，用户删了 cookie
 *     会被旧键复活成显式选择。
 *  7. `.dark` class **在深色档下绝不能删**——全站大量 Tailwind `dark:` 前缀样式依赖它
 *     存在。删了不是「变浅色」而是「没样式」。切浅色 = 摘掉 `.dark` 让 `:root` 生效。
 *
 * 【两个历史坑，抄的时候别踩】
 *  · Vue SFC 里写深色覆盖一律用**平铺** `.dark .foo { }`，**绝不用 scoped
 *    `:global(.dark)`**——会被 Vue 误编译成裸 `.dark` 命中 `<html>`，历史白屏根因。
 *  · 顶栏带 `backdrop-filter` 时它会成为 fixed 后代的包含块；浮层遮罩必须
 *    `absolute + top:100% + height:100vh`，回到 `fixed + top:Npx + bottom:0` 会塌成零高。
 *    （与主题无关，但同一批文件里最容易被顺手改坏，一并钉在这里。）
 */

export const THEME_COOKIE = 'snb_theme'
/** 父域：让 cookie 对所有 *.super-nb.me 可见（子站 SSO / 主题共享的前提） */
export const THEME_COOKIE_DOMAIN = '.super-nb.me'
/** 1 年 */
export const THEME_COOKIE_MAX_AGE = 31536000
/** 🪦 一次性迁移后无条件退役的旧键：fork / studio / help 各自的 localStorage 遗产 */
export const LEGACY_THEME_KEYS = ['theme', 'snb-studio-theme', 'snb-help-theme'] as const

/** 用户的显式选择 */
export type ThemeChoice = 'dark' | 'light'
/** 偏好：显式选择，或 `null` = 跟随系统 */
export type ThemePref = ThemeChoice | null

const isBrowser = () => typeof document !== 'undefined'

/** 写 / 删 cookie 时用的 Domain 段。
 *  ⚠️ 写与删必须用**同一个**返回值，否则删不掉（Domain 不一致 = 另一枚 cookie）。 */
export function themeCookieDomainAttr(host?: string): string {
  const h = host ?? (isBrowser() ? location.hostname : '')
  // 本地开发 / 预览域名（localhost、127.0.0.1、*.pages.dev…）退化成 host-only
  return h === 'super-nb.me' || h.endsWith('.super-nb.me') ? `; Domain=${THEME_COOKIE_DOMAIN}` : ''
}

/** 读父域 cookie。返回 `null` = 没有显式选择 = 跟随系统。 */
export function readThemeCookie(): ThemePref {
  if (!isBrowser()) return null
  const m = document.cookie.match(/(?:^|;\s*)snb_theme=(dark|light)(?:;|$)/)
  return m ? (m[1] as ThemeChoice) : null
}

/** 系统偏好（无 matchMedia 的环境按浅色兜底——白天档是 `:root` 默认分支）。 */
export function systemTheme(): ThemeChoice {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return 'light'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/** 当前**实际生效**的档：显式选择优先，缺席则跟随系统。 */
export function resolveTheme(): ThemeChoice {
  return readThemeCookie() ?? systemTheme()
}

/** 把档挂到 DOM 上。深色 = 加 `.dark`，浅色 = 摘掉它（不是删规则块）。
 *  同时同步 `color-scheme`，让原生滚动条/表单控件跟着换。 */
export function applyTheme(theme: ThemeChoice, root?: HTMLElement): void {
  if (!isBrowser()) return
  const el = root ?? document.documentElement
  el.classList.toggle('dark', theme === 'dark')
  if (el === document.documentElement) el.style.colorScheme = theme
}

/**
 * 写偏好并即时生效。
 * @param pref `'dark'|'light'` = 显式选择；`null` = 回到跟随系统（删 cookie）
 *
 * 🚨 规则 2：**选到与系统一致的值时传 `null`**（用 `setTheme` 会自动这么做），
 * 别写一个同值 cookie——那会把用户钉死。
 */
export function setThemePref(pref: ThemePref, root?: HTMLElement): ThemeChoice {
  if (isBrowser()) {
    const domain = themeCookieDomainAttr()
    if (pref === null) {
      // 删除：Domain / Path 必须与写入时逐字一致，否则删的是另一枚 cookie
      document.cookie = `${THEME_COOKIE}=; Path=/${domain}; Max-Age=0; SameSite=Lax; Secure`
    } else {
      document.cookie =
        `${THEME_COOKIE}=${pref}; Path=/${domain}; Max-Age=${THEME_COOKIE_MAX_AGE}; SameSite=Lax; Secure`
    }
  }
  const next = pref ?? systemTheme()
  applyTheme(next, root)
  return next
}

/** 选一个档：与系统一致时自动退化成「跟随系统」（删 cookie），符合规则 2。 */
export function setTheme(theme: ThemeChoice, root?: HTMLElement): ThemeChoice {
  return setThemePref(theme === systemTheme() ? null : theme, root)
}

/** 开灯 / 关灯：在当前生效档上取反。 */
export function toggleTheme(root?: HTMLElement): ThemeChoice {
  return setTheme(resolveTheme() === 'dark' ? 'light' : 'dark', root)
}

/**
 * 旧 localStorage 键一次性迁移 + **无条件退役**（规则 6）。
 * 只有在「当前没有 cookie」时旧键才有资格升格成显式选择；无论有没有迁移，
 * 三个旧键都必须删掉——留着的话用户删 cookie 后会被旧键复活。
 */
export function purgeLegacyThemeState(): void {
  if (typeof localStorage === 'undefined') return
  try {
    if (readThemeCookie() === null) {
      for (const key of LEGACY_THEME_KEYS) {
        const v = localStorage.getItem(key)
        if (v === 'dark' || v === 'light') {
          setThemePref(v)
          break
        }
      }
    }
    for (const key of LEGACY_THEME_KEYS) localStorage.removeItem(key)
  } catch {
    /* 隐私模式下 localStorage 可能抛异常：主题不是关键路径，吞掉 */
  }
}

export interface InitThemeOptions {
  /** 作用域根，默认 `document.documentElement` */
  root?: HTMLElement
  /** 生效档变化时回调（含聚焦对账带来的变化），用于同步 React state */
  onChange?: (theme: ThemeChoice) => void
  /** 关掉旧键迁移（已确认没有存量的站点可传 false） */
  migrateLegacy?: boolean
}

/**
 * 装上主题：迁移旧键 → 落地当前档 → 挂**聚焦对账**与系统偏好监听。
 * 返回卸载函数。
 *
 * 🚨 规则 3：跨源没有 storage 事件。用户在 A 站切档，B 站只有在重新获得焦点时
 * 重读 cookie 才知道——`focus` + `visibilitychange` 两个都要挂（前者管窗口切换，
 * 后者管标签页切换与移动端回前台）。
 */
export function initTheme(opts: InitThemeOptions = {}): () => void {
  if (!isBrowser()) return () => {}
  const { root, onChange, migrateLegacy = true } = opts
  if (migrateLegacy) purgeLegacyThemeState()

  let last = resolveTheme()
  applyTheme(last, root)
  onChange?.(last)

  const reconcile = () => {
    const next = resolveTheme()
    if (next === last) return
    last = next
    applyTheme(next, root)
    onChange?.(next)
  }
  const onVisible = () => {
    if (document.visibilityState === 'visible') reconcile()
  }

  window.addEventListener('focus', reconcile)
  document.addEventListener('visibilitychange', onVisible)
  // 跟随系统时，系统换档要立刻跟上（有显式 cookie 时 resolveTheme 不受影响，天然无副作用）
  const mq =
    typeof window.matchMedia === 'function' ? window.matchMedia('(prefers-color-scheme: dark)') : null
  mq?.addEventListener?.('change', reconcile)

  return () => {
    window.removeEventListener('focus', reconcile)
    document.removeEventListener('visibilitychange', onVisible)
    mq?.removeEventListener?.('change', reconcile)
  }
}

/**
 * 首帧防闪 boot 片段（规则 4）——**内联进 `<head>`，放在任何 CSS 之前**。
 * 必须是同步内联脚本：外链或 defer 都会先渲染一帧错档。
 * 非 React 站点直接把这段字符串塞进 HTML；构建期模板也可以复制字面量。
 *
 * ⚠️ 兜底走 `light`：`:root` 就是白天档，异常时不加 `.dark` 至少能读。
 */
export const THEME_BOOT_SNIPPET = `<script>(function(){try{var m=document.cookie.match(/(?:^|;\\s*)snb_theme=(dark|light)(?:;|$)/);var t=m?m[1]:(window.matchMedia&&window.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light');var e=document.documentElement;e.classList.toggle('dark',t==='dark');e.style.colorScheme=t}catch(_){}})()</script>`
