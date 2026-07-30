// 灵感库：gallery-svc API 版（列表/搜索/类目筛选走服务端，详情点按需取）
// 无限滚动 + 防抖搜索 + 代际竞态防护等数据逻辑保持不变；
// 署名常显（源库 CC BY 4.0 必须署名）。
//
// 2026-07-29 画图机位改版（Claude Design 定稿）三处结构变化：
// ① 卡片换本地 WallCard（加厚遮罩信息层）——ui 的 MasonryCard 只有 20% 遮罩，浅色图上标题
//    与署名读不出（1.6:1），那是 5778 条素材唯一语义线索被掐掉；
// ② 筛选胶囊换本地 FilterChip（纸白填充选中）——ui 的 Chip 选中是白字压橙 2.61:1，
//    而旁边排序控件是纸白填充，同一屏「选中」两种语言；
// ③ 桌面点卡片开大图（WallLightbox），提示词全文与「直接使用/复制」同界面完成；
//    触屏窄屏仍走 PromptSheet 抽屉。逛不拦、生成才拦的分寸一律不动。
import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, MasonryGrid, Skeleton } from '../ui'
import { EmptyState, ErrorState } from './parts'
import {
  categoryName,
  fetchCategories,
  fetchPromptDetail,
  fetchPrompts,
  type CategoryAxis,
  type CategoryItem,
  type CategoryTree,
  type GallerySort,
  type PromptListItem,
  type PromptDetail,
} from '../lib/galleryApi'
import type { ApplyPayload } from '../App'
import { BalancedMasonry } from './BalancedMasonry'
import { PromptSheet } from './PromptSheet'
import { WallCard } from './WallCard'
import { WallLightbox } from './WallLightbox'
import { FilterChip } from './FilterChip'
import { CardStat } from './CardStat'
import { locale, t } from '../i18n'
import { st } from './i18nStudio'
import { useMediaQuery } from './useMediaQuery'
import { useInteractions } from './useInteractions'
import { useAuthUser } from '../auth/useAuth'
import { loginUrl } from '../auth/apiFetch'

interface Props {
  onApply: (item: ApplyPayload) => void
}

/** 「更多筛选」里的两根副轴：主轴（场景）常驻露出，这两轴收进浮层 */
const EXTRA_AXES: CategoryAxis[] = ['style', 'subject']

/** 手机首屏类目露出的枚数（其余靠「+N 类」计数钮展开，不靠切边猜） */
const MOBILE_CATS = 2

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

