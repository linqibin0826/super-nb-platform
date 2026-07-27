import type { HTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface GlassCardProps extends HTMLAttributes<HTMLDivElement> {
  /** soft=站内卡；cinematic=登录/暗景大卡（更大圆角与投影） */
  variant?: 'soft' | 'cinematic'
}

/* 🪦 玻璃已随零发光网吧 v2 退役（2026-07-27 站长拍板实心化）：
   blur+saturate+内高光整段删除，换实心面板 panel + 栏位线 rule——
   与 fork 的开卡台/机柜面同一材质。组件名与 API 保持不动（消费方零改动），
   名字里的 Glass 留作历史：它现在是一块实心的「玻璃」。
   backdrop-filter 全站唯一例外是滚动内容上的 sticky 顶栏（AppHeader）。 */
const variants = {
  soft: 'rounded-2xl border border-dark-700 bg-dark-800 shadow-card transition-all duration-300',
  cinematic: 'relative overflow-hidden rounded-glass border border-dark-700 bg-dark-800 text-white shadow-glass',
} as const

export function GlassCard({ variant = 'soft', className, ...rest }: GlassCardProps) {
  return <div className={cx(variants[variant], className)} {...rest} />
}
