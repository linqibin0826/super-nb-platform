import { useEffect, useState } from 'react'
import {
  clearThemeCookie,
  effectiveTheme,
  migrateLegacyThemeKey,
  readThemeCookie,
  systemTheme,
  writeThemeCookie,
  type Theme,
} from './themeCookie'

export type { Theme }

// 主题跨子域名共享：父域 cookie snb_theme 是唯一真源（契约见 themeCookie.ts，改必四站同步）。
// 模块加载期一次性迁移旧键 snb-studio-theme → cookie 并退役（在 useState 初值读 cookie 之前跑）。
migrateLegacyThemeKey('snb-studio-theme')

/** 明暗主题：显式选择写 cookie、跟随系统删 cookie；实时跟随系统 + 聚焦对账跨子域名跟随。 */
export function useTheme(): [Theme, () => void] {
  const [theme, setTheme] = useState<Theme>(effectiveTheme)

  // 页面级滚动条 color-scheme 落 <html>（ThemeScope 的 .dark 只盖应用容器 div，管不到文档滚动条）
  useEffect(() => {
    document.documentElement.style.colorScheme = theme
  }, [theme])

  // 无显式选择时跟随系统深浅切换
  useEffect(() => {
    const mql = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = (e: MediaQueryListEvent): void => {
      if (!readThemeCookie()) setTheme(e.matches ? 'dark' : 'light')
    }
    mql.addEventListener('change', onChange)
    return () => mql.removeEventListener('change', onChange)
  }, [])

  // 聚焦对账：主站/活动/指南改了 cookie（不同 origin，无 storage 事件），切回本页跟上
  useEffect(() => {
    const reconcile = (): void => setTheme(effectiveTheme())
    window.addEventListener('focus', reconcile)
    document.addEventListener('visibilitychange', reconcile)
    return () => {
      window.removeEventListener('focus', reconcile)
      document.removeEventListener('visibilitychange', reconcile)
    }
  }, [])

  function toggleTheme(): void {
    const next: Theme = theme === 'dark' ? 'light' : 'dark'
    if (next === systemTheme()) clearThemeCookie()
    else writeThemeCookie(next)
    setTheme(next)
  }

  return [theme, toggleTheme]
}
