// 全站公用件 v3 的 studio 本地实现（2026-07-29）。
//
// 🚨 为什么在这儿而不是 src/ui：本仓的 src/ui 是 vendor 副本，本轮一律只读；
// 上游 super-nb-ui 已落地公用件 v3（Button md 改 44/r8、secondary 改透明描边、
// Chip 两层化、新增 StatusLamp/GuestGate/EmptyState/ErrorState 与三条 <a> 配方），
// 副本还没同步。收口时由编排者统一做 vendor 同步并去重——**本文件的数值必须与
// GlobalParts.dc.html 定稿逐条一致**，将来换成 vendor 版才不会有像素差。
// 命名刻意对齐上游（ctaAnchorClass / secondaryAnchorClass / ghostAnchorClass /
// StatusLamp / EmptyState / ErrorState），换件时是改 import 不是改结构。
//
// ⚠️ 纪律：这些配方只用在**本目录自己写的 <a>/<button>** 上，绝不拿来 className
// 覆盖 vendor 组件的内部结构（Chip 两层化之后那类覆盖必失配）。
import type { ReactNode } from 'react'

const MOTION = 'duration-[120ms] ease-[cubic-bezier(0.2,0,0,1)]'

/** 主按钮四态：高 44 / px 20 / r8 / 14px semibold / 纸白底沥青字 /
 *  底边 0 2px 0 黑45%；hover 提白抬 1px 底边 3px；按下 #DED8CE 沉 1px 底边 1px 55ms；
 *  禁用 elv 底 t3 字无底边。 */
export const ctaClass =
  'inline-flex h-11 items-center justify-center rounded-[8px] border-0 px-5 text-sm font-semibold ' +
  'bg-paper text-asphalt shadow-[0_2px_0_rgba(0,0,0,0.45)] ' +
  `transition-[transform,background-color,box-shadow] ${MOTION} ` +
  'hover:bg-[#FFFFFF] hover:-translate-y-px hover:shadow-[0_3px_0_rgba(0,0,0,0.45)] ' +
  'active:bg-[#DED8CE] active:translate-y-px active:shadow-[0_1px_0_rgba(0,0,0,0.45)] active:duration-[55ms] ' +
  'focus:outline-none focus-visible:ring-2 focus-visible:ring-paper/60 ' +
  'disabled:cursor-not-allowed disabled:bg-snb-elv disabled:text-snb-t3 disabled:shadow-none ' +
  'disabled:translate-y-0 disabled:hover:bg-snb-elv motion-reduce:transform-none'

/** <a> 版主按钮（无 disabled 态） */
export const ctaAnchorClass =
  'inline-flex h-11 items-center justify-center rounded-[8px] px-5 text-sm font-semibold no-underline ' +
  'bg-paper text-asphalt shadow-[0_2px_0_rgba(0,0,0,0.45)] ' +
  `transition-[transform,background-color,box-shadow] ${MOTION} ` +
  'hover:bg-[#FFFFFF] hover:-translate-y-px hover:shadow-[0_3px_0_rgba(0,0,0,0.45)] ' +
  'active:bg-[#DED8CE] active:translate-y-px active:shadow-[0_1px_0_rgba(0,0,0,0.45)] active:duration-[55ms] ' +
  'focus:outline-none focus-visible:ring-2 focus-visible:ring-paper/60 motion-reduce:transform-none'

/** 次按钮：透明底 + hairline-strong 描边 + 纸白字；hover 底 #171A20、边 .3；按下沉 1px */
export const secondaryClass =
  'inline-flex h-11 items-center justify-center rounded-[8px] border border-snb-hairline-strong px-5 text-sm ' +
  `bg-transparent text-snb-t1 transition-[transform,background-color,border-color] ${MOTION} ` +
  'hover:bg-snb-panel hover:border-[rgba(239,235,228,0.3)] active:translate-y-px active:duration-[55ms] ' +
  'focus:outline-none focus-visible:ring-2 focus-visible:ring-paper/60 ' +
  'disabled:cursor-not-allowed disabled:border-snb-hairline disabled:text-snb-t3 disabled:translate-y-0 ' +
  'disabled:hover:bg-transparent motion-reduce:transform-none'

