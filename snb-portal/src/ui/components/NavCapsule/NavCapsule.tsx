import { type ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface NavCapsuleItem {
  label: string
  href: string
  icon?: ReactNode
  /** 当前位：白胶囊（深色为比轨道亮一档的胶囊）+ semibold */
  current?: boolean
  /** 强调项：主字色 + 品牌色图标 */
  accent?: boolean
  /** 强调项右侧 6px 品牌橙小点（「有新活动」提示，不呼吸） */
  dot?: boolean
  target?: string
}

export interface NavCapsuleProps {
  items: NavCapsuleItem[]
  className?: string
  'aria-label'?: string
}

/** 全站导航胶囊分段控件（苹果式 v3，🪦 键帽行随网吧退役）：
 *  灰轨 `--snb-well` 3px 内边距 + 每项 30px 高胶囊；当前位=白胶囊 + 1px 3px 12% 投影 + semibold；
 *  非当前位 t2 字（t3 压灰轨只有 4.27:1）hover 提 t1；动效 200ms + ease-snb-quick。
 *  ⚠️ 不用 `dark:` 变体表达两档差异——当前位底走 --snb-key-active-bg（浅 #FFF / 深 #3A3A3C）。 */
export function NavCapsule({ items, className, ...rest }: NavCapsuleProps) {
  return (
    <nav
      className={cx('inline-flex items-center gap-0.5 rounded-full bg-snb-well p-[3px]', className)}
      {...rest}
    >
      {items.map((item) => (
        <a
          key={item.href}
          href={item.href}
          target={item.target}
          aria-current={item.current ? 'page' : undefined}
          className={cx(
            'inline-flex h-[30px] items-center gap-1.5 whitespace-nowrap rounded-full px-3.5 text-[13px] font-medium no-underline transition-[background-color,color,box-shadow] duration-quick ease-snb-quick',
            item.current
              ? 'bg-snb-key-active font-semibold text-snb-t1 shadow-[0_1px_3px_rgba(0,0,0,0.12)]'
              : item.accent
                ? 'text-snb-t1 [&_svg]:text-snb-safety'
                : 'text-snb-t2 hover:text-snb-t1'
          )}
        >
          {item.icon}
          {item.label}
          {item.dot && <span aria-hidden="true" className="h-1.5 w-1.5 flex-none rounded-full bg-snb-safety" />}
        </a>
      ))}
    </nav>
  )
}
