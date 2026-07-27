import { type ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface NavCapsuleItem {
  label: string
  href: string
  icon?: ReactNode
  /** 当前位：微亮平底 + 纸白字（🪦 明暗反转片随恒暗+零发光退役） */
  current?: boolean
  /** 强调项：纸白字 + 安全橙图标（🪦 赤陶/霓虹浅底退役——强调配额给呼吸点） */
  accent?: boolean
  /** 强调项右侧呼吸点（安全橙扩散环） */
  dot?: boolean
  target?: string
}

export interface NavCapsuleProps {
  items: NavCapsuleItem[]
  className?: string
  'aria-label'?: string
}

/** 全站导航胶囊：实心面板 + 栏位线（零发光网吧 v2）。
 *  🪦 玻璃底 blur 与分隔竖线随 v2 退役——平钮靠间距分组，竖线是噪声。 */
export function NavCapsule({ items, className, ...rest }: NavCapsuleProps) {
  return (
    <nav
      className={cx(
        'inline-flex items-center gap-0.5 rounded-full border border-dark-700 bg-dark-800 p-1.5 shadow-glass-sm',
        className
      )}
      {...rest}
    >
      {items.map((item) => (
        <a
          key={item.href}
          href={item.href}
          target={item.target}
          aria-current={item.current ? 'page' : undefined}
          className={cx(
            'inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-3.5 py-1.5 text-[13px] font-semibold transition-colors',
            item.current
              ? 'bg-snb-t1/[0.07] text-snb-t1'
              : item.accent
                ? 'text-snb-t1 hover:bg-snb-t1/[0.06] [&_svg]:text-snb-safety'
                : 'text-snb-t2 hover:bg-snb-t1/[0.06] hover:text-snb-t1'
          )}
        >
          {item.icon}
          {item.label}
          {item.dot && (
            <span
              className="h-1.5 w-1.5 rounded-full bg-primary-500 animate-snb-dot motion-reduce:animate-none"
              aria-hidden="true"
            />
          )}
        </a>
      ))}
    </nav>
  )
}
