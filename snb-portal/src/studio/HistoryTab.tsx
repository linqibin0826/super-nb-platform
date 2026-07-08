// 按 Claude Design 行卡 + 「创作详情」Modal；数据改走服务端 generationsApi（含 presigned URL）。
import { useCallback, useEffect, useRef, useState } from 'react'
import { Alert, Button, Modal, QuoteLine } from '@super-nb/ui'
import {
  listGenerations,
  getGeneration,
  deleteGeneration,
  type GenerationListItem,
  type GenerationDetail,
} from '../lib/generationsApi'
import { useAuthUser } from '../auth/useAuth'
import { loginUrl } from '../auth/apiFetch'
import { locale, t } from '../i18n'

interface Props {
  refreshToken: number
  onApply: (payload: { prompt: string; size: string; quality: string; n: number }) => void
  onPreview: (images: string[], index: number) => void
  onGoGallery: () => void
}

type Rec = GenerationListItem | GenerationDetail

/** 「今天 14:32」/「昨天 21:45」/ 更早 zh「6月30日」、en 本地化日期 */
function formatTime(iso: string): string {
  const d = new Date(iso)
  const now = new Date()
  const hm = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  const sameDay = (a: Date, b: Date) =>
    a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
  if (sameDay(d, now)) return `${t('studio.history.today')} ${hm}`
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (sameDay(d, yesterday)) return `${t('studio.history.yesterday')} ${hm}`
  return locale === 'zh' ? `${d.getMonth() + 1}月${d.getDate()}日` : d.toLocaleDateString()
}

/** 规格三段：张数 · 尺寸 · 质量 */
function specText(record: Rec): string {
  return [
    t('studio.editor.countUnit', { n: record.n }),
    record.size,
    t(`playground.form.qualitiesShort.${record.quality}`),
  ].join(' · ')
}

function costText(cost: number | null): string {
  return cost != null ? `$${cost.toFixed(2)}` : '—'
}

