import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface ThemeSwitchProps
  extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'onToggle'> {
  /** 当前是否暗色（滑块位置与图标随之切换） */
  dark: boolean
  /** 点击切换回调 */
  onToggle: () => void
}

/**
 * 全站统一的深浅主题开关（规范 v1，2026-07-05 站长拍板「统一成开关」）。
 *
 * 形态以 VitePress VPSwitchAppearance 为基准（learn /help 首发、站长认可）：
 * 40×22 胶囊轨道 / 18px 圆滑块暗色右移 18px / 滑块内 12px 日月图标 / hover 赤陶描边。
 *
 * ⚠️ 契约：本组件与 templates/app-header.html 的 `.snb-theme-switch` 样式段、
 * fork `style.css` 同名样式是同一规范的多种载体，尺寸/位移/配色改动必须同步。
 * 消费方：studio TopBar（本组件）、fork 控制台两处（Vue 抄样式）、activity.html（静态抄样式）。
 */
export const ThemeSwitch = forwardRef<HTMLButtonElement, ThemeSwitchProps>(function ThemeSwitch(
  { dark, onToggle, className, ...rest },
  ref
) {
  return (
    <button
      ref={ref}
      type="button"
      role="switch"
      aria-checked={dark}
      onClick={onToggle}
      className={cx(
        'relative block h-[22px] w-10 flex-none rounded-full border border-snb-hairline-strong',
        'bg-snb-t1/5 transition-colors duration-200 hover:border-primary-500',
        'focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50',
        className
      )}
      {...rest}
    >
      <span
        className={cx(
          'absolute left-px top-px flex h-[18px] w-[18px] items-center justify-center rounded-full',
          'bg-white text-snb-t2 shadow-sm transition-transform duration-200 dark:bg-snb-elv',
          dark && 'translate-x-[18px]'
        )}
      >
        {dark ? (
          // 暗色：月亮
          <svg
            width="12"
            height="12"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
          </svg>
        ) : (
          // 亮色：太阳
          <svg
            width="12"
            height="12"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            aria-hidden="true"
          >
            <circle cx="12" cy="12" r="4" />
            <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
          </svg>
        )}
      </span>
    </button>
  )
})
