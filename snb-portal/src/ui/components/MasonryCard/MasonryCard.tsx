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
        'group relative mb-4 break-inside-avoid overflow-hidden rounded-xl border border-white/[0.06] bg-dark-900 shadow-card transition-shadow duration-300 focus-within:shadow-card-hover hover:shadow-card-hover',
        interactive && 'cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50',
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
            'block w-full transition-transform duration-500 group-hover:scale-[1.03] motion-reduce:transform-none',
            ratio ? 'h-full object-cover' : 'h-auto'
          )}
        />
      </div>
      {stats ? (
        // 新版：stats 常驻图底（社会证明），overlay 在其上方 hover/聚焦时展开
        <div className="pointer-events-none absolute inset-x-0 bottom-0 flex flex-col bg-gradient-to-t from-black/70 via-black/20 to-transparent p-3">
          {overlay && (
            <div className="max-h-0 overflow-hidden opacity-0 transition-all duration-200 ease-out group-hover:mb-2 group-hover:max-h-44 group-hover:opacity-100 focus-within:mb-2 focus-within:max-h-44 focus-within:opacity-100 [&>*]:pointer-events-auto">
              {overlay}
            </div>
          )}
          <div className="[&_button]:pointer-events-auto">{stats}</div>
          {caption && <p className="pointer-events-none mt-1.5 truncate text-[11px] text-white/55">{caption}</p>}
        </div>
      ) : (
        (overlay || caption) && (
          // 旧版（向后兼容）：overlay/caption 全悬停显示
          <div className="pointer-events-none absolute inset-0 flex flex-col justify-end bg-gradient-to-t from-black/85 via-black/35 to-transparent p-3 opacity-0 transition-opacity duration-200 focus-within:pointer-events-auto focus-within:opacity-100 group-hover:pointer-events-auto group-hover:opacity-100">
            {overlay}
            {caption && <p className="mt-2 truncate text-center text-[11px] text-white/60">{caption}</p>}
          </div>
        )
      )}
    </div>
  )
}
