import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { motion, useReducedMotion } from 'motion/react'
import { Button } from '../ui'
import { fetchPromptDetail, type PromptListItem } from '../lib/galleryApi'
import { t } from '../i18n'

interface PromptSheetProps {
  item: PromptListItem
  /** 详情/操作进行中：禁用按钮 */
  pending: boolean
  onUse: (item: PromptListItem) => void
  onCopy: (item: PromptListItem) => void
  copied: boolean
  onClose: () => void
  // 触屏赞/藏兜底（不传则不渲染该行）
  liked?: boolean
  favorited?: boolean
  likeCount?: number
  favCount?: number
  onToggleLike?: () => void
  onToggleFavorite?: () => void
}

/** 移动端灵感卡底部抽屉：大图预览 + 标题/作者 + 提示词全文（可滚动）+ 直接使用/复制。
 *  触屏无 hover 浮层的兜底操作入口。桌面走 hover 浮层、不挂本组件。 */
export function PromptSheet({
  item,
  pending,
  onUse,
  onCopy,
  copied,
  onClose,
  liked,
  favorited,
  likeCount,
  favCount,
  onToggleLike,
  onToggleFavorite,
}: PromptSheetProps) {
  const reduceMotion = useReducedMotion()
  const [promptText, setPromptText] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setPromptText(null)
    fetchPromptDetail(item.id)
      .then((detail) => {
        if (!cancelled) setPromptText(detail.promptText)
      })
      .catch(() => {
        if (!cancelled) setPromptText('')
      })
    return () => {
      cancelled = true
    }
  }, [item.id])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [onClose])

  const caption = item.authorName ? `${item.title} · @${item.authorName}` : item.title

  // 经 body 门户渲染：抽屉不受 App 内容层 `relative z-[1]` 堆叠上下文束缚，
  // 否则 z-50 仍被根级 z-40 悬浮票据盖住（与 Lightbox 同处理）。
  return createPortal(
    <div
      className="fixed inset-0 z-50 flex flex-col justify-end sm:hidden"
      role="dialog"
      aria-modal="true"
      aria-label={item.title}
    >
      {/* 背板：点击关闭 */}
      <button
        type="button"
        aria-label={t('studio.gallery.sheetClose')}
        onClick={onClose}
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
      />
      <motion.div
        initial={reduceMotion ? false : { y: '100%' }}
        animate={{ y: 0 }}
        transition={reduceMotion ? { duration: 0 } : { type: 'spring', stiffness: 420, damping: 40 }}
        className="relative max-h-[85vh] overflow-y-auto rounded-t-[22px] border-t border-snb-hairline-strong bg-snb-panel px-4 pb-[max(20px,env(safe-area-inset-bottom))] pt-3"
      >
        <div aria-hidden="true" className="mx-auto mb-3 h-1 w-10 rounded-full bg-snb-hairline-strong" />
        <img
          src={item.imageUrl}
          alt={item.title}
          className="mb-3 w-full rounded-xl object-cover"
          style={
            item.imageW > 0 && item.imageH > 0
              ? { aspectRatio: `${item.imageW} / ${item.imageH}` }
              : undefined
          }
        />
        <p className="mb-2 text-center text-[13px] text-snb-t3">{caption}</p>
        <div className="mb-4 max-h-[28vh] overflow-y-auto rounded-xl border border-snb-hairline bg-snb-elv/50 p-3 text-[13.5px] leading-[1.7] text-snb-t1">
          {promptText === null ? (
            <span className="text-snb-t3">{t('studio.gallery.sheetLoading')}</span>
          ) : (
            promptText || <span className="text-snb-t3">—</span>
          )}
        </div>
        {(onToggleLike || onToggleFavorite) && (
          <div className="mb-3 flex gap-2.5">
            <button
              type="button"
              aria-label={t('studio.gallery.like')}
              aria-pressed={liked}
              onClick={onToggleLike}
              className={`flex flex-1 items-center justify-center gap-1.5 rounded-xl border px-4 py-2.5 text-sm font-medium transition-colors ${
                liked
                  ? 'border-rose-400/40 bg-rose-500/10 text-rose-400'
                  : 'border-snb-hairline-strong bg-snb-elv text-snb-t1'
              }`}
            >
              <span aria-hidden>{liked ? '♥' : '♡'}</span>
              <span className="tabular-nums">{likeCount ?? 0}</span>
            </button>
            <button
              type="button"
              aria-label={t('studio.gallery.save')}
              aria-pressed={favorited}
              onClick={onToggleFavorite}
              className={`flex flex-1 items-center justify-center gap-1.5 rounded-xl border px-4 py-2.5 text-sm font-medium transition-colors ${
                favorited
                  ? 'border-amber-400/40 bg-amber-500/10 text-amber-400'
                  : 'border-snb-hairline-strong bg-snb-elv text-snb-t1'
              }`}
            >
              <span aria-hidden>{favorited ? '★' : '☆'}</span>
              <span className="tabular-nums">{favCount ?? 0}</span>
            </button>
          </div>
        )}
        <div className="flex gap-2.5">
          <Button
            variant="primary"
            size="md"
            className="flex-1"
            disabled={pending}
            onClick={() => onUse(item)}
          >
            {t('studio.gallery.use')}
          </Button>
          <Button
            variant="secondary"
            size="md"
            className="flex-1"
            disabled={pending}
            onClick={() => onCopy(item)}
          >
            {copied ? t('studio.gallery.copied') : t('studio.gallery.copy')}
          </Button>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="mt-2 w-full rounded-xl px-4 py-2.5 text-[13px] text-snb-t3 transition-colors hover:text-snb-t1"
        >
          {t('studio.gallery.sheetClose')}
        </button>
      </motion.div>
    </div>,
    document.body
  )
}