/** 空类目：显示但不可点（摆 41 枚胶囊、点 38 枚是空结果，那不是筛选是陷阱） */
function EmptyCat({ label }: { label: string }) {
  return (
    <span className="inline-flex h-8 items-center gap-2 rounded-full border border-snb-hairline px-3.5 text-[13px] text-snb-t3">
      {label}
      <span className="font-mono text-[11px] tabular-nums">0</span>
    </span>
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
  const [openIndex, setOpenIndex] = useState<number | null>(null) // 大图浮层当前位（items 下标）
  const [catsOpen, setCatsOpen] = useState(false) // 手机：类目行是否已展开（「+N 类」）
  const [moreOpen, setMoreOpen] = useState(false) // 「更多筛选 · 风格」浮层

  // 触屏（无 hover）：卡片浮层按钮点不到。窄屏走底部抽屉（手势更顺），
  // 触屏宽屏（平板）与桌面一律走大图浮层——抽屉是 sm:hidden 的，平板上点了会没反应。
  const isTouch =
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(hover: none)').matches
  const isWide = useMediaQuery('(min-width: 640px)')

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
      setOpenIndex(null) // 换了一叠图，大图浮层里的下标就不作数了（搜索防抖回来这一路也要收）
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
    setOpenIndex(null) // 换筛选=换了「本次筛选」这一叠，浮层里的位次就不作数了
    setActiveCategory((cur) => (cur === slug ? null : slug))
  }

  /** 整卡点击：桌面/平板开大图浮层，触屏窄屏开底部抽屉 */
  function openCard(item: PromptListItem): void {
    if (isTouch && !isWide) {
      setSheetItem(item)
      return
    }
    const index = items.findIndex((it) => it.id === item.id)
    if (index >= 0) setOpenIndex(index)
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

  const scenes = (categories?.scene ?? []).filter((cat) => cat.count > 0)
  const hasActiveScene = scenes.some((cat) => cat.slug === activeCategory)
  // 副轴（风格/主体）：有货的才可点，全空的轴只出一行说明——不摆点进去是空结果的胶囊
  const extraStocked = EXTRA_AXES.map((axis) => (categories?.[axis] ?? []).filter((c) => c.count > 0))
  const hasExtraStocked = extraStocked.some((list) => list.length > 0)
  const subjectAllEmpty = (categories?.subject ?? []).every((c) => c.count === 0)

  return (
    <div className="space-y-5">
      {/* 工具条第一行：搜索 + 排序（原先各占一行、一头一尾没关联，手机上光外壳就吃掉半屏）。
          排序四档与类目共用 FilterChip：全站「选中 = 纸白填充」只有一种语言。 */}
      <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
        {/* 搜索框按定稿本地实现（高 44 / 面板底 / hairline-strong 边 / r8 / mono 斜杠提示位）。
            🚨 不用 vendor Input：那需要 [&>input] 去够它的内部结构，
            上游公用件 v3 一改结构这类覆盖必失配（Chip 两层化就是先例） */}
        <label className="flex h-11 w-full min-w-[220px] flex-1 items-center gap-2.5 rounded-[8px] border border-snb-hairline-strong bg-snb-panel px-3.5 transition-colors duration-[120ms] ease-[cubic-bezier(0.2,0,0,1)] focus-within:border-snb-hairline-heavy sm:max-w-[360px]">
          <span aria-hidden="true" className="flex-none font-mono text-[13px] text-snb-t3">
            /
          </span>
          <input
            type="text"
            value={keyword}
            aria-label={st('studio.filters.searchPlaceholder')}
            placeholder={st('studio.filters.searchPlaceholder')}
            onChange={(e) => setKeyword(e.target.value)}
            className="min-w-0 flex-1 border-0 bg-transparent p-0 text-sm text-snb-t1 outline-none placeholder:text-snb-t3 focus:ring-0"
          />
        </label>
        {!loadError && (
          <div
            role="group"
            aria-label={t('studio.gallery.sort')}
            className="flex max-w-full shrink-0 items-center gap-0.5 overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
          >
            {SORT_SEGMENTS.map(({ value, labelKey }) => (
              <FilterChip
                key={labelKey}
                className="shrink-0"
                active={value === sortBy}
                onClick={() => {
                  setOpenIndex(null)
                  setSortBy(value)
                }}
              >
                {st(labelKey)}
              </FilterChip>
            ))}
          </div>
        )}
      </div>

      {/* 工具条第二行：主轴（场景）类目。真源三轴里只有场景轴有货，露出的就只放它。
          <md 单行横滑 + 「+N 类」计数钮（不靠切边猜还有多少）；≥md 恢复换行铺开。 */}
      {scenes.length > 0 && (
        <div
          className={`flex min-w-0 items-center gap-0.5 overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden md:flex-wrap md:overflow-visible ${
            catsOpen ? 'flex-wrap overflow-visible' : ''
          }`}
        >
          <FilterChip
            className="shrink-0"
            active={!hasActiveScene}
            count={total > 0 && !hasActiveScene && query === '' ? total.toLocaleString() : undefined}
            onClick={() => {
              if (hasActiveScene) {
                setOpenIndex(null)
                setActiveCategory(null)
              }
            }}
          >
            {st('studio.filters.all')}
          </FilterChip>
          {scenes.map((cat, i) => (
            <FilterChip
              key={cat.slug}
              className={`shrink-0 ${!catsOpen && i >= MOBILE_CATS ? 'hidden md:inline-flex' : ''}`}
              active={activeCategory === cat.slug}
              count={cat.count.toLocaleString()}
              onClick={() => toggleCategory(cat.slug)}
            >
              {categoryName(cat, locale)}
            </FilterChip>
          ))}
          {scenes.length > MOBILE_CATS && (
            <FilterChip dashed className="shrink-0 md:hidden" onClick={() => setCatsOpen((v) => !v)}>
              {catsOpen
                ? st('studio.filters.lessCats')
                : st('studio.filters.moreCats', { n: scenes.length - MOBILE_CATS })}
            </FilterChip>
          )}
        </div>
      )}

      {/* 「更多筛选 · 风格」：副轴收在这儿，空档显示但不可点 */}
      {hasExtraStocked && (
        <div>
          <FilterChip dashed onClick={() => setMoreOpen((v) => !v)}>
            {moreOpen ? st('studio.filters.lessStyles') : st('studio.filters.moreStyles')}
          </FilterChip>
          {moreOpen && (
            <div className="mt-1.5 flex flex-col gap-3 rounded-[10px] border border-snb-hairline-strong bg-snb-panel px-4 py-3.5">
              {EXTRA_AXES.map((axis, ai) => {
                const list: CategoryItem[] = categories?.[axis] ?? []
                if (extraStocked[ai].length === 0) return null
                return (
                  <div key={axis}>
                    <span className="font-mono text-[11px] tracking-[0.14em] text-snb-t3">
                      {t(`studio.gallery.axis.${axis}`)}
                    </span>
                    <div className="mt-0.5 flex flex-wrap items-center gap-0.5">
                      {list.map((cat) =>
                        cat.count > 0 ? (
                          <FilterChip
                            key={cat.slug}
                            active={activeCategory === cat.slug}
                            count={cat.count.toLocaleString()}
                            onClick={() => toggleCategory(cat.slug)}
                          >
                            {categoryName(cat, locale)}
                          </FilterChip>
                        ) : (
                          <span key={cat.slug} className="px-0.5 py-1.5">
                            <EmptyCat label={categoryName(cat, locale)} />
                          </span>
                        )
                      )}
                    </div>
                  </div>
                )
              })}
              {subjectAllEmpty && (
                <p className="m-0 font-mono text-[11.5px] leading-[1.8] text-snb-t3">
                  {st('studio.filters.styleNote')}
                </p>
              )}
            </div>
          )}
        </div>
      )}

      {/* 加载失败 → 错误态（功能红描边 + 三要素文案 + 「再试一次」）；
          绝不与「真的没有数据」的空态互换。不影响生图，票据照常可用 */}
      {loadError ? (
        <ErrorState
          title={st('studio.wallError.title')}
          body={st('studio.wallError.body')}
          actionLabel={st('studio.wallError.action')}
          onAction={retry}
        />
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
        /* 空态：请求成功但这一筛真的没货。按钮指向能把这里填满的动作（清筛选回全部），
           不许只写「没有匹配的案例」 */
        <EmptyState
          title={st('studio.empty.title')}
          body={st('studio.empty.body')}
          actionLabel={st('studio.empty.action')}
          onAction={() => {
            setActiveCategory(null)
            setKeyword('')
            setCatsOpen(false)
            setMoreOpen(false)
          }}
        />
      ) : (
        /* JS 分列瀑布流（BalancedMasonry）：新卡贪心落最矮列、老卡不挪位，
           修掉 CSS columns「追加只砸右侧列 + 跨列跳动」的顽疾；批次内弹簧错峰入场。
           imageW/h 撑 aspect-ratio 预占位（没有占位比例卡片高 0 →
           瀑布流塌陷 → 哨兵常驻视口把分页一口气拉完）；缺宽高时 WallCard 内部
           先 3:4 占位、图片落地换天然比例，避免永久裁切 */
        <BalancedMasonry
          items={items}
          resetKey={wallGen}
          renderItem={(item) => (
            <WallCard
              src={item.imageUrl}
              alt={item.title}
              width={item.imageW}
              height={item.imageH}
              title={item.title}
              author={item.authorName}
              onOpen={() => openCard(item)}
              // 常驻数据条：标题 + 赞/藏计数（社会证明）+ 署名（CC BY 4.0 署名要求，常显更合规）
              stats={
                <>
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
                </>
              }
              // hover 才展开的深层动作：使用/复制（触屏由整卡点击兜底）
              actions={
                <div className="flex gap-2">
                  <Button
                    variant="primary"
                    size="sm"
                    className="flex-1"
                    disabled={pendingId === item.id}
                    onClick={(e) => {
                      e.stopPropagation()
                      applyItem(item)
                    }}
                  >
                    {t('studio.gallery.use')}
                  </Button>
                  <Button
                    variant="overlay"
                    size="sm"
                    className="flex-1"
                    disabled={pendingId === item.id}
                    onClick={(e) => {
                      e.stopPropagation()
                      copyItem(item)
                    }}
                  >
                    {copiedId === item.id ? t('studio.gallery.copied') : t('studio.gallery.copy')}
                  </Button>
                </div>
              }
            />
          )}
        />
      )}

      {/* 署名常显不是排版偏好：源库按 CC BY 4.0 收录，不署名就不能用 */}
      {!loading && !loadError && items.length > 0 && (
        <p className="m-0 font-mono text-[11.5px] text-snb-t3">{st('studio.wall.ccNote')}</p>
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

      {/* 桌面/平板大图浮层：大图 + 提示词全文 + 直接使用/复制，同界面完成。
          筛选或分页把这张挤掉时（下标越界）自动作废，不残留一张对不上号的图 */}
      {openIndex !== null && openIndex < items.length && (
        <WallLightbox
          item={items[openIndex]}
          index={openIndex + 1}
          isMember={!!user}
          liked={liked.has(items[openIndex].id)}
          favorited={favorited.has(items[openIndex].id)}
          likeCount={likeCounts.get(items[openIndex].id) ?? items[openIndex].likeCount}
          favCount={favCounts.get(items[openIndex].id) ?? items[openIndex].favCount}
          onToggleLike={() => toggle('like', items[openIndex].id)}
          onToggleFavorite={() => toggle('favorite', items[openIndex].id)}
          pending={pendingId === items[openIndex].id}
          copied={copiedId === items[openIndex].id}
          onUse={() => {
            applyItem(items[openIndex])
            setOpenIndex(null)
          }}
          onCopy={() => copyItem(items[openIndex])}
          onPrev={() => setOpenIndex((i) => (i === null ? null : (i + items.length - 1) % items.length))}
          onNext={() => setOpenIndex((i) => (i === null ? null : (i + 1) % items.length))}
          onClose={() => setOpenIndex(null)}
        />
      )}

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
