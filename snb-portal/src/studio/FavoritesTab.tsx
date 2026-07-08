// 我的收藏：登录用户的灵感库收藏清单（收藏的是灵感条目/提示词，服务端 gallery 库）。
// 未登录 → 空态引导登录；登录 → 拉 /me/favorites 瀑布流（分页续读）。桌面 hover 浮层 + 触屏抽屉兜底。
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Alert, Button, MasonryCard, MasonryGrid, Skeleton } from '../ui'
import { fetchMyFavorites, fetchPromptDetail, type PromptListItem } from '../lib/galleryApi'
import { useInteractions } from './useInteractions'
import { useAuthUser } from '../auth/useAuth'
import { loginUrl } from '../auth/apiFetch'
import type { ApplyPayload } from '../App'
import { BalancedMasonry } from './BalancedMasonry'
import { PromptSheet } from './PromptSheet'
import { CardStat } from './CardStat'
import { t } from '../i18n'

interface Props {
  onApply: (item: ApplyPayload) => void
  // 本 tab 当前是否可见。面板首访后常驻挂载（App 用 hidden 切换），故须在「切回可见」时
  // 重新拉取——否则在灵感库新增的收藏不会出现在这张陈旧的列表里。缺省 true 兼容既有调用。
  active?: boolean
}

