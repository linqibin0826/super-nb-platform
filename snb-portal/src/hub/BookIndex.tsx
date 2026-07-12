import { Fragment, useEffect, useState, type CSSProperties } from 'react'
import { Link } from 'react-router-dom'
import { t } from '../i18n'
import type { BookChapter, BookData } from './api'
import { actName, lessonLabel, loadProgress, numLabel, type BookProgress } from './bookShared'

/** 封面点题带 + 幕间注（bespoke 编辑性文案，数据不携带；未登记的书自动省略）。 */
const BESPOKE_COVER: Record<string, { forms?: string[]; formsCaption?: string; authorUrl?: string; actNotes?: Record<string, string> }> = {
  'codex-complete-guide-zh': {
    forms: ['CLI', 'App', 'Cloud', 'IDE', 'Chrome'],
    formsCaption: '五种形态 · 一个系统',
    authorUrl: 'https://x.com/AlchainHust',
    actNotes: { 序: '作者的话', 基础: '装上、跑通、做出第一个东西', 进阶: '四种形态逐个吃透', 实战: '从扩展能力到完整产品', 附录: '用到再翻' },
  },
}

interface Act {
  key: string
  name: string
  stat: string
  note?: string
  chapters: BookChapter[]
}

/** 连续同眉标聚成一幕；幕头带真实统计（第 a – b 讲 · Σ 分钟）。 */
function groupActs(book: BookData, notes?: Record<string, string>): Act[] {
  const acts: Act[] = []
  for (const c of book.chapters) {
    const key = c.kind === 'chapter' ? c.eyebrow : c.kind
    const last = acts[acts.length - 1]
    if (last?.key === key) {
      last.chapters.push(c)
      continue
    }
    acts.push({ key, name: actName(c), stat: '', note: notes?.[c.eyebrow], chapters: [c] })
  }
  for (const a of acts) {
    const mins = a.chapters.reduce((s, c) => s + c.minutes, 0)
    const kind = a.chapters[0].kind
    if (kind === 'chapter') {
      const from = Number(a.chapters[0].num)
      const to = Number(a.chapters[a.chapters.length - 1].num)
      a.stat = from === to ? t('hub.book.lessonsOne', { a: from, m: mins }) : t('hub.book.lessonsRange', { a: from, b: to, m: mins })
    } else if (kind === 'appendix') {
      a.stat = t('hub.book.refsStat', { n: a.chapters.length, m: mins })
    } else if (!a.note) {
      a.note = t('hub.book.authorNote')
    }
  }
  return acts
}

/**
 * 电子书目录页：刊头 + 节目单——每讲一整行（衬线序号 / 讲题 / 作者导语当钩子 /
 * 与时长成正比的墨条，读过填实），幕间标题带真实统计。进度长在列表里。
 */
export function BookIndex({ slug, book }: { slug: string; book: BookData }) {
  const [prog, setProg] = useState<BookProgress>({ pos: null, read: new Set() })

  useEffect(() => {
    document.title = `${book.title} · ${t('hub.title')}`
    setProg(loadProgress(slug))
  }, [slug, book])

  const bespoke = BESPOKE_COVER[slug]
  const acts = groupActs(book, bespoke?.actNotes)
  const first = book.chapters[0]
  const resume = prog.pos ? book.chapters.find((c) => c.index === prog.pos!.index) : undefined
  const maxMin = Math.max(...book.chapters.map((c) => c.minutes), 1)
  let seq = 0

  return (
    <main className="hub-book" data-testid="hub-ebook">
      <div className="hub-book-col hub-index">
        <header className="hub-cover">
          <div className="hub-cover-eyebrow">
            <span className="dot" aria-hidden="true" />
            {[t('hub.book.serial'), book.badge].filter(Boolean).join(' · ')}
          </div>
          <h1 className="hub-cover-title">{book.title}</h1>
          {book.subtitle && <p className="hub-cover-deck">{book.subtitle}</p>}
          {bespoke?.forms && (
            <div className="hub-forms" aria-hidden="true">
              {bespoke.forms.map((f, i) => (
                <Fragment key={f}>
                  {i > 0 && <span className="fa">→</span>}
                  <span className="f">{f}</span>
                </Fragment>
              ))}
              {bespoke.formsCaption && <span className="fcap">{bespoke.formsCaption}</span>}
            </div>
          )}
          {first && (
            <div className="hub-cta">
              {resume ? (
                <>
                  <Link className="hub-btn-read" to={`/a/${slug}/${resume.index}`} data-testid="hub-book-resume">
                    {t('hub.book.continueAt', { l: lessonLabel(resume) })}
                    <span className="sub">{resume.title.split(/[：:]/)[0]}</span>
                  </Link>
                  <Link className="hub-cta-ghost" to={`/a/${slug}/${first.index}`}>{t('hub.book.restart')}</Link>
                </>
              ) : (
                <Link className="hub-btn-read" to={`/a/${slug}/${first.index}`}>{t('hub.book.startFirst')} →</Link>
              )}
            </div>
          )}
        </header>

        <nav data-testid="hub-book-index" aria-label={t('hub.book.allParts', { n: book.chapters.length })}>
          {acts.map((act) => (
            <section key={act.key}>
              <div className="hub-act">
                <span className="name">{act.name}</span>
                {act.stat && <span>{act.stat}</span>}
                {act.note && <span className="desc">{act.note}</span>}
              </div>
              {act.chapters.map((c) => {
                const isResume = prog.pos?.index === c.index
                const wasRead = prog.read.has(c.index)
                const compact = c.kind !== 'chapter'
                const cls = ['hub-row', compact && 'compact', wasRead && !isResume && 'read', isResume && 'resume']
                  .filter(Boolean)
                  .join(' ')
                const pct = isResume
                  ? Math.max(12, Math.round((prog.pos?.at ?? 0) * 100))
                  : Math.max(14, Math.round((c.minutes / maxMin) * 100))
                const state = isResume ? t('hub.book.lastRead') : wasRead ? t('hub.book.read') : ''
                return (
                  <Link key={c.index} className={cls} style={{ '--i': seq++ } as CSSProperties} to={`/a/${slug}/${c.index}`}>
                    <span className="num">{numLabel(c)}</span>
                    <span className="rbody">
                      <span className="rtitle">{c.title}</span>
                      {c.intro && !compact && <span className="rhook">{c.intro}</span>}
                    </span>
                    <span className="rmeta">
                      <span className="rmin">
                        {state && <b className="state">{state} · </b>}
                        {t('hub.book.minutes', { m: c.minutes })}
                      </span>
                      <span className="rtrack"><i style={{ width: `${pct}%` }} /></span>
                    </span>
                  </Link>
                )
              })}
            </section>
          ))}
        </nav>

        <footer className="hub-book-foot" data-testid="hub-book-credit">
          <p>
            {t('hub.book.adaptedFrom', { a: book.author ?? '' })}
            {bespoke?.authorUrl && (
              <>
                {' · '}
                <a href={bespoke.authorUrl} target="_blank" rel="noopener noreferrer">
                  {t('hub.book.seeOriginal')}
                </a>
              </>
            )}
          </p>
        </footer>
      </div>
    </main>
  )
}
