import type { HTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export type BadgeTone = 'primary' | 'success' | 'warning' | 'danger' | 'gray'

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: BadgeTone
}

const tones: Record<BadgeTone, string> = {
  primary: 'bg-primary-100 text-primary-700 dark:bg-primary-900/30 dark:text-primary-400',
  success: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
  warning: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  danger: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  gray: 'bg-black/[0.06] text-snb-t2 dark:bg-white/[0.08]',
}

export function Badge({ tone = 'primary', className, ...rest }: BadgeProps) {
  return (
    <span
      className={cx('inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium', tones[tone], className)}
      {...rest}
    />
  )
}
