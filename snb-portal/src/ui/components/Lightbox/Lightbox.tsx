import { useEffect, useRef, type TouchEvent as ReactTouchEvent } from 'react'

export interface LightboxProps {
  /** 当前批次全部图片 url；长度 1 时只渲染大图 + 关闭，不出箭头/缩略条/键盘/滑动 */
  images: string[]
  /** null = 不渲染（关闭） */
  index: number | null
  onIndexChange: (index: number) => void
  onClose: () => void
  /** 不传则不渲染下载按钮 */
  onDownload?: (index: number) => void
  alt?: (index: number) => string
  prevLabel?: string
  nextLabel?: string
  closeLabel?: string
  downloadLabel?: string
}

const SWIPE_THRESHOLD_PX = 40

/** 全屏图片预览：黑玻璃遮罩，点大图/背景关闭；批次 >1 张时加左右切换（首尾循环）+
 *  底部缩略条 + 键盘 ←→/Esc + 触屏水平滑动；提供 onDownload 才出下载按钮。 */
export function Lightbox({
  images,
  index,
  onIndexChange,
  onClose,
  onDownload,
  alt = (i) => `preview ${i + 1}`,
  prevLabel = 'Previous',
  nextLabel = 'Next',
  closeLabel = 'Close',
  downloadLabel = 'Download',
}: LightboxProps) {
  const touchStart = useRef<{ x: number; y: number } | null>(null)
  const hasMultiple = images.length > 1

  useEffect(() => {
    if (index === null) return
    const current = index
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose()
        return
      }
      if (!hasMultiple) return
      if (e.key === 'ArrowLeft') onIndexChange((current - 1 + images.length) % images.length)
      else if (e.key === 'ArrowRight') onIndexChange((current + 1) % images.length)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [index, hasMultiple, images.length, onIndexChange, onClose])

  if (index === null) return null

  const onTouchStart = (e: ReactTouchEvent) => {
    touchStart.current = { x: e.touches[0].clientX, y: e.touches[0].clientY }
  }

  const onTouchEnd = (e: ReactTouchEvent) => {
    const start = touchStart.current
    touchStart.current = null
    if (!start || !hasMultiple) return
    const dx = e.changedTouches[0].clientX - start.x
    const dy = e.changedTouches[0].clientY - start.y
    if (Math.abs(dx) < SWIPE_THRESHOLD_PX || Math.abs(dx) < Math.abs(dy)) return
    if (dx < 0) onIndexChange((index + 1) % images.length)
    else onIndexChange((index - 1 + images.length) % images.length)
  }

  return (
    <div
      className="fixed inset-0 z-50 flex cursor-zoom-out flex-col items-center justify-center gap-4 bg-black/85 p-4 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        className="relative flex max-h-[calc(100%-80px)] max-w-full items-center justify-center"
        onTouchStart={onTouchStart}
        onTouchEnd={onTouchEnd}
      >
        <img src={images[index]} alt={alt(index)} className="max-h-full max-w-full rounded-xl shadow-2xl" />

        <div className="absolute right-3 top-3 flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
          {onDownload && (
            <button
              type="button"
              className="cursor-pointer rounded-[10px] bg-primary-500 px-3 py-1.5 text-xs text-white"
              onClick={() => onDownload(index)}
            >
              {downloadLabel}
            </button>
          )}
          <button
            type="button"
            aria-label={closeLabel}
            className="cursor-pointer rounded-full border border-white/35 bg-black/35 px-2.5 py-1.5 text-sm text-white backdrop-blur-sm"
            onClick={onClose}
          >
            ✕
          </button>
        </div>

        {hasMultiple && (
          <>
            <button
              type="button"
              aria-label={prevLabel}
              className="absolute left-2 top-1/2 -translate-y-1/2 cursor-pointer rounded-full border border-white/35 bg-black/35 px-3 py-2 text-lg text-white backdrop-blur-sm"
              onClick={(e) => {
                e.stopPropagation()
                onIndexChange((index - 1 + images.length) % images.length)
              }}
            >
              ‹
            </button>
            <button
              type="button"
              aria-label={nextLabel}
              className="absolute right-2 top-1/2 -translate-y-1/2 cursor-pointer rounded-full border border-white/35 bg-black/35 px-3 py-2 text-lg text-white backdrop-blur-sm"
              onClick={(e) => {
                e.stopPropagation()
                onIndexChange((index + 1) % images.length)
              }}
            >
              ›
            </button>
          </>
        )}
      </div>

      {hasMultiple && (
        <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
          {images.map((src, i) => (
            <button
              key={i}
              type="button"
              aria-label={alt(i)}
              aria-current={i === index}
              onClick={() => onIndexChange(i)}
              className={`h-14 w-14 shrink-0 overflow-hidden rounded-[10px] border-2 ${
                i === index ? 'border-primary-500' : 'border-transparent'
              }`}
            >
              <img src={src} alt="" className="h-full w-full object-cover" />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
