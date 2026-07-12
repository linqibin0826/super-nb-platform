import { useEffect, useState, type ComponentType } from 'react'
import { Link } from 'react-router-dom'
import DOMPurify from 'dompurify'
import { Skeleton } from '../ui'
import { t } from '../i18n'
import { getBook, type BookChapter, type BookData } from './api'
import { TimecodeRail } from './TimecodeRail'
import { AgentsDiscoveryStage } from './AgentsDiscoveryStage'

type State = { kind: 'loading' } | { kind: 'missing' } | { kind: 'ready'; book: BookData }

/** 机制舞台注册表（bespoke）：仅指定书的指定编号插入定制动图，非通用能力。 */
const BESPOKE_STAGE: Record<string, Record<string, ComponentType>> = {
  'codex-complete-guide-zh': { '05': AgentsDiscoveryStage },
}

function chipLabel(c: BookChapter): string {
  return c.kind === 'appendix' ? (c.num ?? '') : `§${c.num}`
}
function resumeLabel(c: BookChapter): string {
  const pfx = c.num ? (c.kind === 'appendix' ? `附录 ${c.num}` : `§${c.num}`) + ' ' : ''
  return pfx + c.title
}

/**
 * 电子书「一镜到底」长读版：一次 fetch book.json，一条流渲染全书 + 时码轨。
 * 无目录落地页、无章节路由、无翻页器——深链接靠锚点 #s{index}。自持 masthead（不走文章页头）。
 */
export function EbookLongRead({ slug, path }: { slug: string; path: string }) {
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [scan, setScan] = useState(false)
  const [resume, setResume] = useState<BookChapter | null>(null)

  useEffect(() => {
    let alive = true
    setState({ kind: 'loading' })
    setResume(null)
    if (!path) {
      setState({ kind: 'missing' })
      return
    }
    getBook(path)
      .then((book) => {
        if (!alive) return
        // 续读位置必须在此刻读取——早于 TimecodeRail 挂载即写 localStorage 覆盖存档
        try {
          const saved = JSON.parse(localStorage.getItem(`hub-book-pos:${slug}`) || 'null')
          const ch = saved && book.chapters.find((c) => c.index === saved.index)
          if (ch && ch.index > book.chapters[0].index) setResume(ch)
        } catch {
          /* localStorage 不可用忽略 */
        }
        setState({ kind: 'ready', book })
      })
      .catch(() => alive && setState({ kind: 'missing' }))
    return () => {
      alive = false
    }
  }, [path, slug])

  useEffect(() => {
    if (state.kind === 'ready') document.title = `${state.book.title} · ${t('hub.title')}`
  }, [state])

  if (state.kind === 'loading') {
    return (
      <main className="hub-book" data-testid="hub-ebook">
        <div className="hub-book-col">
          <Skeleton className="mb-4 mt-10 h-6 w-40" />
          <Skeleton className="mb-3 h-14 w-3/4" />
          <Skeleton className="mb-8 h-4 w-52" />
          <Skeleton className="h-72 w-full" />
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
  const jump = (index: number) => document.getElementById(`s${index}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  // 姓名章印文字符（作者首字）经 CSS 变量注入引语卡；缺省用左引号
  const stampStyle = { ['--book-stamp' as string]: JSON.stringify((book.author ?? '“').slice(0, 1)) }

  return (
    <main className={`hub-book${scan ? ' is-scan' : ''}`} style={stampStyle} data-testid="hub-ebook">
      <TimecodeRail slug={slug} chapters={book.chapters} />

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
          <div className="hub-mast-cta">
            <button type="button" className="hub-btn-read" onClick={() => jump(book.chapters[0].index)}>
              {t('hub.book.start')} ↓
            </button>
            <button type="button" className="hub-link-scan" onClick={() => setScan(true)}>
              {t('hub.book.scanHint')}
            </button>
          </div>
        </header>

        {resume && !scan && (
          <button type="button" className="hub-resume" data-testid="hub-book-resume" onClick={() => jump(resume.index)}>
            <span className="dot" aria-hidden="true" />
            <span>{t('hub.book.resumeAt')} <b>{resumeLabel(resume)}</b></span>
            <span className="go">{t('hub.book.resumeGo')} →</span>
          </button>
        )}

        {scan && (
          <div className="hub-scan-banner" data-testid="hub-book-scan">
            <span>{t('hub.book.scanBanner')}</span>
            <button type="button" className="x" onClick={() => setScan(false)}>{t('hub.book.scanExit')}</button>
          </div>
        )}

        <article>
          {book.chapters.map((ch, i) => {
            const Stage = BESPOKE_STAGE[slug]?.[ch.num ?? '']
            return (
              <div key={ch.index}>
                {i > 0 && (
                  <div className="hub-breath" aria-hidden="true">
                    <span>{ch.num ?? ''}</span>
                  </div>
                )}
                <section className="hub-sec" id={`s${ch.index}`} data-book-sec={ch.index}>
                  <div className="hub-sec-head">
                    <div className="hub-eyebrow">{ch.eyebrow}</div>
                    <div className="hub-sec-titrow">
                      {ch.num && <span className="hub-numchip">{chipLabel(ch)}</span>}
                      <h2 className="hub-sec-title">{ch.title}</h2>
                    </div>
                    {ch.en && <p className="hub-sec-en">{ch.en}</p>}
                    {ch.intro && <p className="hub-sec-intro">{ch.intro}</p>}
                  </div>
                  <div className="hub-sec-body">
                    {Stage && <Stage />}
                    <div
                      className="hub-prose"
                      // 管线转换已白名单重建 + sanitize；前端 DOMPurify 再兜一层（放行姓名章 data-seal）
                      dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(ch.html, { ADD_ATTR: ['data-seal', 'target'] }) }}
                    />
                  </div>
                </section>
              </div>
            )
          })}
        </article>

        <footer className="hub-book-foot">
          <p>{t('hub.book.footNote')}</p>
        </footer>
      </div>
    </main>
  )
}
