// 灵感卡常驻数据条上的点赞/收藏胶囊：图底玻璃底 + 品牌 ember(赞)/amber(藏) 填充 + 点按回弹。
// 常驻可见（社会证明），触屏也能直接点——stopPropagation 不触发整卡 onActivate（开抽屉）。
import { motion } from 'motion/react'

interface Props {
  kind: 'like' | 'save'
  on: boolean
  count: number
  label: string
  onToggle: () => void
}

export function CardStat({ kind, on, count, label, onToggle }: Props) {
  // 只给图标上品牌色（点亮时 ember/amber），计数恒白——整颗按钮变色不是产品惯例、且计数发红显脏
  const iconColor = on ? (kind === 'like' ? 'text-snb-ember' : 'text-snb-amber') : 'text-white'
  return (
    <motion.button
      type="button"
      aria-label={label}
      aria-pressed={on}
      whileTap={{ scale: 0.9 }}
      onClick={(e) => {
        e.stopPropagation()
        onToggle()
      }}
      // 焦点环：白/70 在亮图上糊成一圈浅灰晕（显脏）→ 改用带偏移的清晰白环，像有意的焦点态而非光晕
      className="inline-flex items-center gap-1.5 rounded-full bg-black/40 px-2.5 py-1 text-xs font-semibold text-white backdrop-blur-sm transition-colors hover:bg-black/55 focus:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-black/50"
    >
      <span className={`flex ${iconColor}`}>
        {kind === 'like' ? <HeartIcon filled={on} /> : <BookmarkIcon filled={on} />}
      </span>
      <span className="tabular-nums">{count}</span>
    </motion.button>
  )
}

function HeartIcon({ filled }: { filled: boolean }) {
  return (
    <svg
      className="h-3.5 w-3.5 shrink-0"
      viewBox="0 0 24 24"
      fill={filled ? 'currentColor' : 'none'}
      stroke="currentColor"
      strokeWidth={filled ? 0 : 2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {/* Feather heart：居中对称，缩到小尺寸不变形 */}
      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
    </svg>
  )
}

function BookmarkIcon({ filled }: { filled: boolean }) {
  return (
    <svg
      className="h-3.5 w-3.5 shrink-0"
      viewBox="0 0 24 24"
      fill={filled ? 'currentColor' : 'none'}
      stroke="currentColor"
      strokeWidth={filled ? 0 : 2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
    </svg>
  )
}
