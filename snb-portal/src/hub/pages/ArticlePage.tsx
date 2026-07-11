import { useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import DOMPurify from 'dompurify'
import { Chip, Skeleton } from '../../ui'
import { t } from '../../i18n'
import { getArticle, NotFoundError, type ArticleDetail } from '../api'
import { ReadingProgress } from '../ReadingProgress'
import { readingMinutes } from '../readingTime'
import { EbookBody } from '../EbookBody'

type State =
  | { kind: 'loading' }
  | { kind: 'ready'; article: ArticleDetail }
  | { kind: 'notFound' }
  | { kind: 'error' }

function formatDate(iso: string): string {
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 文章详情页：编辑部式阅读版式（进度线/速览面板/编号分节）。正文=管线预渲染 HTML + DOMPurify 兜底（纵深防御，照公告口径默认白名单）；电子书同版式，正文位换书体纸面卡（站长拍板：当普通教程，不做独立阅读器）。 */
export function ArticlePage() {
  const { slug = '', chapter: chapterRaw } = useParams()
  const chapter = chapterRaw && /^\d+$/.test(chapterRaw) ? Number(chapterRaw) : undefined
  const [state, setState] = useState<State>({ kind: 'loading' })

  // 章节切换/换文回到页顶（路由变化 React Router 不自动滚）
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [slug, chapterRaw])

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
      <main className="mx-auto w-full max-w-[43rem] px-5 py-8 sm:py-10" data-testid="hub-article">
        <Skeleton className="mb-8 h-4 w-24" />
        <Skeleton className="mb-3 h-5 w-40" />
        <Skeleton className="mb-2 h-10 w-full" />
        <Skeleton className="mb-6 h-10 w-3/5" />
        <Skeleton className="mb-8 h-4 w-52" />
        <Skeleton className="mb-8 h-24" />
        <Skeleton className="h-64" />
      </main>
    )
  }

  if (state.kind === 'notFound' || state.kind === 'error') {
    return (
      <main className="mx-auto w-full max-w-[43rem] px-5 py-16 text-center" data-testid="hub-article">
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
  const isEbook = a.type === 'ebook'
  if (!isEbook && chapterRaw) {
    return <Navigate to={`/a/${a.slug}`} replace /> // 章节段只对电子书有意义
  }
  const minutes = isEbook ? null : readingMinutes(a.bodyHtml ?? '')

  return (
    <main className="mx-auto w-full max-w-[43rem] px-5 py-8 sm:py-10" data-testid="hub-article">
      <ReadingProgress />

      <nav className="mb-8">
        <Link className="text-[13px] text-snb-t3 transition-colors hover:text-snb-t1" to="/">
          ← {t('hub.article.backHome')}
        </Link>
      </nav>

      <header className="mb-7">
        <div className="mb-4 flex flex-wrap items-center gap-2">
          <Chip>{a.categoryName}</Chip>
          {a.tags.slice(0, 4).map((tag) => (
            <span key={tag} className="rounded bg-snb-t1/[0.05] px-1.5 py-0.5 text-xs text-snb-t3">
              {tag}
            </span>
          ))}
        </div>
        <h1 className="mb-4 font-display text-[1.9rem] font-bold leading-[1.32] tracking-[-0.01em] text-snb-t1 sm:text-[2.3rem] sm:leading-[1.26]">
          {a.title}
        </h1>
        <p className="flex flex-wrap items-center gap-x-2 gap-y-1 text-[13px] text-snb-t3" data-testid="hub-byline">
          <time dateTime={a.publishedAt}>{formatDate(a.publishedAt)}</time>
          {minutes !== null && (
            <>
              <span aria-hidden="true">·</span>
              <span>{t('hub.article.readingTime', { n: minutes })}</span>
            </>
          )}
          {a.sourceName && (
            <>
              <span aria-hidden="true">·</span>
              <span>
                {t('hub.article.source')}
                {a.sourceName}
              </span>
            </>
          )}

        </p>
      </header>

      {a.summary && (
        <aside className="hub-tldr" data-testid="hub-tldr">
          <span className="hub-tldr-tab">{t('hub.article.tldr')}</span>
          <p>{a.summary}</p>
        </aside>
      )}

      {a.coverUrl && (
        <figure className="mb-9 overflow-hidden rounded-2xl border border-snb-hairline">
          <img alt="" className="block w-full" loading="lazy" src={a.coverUrl} />
        </figure>
      )}

      {isEbook ? (
        <EbookBody slug={a.slug} path={a.ebookPath ?? ''} chapter={chapter} />
      ) : (
        <article
          className="hub-prose"
          // 管线已预渲染并 sanitize；此处 DOMPurify 默认白名单再兜一层（纵深防御）
          dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(a.bodyHtml ?? '') }}
        />
      )}

      {a.sourceName && (
        <footer
          className="mt-12 rounded-xl border border-snb-hairline bg-snb-well/60 px-4 py-3 text-[13px] leading-relaxed text-snb-t3"
          data-testid="hub-source"
        >
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

      <nav className="mt-10 border-t border-snb-hairline pt-6">
        <Link className="text-[13px] text-snb-t3 transition-colors hover:text-snb-t1" to="/">
          ← {t('hub.article.backHome')}
        </Link>
      </nav>
    </main>
  )
}
