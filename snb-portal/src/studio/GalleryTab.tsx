// 灵感库：gallery-svc API 版（列表/搜索/类目筛选走服务端，详情点按需取）
// 视觉层走 @super-nb/ui（MasonryGrid/MasonryCard/Chip/Alert/Input/Button/Skeleton）+ 语义 token；
// 无限滚动 + 防抖搜索 + 代际竞态防护等数据逻辑保持不变；
// 署名保留在悬停层（源库 CC BY 4.0 必须署名）。
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  Alert,
  Button,
  Chip,
  Input,
  MasonryCard,
  MasonryGrid,
  Skeleton,
  type MasonryCardProps,
} from '@super-nb/ui'
import {
  categoryName,
  fetchCategories,
  fetchPromptDetail,
  fetchPrompts,
  type CategoryAxis,
  type CategoryTree,
  type GallerySort,
  type PromptListItem,
  type PromptDetail,
} from '../lib/galleryApi'
import type { ApplyPayload } from '../App'
import { BalancedMasonry } from './BalancedMasonry'
import { PromptSheet } from './PromptSheet'
import { CardStat } from './CardStat'
import { locale, t } from '../i18n'
import { useInteractions } from './useInteractions'
import { useAuthUser } from '../auth/useAuth'
import { loginUrl } from '../auth/apiFetch'

interface Props {
  onApply: (item: ApplyPayload) => void
}

const AXES: CategoryAxis[] = ['scene', 'style', 'subject']

// 无限滚动的「自动预算」：触底自动加载最多 12 页（首屏后 ≈288 张），用完停下等点按钮——
// 挂机滑到底/脚本滚动不再无限拉接口；点一下按钮即再续 12 页预算。
// （4→12，2026-07-07 站长反馈：4 页刚滑一会儿就停、按钮出现太早）
// ⚠️ 预算走 ref 且只在加载真正发起后才扣：观察器重建的立即回报可能连发，
// 若先扣后拦（曾配 700ms 节流吞掉请求）会把预算烧在半路，自动加载就永久停摆。
// 页尾续读按钮常驻（不看预算），停摆也只是少了自动、点按钮照样走。
const AUTO_LOAD_BUDGET = 12

// 排序段控件：四档均为真排序（likes/favorites 走服务端热度倒序）
const SORT_SEGMENTS: Array<{ value: GallerySort; labelKey: string }> = [
  { value: 'featured', labelKey: 'studio.gallery.sortFeatured' },
  { value: 'newest', labelKey: 'studio.gallery.sortNewest' },
  { value: 'likes', labelKey: 'studio.gallery.sortLikes' },
  { value: 'favorites', labelKey: 'studio.gallery.sortFavs' },
]

// 缺宽高条目的薄包装：MasonryCard 对给定宽高施加永久 aspect-ratio + object-cover，
// 直接兜底 3:4 会把这类图永久裁切（8b46698 修过的症状）。这里先以 3:4 占位防瀑布流塌陷，
// 内部 img 加载完成后换成天然比例。load 事件不冒泡但可捕获，故在外层 display:contents
// 的壳上挂捕获监听（不破坏 CSS columns 的分栏断行）；命中缓存时 load 可能抢在监听
// 挂上之前，先补查一次已完成的 img。
function AutoRatioCard(props: Omit<MasonryCardProps, 'width' | 'height'>) {
  const shellRef = useRef<HTMLDivElement | null>(null)
  const [dims, setDims] = useState<{ w: number; h: number } | null>(null)

  useEffect(() => {
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
  }, [])

  return (
    <div ref={shellRef} className="contents">
      <MasonryCard {...props} width={dims?.w ?? 480} height={dims?.h ?? 640} />
    </div>
  )
}

