import { cx } from '../../lib/cx'
import { useTheme } from '../../theme/useTheme'
import type { ThemeChoice } from '../../theme/snbTheme'

export interface ThemeToggleProps {
  /**
   * 受控档位。不传则组件自己接主题契约（读父域 cookie + 聚焦对账），
   * 顶栏直接放一枚即可用；传了就必须同时传 `onToggle`。
   */
  theme?: ThemeChoice
  onToggle?: () => void
  className?: string
}

/**
 * 主题开关「浅色模式 / 深色模式」（浅色档值表 §05 拍板）。
 *
 * 位置：顶栏最右，头像左侧；访客态放在「登录」左边。
 * 形态：44×44 幽灵钮（视觉小、热区不小），里面是 14px 的**半明半暗圆点**——
 *   左半墨、右半空，**朝向即当前档**；🚫 不用太阳月亮图标。
 * 文案：「浅色模式 / 深色模式」；半明半暗圆点保留（朝向即当前档，一个节点两档不重排）。
 * 动效：只走 200ms 常规档（duration-quick）+ ease-snb-quick；圆点用 `rotate` 不用换节点，**页面不重排**。
 *
 * 圆点做法：`linear-gradient(90deg, currentColor 50%, transparent 50%)` + 1.5px
 * currentColor 描边，整体 `rotate(180deg)` 表示另一档——一个节点两档，没有布局变化。
 */
export function ThemeToggle({ theme: controlled, onToggle, className }: ThemeToggleProps) {
  const auto = useTheme()
  const theme = controlled ?? auto.theme
  const onClick = onToggle ?? auto.toggle
  const isDark = theme === 'dark'
  // 当前深色 → 按下去切到「浅色模式」；当前浅色 → 按下去切到「深色模式」
  const label = isDark ? '浅色模式' : '深色模式'

  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={`切换到${label}`}
      title={label}
      className={cx(
        'grid h-11 w-11 flex-none place-items-center rounded-full border border-snb-hairline-strong bg-transparent p-0 text-snb-t1 transition-colors duration-quick ease-snb-quick hover:bg-snb-t1/[0.06] focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus',
        className
      )}
    >
      <span
        aria-hidden="true"
        className={cx(
          'block h-3.5 w-3.5 rounded-full border-[1.5px] border-current transition-transform duration-quick ease-snb-quick motion-reduce:transition-none',
          '[background:linear-gradient(90deg,currentColor_50%,transparent_50%)]',
          isDark && 'rotate-180'
        )}
      />
    </button>
  )
}
