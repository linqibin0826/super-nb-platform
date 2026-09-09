import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'

export type StatTone = 'primary' | 'success' | 'warning' | 'danger'

export interface StatCardProps {
  label: string
  value: ReactNode
  icon?: ReactNode
  tone?: StatTone
  trend?: { direction: 'up' | 'down'; text: string }
  className?: string
}

// 字色一律走 *-ink 槽（同 Badge）：本色压 10% 染色底余量不足 4.5:1
const iconTones: Record<StatTone, string> = {
  primary: 'bg-snb-safety/10 text-snb-safety-ink',
  success: 'bg-snb-live/15 text-snb-live-ink',
  warning: 'bg-snb-safety/10 text-snb-safety-ink',
  danger: 'bg-snb-danger/10 text-snb-danger-ink',
}

export function StatCard({ label, value, icon, tone = 'primary', trend, className }: StatCardProps) {
  return (
    <div
      className={cx('flex items-start gap-4 rounded-3xl bg-snb-panel p-5 shadow-glass-sm', className)}
    >
      {icon && (
        <div className={cx('flex h-12 w-12 items-center justify-center rounded-lg text-xl', iconTones[tone])}>
          {icon}
        </div>
      )}
      <div className="min-w-0">
        <p className="text-sm text-snb-t3">{label}</p>
        <p className="truncate text-[26px] font-semibold tabular-nums tracking-[-0.02em] text-snb-t1">{value}</p>
        {trend && (
          <p
            className={cx(
              'mt-1 flex items-center gap-1 text-xs font-medium',
              trend.direction === 'up' ? 'text-snb-live-ink' : 'text-snb-danger'
            )}
          >
            <span>{trend.direction === 'up' ? '↑' : '↓'}</span> {trend.text}
          </p>
        )}
      </div>
    </div>
  )
}
