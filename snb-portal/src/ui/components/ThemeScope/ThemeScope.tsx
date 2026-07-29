import type { HTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface ThemeScopeProps extends HTMLAttributes<HTMLDivElement> {
  /**
   * 主题作用域：
   * - `'inherit'`（**应用外壳该用这个**）：不钉任何档，跟着 `<html>` 上的 `.dark` 走，
   *   也就是跟着父域 cookie `snb_theme` 走。只提供外壳的底色/字色/字体。
   * - `'dark'` / `'light'`：**局部**钉死一档（playground、双档对照台、
   *   压在暗景上的 Hero 区块）。
   *
   * ⚠️ 整站主题不要靠 `'dark'`/`'light'` 钉——整站真源是父域 cookie `snb_theme`
   * + `<html>` 上的 `.dark`，见 `src/theme/snbTheme.ts` 契约与 `THEME_BOOT_SNIPPET`
   * 首帧防闪。2026-07-26～07-29 恒暗期间 portal 四个站点的根外壳都写着
   * `theme="dark"`，那是分期护栏；双档补全后一律改回 `'inherit'`。
   */
  theme?: 'light' | 'dark' | 'inherit'
}

export function ThemeScope({ theme = 'light', className, ...rest }: ThemeScopeProps) {
  return (
    <div
      className={cx(
        // ⚠️ 浅色**必须**显式挂 `snb-light`，不能只靠「没有 .dark」：CSS 变量按继承生效，
        // `:root` 只匹配 <html>，深色祖先里嵌一个不带 .dark 的容器仍会继承到深色值
        // （真机抓出来的：双档对照台左边那半原本整块是深色）。
        // 反过来 `'inherit'` 就是**什么都不挂**——挂上任何一个都会把外壳钉死。
        theme === 'dark' ? 'dark' : theme === 'light' ? 'snb-light' : undefined,
        'bg-snb-bg font-sans text-snb-t1 antialiased',
        className
      )}
      {...rest}
    />
  )
}
