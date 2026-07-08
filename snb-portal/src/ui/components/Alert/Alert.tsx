import type { HTMLAttributes, ReactNode } from 'react'
import { cx } from '../../lib/cx'

export type AlertTone = 'tip' | 'warning' | 'danger' | 'info'

export interface AlertProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  tone?: AlertTone
  title?: ReactNode
}

const tones: Record<AlertTone, string> = {
  tip: 'border-l-primary-500',
  warning: 'border-l-snb-amber',
  danger: 'border-l-snb-ember',
  info: 'border-l-dark-500',
}

/** 记录卡：左 2px 语义色条 + 发丝线边（learn 警示块母题） */
export function Alert({ tone = 'tip', title, className, children, ...rest }: AlertProps) {
  return (
    <div
      className={cx(
        'rounded-[10px] border border-snb-hairline border-l-2 px-[18px] py-4 text-sm text-snb-t2',
        tones[tone],
        className
      )}
      {...rest}
    >
      {title != null && <p className="mb-1 font-bold tracking-[0.02em] text-snb-t1">{title}</p>}
      {children}
    </div>
  )
}
