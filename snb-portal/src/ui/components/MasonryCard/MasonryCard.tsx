import type { HTMLAttributes, KeyboardEvent, ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface MasonryGridProps extends HTMLAttributes<HTMLDivElement> {}

/** 瀑布流容器：CSS columns（studio 灵感库同款断点） */
export function MasonryGrid({ className, ...rest }: MasonryGridProps) {
  return <div className={cx('columns-2 gap-4 md:columns-3 xl:columns-4', className)} {...rest} />
}

export interface MasonryCardProps {
  src: string
  alt?: string
  /** 图片原始宽高：给出时按精确比例占位，防瀑布流塌陷/跳动 */
  width?: number
  height?: number
  /** 悬停/聚焦浮层内容（按钮组等），渐变遮罩内底部对齐 */
  overlay?: ReactNode
  /** 常驻底栏（点赞收藏等社会证明）：给出时常显于图底、不随 hover 隐藏，
   *  overlay 改为在其上方 hover 展开。不给出则维持旧版（overlay/caption 全悬停）。 */
  stats?: ReactNode
  /** 署名行：有 stats 时常显于底栏下方，否则随旧版浮层显示 */
  caption?: ReactNode
  /** 触屏激活回调：给出时整卡可点/可键盘激活（触屏无 hover 浮层的兜底入口）。
   *  不给出时卡片是纯展示（现状），仅靠 hover 浮层内按钮交互。 */
  onActivate?: () => void
  className?: string
}

export function MasonryCard({ src, alt = '', width, height, overlay, stats, caption, onActivate, className }: MasonryCardProps) {
  const ratio = width && height ? `${width} / ${height}` : undefined
  const interactive = typeof onActivate === 'function'
  const onKeyDown = interactive
    ? (e: KeyboardEvent<HTMLDivElement>) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          onActivate!()
        }
      }
    : undefined
  return (
    <div
      role={interactive ? 'button' : undefined}
      tabIndex={interactive ? 0 : undefined}
      aria-label={interactive ? alt || undefined : undefined}
      onClick={interactive ? onActivate : undefined}
      onKeyDown={onKeyDown}
      className={cx(
        // 卡框（图片外的部分）跟着主题翻：浅色底走深井 well（图未加载时是块桌面色），
        // 描边走 hairline；深色保持旧值原样。
        // 🚨 但**图上的一切不翻**——见下面遮罩层的注释
        'group relative mb-4 break-inside-avoid overflow-hidden rounded-xl border border-snb-hairline bg-snb-well shadow-card transition-shadow duration-quick ease-snb focus-within:shadow-card-hover hover:shadow-card-hover dark:border-white/[0.06] dark:bg-dark-900',
        interactive && 'cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus',
        className
      )}
    >
      <div className="w-full overflow-hidden" style={ratio ? { aspectRatio: ratio } : undefined}>
        <img
          src={src}
          alt={alt}
          loading="lazy"
          decoding="async"
          className={cx(
            'block w-full transition-transform duration-settle ease-snb group-hover:scale-[1.03] motion-reduce:transform-none',
            ratio ? 'h-full object-cover' : 'h-auto'
          )}
        />
      </div>
      {stats ? (
        // 新版：stats 常驻图底（社会证明），overlay 在其上方 hover/聚焦时展开。
        // 🚨🚨 **图上的遮罩与其上的纸白字，两档都保持深色遮罩 + 纸白字，绝不跟着主题翻**：
        // 图片可读性设施与主题无关——底下是用户的图（可能是白仪表盘也可能是黑夜景），
        // 白天把遮罩翻浅、把白字翻墨，等于墨字压深底 1.06–2.25:1 全瞎。
        // 这正是浅色化时最经典的翻车点（机械替换表里专门为它留了「深色画布语境不翻」的例外）。
        // 🚨 遮罩必须够厚：旧版 from-black/70 via-black/20 在浅色图（白仪表盘/米色信息图/
        // 热敏小票截图）上，署名那一档只剩 20% 黑，实测 1.6:1——5778 条素材的语义线索
        // 与 CC BY 署名要求一起被掐掉。改四段加厚（0 → .62@34% → .9@68% → .97@100%），
        // 合成底 ≈ #26282C，白字 ≈15:1，与深浅图无关恒定可读。
        <div className="pointer-events-none absolute inset-x-0 bottom-0 flex min-h-24 flex-col justify-end [background:linear-gradient(180deg,rgba(14,16,20,0)_0%,rgba(14,16,20,0.62)_34%,rgba(14,16,20,0.9)_68%,rgba(14,16,20,0.97)_100%)] px-3 pb-2.5">
          {overlay && (
            <div className="max-h-0 overflow-hidden opacity-0 transition-all duration-quick ease-snb group-hover:mb-2 group-hover:max-h-44 group-hover:opacity-100 focus-within:mb-2 focus-within:max-h-44 focus-within:opacity-100 [&>*]:pointer-events-auto">
              {overlay}
            </div>
          )}
          <div className="[&_button]:pointer-events-auto">{stats}</div>
          {/* 署名是授权要求不是装饰：实压阴影（不是辉光）保证它压在任何图上都读得出 */}
          {caption && (
            <p className="pointer-events-none mt-1.5 truncate text-[11px] text-[#D8D3CA] [text-shadow:0_1px_2px_rgba(0,0,0,0.95),0_2px_8px_rgba(0,0,0,0.75)]">
              {caption}
            </p>
          )}
        </div>
      ) : (
        (overlay || caption) && (
          // 旧版（向后兼容）：overlay/caption 全悬停显示
          <div className="pointer-events-none absolute inset-0 flex flex-col justify-end bg-gradient-to-t from-black/85 via-black/35 to-transparent p-3 opacity-0 transition-opacity duration-quick ease-snb focus-within:pointer-events-auto focus-within:opacity-100 group-hover:pointer-events-auto group-hover:opacity-100">
            {overlay}
            {caption && <p className="mt-2 truncate text-center text-[11px] text-white/60">{caption}</p>}
          </div>
        )
      )}
    </div>
  )
}
