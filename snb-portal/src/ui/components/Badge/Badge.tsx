import type { HTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export type BadgeTone = 'primary' | 'success' | 'warning' | 'danger' | 'gray'

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: BadgeTone
}

// 语义色只做浅底 + 同色字（10% 底），正文世界仍是灰阶；stock emerald/red 清零
// 字色一律走 *-ink 槽：本色 safety/danger 压染色底与 well 余量不足 4.5:1
const tones: Record<BadgeTone, string> = {
  primary: 'bg-snb-safety/10 text-snb-safety-ink',
  success: 'bg-snb-live/15 text-snb-live-ink',
  warning: 'bg-snb-safety/10 text-snb-safety-ink',
  danger: 'bg-snb-danger/10 text-snb-danger-ink',
  gray: 'bg-snb-well text-snb-t2',
}

export function Badge({ tone = 'primary', className, ...rest }: BadgeProps) {
  return (
    <span
      className={cx('inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium', tones[tone], className)}
      {...rest}
    />
  )
}
