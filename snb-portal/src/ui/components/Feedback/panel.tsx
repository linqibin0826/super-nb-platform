import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'

/** 空态与错误态的公共骨架（GlobalParts v3 §05）：
 *  44px 圆圈图标 + 15px/600 标题 + 13.5px dim 说明（max 34ch）+ 动作区。
 *  两态只差三处：容器边色 / 图标色 / 文案与按钮——骨架一个字都不许分家。 */
export interface StatePanelProps {
  tone: 'empty' | 'error'
  icon: ReactNode
  title: ReactNode
  description?: ReactNode
  /** 动作区：空态一颗「能把这里填满」的按钮；错误态必带「再试一次」 */
  actions?: ReactNode
  className?: string
}

export function StatePanel({ tone, icon, title, description, actions, className }: StatePanelProps) {
  return (
    <div
      className={cx(
        'flex flex-col items-center gap-3 rounded-xl bg-snb-panel px-6 py-11 text-center max-md:px-4',
        // 错误态容器换功能红描边——一眼能和空态分开，不许「假装暂无数据」
        tone === 'error' ? 'border border-snb-danger/45' : 'border border-snb-hairline',
        className
      )}
      role={tone === 'error' ? 'alert' : undefined}
    >
      <span
        aria-hidden="true"
        className={cx(
          'grid h-11 w-11 place-items-center rounded-full border font-mono text-[18px]',
          tone === 'error'
            ? 'border-snb-danger font-bold text-snb-danger'
            : 'border-snb-hairline-strong text-snb-t3'
        )}
      >
        {icon}
      </span>
      <div className="text-[15px] font-semibold text-snb-t1">{title}</div>
      {description && (
        <div className="max-w-[34ch] text-[13.5px] leading-relaxed text-snb-t2">{description}</div>
      )}
      {actions && <div className="mt-1.5 flex flex-wrap items-center justify-center gap-3">{actions}</div>}
    </div>
  )
}
