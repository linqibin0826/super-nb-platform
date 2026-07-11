import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import DOMPurify from 'dompurify'
import { Skeleton } from '../ui'
import { t } from '../i18n'

/** toc.json 契约（发布管线 buildToc 产出；dev 由 devBooks 现场转换吐同构数据）。 */
export interface BookToc {
  title: string
  subtitle?: string
  en?: string
  badge?: string
  author?: string
  metaLines?: string[]
  chapters: { index: number; title: string; en?: string }[]
}

type State =
  | { kind: 'loading' }
  | { kind: 'missing' }
  | { kind: 'ready'; toc: BookToc }

/**
 * 电子书正文（站内血统版，2026-07-11 站长拍板弃原样式）：发布管线已把书转换成
 * toc.json + 每章 hub-prose 语义片段，这里纯 fetch + 渲染，与普通文章同一排版路径，
 * 明暗双主题自动跟随。书首页＝书档案卡 + 目录；章节页＝章头 + 正文 + 上一/下一章。
 */
export function EbookBody({ slug, path, chapter }: { slug: string; path: string; chapter?: number }) {
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [bodyHtml, setBodyHtml] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    setState({ kind: 'loading' })
    if (!path) {
      setState({ kind: 'missing' })
      return
    }
    fetch(`/${path}/toc.json`)
      .then((res) => (res.ok ? res.json() : Promise.reject(new Error('http ' + res.status))))
      .then((toc: BookToc) => alive && setState({ kind: 'ready', toc }))
      .catch(() => alive && setState({ kind: 'missing' }))
    return () => {
      alive = false
    }
  }, [path])

  const total = state.kind === 'ready' ? state.toc.chapters.length : 0
  const valid = chapter !== undefined && chapter >= 1 && chapter <= total
  const current = valid && state.kind === 'ready' ? state.toc.chapters[chapter! - 1] : null

  useEffect(() => {
    let alive = true
    setBodyHtml(null)
    if (!current) return
    fetch(`/${path}/${current.index}.html`)
      .then((res) => (res.ok ? res.text() : Promise.reject(new Error('http ' + res.status))))
      .then((html) => alive && setBodyHtml(html))
      .catch(() => alive && setBodyHtml(''))
    return () => {
      alive = false
    }
  }, [path, current?.index])

  useEffect(() => {
    if (state.kind !== 'ready') return
    if (current) document.title = `${current.title} · ${state.toc.title} · ${t('hub.title')}`
    else document.title = `${state.toc.title} · ${t('hub.title')}`
  }, [state, current])

  if (state.kind === 'loading') {
    return (
      <div data-testid="hub-ebook-loading">
        <Skeleton className="mb-3 h-24 w-full rounded-2xl" />
        <Skeleton className="h-64 w-full rounded-2xl" />
      </div>
    )
  }

  if (state.kind === 'missing') {
    return (
      <div
        className="rounded-2xl border border-snb-hairline bg-snb-well/60 px-4 py-10 text-center text-sm text-snb-t2"
        data-testid="hub-reader-missing"
      >
        {t('hub.reader.missing')}
      </div>
    )
  }

  const toc = state.toc
  const prev = current && chapter! > 1 ? toc.chapters[chapter! - 2] : null
  const next = current && chapter! < total ? toc.chapters[chapter!] : null

  if (!current) {
    return (
      <div data-testid="hub-ebook">
        {(toc.subtitle || toc.badge || toc.author || toc.metaLines) && (
          <section
            className="mb-6 rounded-2xl border border-[rgb(var(--hub-accent)/0.28)] bg-[rgb(var(--hub-accent)/0.06)] p-5"
            data-testid="hub-book-meta"
          >
            {toc.subtitle && <p className="font-display text-[17px] font-semibold text-snb-t1">{toc.subtitle}</p>}
            {toc.en && <p className="mt-1 text-[13px] italic text-snb-t3">{toc.en}</p>}
            {toc.metaLines && (
              <ul className="mt-3 space-y-1 text-[13px] leading-relaxed text-snb-t2">
                {toc.metaLines.map((line) => (
                  <li key={line}>{line}</li>
                ))}
              </ul>
            )}
            {(toc.badge || toc.author) && (
              <p className="mt-3 text-xs text-snb-t3">
                {[toc.author, toc.badge].filter(Boolean).join(' · ')}
              </p>
            )}
          </section>
        )}

        <nav
          className="rounded-2xl border border-snb-hairline p-5"
          aria-label={t('hub.book.toc')}
          data-testid="hub-book-toc"
        >
          <h2 className="mb-2 flex items-baseline justify-between text-sm font-semibold text-snb-t1">
            {t('hub.book.toc')}
            <span className="font-mono text-xs font-normal text-snb-t3">{total}</span>
          </h2>
          <ol className="grid gap-x-6 sm:grid-cols-2">
            {toc.chapters.map((ch) => (
              <li key={ch.index}>
                <Link
                  className="block rounded-lg px-2 py-2.5 text-sm leading-snug text-snb-t1 transition-colors hover:bg-snb-well"
                  to={`/a/${slug}/${ch.index}`}
                >
                  {ch.title}
                </Link>
              </li>
            ))}
          </ol>
        </nav>
      </div>
    )
  }

  return (
    <div data-testid="hub-ebook">
      <nav className="mb-6 flex items-center justify-between text-[13px]" data-testid="hub-book-strip">
        <Link className="text-snb-t3 transition-colors hover:text-snb-t1" to={`/a/${slug}`}>
          ← {t('hub.book.toc')}
        </Link>
        <span className="font-mono text-xs text-snb-t3">
          {chapter} / {total}
        </span>
      </nav>

      <header className="mb-6 border-b border-snb-hairline pb-5">
        <h2 className="font-display text-[1.45rem] font-bold leading-snug text-snb-t1">{current.title}</h2>
        {current.en && <p className="mt-1 text-[13px] italic text-snb-t3">{current.en}</p>}
      </header>

      {bodyHtml === null ? (
        <div data-testid="hub-chapter-loading">
          <Skeleton className="mb-2 h-5 w-3/4" />
          <Skeleton className="mb-2 h-5 w-full" />
          <Skeleton className="h-64 w-full" />
        </div>
      ) : (
        <article
          className="hub-prose"
          // 管线转换已白名单重建 + sanitize；此处 DOMPurify 默认白名单再兜一层（与文章路径同纪律）
          dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(bodyHtml) }}
        />
      )}

      <nav
        className="mt-10 flex items-center justify-between gap-4 border-t border-snb-hairline pt-5 text-sm"
        data-testid="hub-book-pager"
      >
        {prev ? (
          <Link
            className="min-w-0 flex-1 truncate text-left text-snb-t2 transition-colors hover:text-snb-t1"
            to={`/a/${slug}/${prev.index}`}
            aria-label={t('hub.book.prev')}
          >
            ← {prev.title}
          </Link>
        ) : (
          <span className="flex-1" aria-hidden="true" />
        )}
        <Link className="shrink-0 text-snb-t3 transition-colors hover:text-snb-t1" to={`/a/${slug}`}>
          {t('hub.book.toc')}
        </Link>
        {next ? (
          <Link
            className="min-w-0 flex-1 truncate text-right text-snb-t2 transition-colors hover:text-snb-t1"
            to={`/a/${slug}/${next.index}`}
            aria-label={t('hub.book.next')}
          >
            {next.title} →
          </Link>
        ) : (
          <span className="flex-1" aria-hidden="true" />
        )}
      </nav>
    </div>
  )
}
