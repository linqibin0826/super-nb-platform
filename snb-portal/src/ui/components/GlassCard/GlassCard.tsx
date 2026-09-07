import type { HTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface GlassCardProps extends HTMLAttributes<HTMLDivElement> {
  /** soft=站内卡；cinematic=登录/暗景大卡（更大圆角与投影） */
  variant?: 'soft' | 'cinematic'
}

/* 统一面板（苹果式 v3）：无描边、实白面、双层柔投影；名字里的 Glass 留作历史——
   玻璃只在粘性顶栏与浮层，内容面永远实底。 */
const variants = {
  soft: 'rounded-3xl bg-snb-panel shadow-glass-sm',
  cinematic: 'relative overflow-hidden rounded-4xl bg-snb-panel text-snb-t1 shadow-glass',
} as const

export function GlassCard({ variant = 'soft', className, ...rest }: GlassCardProps) {
  return <div className={cx(variants[variant], className)} {...rest} />
}
