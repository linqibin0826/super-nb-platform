import type { HTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface GlassCardProps extends HTMLAttributes<HTMLDivElement> {
  /** soft=站内玻璃卡（随主题）；cinematic=电影页登录卡（始终深色，用于视频/暗景之上） */
  variant?: 'soft' | 'cinematic'
}

const variants = {
  soft: 'rounded-2xl border border-[var(--snb-glass-border)] bg-[var(--snb-glass-bg)] shadow-glass backdrop-blur-xl transition-all duration-300',
  cinematic:
    'relative overflow-hidden rounded-glass border border-white/[0.14] bg-[rgba(16,16,18,0.62)] text-white shadow-[0_24px_60px_-28px_rgba(0,0,0,0.7),inset_0_1px_0_rgba(255,255,255,0.12)] backdrop-blur-[20px] backdrop-saturate-[1.4]',
} as const

export function GlassCard({ variant = 'soft', className, ...rest }: GlassCardProps) {
  return <div className={cx(variants[variant], className)} {...rest} />
}
