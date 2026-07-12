import { useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { Skeleton } from '../ui'
import { t } from '../i18n'
import { getBook, type BookData } from './api'
import { BookIndex } from './BookIndex'
import { BookPart } from './BookPart'

type State = { kind: 'loading' } | { kind: 'missing' } | { kind: 'ready'; book: BookData }

/**
 * 电子书阅读编排器：一次 fetch book.json（全书），按路由 part 分流——
 * 无 part → 节目单目录（BookIndex）；有 part → 那一讲独立页（BookPart）。
 * 站长第六稿拍板「节目单」方向：每讲拆开、目录当分集页卖内容（非书目录）。
 */
export function EbookLongRead({ slug, path }: { slug: string; path: string }) {
  const { part: partRaw } = useParams()
  const partIndex = partRaw && /^\d+$/.test(partRaw) ? Number(partRaw) : undefined
  const [state, setState] = useState<State>({ kind: 'loading' })

  useEffect(() => {
    let alive = true
    setState({ kind: 'loading' })
    if (!path) {
      setState({ kind: 'missing' })
      return
    }
    getBook(path)
      .then((book) => alive && setState({ kind: 'ready', book }))
      .catch(() => alive && setState({ kind: 'missing' }))
    return () => {
      alive = false
    }
  }, [path])

  // 换部分/换书回页顶
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [partRaw, slug])

  if (state.kind === 'loading') {
    return (
      <main className="hub-book" data-testid="hub-ebook">
        <div className="hub-book-col">
          <Skeleton className="mb-4 mt-10 h-6 w-40" />
          <Skeleton className="mb-3 h-14 w-3/4" />
          <Skeleton className="mb-8 h-4 w-52" />
          <Skeleton className="h-64 w-full" />
        </div>
      </main>
    )
  }

  if (state.kind === 'missing') {
    return (
      <main className="hub-book" data-testid="hub-ebook">
        <div className="hub-book-col flex min-h-[50vh] flex-col items-center justify-center gap-4 text-center">
          <p className="text-snb-t2" data-testid="hub-reader-missing">{t('hub.reader.missing')}</p>
          <Link className="text-sm text-snb-t2 underline underline-offset-4 hover:text-snb-t1" to="/">
            {t('hub.article.backHome')}
          </Link>
        </div>
      </main>
    )
  }

  const book = state.book
  const chapter = partIndex ? book.chapters.find((c) => c.index === partIndex) : undefined
  if (partIndex && !chapter) {
    return <Navigate to={`/a/${slug}`} replace /> // 无效 part 回目录
  }
  return chapter ? <BookPart slug={slug} book={book} chapter={chapter} /> : <BookIndex slug={slug} book={book} />
}
