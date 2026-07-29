import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface ChipProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  active?: boolean
}

/** 筛选胶囊（GlobalParts v3 §03）：选中=cta 填充，未选=hairline-strong 描边 dim 字。
 *  🚨「橙底白字」整条退役（实测 2.61:1）——选中态与主按钮统一走 --snb-cta-*：
 *  深夜纸白填充沥青字（16.02:1）、白天墨块填充纸白字（16.52:1）。
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
          'inline-flex h-8 items-center rounded-full px-4 text-[13.5px] transition-colors duration-quick ease-snb',
          active
            ? 'bg-snb-cta font-semibold text-snb-cta-fg'
            : 'border border-snb-hairline-strong text-snb-t2 group-hover:border-snb-hairline-heavy group-hover:text-snb-t1'
        )}
      >
        {children}
      </span>
    </button>
  )
})
