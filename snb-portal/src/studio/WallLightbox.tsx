// 桌面「点开看大图」（设计定稿 Studio.dc.html「点开看大图」屏）：
// 大图 + 提示词全文 + 「直接使用 / 复制提示词」三件在同一个界面里完成——
// 原来桌面点卡片完全没反应、图只以 330px 软图形态出现，这是那条问题的落地。
// · 分寸不变：提示词随便看随便抄、不拦登录；点赞收藏与照着出图才提示开卡。
// · 计数写「第 N 张 · 本次筛选」——不写「N / 5778」，那会被读成在全库翻页。
// · 触屏窄屏仍走 PromptSheet 底部抽屉（手势更顺），本件只服务桌面/宽屏。
import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { ctaClass, secondaryAnchorClass, secondaryClass } from './parts'
import { fetchPromptDetail, type PromptDetail, type PromptListItem } from '../lib/galleryApi'
import { registerUrl } from './links'
import { st } from './i18nStudio'
import { t } from '../i18n'

interface WallLightboxProps {
  item: PromptListItem
  /** 当前筛选结果里的第几张（1 起） */
  index: number
  /** 已登录：出赞/藏两钮；访客：出「开卡上机」说明 */
  isMember: boolean
  liked: boolean
  favorited: boolean
  likeCount: number
  favCount: number
  onToggleLike: () => void
  onToggleFavorite: () => void
  /** 详情/剪贴板进行中：两个动作钮禁用（沿用列表页的 pendingId 口径） */
  pending: boolean
  copied: boolean
  onUse: () => void
  onCopy: () => void
  onPrev: () => void
  onNext: () => void
  onClose: () => void
}

/** 面板里的次级动作钮（赞/藏）：hairline 描边 + 纸白字，选中不用橙填充 */
function PanelToggle(props: { label: string; on: boolean; glyph: string; count: number; onClick: () => void }) {
  return (
    <button
      type="button"
      aria-label={props.label}
      aria-pressed={props.on}
      onClick={props.onClick}
      className={`inline-flex h-11 items-center justify-center gap-2 rounded-[8px] border px-4 text-[13.5px] transition-colors duration-[120ms] ease-[cubic-bezier(0.2,0,0,1)] focus:outline-none focus-visible:ring-2 focus-visible:ring-paper/60 ${
        props.on
          ? 'border-transparent bg-snb-cta font-semibold text-snb-cta-fg'
          : 'border-snb-hairline-strong text-snb-t1 hover:border-[rgba(239,235,228,0.3)] hover:bg-snb-panel'
      }`}
    >
      <span aria-hidden="true">{props.glyph}</span>
      {props.label}
      <span className="font-mono text-xs tabular-nums opacity-70">{props.count}</span>
    </button>
  )
}

