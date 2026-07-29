// 筛选胶囊（公用件规范 v3）：选中 = 纸白填充 + 沥青字（白字压橙整条退役）；
// 未选 = hairline-strong 描边 + 文二。视觉高 32，外层透明内边距把热区撑到 ≥44。
// ui 层的 Chip 仍是旧橙色选中态（共享件本轮只读），studio 画墙一律换用本件。
import type { ReactNode } from 'react'

interface FilterChipProps {
  active?: boolean
  /** 计数尾标（真源类目条数）；undefined 不渲染 */
  count?: string | number
  /** 虚线样式（「更多筛选」「+N 类」这类开关钮） */
  dashed?: boolean
  onClick?: () => void
  children: ReactNode
  /** 加在外层 button 上（响应式显隐等） */
  className?: string
}

export function FilterChip({ active = false, count, dashed = false, onClick, children, className }: FilterChipProps) {
  const pill = active
    ? 'bg-snb-cta font-semibold text-snb-cta-fg'
    : dashed
      ? 'border border-dashed border-[rgba(239,235,228,0.28)] text-snb-t2 group-hover/fc:border-[rgba(239,235,228,0.4)] group-hover/fc:text-snb-t1'
      : 'border border-snb-hairline-strong text-snb-t2 group-hover/fc:border-[rgba(239,235,228,0.4)] group-hover/fc:text-snb-t1'
  return (
    <button
      type="button"
      aria-pressed={dashed ? undefined : active}
      onClick={onClick}
      className={`group/fc inline-flex items-center px-0.5 py-1.5 focus:outline-none ${className ?? ''}`}
    >
      <span
        className={`inline-flex h-8 items-center gap-2 whitespace-nowrap rounded-full px-3.5 text-[13px] transition-colors duration-[120ms] ease-[cubic-bezier(0.2,0,0,1)] group-focus-visible/fc:ring-2 group-focus-visible/fc:ring-paper/60 ${pill}`}
      >
        {children}
        {count !== undefined && (
          <span className={`font-mono text-[11px] tabular-nums ${active ? 'font-normal opacity-65' : 'text-snb-t3'}`}>
            {count}
          </span>
        )}
      </span>
    </button>
  )
}
