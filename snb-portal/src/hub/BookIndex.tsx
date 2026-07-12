import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { t } from '../i18n'
import type { BookChapter, BookData } from './api'

/** 编号标签：前言=序、章节=§NN、附录=X。 */
export function chipLabel(c: BookChapter): string {
  if (!c.num) return '序'
  return c.kind === 'appendix' ? c.num : `§${c.num}`
}

/**
 * 电子书目录：书封面 masthead + 14 部分封面卡片墙（点卡进那部分独立页）。
 * 把整本书当成一个「多篇的迷你专栏」，与站内卡片流一脉相承。
 */
export function BookIndex({ slug, book }: { slug: string; book: BookData }) {
  const [resume, setResume] = useState<BookChapter | null>(null)

  useEffect(() => {
    document.title = `${book.title} · ${t('hub.title')}`
    try {
      const saved = JSON.parse(localStorage.getItem(`hub-book-pos:${slug}`) || 'null')
      const ch = saved && book.chapters.find((c) => c.index === saved.index)
      if (ch && ch.index !== book.chapters[0].index) setResume(ch)
    } catch {
      /* localStorage 不可用忽略 */
    }
  }, [slug, book])

  const first = book.chapters[0]

  return (
    <main className="hub-book" data-testid="hub-ebook">
      <div className="hub-book-col">
        <header className="hub-mast">
          {book.badge && <span className="hub-badge">{book.badge}</span>}
          <h1 className="hub-mast-title">{book.title}</h1>
          {book.subtitle && <p className="hub-mast-sub">{book.subtitle}</p>}
          {book.en && <p className="hub-mast-en">{book.en}</p>}
          {(book.author || book.metaLines) && (
            <p className="hub-mast-by">
              {[book.author && `${book.author} 著`, ...(book.metaLines ?? [])].filter(Boolean).join(' · ')}
            </p>
          )}
          <div className="hub-stats" data-testid="hub-book-stats">
            <span><b>{book.chapters.length}</b> {t('hub.book.parts')}</span>
            <i aria-hidden="true">·</i>
            <span><b>{book.totalMinutes}</b> {t('hub.book.minutes')}</span>
          </div>
          {first && (
            <div className="hub-mast-cta">
              <Link className="hub-btn-read" to={`/a/${slug}/${first.index}`}>{t('hub.book.start')} →</Link>
            </div>
          )}
        </header>

        {resume && (
          <Link className="hub-resume" data-testid="hub-book-resume" to={`/a/${slug}/${resume.index}`}>
            <span className="dot" aria-hidden="true" />
            <span>{t('hub.book.resumeAt')} <b>{chipLabel(resume)} {resume.title}</b></span>
            <span className="go">{t('hub.book.resumeGo')} →</span>
          </Link>
        )}

        <nav className="hub-cards-wall" aria-label={t('hub.book.contents')} data-testid="hub-book-index">
          {book.chapters.map((c) => (
            <Link key={c.index} className="hub-part-card" to={`/a/${slug}/${c.index}`}>
              <span className="pc-num">{chipLabel(c)}</span>
              <span className="pc-eyebrow">{c.eyebrow}</span>
              <span className="pc-title">{c.title}</span>
              {c.en && <span className="pc-en">{c.en}</span>}
              <span className="pc-min">{c.minutes} {t('hub.book.min')}</span>
            </Link>
          ))}
        </nav>

        <footer className="hub-book-foot">
          <p>{t('hub.book.footNote')}</p>
        </footer>
      </div>
    </main>
  )
}
