import { useEffect, useState } from 'react'
import type { BookChapter } from './api'
import { t } from '../i18n'

/** 刻度标签：前言/§NN/附录X。 */
function tickLabel(c: BookChapter): string {
  if (!c.num) return '前言'
  return c.kind === 'appendix' ? `附录 ${c.num}` : `§${c.num}`
}
function pillLabel(c: BookChapter | undefined): string {
  if (!c) return ''
  if (!c.num) return '前言'
  return c.kind === 'appendix' ? `附${c.num}` : `§${c.num}`
}

/**
 * 时码轨：全书 chapters 的常驻侧轨（桌面竖轨 / 移动右下浮标），随滚动实时标「你在哪」。
 * 点刻度平滑锚点跳转（非路由）；active 变化写 localStorage 供续读。收编旧版「目录+翻页条+我在哪」三件道具。
 */
export function TimecodeRail({ slug, chapters }: { slug: string; chapters: BookChapter[] }) {
  const [active, setActive] = useState(chapters[0]?.index ?? 1)

  useEffect(() => {
    const secs = chapters
      .map((c) => document.getElementById(`s${c.index}`))
      .filter((el): el is HTMLElement => el != null)
    if (secs.length === 0) return
    const io = new IntersectionObserver(
      (ents) => {
        const vis = ents
          .filter((e) => e.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)
        if (vis.length > 0) setActive(Number((vis[0].target as HTMLElement).dataset.bookSec))
      },
      { rootMargin: '-20% 0px -60% 0px', threshold: 0 },
    )
    secs.forEach((s) => io.observe(s))
    return () => io.disconnect()
  }, [chapters])

  useEffect(() => {
    try {
      localStorage.setItem(`hub-book-pos:${slug}`, JSON.stringify({ index: active }))
    } catch {
      /* localStorage 不可用忽略 */
    }
  }, [slug, active])

  const jump = (index: number) => document.getElementById(`s${index}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  const activePos = chapters.findIndex((c) => c.index === active) + 1
  const activeCh = chapters[activePos - 1]

  return (
    <>
      <nav className="hub-rail" aria-label={t('hub.book.railLabel')} data-testid="hub-book-rail">
        <div className="hub-rail-track">
          {chapters.map((c) => (
            <button
              key={c.index}
              type="button"
              className={`hub-tick${c.index === active ? ' is-active' : ''}`}
              onClick={() => jump(c.index)}
              aria-current={c.index === active ? 'true' : undefined}
            >
              <span className="lbl">{tickLabel(c)}</span>
              <span className="mark" aria-hidden="true" />
            </button>
          ))}
        </div>
      </nav>

      <button
        type="button"
        className="hub-pill"
        data-testid="hub-book-pill"
        onClick={() => jump((chapters[activePos] ?? chapters[0]).index)}
        aria-label={t('hub.book.railLabel')}
      >
        <span className="dot" aria-hidden="true" />
        <b>{pillLabel(activeCh)}</b>
        <span>· {activePos}/{chapters.length}</span>
      </button>
    </>
  )
}
