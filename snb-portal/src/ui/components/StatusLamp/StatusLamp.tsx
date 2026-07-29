import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'

/** 三态（GlobalParts v3 §06）：正在发生 / 待开 / 已结束。橙不给未发生的事。 */
export type StatusLampState = 'live' | 'pending' | 'ended'

export interface StatusLampProps {
  state: StatusLampState
  /** 灯右侧文案；不传则只出一颗灯（导航项/角标里当零件用） */
  children?: ReactNode
  /** 灯本体的额外类（只出灯时 className 也落在灯上） */
  dotClassName?: string
  className?: string
}

// 🚨 双档：呼吸环颜色跟着 --snb-safety 翻（深 rgba(255,92,0,.5) / 浅 rgba(186,68,0,.5)），
// 由 animate-snb-dot 的 keyframes 直接读变量，组件这边不写死。
// 灭态两色不能沿用 t3：深色的 #828B96 压纸只有 2.98:1（压底那一档卡在线上、当不住边界），
// 浅色单独走 --snb-lamp-off #86837C（压底 3.27 / 压面 3.60:1）。
const dotTone: Record<StatusLampState, string> = {
  // 呼吸只属于「正在发生」：2200ms 平色扩散环（box-shadow 零模糊半径），不是光晕
  live: 'bg-snb-safety animate-snb-dot motion-reduce:animate-none',
  pending: 'border-[1.5px] border-snb-lamp-off bg-transparent',
  ended: 'bg-snb-lamp-off',
}

const labelTone: Record<StatusLampState, string> = {
  live: 'text-snb-t2',
  pending: 'text-snb-t2',
  ended: 'text-snb-t3',
}

/**
 * 状态灯唯一版（GlobalParts v3 §06，收编全站五个各写各的实现）。
 * 8px 圆点 + 字距 10；页面上唯一会动的东西就是它——数据永远不闪。
 * 灯不可点，不受 44 热区约束；整行可点时热区归整行。
 */
export function StatusLamp({ state, children, dotClassName, className }: StatusLampProps) {
  const dot = (
    <span
      aria-hidden="true"
      className={cx(
        'h-2 w-2 flex-none rounded-full',
        dotTone[state],
        dotClassName,
        children === undefined ? className : undefined
      )}
    />
  )
  if (children === undefined) return dot
  return (
    <span className={cx('inline-flex items-center gap-2.5 text-[13.5px]', labelTone[state], className)}>
      {dot}
      {children}
    </span>
  )
}
