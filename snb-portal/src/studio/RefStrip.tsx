import { useEffect, useRef, useState } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { t } from '../i18n'
import type { RefImage } from './useRefImages'

interface RefThumbProps {
  item: RefImage
  reduceMotion: boolean
  onRemove: () => void
}

/** 单枚样片：loading = 暖调骨架扫光；ready = 真图淡入 + 一次性赤陶落位微光 + 角标删除。
 *  删除键收在样片内右上角（不再负偏移探出），配合父级隐藏滚动条，杜绝那条误当进度条的灰杠。 */
function RefThumb({ item, reduceMotion, onRemove }: RefThumbProps) {
  return (
    <motion.div
      layout
      initial={reduceMotion ? false : { opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={reduceMotion ? { opacity: 0 } : { opacity: 0, scale: 0.8 }}
      transition={reduceMotion ? { duration: 0 } : { type: 'spring', stiffness: 500, damping: 32 }}
      className="group relative h-16 w-16 flex-none"
    >
      {item.status === 'loading' ? (
        <div className="relative h-16 w-16 overflow-hidden rounded-xl border border-snb-hairline bg-snb-elv">
          <span
            aria-hidden="true"
            className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-[snbShimmer_1.4s_ease-in-out_infinite] dark:via-white/10 motion-reduce:hidden"
          />
        </div>
      ) : (
        <>
          <img
            src={item.url}
            alt={t('studio.editor.refAdd')}
            className="h-16 w-16 rounded-xl border border-snb-hairline-strong object-cover shadow-[0_1px_4px_rgba(70,50,38,0.14)] animate-[snbRefLand_0.7s_ease-out] transition-transform duration-200 group-hover:-translate-y-0.5 dark:shadow-[0_2px_6px_rgba(0,0,0,0.5)] motion-reduce:animate-none motion-reduce:transform-none"
          />
          <button
            type="button"
            onClick={onRemove}
            aria-label={t('studio.editor.refRemove')}
            className="absolute right-1 top-1 flex h-[18px] w-[18px] items-center justify-center rounded-full bg-black/50 p-0 text-[12px] leading-none text-white opacity-0 backdrop-blur-sm transition-opacity duration-150 hover:bg-black/65 focus:outline-none focus-visible:opacity-100 focus-visible:ring-2 focus-visible:ring-primary-500/60 group-hover:opacity-100 [@media(hover:none)]:opacity-100"
          >
            ×
          </button>
        </>
      )}
    </motion.div>
  )
}

interface RefStripProps {
  refs: RefImage[]
  onRemove: (id: string) => void
  max: number
}

/** 参考图样片行：从票据输入行下沉的独立一行，横向铺开、超出横滑，右缘渐隐提示可滑，
 *  行尾计数 N/max（接近上限转赤陶）。空态由父组件负责不挂载本行，保持票据轻盈。 */
export function RefStrip({ refs, onRemove, max }: RefStripProps) {
  const reduceMotion = useReducedMotion()
  const scrollRef = useRef<HTMLDivElement>(null)
  const [canScrollRight, setCanScrollRight] = useState(false)

  // 溢出时右缘显渐隐遮罩，暗示还能往右滑；滚动/增删/尺寸变都重算
  useEffect(() => {
    const el = scrollRef.current
    if (!el) return
    const update = () => setCanScrollRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 1)
    update()
    el.addEventListener('scroll', update, { passive: true })
    const ro = new ResizeObserver(update)
    ro.observe(el)
    return () => {
      el.removeEventListener('scroll', update)
      ro.disconnect()
    }
  }, [refs.length])

  return (
    <div className="flex items-center gap-2.5">
      <div className="relative min-w-0 flex-1">
        <div
          ref={scrollRef}
          aria-label={t('studio.editor.refAdd')}
          className="snb-scrollbar-none flex items-center gap-2 overflow-x-auto py-1"
        >
          <AnimatePresence initial={false}>
            {refs.map((r) => (
              <RefThumb key={r.id} item={r} reduceMotion={!!reduceMotion} onRemove={() => onRemove(r.id)} />
            ))}
          </AnimatePresence>
        </div>
        <div
          aria-hidden="true"
          className={`pointer-events-none absolute inset-y-0 right-0 w-8 bg-gradient-to-l from-snb-panel to-transparent transition-opacity duration-200 ${
            canScrollRight ? 'opacity-100' : 'opacity-0'
          }`}
        />
      </div>
      <span
        className={`flex-none text-xs tabular-nums transition-colors ${
          refs.length >= max ? 'font-medium text-primary-500' : 'text-snb-t3'
        }`}
      >
        {refs.length} / {max}
      </span>
    </div>
  )
}
