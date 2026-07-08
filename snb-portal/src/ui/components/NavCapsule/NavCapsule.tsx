import { Fragment, type ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface NavCapsuleItem {
  label: string
  href: string
  icon?: ReactNode
  /** 当前位：明暗反转片（浅色=墨片纸字 / 暗色=纸片墨字） */
  current?: boolean
  /** 强调项：赤陶浅底（促销视觉语言） */
  accent?: boolean
  /** 强调项右侧呼吸点 */
  dot?: boolean
  target?: string
}

export interface NavCapsuleProps {
  items: NavCapsuleItem[]
  className?: string
  'aria-label'?: string
}

/** 全站导航胶囊：玻璃底 + 发丝描边（learn 暮色版/fork 纸面版的统一泛化） */
export function NavCapsule({ items, className, ...rest }: NavCapsuleProps) {
  return (
    <nav
      className={cx(
        'inline-flex items-center gap-0.5 rounded-full border border-[var(--snb-glass-border)] bg-[var(--snb-glass-bg)] p-1.5 shadow-glass-sm backdrop-blur-md',
        className
      )}
      {...rest}
    >
      {items.map((item, i) => (
        <Fragment key={item.href}>
          {i > 0 && <span className="h-4 w-px bg-snb-hairline-strong" aria-hidden="true" />}
          <a
            href={item.href}
            target={item.target}
            aria-current={item.current ? 'page' : undefined}
            className={cx(
              'inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-3.5 py-1.5 text-[13px] font-semibold transition-colors',
              item.current
                ? 'bg-dark-800/90 text-paper shadow-sm dark:bg-paper/90 dark:text-dark-950'
                : item.accent
                  ? 'bg-primary-500/20 text-primary-700 hover:bg-primary-500/30 dark:text-primary-200'
                  : 'text-snb-t2 hover:bg-snb-t1/[0.06] hover:text-snb-t1'
            )}
          >
            {item.icon}
            {item.label}
            {item.dot && (
              <span
                className="h-1.5 w-1.5 rounded-full bg-primary-400 animate-snb-dot motion-reduce:animate-none"
                aria-hidden="true"
              />
            )}
          </a>
        </Fragment>
      ))}
    </nav>
  )
}
