import { useEffect, type ComponentType } from 'react'
import { Link } from 'react-router-dom'
import DOMPurify from 'dompurify'
import { t } from '../i18n'
import type { BookChapter, BookData } from './api'
import { AgentsDiscoveryStage } from './AgentsDiscoveryStage'
import { chipLabel } from './BookIndex'

/** 机制舞台注册表（bespoke）：仅指定书的指定编号插入定制动图，非通用能力。 */
const BESPOKE_STAGE: Record<string, Record<string, ComponentType>> = {
  'codex-complete-guide-zh': { '05': AgentsDiscoveryStage },
}

function navLabel(c: BookChapter): string {
  return `${chipLabel(c)} ${c.title}`
}

/**
 * 电子书单部分独立页：顶条（←目录 · k/N）+ 章头（眉标/编号/衬线题/英文/导语）+ 正文（含机制舞台）
 * + 上一/下一部分导航。深链接 /a/:slug/:index，读到即写 localStorage 供续读。
 */
export function BookPart({ slug, book, chapter }: { slug: string; book: BookData; chapter: BookChapter }) {
  const idx = book.chapters.findIndex((c) => c.index === chapter.index)
  const prev = idx > 0 ? book.chapters[idx - 1] : null
  const next = idx < book.chapters.length - 1 ? book.chapters[idx + 1] : null
  const Stage = BESPOKE_STAGE[slug]?.[chapter.num ?? '']

  useEffect(() => {
    document.title = `${chipLabel(chapter)} ${chapter.title} · ${book.title} · ${t('hub.title')}`
    try {
      localStorage.setItem(`hub-book-pos:${slug}`, JSON.stringify({ index: chapter.index }))
    } catch {
      /* localStorage 不可用忽略 */
    }
  }, [slug, chapter, book.title])

  return (
    <main className="hub-book" data-testid="hub-ebook">
      <div className="hub-book-col hub-part">
        <nav className="hub-part-top" data-testid="hub-book-parttop">
          <Link className="back" to={`/a/${slug}`}>← {t('hub.book.contents')}</Link>
          <span className="pos">{idx + 1} / {book.chapters.length}</span>
        </nav>

        <header className="hub-sec-head">
          <div className="hub-eyebrow">{chapter.eyebrow}</div>
          <div className="hub-sec-titrow">
            {chapter.num && <span className="hub-numchip">{chipLabel(chapter)}</span>}
            <h1 className="hub-sec-title">{chapter.title}</h1>
          </div>
          {chapter.en && <p className="hub-sec-en">{chapter.en}</p>}
          {chapter.intro && <p className="hub-sec-intro">{chapter.intro}</p>}
        </header>

        <div className="hub-sec-body">
          {Stage && <Stage />}
          <div
            className="hub-prose"
            // 管线转换已白名单重建 + sanitize；前端 DOMPurify 再兜一层（放行姓名章 data-seal）
            dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(chapter.html, { ADD_ATTR: ['data-seal', 'target'] }) }}
          />
        </div>

        <nav className="hub-part-nav" data-testid="hub-book-partnav">
          {prev ? (
            <Link className="prev" to={`/a/${slug}/${prev.index}`} aria-label={t('hub.book.prevPart')}>
              ← {navLabel(prev)}
            </Link>
          ) : (
            <span className="prev" aria-hidden="true" />
          )}
          <Link className="toc" to={`/a/${slug}`}>{t('hub.book.contents')}</Link>
          {next ? (
            <Link className="next" to={`/a/${slug}/${next.index}`} aria-label={t('hub.book.nextPart')}>
              {navLabel(next)} →
            </Link>
          ) : (
            <span className="next" aria-hidden="true" />
          )}
        </nav>
      </div>
    </main>
  )
}
