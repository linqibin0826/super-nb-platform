import { useEffect, useState } from 'react'
import { Button, Skeleton, Tabs } from '../../ui'
import { t } from '../../i18n'
import { getCategories, type CategoryView } from '../api'
import { useArticles } from '../useArticles'
import { ArticleCard } from '../ArticleCard'
import { SerialSpotlight } from '../SerialSpotlight'

const ALL = '__all__'

/** 瀑布流列数：随断点 1/2/3（与旧网格断点一致 sm/lg）。 */
function useColumns(): number {
  const [cols, setCols] = useState(1)
  useEffect(() => {
    const sm = window.matchMedia('(min-width: 640px)')
    const lg = window.matchMedia('(min-width: 1024px)')
    const update = () => setCols(lg.matches ? 3 : sm.matches ? 2 : 1)
    update()
    sm.addEventListener('change', update)
    lg.addEventListener('change', update)
    return () => {
      sm.removeEventListener('change', update)
      lg.removeEventListener('change', update)
    }
  }, [])
  return cols
}

/** 列表页：分类 tab（电子书不设类目，走顶部连载专栏位）+ 瀑布流卡片墙 + 加载更多 + 空/错态。 */
export function ListPage() {
  const [categories, setCategories] = useState<CategoryView[]>([])
  const [active, setActive] = useState(ALL)
  const cols = useColumns()
  const { items, loading, error, hasMore, initialLoading, loadMore, retry } = useArticles(
    active === ALL ? null : active,
  )

  useEffect(() => {
    getCategories().then(setCategories).catch(() => setCategories([]))
  }, [])

  const tabs = [
    { id: ALL, label: t('hub.list.all') },
    // 电子书不作类目示人（站长 2026-07-12）：整本连载走顶部专栏位
    ...categories.filter((c) => c.slug !== 'ebooks').map((c) => ({ id: c.slug, label: c.name })),
  ]

  // 瀑布流：电子书不进卡片墙；round-robin 分列——首行是最新三篇，加载更多只在各列尾部追加不重排
  const wall = items.filter((a) => a.type !== 'ebook')
  const columns = Array.from({ length: cols }, (_, c) => wall.filter((_, i) => i % cols === c))

  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-8" data-testid="hub-list">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <Tabs items={tabs} active={active} onSelect={setActive} />
        {/* 使用手册常驻直达位（Header 规范 v2：「使用指南」退出全站顶栏，动线改经内容中心） */}
        <a
          href="https://help.super-nb.me/"
          className="inline-flex flex-none items-center gap-1.5 rounded-full border border-primary-500/40 px-3.5 py-1.5 text-[13px] font-medium text-primary-600 transition-colors hover:bg-primary-500/10 dark:text-primary-300"
        >
          📖 {t('hub.list.manual')}
        </a>
      </div>

      {active === ALL && <SerialSpotlight />}

      {initialLoading && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className={i % 2 ? 'h-48' : 'h-72'} />
          ))}
        </div>
      )}

      {error && !loading && (
        <div className="py-16 text-center text-snb-t2" data-testid="hub-error">
          <p className="mb-4">{t('hub.list.error')}</p>
          <Button variant="secondary" onClick={retry} data-testid="hub-retry">
            {t('hub.list.retry')}
          </Button>
        </div>
      )}

      {!initialLoading && !error && wall.length === 0 && (
        <p className="py-16 text-center text-snb-t2" data-testid="hub-empty">{t('hub.list.empty')}</p>
      )}

      {wall.length > 0 && (
        <>
          <div className="flex items-start gap-4">
            {columns.map((col, ci) => (
              <div key={ci} className="flex min-w-0 flex-1 flex-col gap-4">
                {col.map((a) => (
                  <ArticleCard key={a.slug} article={a} />
                ))}
              </div>
            ))}
          </div>
          <div className="mt-8 text-center">
            {hasMore ? (
              <Button variant="secondary" onClick={loadMore} disabled={loading} data-testid="hub-load-more">
                {loading ? t('hub.list.loading') : t('hub.list.loadMore')}
              </Button>
            ) : (
              <p className="text-sm text-snb-t3" data-testid="hub-no-more">{t('hub.list.noMore')}</p>
            )}
          </div>
        </>
      )}
    </main>
  )
}
