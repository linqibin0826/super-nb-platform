import type { CSSProperties, ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { ctaAnchorClass } from '../../lib/cta'
import { StatusLamp } from '../StatusLamp/StatusLamp'

/** 骨架预设：数据表格 / 卡片列表 / 单个大数字（GlobalParts v3 §04 三场景） */
export type GuestGatePreset = 'table' | 'cards' | 'metric'

export interface GuestGateProps {
  /** 访客视图标注（mono 小字），默认「访客视图 · 数据未接通」 */
  notice?: ReactNode
  /** 骨架预设；传 children 时以 children 为准 */
  preset?: GuestGatePreset
  /** 自定义骨架槽（必须是静态色块——骨架也不许闪） */
  children?: ReactNode
  /** 整宽 CTA 文案，默认「开卡上机」 */
  ctaLabel?: ReactNode
  /** CTA 目标（注册/开卡页）；不传则退化成 onCta 按钮 */
  ctaHref?: string
  onCta?: () => void
  /** 一句「登录后这里是…」——说清填满以后长什么样，不要写「请登录」 */
  hint?: ReactNode
  className?: string
}

/** 静态骨架块：色块恒为 elv #242A33，**没有 animate-pulse**——
 *  数据不闪是红线，骨架属于数据的占位，同样不许闪。 */
function Bar({ className, style }: { className?: string; style?: CSSProperties }) {
  // ⚠️ 圆角由调用处给：细条走 rounded-full（高 10~12 即药丸），方块走 rounded-[8px]。
  // 基类里写 rounded-full 会赢过后写的 rounded-[8px]（Tailwind 看生成顺序不看类串顺序）
  return <span aria-hidden="true" style={style} className={cx('block bg-snb-elv', className)} />
}

const presets: Record<GuestGatePreset, ReactNode> = {
  // ① 数据表格：表头一行 + 分隔线 + 三行数据
  table: (
    <div className="flex flex-col gap-2.5">
      <div className="grid grid-cols-[2fr_3fr_1.5fr] gap-3">
        <Bar className="h-2.5 w-[60%] rounded-full" />
        <Bar className="h-2.5 w-1/2 rounded-full" />
        <Bar className="h-2.5 w-[70%] rounded-full" />
      </div>
      <div className="border-t border-snb-hairline" />
      {[
        ['100%', '80%', '60%'],
        ['100%', '65%', '60%'],
        ['100%', '75%', '60%'],
      ].map((row, i) => (
        <div key={i} className="grid grid-cols-[2fr_3fr_1.5fr] gap-3">
          {row.map((w, j) => (
            <Bar key={j} className="h-3 rounded-full" style={{ width: w }} />
          ))}
        </div>
      ))}
    </div>
  ),
  // ② 卡片列表：40 方缩略图 + 两行文字
  cards: (
    <div className="flex flex-col gap-3">
      {['55%', '65%', '50%'].map((w, i) => (
        <div key={i} className="flex items-center gap-3">
          <Bar className="h-10 w-10 flex-none rounded-[8px]" />
          <div className="flex flex-1 flex-col gap-2">
            <Bar className="h-3 rounded-full" style={{ width: w }} />
            <Bar className="h-2.5 w-[35%] rounded-full" />
          </div>
        </div>
      ))}
    </div>
  ),
  // ③ 单个大数字：小标 + 大数字条 + 一行注解
  metric: (
    <div className="flex flex-col gap-3 py-2">
      <Bar className="h-2.5 w-[30%] rounded-full" />
      <Bar className="h-9 w-[55%] rounded-[8px]" />
      <Bar className="h-2.5 w-[45%] rounded-full" />
    </div>
  ),
}

/**
 * 访客态公用件 GuestGate（GlobalParts v3 §04，提炼自签到页）。
 * 结构恒定：空心灯 +「访客视图 · 数据未接通」→ 静态骨架 → 整宽纸白 CTA → 一句登录后的样子。
 * 🚨 禁止：红色售罄印章 / 灰死按钮 /「已截止」当首屏 / 空表格硬摆——
 * 访客首屏说的是「登录后你能得到什么」，不是「你不能用」。
 */
export function GuestGate({
  notice = '访客视图 · 数据未接通',
  preset = 'table',
  children,
  ctaLabel = '开卡上机',
  ctaHref,
  onCta,
  hint,
  className,
}: GuestGateProps) {
  return (
    <div
      className={cx(
        'flex flex-col gap-4 rounded-xl border border-snb-hairline bg-snb-panel p-5',
        className
      )}
    >
      <div className="flex items-center gap-2">
        <StatusLamp state="pending" />
        <span className="font-mono text-[12.5px] tracking-[0.04em] text-snb-t3">{notice}</span>
      </div>
      <div>{children ?? presets[preset]}</div>
      {ctaHref ? (
        <a href={ctaHref} className={cx(ctaAnchorClass, 'w-full')}>
          {ctaLabel}
        </a>
      ) : (
        <button type="button" onClick={onCta} className={cx(ctaAnchorClass, 'w-full')}>
          {ctaLabel}
        </button>
      )}
      {hint && <p className="m-0 text-[13px] leading-relaxed text-snb-t2">{hint}</p>}
    </div>
  )
}
