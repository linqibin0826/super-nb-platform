// 灵感卡（设计定稿 StudioCardProof · A 加厚遮罩拍板版，2026-07-29）：
// 浅色图最坏用例（白仪表盘/米色信息图/热敏小票）也必须读得出——
// · 底部信息层固定 96px，四段遮罩 rgba(14,16,20) 0 → .62(34%) → .9(68%) → .97(100%)，
//   合成底 ≈ #26282C，白字 ≈15:1；
// · 标题 14/600 纯白 + 纯黑实压阴影（不是辉光）；署名 #D8D3CA 同阴影（CC BY 4.0 署名常显）；
// · 赞/藏计数走 rgba(14,16,20,.72) 实底小牌（CardStat），恒定对比不吃图的深浅；
// · 图片按 imageW/H 预占 aspect-ratio（CLS 修复），缺宽高 3:4 占位、载完换天然比例；
// · 整卡可点（cursor-zoom-in）：桌面开大图 / 触屏开抽屉；hover 才展开「直接使用/复制」。
import { useEffect, useRef, useState, type KeyboardEvent, type ReactNode } from 'react'

export interface WallCardProps {
  src: string
  alt: string
  /** 图片原始宽高；>0 时精确预占位，否则 3:4 占位 + 加载完成换天然比例 */
  width?: number
  height?: number
  title: string
  author: string | null
  /** 常驻计数排（CardStat 对） */
  stats: ReactNode
  /** hover/聚焦才展开的动作排（直接使用/复制）；触屏由整卡点击兜底 */
  actions?: ReactNode
  /** 整卡激活：桌面=点开看大图，触屏=弹抽屉 */
  onOpen?: () => void
}

export function WallCard({ src, alt, width, height, title, author, stats, actions, onOpen }: WallCardProps) {
  const shellRef = useRef<HTMLDivElement | null>(null)
  const known = typeof width === 'number' && typeof height === 'number' && width > 0 && height > 0
  const [dims, setDims] = useState<{ w: number; h: number } | null>(known ? { w: width!, h: height! } : null)

  // 缺宽高：先 3:4 占位防瀑布流塌陷，图片载完换天然比例（load 不冒泡但可捕获；
  // 缓存命中可能抢在监听之前，先补查一次 complete）——原 AutoRatioCard 同款语义
  useEffect(() => {
    if (known) return
    const shell = shellRef.current
    if (!shell) return
    const img = shell.querySelector('img')
    if (img?.complete && img.naturalWidth > 0) {
      setDims({ w: img.naturalWidth, h: img.naturalHeight })
      return
    }
    const onLoad = (e: Event) => {
      const target = e.target
      if (target instanceof HTMLImageElement && target.naturalWidth > 0) {
        setDims({ w: target.naturalWidth, h: target.naturalHeight })
      }
    }
    shell.addEventListener('load', onLoad, true)
    return () => shell.removeEventListener('load', onLoad, true)
  }, [known])

  const ratio = dims ? `${dims.w} / ${dims.h}` : '3 / 4'
  const interactive = typeof onOpen === 'function'
  const onKeyDown = interactive
    ? (e: KeyboardEvent<HTMLDivElement>) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          onOpen!()
        }
      }
    : undefined

  return (
    <div
      ref={shellRef}
      role={interactive ? 'button' : undefined}
      tabIndex={interactive ? 0 : undefined}
      aria-label={interactive ? alt || undefined : undefined}
      onClick={interactive ? onOpen : undefined}
      onKeyDown={onKeyDown}
      className={`group relative mb-3.5 break-inside-avoid overflow-hidden rounded-xl border border-snb-hairline bg-snb-panel transition-[border-color,transform] duration-[120ms] ease-[cubic-bezier(0.2,0,0,1)] hover:-translate-y-0.5 hover:border-[rgba(239,235,228,0.4)] motion-reduce:transform-none ${
        interactive ? 'cursor-zoom-in focus:outline-none focus-visible:ring-2 focus-visible:ring-paper/60' : ''
      }`}
    >
      <div className="w-full overflow-hidden" style={{ aspectRatio: ratio }}>
        <img src={src} alt={alt} loading="lazy" decoding="async" className="block h-full w-full object-cover" />
      </div>

      {/* 尺寸小牌：真源宽高已知才挂（实底 0.82，恒定对比） */}
      {dims && (
        <span className="pointer-events-none absolute right-2.5 top-2.5 inline-flex h-[22px] items-center rounded-md bg-[rgba(14,16,20,0.82)] px-2 font-mono text-[11px] tracking-[0.02em] text-white">
          {dims.w}×{dims.h}
        </span>
      )}

      {/* 信息层：固定 96 高四段加厚遮罩，标题/计数/署名三件常驻 */}
      <div className="absolute inset-x-0 bottom-0 flex flex-col justify-end gap-1.5 px-3 pb-2.5 [background:linear-gradient(180deg,rgba(14,16,20,0)_0%,rgba(14,16,20,0.62)_34%,rgba(14,16,20,0.9)_68%,rgba(14,16,20,0.97)_100%)] min-h-24">
        {actions && (
          <div className="pointer-events-none max-h-0 overflow-hidden opacity-0 transition-all duration-200 ease-out focus-within:mb-1 focus-within:max-h-44 focus-within:opacity-100 group-hover:mb-1 group-hover:max-h-44 group-hover:opacity-100 [&>*]:pointer-events-auto">
            {actions}
          </div>
        )}
        <p className="truncate text-[14px] font-semibold leading-[1.35] text-[#FFFFFF] [text-shadow:0_1px_2px_rgba(0,0,0,0.95),0_2px_8px_rgba(0,0,0,0.75)]">
          {title}
        </p>
        <div className="flex items-center justify-between gap-2.5">
          <div className="flex flex-none gap-1.5 [&_button]:pointer-events-auto">{stats}</div>
          {author && (
            <span className="truncate font-mono text-xs text-[#D8D3CA] [text-shadow:0_1px_2px_rgba(0,0,0,0.95),0_2px_8px_rgba(0,0,0,0.75)]">
              @{author}
            </span>
          )}
        </div>
      </div>
    </div>
  )
}
