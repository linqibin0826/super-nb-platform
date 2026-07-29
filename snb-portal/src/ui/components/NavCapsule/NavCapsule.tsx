import { type ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { StatusLamp } from '../StatusLamp/StatusLamp'

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
 *  每项一颗键帽（**高 36** / 8px 圆角 / 底边 3px 实边非投影），hover 悬指抬 1px，
 *  当前位 = 整颗按到底（下沉 2px + 底边收平 1px + semibold）；动效 120ms + ease-snb。
 *  🪦 药丸胶囊壳 2026-07-28 当天退役；🪦 玻璃底 blur 与分隔竖线随 v2 退役。
 *
 *  🚨 双档差异（浅色档不是简单换值）：底边在深色是「黑半透实边」rgba(0,0,0,.5)，
 *  在浅色换成**实色暖灰侧壁 #C9C2B4**——纸上没有黑底边这回事，厚度靠实体侧壁做，
 *  不是把影子调深。两档都收在 --snb-key-side 里，组件只写 border-b-snb-key-side。 */
export function NavCapsule({ items, className, ...rest }: NavCapsuleProps) {
  return (
    <nav
      className={cx('inline-flex items-center gap-1.5', className)}
      {...rest}
    >
      {items.map((item) => (
        <a
          key={item.href}
          href={item.href}
          target={item.target}
          aria-current={item.current ? 'page' : undefined}
          className={cx(
            // 键身描边走 --snb-key-border（深 #242A33 / 浅 rgba 墨 .12）。
            // ⚠️ 这里**不能**写成 `dark:border-dark-700`：dark 变体的 `:is(.dark *)` 是
            // (0,2,0)，会盖掉后面 (0,1,0) 的 `border-b-*` 侧壁，深色键帽底边会从
            // 黑半透变成栏位线实色（真机抓出来的，肉眼 review 看不出）
            'inline-flex h-9 items-center gap-1.5 whitespace-nowrap rounded-[8px] border border-snb-key-border px-3.5 text-[13.5px] transition-all duration-quick ease-snb',
            item.current
              ? // 当前位=按到底：深色微亮平底 / 浅色抬升面填充（13.09:1），侧壁收到 1px
                'translate-y-[2px] border-b border-b-snb-key-active-edge bg-snb-key-active font-semibold text-snb-t1'
              : item.accent
                ? 'border-b-[3px] border-b-snb-key-side bg-snb-panel font-medium text-snb-t1 hover:-translate-y-px hover:border-b-4 [&_svg]:text-snb-safety'
                : 'border-b-[3px] border-b-snb-key-side bg-snb-panel font-medium text-snb-t2 hover:-translate-y-px hover:border-b-4 hover:text-snb-t1'
          )}
        >
          {item.icon}
          {item.label}
          {/* 状态灯唯一版：8px 橙实心 + 2200ms 平色扩散环（🪦 各写各的 6px 裸点已收编） */}
          {item.dot && <StatusLamp state="live" />}
        </a>
      ))}
    </nav>
  )
}
