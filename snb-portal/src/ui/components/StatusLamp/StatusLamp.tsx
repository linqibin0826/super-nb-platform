import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'

/** 三态（GlobalParts v3 §06）：正在发生 / 待开 / 已结束。呼吸不给未发生的事。 */
export type StatusLampState = 'live' | 'pending' | 'ended'

export interface StatusLampProps {
  state: StatusLampState
  /** 灯右侧文案；不传则只出一颗灯（导航项/角标里当零件用） */
  children?: ReactNode
  /** 灯本体的额外类（只出灯时 className 也落在灯上） */
  dotClassName?: string
  className?: string
}

// 灭态灯不沿用 t3，单独走 --snb-lamp-off（浅 #AEAEB2 / 深 #6E6E73）：
// 它是装饰点不是文本，3:1 非文本门槛不适用，取值只求两档看得见、不抢主色。
// 在线点=绿（唯一允许的绿，只做点不做字）；扩散环由 animate-snb-dot 读 --snb-live。
// 🪦 橙点随 v2 退役：赤陶橙（--snb-safety）只给「有新活动」的 6px 小点（NavCapsule 自渲染），不再当状态灯。
const dotTone: Record<StatusLampState, string> = {
  live: 'bg-snb-live animate-snb-dot motion-reduce:animate-none',
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
