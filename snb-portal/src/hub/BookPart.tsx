import { useEffect, useMemo, type ComponentType } from 'react'
import { Link } from 'react-router-dom'
import DOMPurify from 'dompurify'
import { t } from '../i18n'
import type { BookChapter, BookData } from './api'
import { AgentsDiscoveryStage } from './AgentsDiscoveryStage'
import { actName, lessonLabel, loadProgress, markRead, numLabel, savePos, touchPos } from './bookShared'

/** 机制舞台注册表（bespoke）：仅指定书的指定编号插入定制动图，非通用能力。 */
const BESPOKE_STAGE: Record<string, Record<string, ComponentType>> = {
  'codex-complete-guide-zh': { '05': AgentsDiscoveryStage },
}

/** 讲头眉标：开篇 / {眉标}篇 · 第 N 讲 / 附录 X。 */
function crumb(c: BookChapter): string {
  if (c.kind === 'preface') return t('hub.book.opening')
  if (c.kind === 'appendix') return lessonLabel(c)
  return `${actName(c)} · ${lessonLabel(c)}`
}

/**
 * 讲次页：顶条（← 全部 N 篇 + 14 格进度刻度）在桌面，讲头/正文（含机制舞台）/「下一讲」
 * 邀请块包进与文章页同款纸张卡（hub-sheet，暗色随全站=panel 墨卡）。滚动比落
 * localStorage（续读条带进度），读过 85% 记已读。
 */
export function BookPart({ slug, book, chapter }: { slug: string; book: BookData; chapter: BookChapter }) {
  const idx = book.chapters.findIndex((c) => c.index === chapter.index)
  const prev = idx > 0 ? book.chapters[idx - 1] : null
  const next = idx < book.chapters.length - 1 ? book.chapters[idx + 1] : null
  const Stage = BESPOKE_STAGE[slug]?.[chapter.num ?? '']
  // eslint-disable-next-line react-hooks/exhaustive-deps -- 换讲时重读已读集（markRead 同讲内不需要回显）
  const read = useMemo(() => loadProgress(slug).read, [slug, chapter.index])

  useEffect(() => {
    document.title = `${lessonLabel(chapter)} ${chapter.title} · ${book.title} · ${t('hub.title')}`
  }, [chapter, book.title])

  // 进度：进讲记位置（同讲保留滚动比）；滚动 rAF 节流写比值，≥85% 记已读；短页直接记已读。
  useEffect(() => {
    touchPos(slug, chapter.index)
    let raf = 0
    const measure = () => {
      raf = 0
      const el = document.documentElement
      const span = el.scrollHeight - el.clientHeight
      if (span < 80) {
        markRead(slug, chapter.index)
        return
      }
      const at = Math.min(1, Math.max(0, el.scrollTop / span))
      savePos(slug, chapter.index, at)
      if (at >= 0.85) markRead(slug, chapter.index)
    }
    const onScroll = () => {
      if (!raf) raf = requestAnimationFrame(measure)
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => {
      window.removeEventListener('scroll', onScroll)
      if (raf) cancelAnimationFrame(raf)
    }
  }, [slug, chapter.index])

  return (
    <main className="hub-book" data-testid="hub-ebook">
      <div className="hub-book-col hub-part">
        <nav className="hub-part-top" data-testid="hub-book-parttop">
          <Link className="back" to={`/a/${slug}`}>← {t('hub.book.allParts', { n: book.chapters.length })}</Link>
          <span className="ticks" aria-hidden="true">
            {book.chapters.map((c) => (
              <i key={c.index} className={c.index === chapter.index ? 'cur' : read.has(c.index) ? 'done' : ''} />
            ))}
          </span>
        </nav>

        <article className="hub-sheet hub-part-sheet">
        <header className="hub-sec-head">
          <div className="hub-eyebrow">
            {crumb(chapter)} <span className="min">／ {t('hub.book.minutes', { m: chapter.minutes })}</span>
          </div>
          <h1 className="hub-sec-title">{chapter.title}</h1>
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

        <nav className="hub-part-next" data-testid="hub-book-partnav">
          {next ? (
            <>
              <div className="label">
                {next.kind === 'chapter' ? t('hub.book.next') : t('hub.book.nextUp')}
                <span className="nmin">{actName(next)} · {t('hub.book.minutes', { m: next.minutes })}</span>
              </div>
              <Link className="next-link" to={`/a/${slug}/${next.index}`}>
                <span className="nt">{numLabel(next)} · {next.title}</span>
                <span className="na" aria-hidden="true">→</span>
              </Link>
              {next.intro && <div className="next-hook">{next.intro}</div>}
            </>
          ) : (
            <>
              <div className="label">{t('hub.book.done')}</div>
              <Link className="next-link" to={`/a/${slug}`}>
                <span className="nt">{t('hub.book.backToAll', { n: book.chapters.length })}</span>
                <span className="na" aria-hidden="true">→</span>
              </Link>
            </>
          )}
        </nav>
        </article>

        <div className="hub-part-foot">
          {prev ? (
            <Link to={`/a/${slug}/${prev.index}`}>← {numLabel(prev)} · {prev.title}</Link>
          ) : (
            <span aria-hidden="true" />
          )}
          <Link to={`/a/${slug}`}>{t('hub.book.allParts', { n: book.chapters.length })}</Link>
        </div>
      </div>
    </main>
  )
}