export function GalleryTab({ onApply }: Props) {
  const [categories, setCategories] = useState<CategoryTree | null>(null)
  const [items, setItems] = useState<PromptListItem[]>([])
  const [page, setPage] = useState(1)
  const [pages, setPages] = useState(1)
  const [total, setTotal] = useState(0) // 当前筛选下的总条数：页尾计数行用
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [query, setQuery] = useState('') // 防抖后的服务端 q
  const [activeCategory, setActiveCategory] = useState<string | null>(null)
  const [sortBy, setSortBy] = useState<GallerySort>('featured')
  const [copiedId, setCopiedId] = useState<string | null>(null)
  const [wallGen, setWallGen] = useState(0) // 首页重载换代号：BalancedMasonry 整墙重排 vs 追加
  const autoBudgetRef = useRef(AUTO_LOAD_BUDGET)
  const [pendingId, setPendingId] = useState<string | null>(null) // 详情拉取中的卡片（防连点）
  const [sheetItem, setSheetItem] = useState<PromptListItem | null>(null) // 触屏抽屉当前项

  // 触屏（无 hover）：卡片浮层按钮点不到，改 tap 弹抽屉。桌面 hover 可用则不挂 onActivate。
  const isTouch =
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(hover: none)').matches

  // 筛选变化的代际号：旧请求回来时对不上号就丢弃，避免竞态串页
  const seqRef = useRef(0)

  const user = useAuthUser()
  const ids = items.map((it) => it.id)
  const { liked, favorited, likeCounts, favCounts, toggle } = useInteractions(
    ids,
    !!user,
    () => {
      window.location.href = loginUrl()
    }
  )

  // 搜索防抖 300ms → 落到 query 才真正打服务端
  useEffect(() => {
    const timer = setTimeout(() => setQuery(keyword), 300)
    return () => clearTimeout(timer)
  }, [keyword])

  const loadFirstPage = useCallback(async () => {
    const seq = ++seqRef.current
    setLoading(true)
    setLoadError(false)
    try {
      const res = await fetchPrompts({ category: activeCategory, q: query, sort: sortBy, page: 1 })
      if (seq !== seqRef.current) return
      setItems(res.items)
      setWallGen(seq)
      autoBudgetRef.current = AUTO_LOAD_BUDGET
      setPage(res.page)
      setPages(res.pages)
      setTotal(res.total)
      setLoading(false)
    } catch {
      if (seq !== seqRef.current) return
      setLoadError(true)
      setLoading(false)
    }
  }, [activeCategory, query, sortBy])

  useEffect(() => {
    void loadFirstPage()
  }, [loadFirstPage])

  // 类目树只拉一次；失败仅隐藏 chips，不阻塞列表与生图
  useEffect(() => {
    let cancelled = false
    fetchCategories()
      .then((tree) => {
        if (!cancelled) setCategories(tree)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  // 返回值 = 本次是否真正发起了加载：观察器据此扣自动预算（被 loadingMore 等挡下的不扣）。
  // 并发由 loadingMore 挡（服务端另有令牌桶限流），不再做时间节流
  const loadMore = useCallback(async (): Promise<boolean> => {
    if (loading || loadingMore || loadError || page >= pages) return false
    const seq = seqRef.current
    setLoadingMore(true)
    try {
      const res = await fetchPrompts({
        category: activeCategory,
        q: query,
        sort: sortBy,
        page: page + 1,
      })
      if (seq !== seqRef.current) return true
      setItems((prev) => [...prev, ...res.items])
      setPage(res.page)
      setPages(res.pages)
      setTotal(res.total)
    } catch {
      // 触底加载失败静默，滚动再触发即重试
    } finally {
      setLoadingMore(false)
    }
    return true
  }, [activeCategory, query, sortBy, page, pages, loading, loadingMore, loadError])

  // 观察者回调经 ref 取最新 loadMore，观察者本身随 items 重建——
  // IntersectionObserver observe 时会立即回调一次当前相交状态，短列表可自动连续加载填满视口
  const loadMoreRef = useRef(loadMore)
  useEffect(() => {
    loadMoreRef.current = loadMore
  }, [loadMore])

  const sentinelRef = useRef<HTMLDivElement | null>(null)
  useEffect(() => {
    const node = sentinelRef.current
    if (!node || typeof IntersectionObserver === 'undefined') return
    const observer = new IntersectionObserver(
      (entries) => {
        if (!entries.some((entry) => entry.isIntersecting)) return
        // 自动加载吃预算，耗尽后停下等常驻的「继续往下看」按钮（防挂机/脚本无限拉）；
        // 只有真正发起的加载才扣预算
        if (autoBudgetRef.current <= 0) return
        void loadMoreRef.current().then((started) => {
          if (started) autoBudgetRef.current -= 1
        })
      },
      { rootMargin: '400px' }
    )
    observer.observe(node)
    return () => observer.disconnect()
  }, [items])

  function toggleCategory(slug: string): void {
    setActiveCategory((cur) => (cur === slug ? null : slug))
  }

  // 「直接使用/复制」都先取详情全文；pendingId 挡连点，失败静默恢复可重试
  async function withDetail(
    item: PromptListItem,
    run: (detail: PromptDetail) => void | Promise<void>
  ): Promise<void> {
    if (pendingId !== null) return
    setPendingId(item.id)
    try {
      await run(await fetchPromptDetail(item.id))
    } catch {
      // 详情拉取/剪贴板失败静默：按钮恢复后重试即可
    } finally {
      setPendingId(null)
    }
  }

  function applyItem(item: PromptListItem): void {
    void withDetail(item, (detail) => {
      onApply({ prompt: detail.promptText })
    })
  }

  function copyItem(item: PromptListItem): void {
    void withDetail(item, async (detail) => {
      await navigator.clipboard.writeText(detail.promptText)
      setCopiedId(item.id)
      setTimeout(() => setCopiedId((id) => (id === item.id ? null : id)), 1500)
    })
  }

  function retry(): void {
    void loadFirstPage()
    if (!categories) {
      fetchCategories()
        .then(setCategories)
        .catch(() => {})
    }
  }

  // 到底提示：最后一页且列表非空、无进行中的加载/错误态时展示
  const atEnd = !loading && !loadError && !loadingMore && items.length > 0 && page >= pages

  return (
    <div className="space-y-6">
      {/* 类目 chips：三轴三行（场景/风格/主体），每轴行首「全部」胶囊（设计稿同款）。
          全局单选：点「全部」清掉本轴的选中；本轴无选中时「全部」即高亮。
          count=0 的类目不渲染（一期单主类目、多落在场景轴，风格/主体空类目别占地方）；
          整轴全空则连轴标签一起隐藏。放在最上：先筛类目，再搜索/排序细调，再看结果。 */}
      {categories && (
        <div className="grid gap-2.5">
          {AXES.map((axis) => {
            const cats = categories[axis].filter((cat) => cat.count > 0)
            if (cats.length === 0) return null
            const axisHasActive = cats.some((cat) => cat.slug === activeCategory)
            return (
              <div key={axis} className="flex min-w-0 items-baseline gap-3.5">
                <span className="w-[60px] shrink-0 text-xs tracking-[0.08em] text-snb-t3">
                  {t(`studio.gallery.axis.${axis}`)}
                </span>
                {/* <md 单行横滑（首屏即见画墙）；≥md 恢复换行。min-w-0 让横滑容器收缩到可用宽度
                    内部滚动，而非撑开整页（flex item 默认 min-width:auto 会顶宽父级→横向滚页）；
                    隐滚动条 + 子项 shrink-0 防压缩换行 */}
                <div className="flex min-w-0 gap-2 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden md:flex-wrap md:overflow-visible md:pb-0">
                  <Chip
                    className="shrink-0 whitespace-nowrap"
                    active={!axisHasActive}
                    onClick={() => {
                      if (axisHasActive) setActiveCategory(null)
                    }}
                  >
                    {t('playground.gallery.all')}
                  </Chip>
                  {cats.map((cat) => (
                    <Chip
                      key={cat.slug}
                      className="shrink-0 whitespace-nowrap"
                      active={activeCategory === cat.slug}
                      onClick={() => toggleCategory(cat.slug)}
                    >
                      {categoryName(cat, locale)}
                    </Chip>
                  ))}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* 搜索 + 排序合并一行工具栏（原先各占一行、一头一尾没关联，视觉零碎）：
          搜索在左（主筛选之外的文本兜底，恒显示）；排序在右（决定结果呈现顺序，
          仅影响结果列表故加载失败时隐藏）。紧邻结果区之上。 */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Input
          type="text"
          value={keyword}
          placeholder={t('playground.gallery.search')}
          onChange={(e) => setKeyword(e.target.value)}
          className="w-full max-w-[280px] [&>input]:rounded-full [&>input]:border-snb-hairline [&>input]:bg-snb-panel/60"
        />
        {/* 窄屏下段数较多易挤到换行断字：label/按钮统一 whitespace-nowrap 保持词形完整，
            实在放不下就让这一小条自己横向滚动，不拖累整行/整页布局 */}
        {!loadError && (
          <div className="flex max-w-full items-center gap-2.5 overflow-x-auto">
            <span className="shrink-0 whitespace-nowrap text-xs text-snb-t3">
              {t('studio.gallery.sort')}
            </span>
            <div
              role="group"
              aria-label={t('studio.gallery.sort')}
              className="flex shrink-0 items-center gap-0.5 rounded-full border border-snb-hairline bg-snb-panel p-[3px]"
            >
              {SORT_SEGMENTS.map(({ value, labelKey }) => {
                const active = value === sortBy
                return (
                  <button
                    key={labelKey}
                    type="button"
                    onClick={() => setSortBy(value)}
                    className={`whitespace-nowrap rounded-full px-[13px] py-[5px] text-[12.5px] transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50 ${
                      active ? 'bg-snb-t1 text-snb-bg' : 'text-snb-t2 hover:text-snb-t1'
                    }`}
                  >
                    {t(labelKey)}
                  </button>
                )
              })}
            </div>
          </div>
        )}
      </div>

      {/* 加载失败：温和降级，不影响生图 */}
      {loadError ? (
        <Alert tone="warning" title={t('studio.gallery.loadFailedTitle')}>
          <p className="text-[13.5px] leading-[1.6]">{t('studio.gallery.loadFailedBody')}</p>
          <div className="mt-3">
            <Button variant="secondary" size="sm" onClick={retry}>
              {t('studio.gallery.reload')}
            </Button>
          </div>
        </Alert>
      ) : loading ? (
        /* 首屏骨架：瀑布流占位，变高防止列高整齐得像表格 */
        <MasonryGrid className="2xl:columns-5">
          {[52, 40, 64, 44, 56, 48, 60, 42].map((h, i) => (
            <Skeleton
              key={i}
              className="mb-4 break-inside-avoid rounded-xl"
              style={{ height: `${h * 4}px` }}
            />
          ))}
        </MasonryGrid>
      ) : items.length === 0 ? (
        <p className="py-8 text-center text-sm text-snb-t3">{t('playground.gallery.empty')}</p>
      ) : (
        /* JS 分列瀑布流（BalancedMasonry）：新卡贪心落最矮列、老卡不挪位，
           修掉 CSS columns「追加只砸右侧列 + 跨列跳动」的顽疾；批次内弹簧错峰入场。
           imageW/h 撑 aspect-ratio 预占位（没有占位比例卡片高 0 →
           瀑布流塌陷 → 哨兵常驻视口把分页一口气拉完）；任一缺失/为 0 时走
           AutoRatioCard：3:4 只占位到图片加载完成，避免永久裁切 */
        <BalancedMasonry
          items={items}
          resetKey={wallGen}
          renderItem={(item) => {
            const cardProps = {
              src: item.imageUrl,
              alt: item.title,
              onActivate: isTouch ? () => setSheetItem(item) : undefined,
              // 常驻数据条：标题 + 赞/藏计数常显（社会证明）+ 署名（CC 署名要求，常显更合规）
              stats: (
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
              ),
              // hover 才展开的深层动作：使用/复制
              overlay: (
                <div className="flex gap-2">
                  <Button
                    variant="primary"
                    size="sm"
                    className="flex-1"
                    disabled={pendingId === item.id}
                    onClick={() => applyItem(item)}
                  >
                    {t('studio.gallery.use')}
                  </Button>
                  <Button
                    variant="overlay"
                    size="sm"
                    className="flex-1"
                    disabled={pendingId === item.id}
                    onClick={() => copyItem(item)}
                  >
                    {copiedId === item.id ? t('studio.gallery.copied') : t('studio.gallery.copy')}
                  </Button>
                </div>
              ),
            }
            return item.imageW > 0 && item.imageH > 0 ? (
              <MasonryCard {...cardProps} width={item.imageW} height={item.imageH} />
            ) : (
              <AutoRatioCard {...cardProps} />
            )
          }}
        />
      )}

      {/* 无限滚动哨兵（常驻；loadMore 内部按状态自行拦截） */}
      <div ref={sentinelRef} className="h-px" aria-hidden />

      {/* 页尾续读区：只要还有下页就常驻——「滚到底什么都没有」曾真实发生过
          （2026-07-05 站长反馈：观察器停摆+按钮只在预算耗尽时渲染，页尾空无）。
          自动加载只是省点击的隐形便利，这个区域才是唯一可靠的续读入口。
          计数行把分页状态写成邀请；加载中原地变禁用态，区域高度不跳 */}
      {!loading && !loadError && items.length > 0 && page < pages && (
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
            onClick={() => {
              autoBudgetRef.current = AUTO_LOAD_BUDGET
              void loadMore()
            }}
          >
            {loadingMore ? (
              <span className="animate-pulse">{t('studio.gallery.loadingMore')}</span>
            ) : (
              <>
                {t('studio.gallery.keepBrowsing')}
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <path d="M6 9l6 6 6-6" />
                </svg>
              </>
            )}
          </Button>
        </div>
      )}
      {atEnd && <p className="text-center text-[12.5px] text-snb-t3">{t('studio.gallery.atEnd')}</p>}

      {/* 触屏抽屉：tap 卡片弹出，复用 applyItem/copyItem（含 pendingId 防连点、copiedId 反馈） */}
      {sheetItem && (
        <PromptSheet
          item={sheetItem}
          pending={pendingId === sheetItem.id}
          copied={copiedId === sheetItem.id}
          onUse={(it) => {
            applyItem(it)
            setSheetItem(null)
          }}
          onCopy={(it) => copyItem(it)}
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
