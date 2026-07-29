import { useEffect, useState } from 'react'
import { t } from '../../i18n'
import { th } from '../hubMessages'
import { getCategories, type CategoryView } from '../api'
import { useArticles } from '../useArticles'
import { ArticleCard } from '../ArticleCard'
import { SerialSpotlight } from '../SerialSpotlight'

const ALL = '__all__'

/** 类目条：选中 = 纸白填充胶囊（公用件筛选胶囊），视觉高 32、透明内边距撑足 44 热区。 */
function CategoryTabs({
  tabs,
  active,
  onSelect,
}: {
  tabs: { id: string; label: string; count: number | null }[]
  active: string
  onSelect: (id: string) => void
}) {
  return (
    <div className="hub-cats-list" role="tablist">
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          className="hub-cat"
          aria-selected={active === tab.id}
          onClick={() => onSelect(tab.id)}
        >
          <span>
            {tab.label}
            {tab.count != null && <span className="n">{tab.count}</span>}
          </span>
        </button>
      ))}
    </div>
  )
}

/**
 * 列表页（杂志架）：架头（含未接线搜索位）+ 连载专栏位 + 类目胶囊 + 等高卡片墙 +
 * 加载更多 + 空/错态配对件。
 * 🚨 砖墙改等高网格、列数纯 CSS 断点：JS 测宽会在首帧后重排整墙（CLS 大头）。
 */
export function ListPage() {
  const [categories, setCategories] = useState<CategoryView[]>([])
  const [active, setActive] = useState(ALL)
  const { items, total, loading, error, hasMore, initialLoading, loadMore, retry } = useArticles(
    active === ALL ? null : active,
  )

  useEffect(() => {
    getCategories().then(setCategories).catch(() => setCategories([]))
  }, [])

  // 电子书不作类目示人（站长 2026-07-12）：整本连载走顶部专栏位；总数仍含它（15 文 + 1 书）
  const visibleCats = categories.filter((c) => c.slug !== 'ebooks')
  const allCount = categories.reduce((n, c) => n + c.count, 0)
  const tabs = [
    { id: ALL, label: t('hub.list.all'), count: allCount > 0 ? allCount : null },
    ...visibleCats.map((c) => ({ id: c.slug, label: c.name, count: c.count })),
  ]
  const activeName = visibleCats.find((c) => c.slug === active)?.name ?? ''
  // 空态的「去隔壁看看」指向条数最多的另一个类目——按钮要指向能把这里填满的动作
  const fallbackCat = visibleCats
    .filter((c) => c.slug !== active && c.count > 0)
    .sort((a, b) => b.count - a.count)[0]

  const wall = items.filter((a) => a.type !== 'ebook')
  const shown = wall.length
  const remaining = Math.max(0, total - items.length)

  return (
    <main className="hub-rack" data-testid="hub-list">
      <section className="hub-rack-head">
        <div>
          <div className="eb">{th('mag.eyebrow')}</div>
          <h1>{t('hub.title')}</h1>
          <p className="ds">{th('mag.desc')}</p>
          {/* 使用手册常驻直达位（Header 规范 v2：「使用指南」退出全站顶栏，唯一入口在杂志架） */}
          <a className="hub-manual" href="https://help.super-nb.me/">
            📖 {t('hub.list.manual')}
          </a>
        </div>
        {/* 搜索：位置已留、样式已定、明写未接线——绝不画搜索结果页 */}
        <div className="hub-search-wrap">
          <div className="hub-search">
            <span className="ic" aria-hidden="true">
              ⌕
            </span>
            <input type="text" disabled placeholder={th('mag.searchPh')} aria-label={th('mag.searchPh')} />
            <span className="off">{th('mag.searchOff')}</span>
          </div>
          <p className="hub-search-note">{th('mag.searchNote')}</p>
        </div>
      </section>

      {/* 专栏位占位：拉回来之前先占住高度，回来了不把整墙往下推 */}
      {active === ALL && (
        <div className="hub-serial-slot">
          <SerialSpotlight />
        </div>
      )}

      <section className="hub-cats">
        <CategoryTabs tabs={tabs} active={active} onSelect={setActive} />
        <div className="sort">{th('mag.sortLine', { n: total })}</div>
      </section>

      {initialLoading && (
        <div className="hub-wall" data-testid="hub-loading" aria-hidden="true">
          {Array.from({ length: 6 }, (_, i) => (
            <div className="hub-cardskel" key={i}>
              <div className="cv" />
              <div className="bd">
                <i />
                <i />
                <i />
                <i />
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 判定规则：请求失败/超时 → 错误态；请求成功但为空 → 空态。二者不可互换 */}
      {error && !loading && (
        <section className="hub-state err" data-testid="hub-error">
          <span className="ic" aria-hidden="true">
            !
          </span>
          <div className="ti">{th('mag.errTitle')}</div>
          <div className="ds">{th('mag.errDesc')}</div>
          <div className="ac">
            <button type="button" className="hub-btn2" onClick={retry} data-testid="hub-retry">
              {th('mag.retry')}
            </button>
            {active !== ALL && (
              <button type="button" className="hub-btn3" onClick={() => setActive(ALL)}>
                {th('mag.errGhost')}
              </button>
            )}
          </div>
        </section>
      )}

      {!initialLoading && !error && shown === 0 && (
        <section className="hub-state" data-testid="hub-empty">
          <span className="ic" aria-hidden="true">
            ∅
          </span>
          <div className="ti">{th('mag.emptyTitle')}</div>
          <div className="ds">
            {fallbackCat
              ? th('mag.emptyDesc', { c: activeName, o: fallbackCat.name })
              : th('mag.emptyDescAll')}
          </div>
          {fallbackCat && (
            <div className="ac">
              <button type="button" className="hub-btn2" onClick={() => setActive(fallbackCat.slug)}>
                {th('mag.emptyBtn', { o: fallbackCat.name })}
              </button>
            </div>
          )}
        </section>
      )}

      {shown > 0 && (
        <>
          <section className="hub-wall">
            {wall.map((a) => (
              <ArticleCard key={a.slug} article={a} />
            ))}
          </section>
          <div className="hub-more">
            {hasMore ? (
              <>
                <button
                  type="button"
                  className="hub-btn2"
                  onClick={loadMore}
                  disabled={loading}
                  data-testid="hub-load-more"
                >
                  {loading ? t('hub.list.loading') : th('mag.more', { n: remaining })}
                </button>
                <span className="cnt">{th('mag.shown', { a: items.length, b: total })}</span>
              </>
            ) : (
              <span className="cnt" data-testid="hub-no-more">
                {th('mag.shown', { a: items.length, b: total })}
              </span>
            )}
          </div>
        </>
      )}
    </main>
  )
}