export const secondaryAnchorClass =
  'inline-flex h-11 items-center justify-center rounded-[8px] border border-snb-hairline-strong px-5 text-sm no-underline ' +
  `bg-transparent text-snb-t1 transition-[transform,background-color,border-color] ${MOTION} ` +
  'hover:bg-snb-panel hover:border-[rgba(239,235,228,0.3)] active:translate-y-px active:duration-[55ms] ' +
  'focus:outline-none focus-visible:ring-2 focus-visible:ring-paper/60 motion-reduce:transform-none'

/** 幽灵：无边 + 文二；hover 纸白字 + rgba(239,235,228,.06) 底 */
export const ghostClass =
  'inline-flex h-11 items-center justify-center rounded-[8px] border-0 px-3.5 text-sm ' +
  `bg-transparent text-snb-t2 transition-[transform,background-color,color] ${MOTION} ` +
  'hover:bg-snb-t1/[0.06] hover:text-snb-t1 active:translate-y-px active:duration-[55ms] ' +
  'focus:outline-none focus-visible:ring-2 focus-visible:ring-paper/60 ' +
  'disabled:cursor-not-allowed disabled:opacity-60 motion-reduce:transform-none'

export type LampState = 'live' | 'pending' | 'ended'

/** 状态灯：8px 圆点，三态。呼吸只属于「正在发生」——2.2s 平色扩散环（零模糊半径），不是光晕。
 *  空心 = 待开（橙不给未发生的事）；灰实心 = 已结束。灯不可点，不受 44 热区约束。 */
export function StatusLamp({ state }: { state: LampState }) {
  const skin =
    state === 'live'
      ? 'animate-snb-dot bg-primary-500 motion-reduce:animate-none'
      : state === 'pending'
        ? 'border-[1.5px] border-snb-t3'
        : 'bg-snb-t3'
  return <span aria-hidden="true" className={`h-2 w-2 flex-none rounded-full ${skin}`} />
}

interface StateProps {
  title: string
  body: string
  /** 空态的按钮必须指向「能把这里填满的那个动作」，不许只写「暂无数据」 */
  actionLabel: string
  onAction: () => void
}

function StateShell({
  danger,
  glyph,
  title,
  body,
  children,
}: {
  danger?: boolean
  glyph: string
  title: string
  body: string
  children: ReactNode
}) {
  return (
    <div
      className={`flex flex-col items-center gap-3 rounded-xl border bg-snb-panel px-4 py-11 text-center sm:px-6 ${
        danger ? 'border-[rgba(229,72,77,0.45)]' : 'border-snb-hairline'
      }`}
    >
      <span
        aria-hidden="true"
        className={`grid h-11 w-11 place-items-center rounded-full border font-mono text-lg ${
          danger ? 'border-snb-ember font-bold text-snb-ember' : 'border-snb-hairline-strong text-snb-t3'
        }`}
      >
        {glyph}
      </span>
      <p className="m-0 text-[15px] font-semibold text-snb-t1">{title}</p>
      <p className="m-0 max-w-[34ch] text-[13.5px] leading-[1.6] text-snb-t2">{body}</p>
      <div className="mt-1.5 flex flex-wrap justify-center gap-3">{children}</div>
    </div>
  )
}

/** 空态：请求成功且结果为空才用它。判定与错误态不可互换 */
export function EmptyState({ title, body, actionLabel, onAction }: StateProps) {
  return (
    <StateShell glyph="∅" title={title} body={body}>
      <button type="button" className={secondaryClass} onClick={onAction}>
        {actionLabel}
      </button>
    </StateShell>
  )
}

/** 错误态：请求失败/超时/解析失败一律走它，绝不假装「暂无数据」。
 *  文案三要素：哪件事失败了 / 责任在谁 / 用户的东西丢没丢。按钮必带「再试一次」 */
export function ErrorState({ title, body, actionLabel, onAction }: StateProps) {
  return (
    <StateShell danger glyph="!" title={title} body={body}>
      <button type="button" className={secondaryClass} onClick={onAction}>
        {actionLabel}
      </button>
    </StateShell>
  )
}
