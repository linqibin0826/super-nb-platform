import { useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { Skeleton } from '../../ui'
import { t } from '../../i18n'
import { getArticle, NotFoundError, type ArticleDetail } from '../api'

type State =
  | { kind: 'loading' }
  | { kind: 'ready'; article: ArticleDetail }
  | { kind: 'missing'; article: ArticleDetail }
  | { kind: 'notFound' }
  | { kind: 'error' }

/**
 * 电子书阅读页：顶部极简条 + 全视口同源 iframe（/books/<slug>.html 由 Caddy 静态服务）。
 * 渲染 iframe 前先 HEAD 探活——文件缺失给明确错误而非白 iframe；保留站点壳为二期解锁拦截留位。
 */
export function ReaderPage() {
  const { slug = '' } = useParams()
  const [state, setState] = useState<State>({ kind: 'loading' })

  useEffect(() => {
    let alive = true
    setState({ kind: 'loading' })
    getArticle(slug)
      .then(async (article) => {
        if (!alive) return
        if (article.type !== 'ebook' || !article.ebookPath) {
          setState({ kind: 'ready', article }) // 非 ebook 由下方 Navigate 处理
          return
        }
        const head = await fetch('/' + article.ebookPath, { method: 'HEAD' })
        if (!alive) return
        setState(head.ok ? { kind: 'ready', article } : { kind: 'missing', article })
      })
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
      <main className="flex h-screen flex-col p-4" data-testid="hub-reader">
        <Skeleton className="mb-3 h-10" />
        <Skeleton className="flex-1" />
      </main>
    )
  }

  if (state.kind === 'notFound' || state.kind === 'error') {
    return (
      <main className="flex h-screen flex-col items-center justify-center gap-4" data-testid="hub-reader">
        <p className="text-snb-t2" data-testid={state.kind === 'notFound' ? 'hub-not-found' : 'hub-error'}>
          {t(state.kind === 'notFound' ? 'hub.article.notFound' : 'hub.list.error')}
        </p>
        <Link className="text-sm text-snb-t2 underline underline-offset-4 hover:text-snb-t1" to="/">
          {t('hub.article.backHome')}
        </Link>
      </main>
    )
  }

  const a = state.article
  if (a.type !== 'ebook') {
    return <Navigate to={`/a/${a.slug}`} replace />
  }

  const bookUrl = '/' + a.ebookPath

  if (state.kind === 'missing') {
    return (
      <main className="flex h-screen flex-col items-center justify-center gap-4" data-testid="hub-reader">
        <p className="text-snb-t2" data-testid="hub-reader-missing">{t('hub.reader.missing')}</p>
        <Link className="text-sm text-snb-t2 underline underline-offset-4 hover:text-snb-t1" to="/">
          {t('hub.article.backHome')}
        </Link>
      </main>
    )
  }

  return (
    <main className="flex h-screen flex-col bg-snb-bg" data-testid="hub-reader">
      <div className="flex h-12 shrink-0 items-center justify-between gap-3 border-b border-snb-hairline px-4">
        <Link
          className="text-sm text-snb-t2 hover:text-snb-t1"
          to="/"
          data-testid="hub-reader-back"
        >
          ← {t('hub.reader.back')}
        </Link>
        <span className="truncate text-sm font-semibold text-snb-t1">{a.title}</span>
        <a
          className="shrink-0 text-sm text-snb-t2 underline-offset-4 hover:text-snb-t1 hover:underline"
          href={bookUrl}
          target="_blank"
          rel="noopener"
          data-testid="hub-reader-open"
        >
          {t('hub.reader.openNew')}
        </a>
      </div>
      {/* 书自带完整浅色纸面样式，iframe 底色固定白，不随站点暗色 */}
      <iframe title={a.title} src={bookUrl} className="w-full flex-1 border-0 bg-white" />
    </main>
  )
}
