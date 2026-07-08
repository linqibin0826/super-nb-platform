// JS 分列瀑布流：取代 CSS columns（多栏是「内容流末尾＝最右列」的填充模型，
// 无限滚动追加的新卡全堆在右侧几列、还会跨列重排跳动——GIF 实锤的病灶）。
// 每列独立 div：新卡按「贪心分给最矮列」落位（用 imageW/h 精确记账），
// 老卡永不挪列；批次内 motion 弹簧错峰入场，图片加载完成淡入（见 index.css .snb-wall）。
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { motion, useReducedMotion } from 'motion/react'
import type { PromptListItem } from '../lib/galleryApi'

interface Slot {
  item: PromptListItem
  /** 批次内错峰入场延迟（秒） */
  delay: number
}

export interface MasonryLayout {
  cols: Slot[][]
  /** 各列累计高度（列宽归一化：h/w 之和 + 间距近似） */
  heights: number[]
  /** 已分配条目数（items 前缀） */
  count: number
}

/** 缺宽高条目按 3:4 占位（与 AutoRatioCard 的占位假设一致），卡片下边距近似 0.05 列宽 */
const FALLBACK_RATIO = 4 / 3
const GAP_RATIO = 0.05
const STAGGER_SEC = 0.045
const STAGGER_CAP_SEC = 0.5

export function emptyLayout(colCount: number): MasonryLayout {
  return { cols: Array.from({ length: colCount }, () => []), heights: Array(colCount).fill(0), count: 0 }
}

/** 把 items 中尚未分配的后缀（layout.count 起）贪心分给最矮列；幂等、只追加不重排 */
export function assignPending(layout: MasonryLayout, items: PromptListItem[]): MasonryLayout {
  if (layout.count >= items.length) return layout
  const batch = items.slice(layout.count)
  batch.forEach((item, i) => {
    const ratio = item.imageW > 0 && item.imageH > 0 ? item.imageH / item.imageW : FALLBACK_RATIO
    let col = 0
    for (let c = 1; c < layout.heights.length; c++) {
      if (layout.heights[c] < layout.heights[col]) col = c
    }
    layout.cols[col].push({ item, delay: Math.min(i * STAGGER_SEC, STAGGER_CAP_SEC) })
    layout.heights[col] += ratio + GAP_RATIO
  })
  layout.count = items.length
  return layout
}

/** 列数跟 tailwind 断点对齐（原 CSS columns 版：2 / md 3 / xl 4 / 2xl 5） */
function calcColumns(): number {
  if (typeof window === 'undefined') return 4
  if (window.matchMedia('(min-width: 1536px)').matches) return 5
  if (window.matchMedia('(min-width: 1280px)').matches) return 4
  if (window.matchMedia('(min-width: 768px)').matches) return 3
  return 2
}

function useColumnCount(): number {
  const [count, setCount] = useState(calcColumns)
  useEffect(() => {
    const queries = [768, 1280, 1536].map((bp) => window.matchMedia(`(min-width: ${bp}px)`))
    const onChange = () => setCount(calcColumns())
    queries.forEach((q) => q.addEventListener('change', onChange))
    return () => queries.forEach((q) => q.removeEventListener('change', onChange))
  }, [])
  return count
}

interface Props {
  items: PromptListItem[]
  /** 筛选/排序换代号：变了整墙重排（否则只追加） */
  resetKey: number | string
  renderItem: (item: PromptListItem) => ReactNode
}

export function BalancedMasonry({ items, resetKey, renderItem }: Props) {
  const colCount = useColumnCount()
  const reduceMotion = useReducedMotion()
  const wallRef = useRef<HTMLDivElement | null>(null)

  // derive-with-cache：同 (resetKey, colCount) 下布局只追加、幂等（StrictMode 双渲染安全）
  const layoutRef = useRef<{ key: string; layout: MasonryLayout } | null>(null)
  const cacheKey = `${resetKey}|${colCount}`
  if (layoutRef.current?.key !== cacheKey) {
    layoutRef.current = { key: cacheKey, layout: emptyLayout(colCount) }
  }
  const layout = assignPending(layoutRef.current.layout, items)

  // 图片加载完成 → 打标淡入（CSS 见 .snb-wall）。命中缓存的 load 可能抢跑，补扫一遍 complete
  useEffect(() => {
    const wall = wallRef.current
    if (!wall) return
    wall.querySelectorAll('img').forEach((img) => {
      if (img.complete && img.naturalWidth > 0) img.dataset.snbLoaded = '1'
    })
  })

  function markLoaded(e: React.SyntheticEvent) {
    if (e.target instanceof HTMLImageElement) e.target.dataset.snbLoaded = '1'
  }

  return (
    <div
      ref={wallRef}
      className="snb-wall flex items-start gap-4"
      onLoadCapture={markLoaded}
      onErrorCapture={markLoaded}
    >
      {layout.cols.map((col, ci) => (
        <div key={ci} className="min-w-0 flex-1">
          {col.map(({ item, delay }) => (
            <motion.div
              key={item.id}
              initial={reduceMotion ? false : { opacity: 0, y: 26, scale: 0.97 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              transition={
                reduceMotion
                  ? { duration: 0 }
                  : { type: 'spring', stiffness: 340, damping: 28, delay }
              }
            >
              {renderItem(item)}
            </motion.div>
          ))}
        </div>
      ))}
    </div>
  )
}
