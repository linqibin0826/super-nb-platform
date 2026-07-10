import { useEffect, useState } from 'react'
import { Button, Skeleton, Tabs } from '../../ui'
import { t } from '../../i18n'
import { getCategories, type CategoryView } from '../api'
import { useArticles } from '../useArticles'
import { ArticleCard } from '../ArticleCard'

const ALL = '__all__'

/** 列表页：分类 tab（全部 + 动态分类）+ 卡片流 + 加载更多 + 空/错态。 */
export function ListPage() {
  const [categories, setCategories] = useState<CategoryView[]>([])
  const [active, setActive] = useState(ALL)
  const { items, loading, error, hasMore, initialLoading, loadMore, retry } = useArticles(
    active === ALL ? null : active,
  )

  useEffect(() => {
    getCategories().then(setCategories).catch(() => setCategories([]))
  }, [])

  const tabs = [
    { id: ALL, label: t('hub.list.all') },
    ...categories.map((c) => ({ id: c.slug, label: c.name })),
  ]

  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-8" data-testid="hub-list">
      <Tabs items={tabs} active={active} onSelect={setActive} className="mb-6" />

      {initialLoading && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-64" />
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

      {!initialLoading && !error && items.length === 0 && (
        <p className="py-16 text-center text-snb-t2" data-testid="hub-empty">{t('hub.list.empty')}</p>
      )}

      {items.length > 0 && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {items.map((a) => (
              <ArticleCard key={a.slug} article={a} />
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
