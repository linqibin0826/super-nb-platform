import { useCallback, useEffect, useState } from 'react'
import { initTheme, resolveTheme, toggleTheme, setTheme, type ThemeChoice } from './snbTheme'

export interface UseThemeResult {
  /** 当前**生效**档（显式选择 or 跟随系统的结果） */
  theme: ThemeChoice
  /** 开灯 / 关灯：在生效档上取反；切到与系统一致的值时自动回「跟随系统」 */
  toggle: () => void
  /** 直接指定档（同样遵守「与系统一致 → 删 cookie」） */
  set: (theme: ThemeChoice) => void
}

/**
 * React 站点的主题接线（契约实现见 `./snbTheme.ts`，本文件只是它的 React 外壳）。
 *
 * 用法：在应用**根组件**调一次即可——它会挂 `focus` / `visibilitychange` 聚焦对账，
 * 挂多份不会出错但没必要。
 *
 * ⚠️ SSR：首帧 `theme` 取 `resolveTheme()`，服务端拿不到 cookie 会返回 `light`；
 * 真正防闪靠 `<head>` 里的 `THEME_BOOT_SNIPPET`，**不要**指望这个 hook 防首帧闪。
 */
export function useTheme(root?: HTMLElement): UseThemeResult {
  const [theme, setThemeState] = useState<ThemeChoice>(() => resolveTheme())

  useEffect(() => initTheme({ root, onChange: setThemeState }), [root])

  const toggle = useCallback(() => setThemeState(toggleTheme(root)), [root])
  const set = useCallback((t: ThemeChoice) => setThemeState(setTheme(t, root)), [root])

  return { theme, toggle, set }
}
