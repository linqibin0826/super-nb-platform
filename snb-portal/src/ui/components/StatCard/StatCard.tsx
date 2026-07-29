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

// 浅色档实测：-600 压 -100 底一律不到 4.5:1（primary 3.37 / emerald 3.32 / red 3.95），
// 白天统一降到 -700（4.83 / 4.84 / 5.30 ✓）；深色分支 -400 原样不动
const iconTones: Record<StatTone, string> = {
  primary: 'bg-primary-100 text-primary-700 dark:bg-primary-900/30 dark:text-primary-400',
  success: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
  warning: 'bg-primary-100 text-primary-700 dark:bg-primary-900/30 dark:text-primary-400',
  danger: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
}

export function StatCard({ label, value, icon, tone = 'primary', trend, className }: StatCardProps) {
  return (
    <div
      className={cx(
        'flex items-start gap-4 rounded-2xl border border-snb-hairline bg-snb-panel p-5 shadow-card transition-all duration-300',
        className
      )}
    >
      {icon && (
        <div className={cx('flex h-12 w-12 items-center justify-center rounded-xl text-xl', iconTones[tone])}>
          {icon}
        </div>
      )}
      <div className="min-w-0">
        <p className="text-sm text-snb-t3">{label}</p>
        <p className="truncate text-2xl font-bold text-snb-t1">{value}</p>
        {trend && (
          <p
            className={cx(
              'mt-1 flex items-center gap-1 text-xs font-medium',
              // emerald-600 压卡片面只有 3.58:1，白天降一档到 -700（5.22:1）；
              // red-600 压面 4.59:1 达标，保持不动
              trend.direction === 'up'
                ? 'text-emerald-700 dark:text-emerald-400'
                : 'text-red-600 dark:text-red-400'
            )}
          >
            <span>{trend.direction === 'up' ? '↑' : '↓'}</span> {trend.text}
          </p>
        )}
      </div>
    </div>
  )
}
