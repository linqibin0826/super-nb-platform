import type { HTMLAttributes, ReactNode } from 'react'
import { cx } from '../../lib/cx'

export type AlertTone = 'tip' | 'warning' | 'danger' | 'info'

export interface AlertProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  tone?: AlertTone
  title?: ReactNode
}

// 语义色条走双档槽位：深色取值与旧的 primary-500 / snb-amber / snb-ember / dark-500
// 逐字相同（#FF5C00 / #FF5C00 / #EA494F / #828B96），浅色自动压深。
// ⚠️ 旧的固定 hex 在纸上都不达标：#FF5C00 压面 2.94:1、#828B96 压面 3.28:1 卡在线上。
// v2 色板没有黄：警告就是安全橙，危险就是功能红。
const tones: Record<AlertTone, string> = {
  tip: 'border-l-snb-safety',
  warning: 'border-l-snb-safety',
  danger: 'border-l-snb-danger',
  info: 'border-l-snb-lamp-off',
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
