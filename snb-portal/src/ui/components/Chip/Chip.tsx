import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface ChipProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  active?: boolean
}

/** 筛选胶囊（GlobalParts v3 §03）：选中=cta 填充，未选=well 底 dim 字。
 *  选中 = `bg-snb-cta text-snb-cta-fg` 强调色填充（浅白字 5.0:1 / 深黑字 8.3:1），
 *  与主按钮统一走 --snb-cta-*；🪦 旧霓虹橙底白字 2.61:1 的口径随 v2 退役。
 *  两层结构是刻意的：外层 button 只做 6px/2px 透明边距把热区撑到 44，
 *  视觉高恒定 32 在内层 span 上——视觉可小，热区不许小。 */
export const Chip = forwardRef<HTMLButtonElement, ChipProps>(function Chip(
  { active = false, className, children, ...rest },
  ref
) {
  return (
    <button
      ref={ref}
      aria-pressed={active}
      className={cx(
        'group inline-flex items-center rounded-full border-none bg-transparent px-0.5 py-1.5 focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus',
        className
      )}
      {...rest}
    >
      <span
        className={cx(
          'inline-flex h-8 items-center rounded-full px-4 text-[13px] font-medium transition-[background-color,color] duration-quick ease-snb-quick',
          active
            ? 'bg-snb-cta font-semibold text-snb-cta-fg'
            : 'bg-snb-well text-snb-t2 group-hover:text-snb-t1'
        )}
      >
        {children}
      </span>
    </button>
  )
})
