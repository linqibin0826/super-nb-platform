/**
 * @vitest-environment jsdom
 * @vitest-environment-options {"url": "https://studio.super-nb.me/"}
 *
 * 环境 URL 必须是 https 的 *.super-nb.me：jsdom 真校验 Secure（http 下写入被丢）
 * 与 Domain=.super-nb.me（域不匹配写入被拒）。有了它，删除断言测的是真实
 * cookie 行为，而不是「有没有拼对字符串」。
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { purgeLegacyThemeState } from '../themeCookie'

/** 种一枚存量 cookie，模拟老用户浏览器里遗留的显式主题选择 */
function seedLegacyCookie(v: 'dark' | 'light') {
  document.cookie = `snb_theme=${v}; Domain=.super-nb.me; Path=/; Secure; SameSite=Lax; Max-Age=31536000`
}
const hasThemeCookie = () => /(?:^|;\s*)snb_theme=(dark|light)\b/.test(document.cookie)

beforeEach(() => {
  document.cookie = 'snb_theme=; Domain=.super-nb.me; Path=/; Max-Age=0'
  localStorage.clear()
})

describe('恒暗后的主题状态清理（四边契约第二边）', () => {
  it('真删掉存量父域 snb_theme cookie（Domain 不匹配是删不掉的）', () => {
    seedLegacyCookie('light')
    expect(hasThemeCookie()).toBe(true)
    purgeLegacyThemeState()
    expect(hasThemeCookie()).toBe(false)
  })

  it('cookie 本就缺席时也不报错', () => {
    expect(hasThemeCookie()).toBe(false)
    expect(() => purgeLegacyThemeState()).not.toThrow()
  })

  it('退役旧 localStorage 键 snb-studio-theme，防「删了又被复活」', () => {
    localStorage.setItem('snb-studio-theme', 'light')
    purgeLegacyThemeState()
    expect(localStorage.getItem('snb-studio-theme')).toBeNull()
  })

  it('localStorage 不可用时不抛错（隐私模式）', () => {
    const spy = vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
      throw new Error('SecurityError')
    })
    expect(() => purgeLegacyThemeState()).not.toThrow()
    spy.mockRestore()
  })

  it('切换 API 已下线——留任何写入口都可能让某处又把站点切回浅色', async () => {
    const m = (await import('../themeCookie')) as Record<string, unknown>
    expect(m.writeThemeCookie).toBeUndefined()
    expect(m.clearThemeCookie).toBeUndefined()
    expect(m.readThemeCookie).toBeUndefined()
    expect(m.effectiveTheme).toBeUndefined()
    expect(m.migrateLegacyThemeKey).toBeUndefined()
  })
})
