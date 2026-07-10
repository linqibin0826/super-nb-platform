import { useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import DOMPurify from 'dompurify'
import { Chip, Skeleton } from '../../ui'
import { t } from '../../i18n'
import { getArticle, NotFoundError, type ArticleDetail } from '../api'

type State =
  | { kind: 'loading' }
  | { kind: 'ready'; article: ArticleDetail }
  | { kind: 'notFound' }
  | { kind: 'error' }

function formatDate(iso: string): string {
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 文章详情页：管线预渲染 HTML + 前端 DOMPurify 兜底注入（纵深防御，照公告口径默认白名单）。 */
export function ArticlePage() {
  const { slug = '' } = useParams()
  const [state, setState] = useState<State>({ kind: 'loading' })

  useEffect(() => {
    let alive = true
    setState({ kind: 'loading' })
    getArticle(slug)
      .then((article) => alive && setState({ kind: 'ready', article }))
      .catch((e) => alive && setState({ kind: e instanceof NotFoundError ? 'notFound' : 'error' }))
    return () => {
      alive = false
    }
  }, [slug])

  useEffect(() => {
    if (state.kind === 'ready') {
      document.title = `${state.article.title} · ${t('hub.title')}`
    }
  }, [state])

  if (state.kind === 'loading') {
    return (
      <main className="mx-auto w-full max-w-3xl px-4 py-8" data-testid="hub-article">
        <Skeleton className="mb-4 h-8 w-2/3" />
        <Skeleton className="mb-2 h-4 w-1/3" />
        <Skeleton className="h-64" />
      </main>
    )
  }

  if (state.kind === 'notFound' || state.kind === 'error') {
    return (
      <main className="mx-auto w-full max-w-3xl px-4 py-16 text-center" data-testid="hub-article">
        <p className="mb-4 text-snb-t2" data-testid={state.kind === 'notFound' ? 'hub-not-found' : 'hub-error'}>
          {t(state.kind === 'notFound' ? 'hub.article.notFound' : 'hub.list.error')}
        </p>
        <Link className="text-sm text-snb-t2 underline underline-offset-4 hover:text-snb-t1" to="/">
          {t('hub.article.backHome')}
        </Link>
      </main>
    )
  }

  const a = state.article
  if (a.type === 'ebook') {
    return <Navigate to={`/reader/${a.slug}`} replace />
  }

  return (
    <main className="mx-auto w-full max-w-3xl px-4 py-8" data-testid="hub-article">
      <header className="mb-6">
        <div className="mb-3 flex items-center gap-2">
          <Chip>{a.categoryName}</Chip>
          {a.tags.slice(0, 4).map((tag) => (
            <span key={tag} className="rounded bg-snb-t1/[0.05] px-1.5 py-0.5 text-xs text-snb-t3">
              {tag}
            </span>
          ))}
        </div>
        <h1 className="mb-2 font-display text-3xl font-bold leading-tight text-snb-t1">{a.title}</h1>
        <p className="text-sm text-snb-t3">{formatDate(a.publishedAt)}</p>
      </header>

      <article
        className="hub-prose"
        // 管线已预渲染并 sanitize；此处 DOMPurify 默认白名单再兜一层（纵深防御）
        dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(a.bodyHtml ?? '') }}
      />

      {a.sourceName && (
        <footer className="mt-10 border-t border-snb-hairline pt-4 text-sm text-snb-t3" data-testid="hub-source">
          {t('hub.article.source')}
          {a.sourceName}
          {a.sourceUrl && (
            <>
              {' · '}
              <a
                className="underline underline-offset-4 hover:text-snb-t1"
                href={a.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                {t('hub.article.original')}
              </a>
            </>
          )}
        </footer>
      )}
    </main>
  )
}
