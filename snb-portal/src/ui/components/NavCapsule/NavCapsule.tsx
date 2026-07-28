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

/** 全站导航键帽行（2026-07-28 站长两轮拍板——网吧的手感记忆是键盘）：
 *  每项一颗键帽（8px 圆角 + 底边 3px 黑半透实边非投影），hover 悬指抬 1px，
 *  当前位 = 整颗按到底（下沉 2px + 底边收平 1px + semibold）。
 *  🪦 药丸胶囊壳 2026-07-28 当天退役；🪦 玻璃底 blur 与分隔竖线随 v2 退役。 */
export function NavCapsule({ items, className, ...rest }: NavCapsuleProps) {
  return (
    <nav className={cx('inline-flex items-center gap-1.5', className)} {...rest}>
      {items.map((item) => (
        <a
          key={item.href}
          href={item.href}
          target={item.target}
          aria-current={item.current ? 'page' : undefined}
          className={cx(
            'inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg border border-dark-700 px-3.5 py-2 text-[13.5px] transition-all duration-[120ms] ease-[cubic-bezier(0.22,0.61,0.36,1)]',
            item.current
              ? 'translate-y-[2px] border-b border-b-dark-700 bg-snb-t1/[0.07] font-semibold text-snb-t1'
              : item.accent
                ? 'border-b-[3px] border-b-black/50 bg-dark-800 font-medium text-snb-t1 hover:-translate-y-px hover:border-b-4 [&_svg]:text-snb-safety'
                : 'border-b-[3px] border-b-black/50 bg-dark-800 font-medium text-snb-t2 hover:-translate-y-px hover:border-b-4 hover:text-snb-t1'
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