export function HistoryTab({ refreshToken, onApply, onPreview, onGoGallery }: Props) {
  const user = useAuthUser()
  const [items, setItems] = useState<GenerationListItem[]>([])
  const [page, setPage] = useState(1)
  const [pages, setPages] = useState(1)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const [detail, setDetail] = useState<GenerationDetail | null>(null)
  const [copied, setCopied] = useState(false)
  const copiedTimer = useRef<number | null>(null)
  const seqRef = useRef(0)

  const load = useCallback(async () => {
    if (!user) return
    const seq = ++seqRef.current
    setLoading(true)
    setLoadError(false)
    try {
      const res = await listGenerations(1)
      if (seq !== seqRef.current) return
      setItems(res.items)
      setPage(res.page)
      setPages(res.pages)
      setLoading(false)
    } catch {
      if (seq !== seqRef.current) return
      setLoadError(true)
      setLoading(false)
    }
  }, [user])

  // 生成后 refreshToken 变 → 重拉第一页（把刚生成的顶到最前）
  useEffect(() => {
    void load()
  }, [load, refreshToken])

  // 卸载时清「已复制」回退定时器
  useEffect(
    () => () => {
      if (copiedTimer.current !== null) window.clearTimeout(copiedTimer.current)
    },
    []
  )

  const loadMore = useCallback(async () => {
    if (loadingMore || page >= pages) return
    const seq = seqRef.current
    setLoadingMore(true)
    try {
      const res = await listGenerations(page + 1)
      if (seq !== seqRef.current) return
      setItems((prev) => [...prev, ...res.items])
      setPage(res.page)
      setPages(res.pages)
    } catch {
      // 静默，可重试
    } finally {
      setLoadingMore(false)
    }
  }, [loadingMore, page, pages])

  async function openDetail(id: string): Promise<void> {
    setCopied(false)
    try {
      setDetail(await getGeneration(id))
    } catch {
      // 静默
    }
  }

  function copyPrompt(record: GenerationDetail): void {
    void navigator.clipboard.writeText(record.prompt)
    setCopied(true)
    if (copiedTimer.current !== null) window.clearTimeout(copiedTimer.current)
    copiedTimer.current = window.setTimeout(() => setCopied(false), 1500)
  }

  async function onDelete(id: string): Promise<void> {
    await deleteGeneration(id)
    setDetail(null)
    await load()
  }

  if (!user) {
    return (
      <div className="mx-auto max-w-[420px] py-16 text-center">
        <p className="text-base font-medium text-snb-t1">{t('studio.history.loginTitle')}</p>
        <p className="mt-2 text-sm text-snb-t3">{t('studio.history.loginBody')}</p>
        <Button variant="primary" className="mt-5" onClick={() => (window.location.href = loginUrl())}>
          {t('studio.history.loginCta')}
        </Button>
      </div>
    )
  }

  if (loadError) {
    return (
      <Alert tone="warning" title={t('studio.history.loadFailed')}>
        <div className="mt-3">
          <Button variant="secondary" size="sm" onClick={() => void load()}>
            {t('studio.gallery.reload')}
          </Button>
        </div>
      </Alert>
    )
  }

  // 只在首次加载（还没有条目）显示骨架；切回重拉保留旧列表后台刷新
  if (loading && items.length === 0) {
    return (
      <div className="mt-6 grid gap-2.5">
        {[0, 1, 2, 3].map((i) => (
          <div
            key={i}
            className="h-[84px] animate-pulse rounded-[16px] border border-snb-hairline bg-snb-panel"
          />
        ))}
      </div>
    )
  }

  if (items.length === 0) {
    return (
      <div className="mt-7 rounded-[20px] border border-dashed border-snb-hairline-strong bg-snb-panel px-8 py-14 text-center">
        <p className="font-display text-lg font-semibold text-snb-t1">
          {t('studio.history.emptyTitle')}
        </p>
        <p className="mx-auto mt-2 max-w-[360px] text-[13.5px] leading-relaxed text-snb-t3">
          {t('studio.history.emptyBody')}
        </p>
        <div className="mt-5 flex justify-center">
          <Button variant="secondary" onClick={onGoGallery}>
            {t('studio.history.goGallery')}
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="mt-6 space-y-4">
      <div className="grid gap-2.5">
        {items.map((record) => (
          <div
            key={record.id}
            role="button"
            tabIndex={0}
            className="flex cursor-pointer items-center gap-4 rounded-[16px] border border-snb-hairline bg-snb-panel p-3.5 pr-[18px] transition hover:border-snb-hairline-strong hover:shadow-card"
            onClick={() => void openDetail(record.id)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault()
                void openDetail(record.id)
              }
            }}
          >
            {record.thumbUrl ? (
              <img
                src={record.thumbUrl}
                alt=""
                className="h-14 w-14 shrink-0 rounded-[10px] border border-snb-hairline object-cover"
              />
            ) : (
              <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-[10px] border border-snb-hairline bg-snb-elv text-[11px] text-snb-ember">
                {t('playground.history.failed')}
              </div>
            )}
            <div className="min-w-0 flex-1">
              <p className="line-clamp-2 text-[13.5px] leading-snug text-snb-t1">{record.prompt}</p>
              <p className="mt-1 text-xs text-snb-t3">
                {specText(record)} · {Math.round(record.elapsedMs / 1000)}s
              </p>
            </div>
            <div className="shrink-0 text-right font-mono text-xs text-snb-t3">
              <div>{formatTime(record.createdAt)}</div>
              <div className="mt-1 font-semibold text-snb-t2">{costText(record.cost)}</div>
            </div>
            <span aria-hidden className="text-sm text-snb-t3">
              ›
            </span>
          </div>
        ))}
      </div>

      {page < pages && (
        <div className="flex justify-center pb-2 pt-2">
          <Button
            variant="primary"
            size="lg"
            className="w-full max-w-[420px]"
            disabled={loadingMore}
            onClick={() => void loadMore()}
          >
            {loadingMore ? t('studio.gallery.loadingMore') : t('studio.gallery.keepBrowsing')}
          </Button>
        </div>
      )}

      {detail !== null && (
        <Modal
          open
          onClose={() => setDetail(null)}
          title={t('studio.history.detailTitle')}
          footer={
            <>
              <Button variant="ghost" size="sm" onClick={() => setDetail(null)}>
                {t('studio.history.close')}
              </Button>
              <Button variant="secondary" size="sm" onClick={() => copyPrompt(detail)}>
                {copied ? t('studio.results.copied') : t('studio.results.copy')}
              </Button>
              <Button
                size="sm"
                onClick={() => {
                  onApply({ prompt: detail.prompt, size: detail.size, quality: detail.quality, n: detail.n })
                  setDetail(null)
                }}
              >
                {t('studio.history.regenerate')}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="text-snb-ember hover:text-snb-ember"
                onClick={() => void onDelete(detail.id)}
              >
                {t('playground.history.delete')}
              </Button>
            </>
          }
        >
          <div className="grid gap-4">
            {detail.outputImages.length > 0 && (
              <div className="grid grid-cols-4 gap-2">
                {detail.outputImages.map((image, index) => {
                  const urls = detail.outputImages.map((o) => o.url)
                  return (
                    <div
                      key={index}
                      role="button"
                      tabIndex={0}
                      aria-label={t('studio.history.viewImage')}
                      className="aspect-[3/4] cursor-zoom-in rounded-[10px] border border-snb-hairline bg-cover bg-center"
                      style={{ backgroundImage: `url("${image.url}")` }}
                      onClick={() => onPreview(urls, index)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault()
                          onPreview(urls, index)
                        }
                      }}
                    />
                  )
                })}
              </div>
            )}
            {detail.refImages.length > 0 && (
              <div>
                <p className="text-[11px] tracking-[0.08em] text-snb-t3">
                  {t('studio.history.refImagesTitle')}
                </p>
                <div className="mt-1.5 flex flex-wrap gap-2">
                  {detail.refImages.map((ref, index) => (
                    <img
                      key={index}
                      src={ref.url}
                      alt=""
                      className="h-16 w-16 rounded-[10px] border border-snb-hairline object-cover"
                    />
                  ))}
                </div>
              </div>
            )}
            <div className="rounded-[12px] border border-snb-hairline bg-snb-panel p-3">
              <p className="text-[11px] tracking-[0.08em] text-snb-t3">
                {t('studio.history.detailPrompt')}
              </p>
              <p className="mt-1.5 text-[13.5px] leading-[1.7] text-snb-t1">{detail.prompt}</p>
            </div>
            <div className="grid gap-2.5">
              <div className="flex items-baseline gap-3 text-[13px]">
                <span className="shrink-0 text-snb-t3">{t('studio.history.detailSpec')}</span>
                <span className="flex-1 -translate-y-[3px] border-b border-dotted border-snb-hairline-strong" />
                <span className="shrink-0 text-snb-t2">{specText(detail)}</span>
              </div>
              <div className="flex items-baseline gap-3 text-[13px]">
                <span className="shrink-0 text-snb-t3">{t('studio.history.detailTime')}</span>
                <span className="flex-1 -translate-y-[3px] border-b border-dotted border-snb-hairline-strong" />
                <span className="shrink-0 text-snb-t2">{formatTime(detail.createdAt)}</span>
              </div>
              <QuoteLine
                label={t('studio.history.detailCost')}
                value={costText(detail.cost)}
                note={t('studio.editor.estimateNote')}
              />
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}
