/**
 * @vitest-environment jsdom
 * @vitest-environment-options {"url": "https://studio.super-nb.me/"}
 *
 * Secure+Domain 校验需 https 的 *.super-nb.me 环境。契约与 fork utils/themeCookie.ts 一致，改必两边同步。
 */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  clearThemeCookie,
  effectiveTheme,
  migrateLegacyThemeKey,
  readThemeCookie,
  writeThemeCookie,
} from '../themeCookie'

function setMatchMedia(dark: boolean) {
  vi.stubGlobal('matchMedia', (q: string) => ({
    matches: dark,
    media: q,
    addEventListener() {},
    removeEventListener() {},
  }))
}
beforeEach(() => {
  document.cookie = 'snb_theme=; Domain=.super-nb.me; Path=/; Max-Age=0'
  localStorage.clear()
  setMatchMedia(false)
})
afterEach(() => vi.unstubAllGlobals())

describe('studio themeCookie 契约', () => {
  it('无 cookie 跟随系统', () => {
    setMatchMedia(true)
    expect(effectiveTheme()).toBe('dark')
  })
  it('write/read/clear 往返', () => {
    writeThemeCookie('light')
    expect(readThemeCookie()).toBe('light')
    clearThemeCookie()
    expect(readThemeCookie()).toBeNull()
  })
  it('cookie 显式值优先于系统', () => {
    setMatchMedia(true)
    writeThemeCookie('light')
    expect(effectiveTheme()).toBe('light')
  })
  it('迁移旧键 snb-studio-theme 并退役', () => {
    localStorage.setItem('snb-studio-theme', 'dark')
    migrateLegacyThemeKey('snb-studio-theme')
    expect(readThemeCookie()).toBe('dark')
    expect(localStorage.getItem('snb-studio-theme')).toBeNull()
  })
  it('cookie 已存在时迁移不覆盖但退役旧键（防复活）', () => {
    writeThemeCookie('light')
    localStorage.setItem('snb-studio-theme', 'dark')
    migrateLegacyThemeKey('snb-studio-theme')
    expect(readThemeCookie()).toBe('light')
    expect(localStorage.getItem('snb-studio-theme')).toBeNull()
  })
})
