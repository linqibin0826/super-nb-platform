import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import DOMPurify from 'dompurify'
import { Skeleton } from '../../ui'
import { t } from '../../i18n'
import { getArticle, NotFoundError, type ArticleDetail } from '../api'
import { ReadingProgress } from '../ReadingProgress'
import { readingMinutes } from '../readingTime'
import { EbookLongRead } from '../EbookLongRead'

type State =
  | { kind: 'loading' }
  | { kind: 'ready'; article: ArticleDetail }
  | { kind: 'notFound' }
  | { kind: 'error' }

function formatDate(iso: string): string {
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 文章详情页：元信息条 + 文章纸张卡（暗色随全站变墨：纸 = panel 墨卡，全站同一套明暗纪律）。正文=管线预渲染 HTML + DOMPurify 兜底（纵深防御，照公告口径默认白名单）。电子书=节目单阅读版（EbookLongRead 自持整页），走独立分支。 */
export function ArticlePage() {
  const { slug = '' } = useParams()
  const [state, setState] = useState<State>({ kind: 'loading' })

  // 换文回到页顶（路由变化 React Router 不自动滚）
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [slug])

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
  if (a.type === 'ebook') {
    return <EbookLongRead slug={a.slug} path={a.ebookPath ?? ''} />
  }
  const minutes = readingMinutes(a.bodyHtml ?? '')

  return (
    <main className="mx-auto w-full max-w-[50rem] px-3 py-7 sm:px-5 sm:py-10" data-testid="hub-article">
      <ReadingProgress />

      <nav className="mb-5 px-1">
        <Link className="text-[13px] text-snb-t3 transition-colors hover:text-snb-t1" to="/">
          ← {t('hub.article.backHome')}
        </Link>
      </nav>

      {/* 元信息条：来源 / 日期 ······ 阅读时长（档案条，随全站明暗） */}
      <div className="hub-metabar" data-testid="hub-byline">
        {a.sourceName && (
          <span>
            {t('hub.article.source')}
            {a.sourceName}
          </span>
        )}
        <time dateTime={a.publishedAt}>{formatDate(a.publishedAt)}</time>
        <span className="min">{t('hub.article.readingTime', { n: minutes })}</span>
      </div>

      {/* 文章纸张卡：暗色随全站变墨（panel 墨卡），内部令牌自然解析暗色档 */}
      <article className="hub-sheet" data-testid="hub-sheet">
        {a.coverUrl && (
          <figure className="hub-sheet-cover">
            <img alt="" loading="lazy" src={a.coverUrl} />
          </figure>
        )}
        <header>
          <div className="hub-sheet-eyebrow">{a.categoryName}</div>
          <h1 className="hub-sheet-title">{a.title}</h1>
        </header>

        {a.summary && (
          <aside className="hub-tldr" data-testid="hub-tldr">
            <span className="hub-tldr-tab">{t('hub.article.tldr')}</span>
            <p>{a.summary}</p>
          </aside>
        )}

        <div
          className="hub-prose"
          // 管线已预渲染并 sanitize；此处 DOMPurify 默认白名单再兜一层（纵深防御）
          dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(a.bodyHtml ?? '') }}
        />

        {(a.sourceName || a.tags.length > 0) && (
          <footer className="hub-sheet-foot">
            {a.sourceName && (
              <span data-testid="hub-source">
                {t('hub.article.source')}
                {a.sourceName}
                {a.sourceUrl && (
                  <>
                    {' · '}
                    <a href={a.sourceUrl} target="_blank" rel="noopener noreferrer">
                      {t('hub.article.original')}
                    </a>
                  </>
                )}
              </span>
            )}
            {a.tags.length > 0 && (
              <span className="tags">
                {a.tags.slice(0, 4).map((tag) => (
                  <span key={tag}>{tag}</span>
                ))}
              </span>
            )}
          </footer>
        )}
      </article>

      <nav className="mt-8 px-1">
        <Link className="text-[13px] text-snb-t3 transition-colors hover:text-snb-t1" to="/">
          ← {t('hub.article.backHome')}
        </Link>
      </nav>
    </main>
  )
}
