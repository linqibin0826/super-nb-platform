import type { HTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface ThemeScopeProps extends HTMLAttributes<HTMLDivElement> {
  /** 主题作用域：dark 时容器盖 .dark 类，语义槽位变量随之翻转 */
  theme?: 'light' | 'dark'
}

export function ThemeScope({ theme = 'light', className, ...rest }: ThemeScopeProps) {
  return (
    <div
      className={cx(theme === 'dark' && 'dark', 'bg-snb-bg font-sans text-snb-t1 antialiased', className)}
      {...rest}
    />
  )
}