export function WallLightbox(p: WallLightboxProps) {
  const { item } = p
  const [detail, setDetail] = useState<PromptDetail | null>(null)
  const [detailFailed, setDetailFailed] = useState(false)

  // 提示词全文按需取（与抽屉同口径）；失败给一横杠而不是空白，不影响看图
  useEffect(() => {
    let cancelled = false
    setDetail(null)
    setDetailFailed(false)
    fetchPromptDetail(item.id)
      .then((d) => {
        if (!cancelled) setDetail(d)
      })
      .catch(() => {
        if (!cancelled) setDetailFailed(true)
      })
    return () => {
      cancelled = true
    }
  }, [item])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') p.onClose()
      else if (e.key === 'ArrowLeft') p.onPrev()
      else if (e.key === 'ArrowRight') p.onNext()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  })

  const ratio = item.imageW > 0 && item.imageH > 0 ? `${item.imageW} / ${item.imageH}` : undefined

  // 经 body 门户渲染：不受 App 内容层 relative z-[1] 的堆叠上下文束缚（与 Lightbox/PromptSheet 同处理）
  return createPortal(
    <div
      className="fixed inset-0 z-[90] flex flex-col bg-snb-bg"
      role="dialog"
      aria-modal="true"
      aria-label={item.title}
    >
      <div className="flex h-[60px] flex-none items-center justify-between gap-4 border-b border-snb-hairline px-3 sm:px-5">
        <div className="flex min-w-0 items-center gap-3">
          <button
            type="button"
            aria-label={st('studio.big.prev')}
            onClick={p.onPrev}
            className="grid h-11 w-11 place-items-center rounded-[8px] border border-snb-hairline-strong text-snb-t1 transition-colors duration-[120ms] ease-[cubic-bezier(0.2,0,0,1)] hover:border-[rgba(239,235,228,0.3)] hover:bg-snb-panel focus:outline-none focus-visible:ring-2 focus-visible:ring-paper/60"
          >
            ←
          </button>
          <button
            type="button"
            aria-label={st('studio.big.next')}
            onClick={p.onNext}
            className="grid h-11 w-11 place-items-center rounded-[8px] border border-snb-hairline-strong text-snb-t1 transition-colors duration-[120ms] ease-[cubic-bezier(0.2,0,0,1)] hover:border-[rgba(239,235,228,0.3)] hover:bg-snb-panel focus:outline-none focus-visible:ring-2 focus-visible:ring-paper/60"
          >
            →
          </button>
          <span className="truncate font-mono text-xs tabular-nums text-snb-t3">
            {st('studio.big.counter', { n: p.index })}
          </span>
        </div>
        <button
          type="button"
          onClick={p.onClose}
          className="inline-flex h-11 items-center gap-2 rounded-[8px] px-3.5 text-[13.5px] text-snb-t2 transition-colors duration-[120ms] ease-[cubic-bezier(0.2,0,0,1)] hover:bg-snb-t1/[0.06] hover:text-snb-t1 focus:outline-none focus-visible:ring-2 focus-visible:ring-paper/60"
        >
          <span className="font-mono text-xs">Esc</span>
          {st('studio.big.close')}
        </button>
      </div>

      <div className="grid min-h-0 flex-1 grid-cols-1 lg:grid-cols-[minmax(0,1fr)_clamp(320px,26vw,420px)]">
        <div className="grid min-w-0 place-items-center overflow-auto bg-[#0B0D11] p-3 sm:p-6">
          {/* aspect-ratio 预占位：图还没到时格子就已经是最终形状，不再靠图片落地推开整页（CLS） */}
          <div
            className="max-h-full w-full max-w-full overflow-hidden rounded-md"
            style={ratio ? { aspectRatio: ratio, maxWidth: 'min(100%, calc(80vh * ' + item.imageW / item.imageH + '))' } : undefined}
          >
            <img
              src={item.imageUrl}
              alt={item.title}
              decoding="async"
              className="block h-full w-full object-contain"
            />
          </div>
        </div>

        <div className="flex min-w-0 flex-col gap-[18px] overflow-y-auto border-snb-hairline bg-snb-panel p-4 sm:p-5 lg:border-l">
          <div>
            <h2 className="m-0 text-[19px] font-bold leading-[1.4] text-snb-t1">{item.title}</h2>
            <div className="mt-2 flex flex-wrap items-center gap-x-3.5 gap-y-2.5">
              {item.authorName && (
                <span className="font-mono text-[13px] text-snb-t2">@{item.authorName}</span>
              )}
              {item.imageW > 0 && item.imageH > 0 && (
                <span className="font-mono text-[12.5px] text-snb-t3">
                  {item.imageW}×{item.imageH}
                </span>
              )}
              {detail?.category && (
                <span className="font-mono text-[12.5px] text-snb-t3">{detail.category.nameZh}</span>
              )}
              <span className="font-mono text-[12.5px] tabular-nums text-snb-t3">
                ♥ {p.likeCount} · ☆ {p.favCount}
              </span>
            </div>
          </div>

          <div>
            <div className="flex items-baseline justify-between gap-3">
              <span className="font-mono text-[11px] tracking-[0.14em] text-snb-t3">
                {st('studio.big.promptTitle')}
              </span>
              <span className="font-mono text-[11.5px] text-snb-t3">{st('studio.big.promptHint')}</span>
            </div>
            <div className="mt-2 select-text rounded-lg border border-snb-hairline bg-snb-well px-4 py-3.5 font-mono text-[13px] leading-[1.95] text-snb-t1 [text-wrap:pretty]">
              {detail === null && !detailFailed ? (
                <span className="text-snb-t3">{t('studio.gallery.sheetLoading')}</span>
              ) : (
                detail?.promptText || <span className="text-snb-t3">—</span>
              )}
            </div>
          </div>

          <div className="flex flex-col gap-2.5">
            <button type="button" className={`${ctaClass} w-full`} disabled={p.pending} onClick={p.onUse}>
              {st('studio.big.use')}
            </button>
            <button
              type="button"
              className={`${secondaryClass} w-full`}
              disabled={p.pending}
              onClick={p.onCopy}
            >
              {p.copied ? st('studio.big.copiedFull') : t('studio.gallery.copy')}
            </button>
          </div>

          <div className="border-t border-snb-hairline pt-4">
            {p.isMember ? (
              <div className="flex flex-wrap gap-3">
                <PanelToggle
                  label={t('studio.gallery.like')}
                  glyph={p.liked ? '♥' : '♡'}
                  on={p.liked}
                  count={p.likeCount}
                  onClick={p.onToggleLike}
                />
                <PanelToggle
                  label={t('studio.gallery.save')}
                  glyph={p.favorited ? '★' : '☆'}
                  on={p.favorited}
                  count={p.favCount}
                  onClick={p.onToggleFavorite}
                />
              </div>
            ) : (
              <div className="flex flex-col gap-2.5">
                <span className="text-[13px] leading-[1.65] text-snb-t2">{st('studio.big.guestNote')}</span>
                <a href={registerUrl()} className={secondaryAnchorClass}>
                  {st('studio.guestBoard.cta')}
                </a>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>,
    document.body
  )
}