export function FavoritesTab({ onApply, active = true }: Props) {
  const user = useAuthUser()
  const [items, setItems] = useState<PromptListItem[]>([])
  const [page, setPage] = useState(1)
  const [pages, setPages] = useState(1)
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const [wallGen, setWallGen] = useState(0)
  const [pendingId, setPendingId] = useState<string | null>(null)
  const [copiedId, setCopiedId] = useState<string | null>(null)
  const [sheetItem, setSheetItem] = useState<PromptListItem | null>(null)
  const seqRef = useRef(0)

  // 触屏（无 hover）：卡片浮层点不到，改 tap 弹抽屉
  const isTouch =
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(hover: none)').matches

  const ids = items.map((it) => it.id)
  // 本页每条都是「我已收藏」→ 用 seed 让星标首帧即 ★（避免回填前显示未收藏、点击方向反）
  const seed = useMemo(() => ({ favorited: items.map((it) => it.id) }), [items])
  const { liked, favorited, likeCounts, favCounts, toggle } = useInteractions(
    ids,
    !!user,
    () => {
      window.location.href = loginUrl()
    },
    seed
  )

  const load = useCallback(async () => {
    if (!user) return
    const seq = ++seqRef.current
    setLoading(true)
    setLoadError(false)
    try {
      const res = await fetchMyFavorites(1)
      if (seq !== seqRef.current) return
      setItems(res.items)
      setPage(res.page)
      setPages(res.pages)
      setTotal(res.total)
      setWallGen(seq)
      setLoading(false)
    } catch {
      if (seq !== seqRef.current) return
      setLoadError(true)
      setLoading(false)
    }
  }, [user])

  // 每次「切回可见」都重拉：面板常驻不卸载，只在 active 由假转真时刷新，
  // 让在灵感库刚收藏的条目出现在最新列表顶部（/me/favorites 按 createdAt DESC）。
  useEffect(() => {
    if (active) void load()
  }, [active, load])

  const loadMore = useCallback(async () => {
    if (loadingMore || page >= pages) return
    const seq = seqRef.current
    setLoadingMore(true)
    try {
      const res = await fetchMyFavorites(page + 1)
      if (seq !== seqRef.current) return
      setItems((prev) => [...prev, ...res.items])
      setPage(res.page)
      setPages(res.pages)
      setTotal(res.total)
    } catch {
      // 静默，点按钮可重试
    } finally {
      setLoadingMore(false)
    }
  }, [loadingMore, page, pages])

  async function applyItem(item: PromptListItem): Promise<void> {
    if (pendingId !== null) return
    setPendingId(item.id)
    try {
      const detail = await fetchPromptDetail(item.id)
      onApply({ prompt: detail.promptText })
    } catch {
      // 静默
    } finally {
      setPendingId(null)
    }
  }

  async function copyItem(item: PromptListItem): Promise<void> {
    if (pendingId !== null) return
    setPendingId(item.id)
    try {
      const detail = await fetchPromptDetail(item.id)
      await navigator.clipboard.writeText(detail.promptText)
      setCopiedId(item.id)
      setTimeout(() => setCopiedId((id) => (id === item.id ? null : id)), 1500)
    } catch {
      // 静默
    } finally {
      setPendingId(null)
    }
  }

  if (!user) {
    return (
      <div className="mx-auto max-w-[420px] py-16 text-center">
        <p className="text-base font-medium text-snb-t1">{t('studio.favorites.loginTitle')}</p>
        <p className="mt-2 text-sm text-snb-t3">{t('studio.favorites.loginBody')}</p>
        <Button variant="primary" className="mt-5" onClick={() => (window.location.href = loginUrl())}>
          {t('studio.favorites.loginCta')}
        </Button>
      </div>
    )
  }

  if (loadError) {
    return (
      <Alert tone="warning" title={t('studio.favorites.loadFailed')}>
        <div className="mt-3">
          <Button variant="secondary" size="sm" onClick={() => void load()}>
            {t('studio.gallery.reload')}
          </Button>
        </div>
      </Alert>
    )
  }

  // 只在首次加载（还没有条目）显示骨架；切回重拉时保留旧列表后台刷新，避免每次切 tab 都闪骨架
  if (loading && items.length === 0) {
    return (
      <MasonryGrid className="2xl:columns-5">
        {[52, 40, 64, 44, 56, 48].map((h, i) => (
          <Skeleton key={i} className="mb-4 break-inside-avoid rounded-xl" style={{ height: `${h * 4}px` }} />
        ))}
      </MasonryGrid>
    )
  }

  if (items.length === 0) {
    return <p className="py-16 text-center text-sm text-snb-t3">{t('studio.favorites.empty')}</p>
  }

  return (
    <div className="space-y-6">
      <BalancedMasonry
        items={items}
        resetKey={wallGen}
        renderItem={(item) => (
          <MasonryCard
            src={item.imageUrl}
            alt={item.title}
            width={item.imageW > 0 ? item.imageW : 480}
            height={item.imageH > 0 ? item.imageH : 640}
            onActivate={isTouch ? () => setSheetItem(item) : undefined}
            stats={
              <div className="flex flex-col gap-1">
                <p className="truncate text-[12.5px] font-medium text-white [text-shadow:0_1px_3px_rgba(0,0,0,0.7)]">
                  {item.title}
                </p>
                <div className="flex items-center justify-between gap-2">
                  <div className="flex gap-1.5">
                    <CardStat
                      kind="like"
                      on={liked.has(item.id)}
                      count={likeCounts.get(item.id) ?? item.likeCount}
                      label={t('studio.gallery.like')}
                      onToggle={() => toggle('like', item.id)}
                    />
                    <CardStat
                      kind="save"
                      on={favorited.has(item.id)}
                      count={favCounts.get(item.id) ?? item.favCount}
                      label={t('studio.gallery.save')}
                      onToggle={() => toggle('favorite', item.id)}
                    />
                  </div>
                  {item.authorName && (
                    <span className="truncate text-[11px] text-white/70 [text-shadow:0_1px_2px_rgba(0,0,0,0.65)]">
                      @{item.authorName}
                    </span>
                  )}
                </div>
              </div>
            }
            overlay={
              <Button
                variant="primary"
                size="sm"
                className="w-full"
                disabled={pendingId === item.id}
                onClick={() => void applyItem(item)}
              >
                {t('studio.gallery.use')}
              </Button>
            }
          />
        )}
      />

      {/* 页尾续读：还有下页就常驻（收藏 >一页时的唯一可靠入口） */}
      {page < pages && (
        <div className="flex flex-col items-center gap-3 pb-2 pt-4">
          {total > items.length && (
            <p className="text-xs tabular-nums text-snb-t3">
              {t('studio.gallery.progress', { seen: items.length, left: total - items.length })}
            </p>
          )}
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

      {/* 触屏抽屉：tap 卡片弹出，含赞/藏/直接使用/复制 */}
      {sheetItem && (
        <PromptSheet
          item={sheetItem}
          pending={pendingId === sheetItem.id}
          copied={copiedId === sheetItem.id}
          onUse={(it) => {
            void applyItem(it)
            setSheetItem(null)
          }}
          onCopy={(it) => void copyItem(it)}
          onClose={() => setSheetItem(null)}
          liked={liked.has(sheetItem.id)}
          favorited={favorited.has(sheetItem.id)}
          likeCount={likeCounts.get(sheetItem.id) ?? sheetItem.likeCount}
          favCount={favCounts.get(sheetItem.id) ?? sheetItem.favCount}
          onToggleLike={() => toggle('like', sheetItem.id)}
          onToggleFavorite={() => toggle('favorite', sheetItem.id)}
        />
      )}
    </div>
  )
}
