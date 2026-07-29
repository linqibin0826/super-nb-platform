/**
 * @vitest-environment jsdom
 * @vitest-environment-options {"url": "https://studio.super-nb.me/"}
 *
 * 环境 URL 必须是 https 的 *.super-nb.me：jsdom 真校验 Secure（http 下写入被丢）
 * 与 Domain=.super-nb.me（域不匹配写入被拒）。有了它，删除断言测的是真实
 * cookie 行为，而不是「有没有拼对字符串」。
 *
 * 【本文件测什么】
 * 契约逻辑本体在 `super-nb-ui/src/theme/snbTheme.test.ts` 有 17 条行为测试，
 * 这里**不重复测逻辑**，只测 portal 这一边接对了：转发层导出齐全、
 * 存量状态迁移/退役正确、四个入口的启动路径拿得到 initTheme。
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  initTheme,
  purgeLegacyThemeState,
  readThemeCookie,
  resolveTheme,
  setTheme,
  toggleTheme,
  THEME_BOOT_SNIPPET,
} from '../themeCookie'

/** 种一枚存量 cookie，模拟老用户浏览器里遗留的显式主题选择 */
function seedCookie(v: 'dark' | 'light') {
  document.cookie = `snb_theme=${v}; Domain=.super-nb.me; Path=/; Secure; SameSite=Lax; Max-Age=31536000`
}
const hasThemeCookie = () => /(?:^|;\s*)snb_theme=(dark|light)\b/.test(document.cookie)

/** jsdom 默认 matchMedia 缺席，systemTheme() 会兜底成 light；要测「跟随系统」得自己造 */
function mockSystem(theme: 'dark' | 'light') {
  vi.stubGlobal(
    'matchMedia',
    vi.fn().mockReturnValue({
      matches: theme === 'dark',
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })
  )
}

beforeEach(() => {
  document.cookie = 'snb_theme=; Domain=.super-nb.me; Path=/; Max-Age=0'
  localStorage.clear()
  document.documentElement.className = ''
  vi.unstubAllGlobals()
})

describe('主题契约第二边（portal 转发 vendor 真源）', () => {
  it('转发层把契约导出齐了——缺一个，四个入口里就有地方接不上', () => {
    expect(typeof initTheme).toBe('function')
    expect(typeof toggleTheme).toBe('function')
    expect(typeof setTheme).toBe('function')
    expect(typeof readThemeCookie).toBe('function')
    expect(THEME_BOOT_SNIPPET).toContain('snb_theme')
  })

  it('cookie 缺席 = 跟随系统（不是「默认深色」）', () => {
    mockSystem('dark')
    expect(readThemeCookie()).toBeNull()
    expect(resolveTheme()).toBe('dark')
    mockSystem('light')
    expect(resolveTheme()).toBe('light')
  })

  it('显式选择压过系统偏好', () => {
    mockSystem('dark')
    seedCookie('light')
    expect(resolveTheme()).toBe('light')
  })

  it('切到与系统一致的值 → 真删 cookie 回「跟随系统」（Domain 不匹配是删不掉的）', () => {
    mockSystem('dark')
    seedCookie('light')
    expect(hasThemeCookie()).toBe(true)
    setTheme('dark') // 与系统一致
    expect(hasThemeCookie()).toBe(false)
    expect(resolveTheme()).toBe('dark')
  })

  it('切档把 .dark 挂上/摘掉，且同步 color-scheme', () => {
    mockSystem('light')
    setTheme('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
    expect(document.documentElement.style.colorScheme).toBe('dark')
    toggleTheme()
    expect(document.documentElement.classList.contains('dark')).toBe(false)
    expect(document.documentElement.style.colorScheme).toBe('light')
  })

  it('旧 localStorage 键 snb-studio-theme 一次性迁移后**无条件退役**', () => {
    mockSystem('dark')
    localStorage.setItem('snb-studio-theme', 'light')
    purgeLegacyThemeState()
    // 迁移：旧键的显式选择升格成父域 cookie
    expect(readThemeCookie()).toBe('light')
    // 退役：留着的话，用户以后删了 cookie 会被旧键复活成显式选择
    expect(localStorage.getItem('snb-studio-theme')).toBeNull()
  })

  it('已有 cookie 时旧键不许翻盘，但照样退役', () => {
    seedCookie('dark')
    localStorage.setItem('snb-studio-theme', 'light')
    purgeLegacyThemeState()
    expect(readThemeCookie()).toBe('dark')
    expect(localStorage.getItem('snb-studio-theme')).toBeNull()
  })

  it('localStorage 不可用时不抛错（隐私模式）', () => {
    const spy = vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
      throw new Error('SecurityError')
    })
    expect(() => purgeLegacyThemeState()).not.toThrow()
    spy.mockRestore()
  })

  it('initTheme 挂聚焦对账并返回卸载函数（跨源没有 storage 事件，全靠它）', () => {
    mockSystem('light')
    const add = vi.spyOn(window, 'addEventListener')
    const docAdd = vi.spyOn(document, 'addEventListener')
    const dispose = initTheme()
    expect(add).toHaveBeenCalledWith('focus', expect.any(Function))
    expect(docAdd).toHaveBeenCalledWith('visibilitychange', expect.any(Function))
    expect(typeof dispose).toBe('function')
    dispose()
    add.mockRestore()
    docAdd.mockRestore()
  })
})
