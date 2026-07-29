import { useEffect, useRef, useState, type RefObject } from 'react'
import { th } from './hubMessages'

export interface Heading {
  id: string
  text: string
}

/**
 * 从管线预渲染的正文里抽 h2 当迷你目录（只列 h2，h3 不进——长文要的是路标不是全景）。
 * 正文是 dangerouslySetInnerHTML，React 不管这些节点：这里顺手补 id（锚点跳转用）
 * 和 data-h2 标记（滚动态定位用），html 变了 React 重设 innerHTML、随 deps 重抽。
 */
export function useHeadings(ref: RefObject<HTMLElement | null>, deps: unknown[]): Heading[] {
  const [items, setItems] = useState<Heading[]>([])
  // eslint-disable-next-line react-hooks/exhaustive-deps -- 依赖数组由调用方按正文内容传入（不是缺省）
  useEffect(() => {
    const root = ref.current
    if (!root) {
      setItems([])
      return
    }
    const found = Array.from(root.querySelectorAll('h2')).map((h, i) => {
      if (!h.id) h.id = `hub-h2-${i}`
      h.setAttribute('data-h2', '')
      return { id: h.id, text: (h.textContent ?? '').trim() }
    })
    setItems(found)
    // eslint-disable-next-line react-hooks/exhaustive-deps -- 同上：deps 由调用方传，不是数组字面量
  }, deps)
  return items
}

/**
 * 阅读滚动态：当前 h2 下标 + 已读百分比。rAF 节流 + 值不变不 setState，
 * 一次滚动最多触发「跨过一个 h2」和「百分比进一位」两种重渲染。
 */
export function useReadingScroll(count: number): { active: number; pct: number } {
  const [state, setState] = useState({ active: 0, pct: 0 })
  const raf = useRef(0)

  useEffect(() => {
    const measure = () => {
      raf.current = 0
      const de = document.documentElement
      const max = de.scrollHeight - de.clientHeight
      const pct = max > 0 ? Math.round(Math.min(1, Math.max(0, de.scrollTop / max)) * 100) : 0
      let active = 0
      // 顶栏 64 + 进度条 4 + 目录条 44 ≈ 112，越过 180 就算「读到这一节了」
      document.querySelectorAll('[data-h2]').forEach((h, i) => {
        if (h.getBoundingClientRect().top < 180) active = i
      })
      setState((s) => (s.active === active && s.pct === pct ? s : { active, pct }))
    }
    const onScroll = () => {
      if (!raf.current) raf.current = requestAnimationFrame(measure)
    }
    measure()
    window.addEventListener('scroll', onScroll, { passive: true })
    window.addEventListener('resize', onScroll)
    return () => {
      if (raf.current) cancelAnimationFrame(raf.current)
      window.removeEventListener('scroll', onScroll)
      window.removeEventListener('resize', onScroll)
    }
  }, [count])

  return state
}

/** 宽屏迷你目录：填的是 1180 以上两侧的空边距，正文 738px 一个像素不动。 */
export function ArticleToc({
  items,
  active,
  pct,
  chars,
}: {
  items: Heading[]
  active: number
  pct: number
  chars: number
}) {
  return (
    <aside className="hub-toc" data-testid="hub-toc">
      {items.length > 1 && (
        <div className="hub-toc-inner">
          <div className="lb">{th('art.tocLabel')}</div>
          <nav aria-label={th('art.tocLabel')}>
            {items.map((h, i) => (
              <a key={h.id} href={`#${h.id}`} className={i === active ? 'on' : undefined}>
                {h.text}
              </a>
            ))}
          </nav>
          <div className="pct">
            {th('art.tocReadPre')} <b>{pct}%</b> {th('art.tocReadChars', { n: chars })}
          </div>
        </div>
      )}
    </aside>
  )
}

/** 窄屏目录条：贴在进度条下面的 44 高常驻条，展开 420ms 落定，条目整行 44 热区。 */
export function ArticleTocBar({ items, active }: { items: Heading[]; active: number }) {
  const [open, setOpen] = useState(false)
  if (items.length < 2) return null
  return (
    <div className="hub-tocbar">
      <button type="button" className="hub-tocbar-btn" aria-expanded={open} onClick={() => setOpen((v) => !v)}>
        <span className="cur">
          <span className="k">{th('art.tocBar')}</span>
          <span className="nm">{items[active]?.text ?? items[0].text}</span>
        </span>
        <span className="tg">{open ? th('art.tocClose') : th('art.tocOpen', { n: items.length })}</span>
      </button>
      <div className={open ? 'hub-tocbar-panel open' : 'hub-tocbar-panel'}>
        <nav aria-label={th('art.tocLabel')}>
          {items.map((h, i) => (
            <a key={h.id} href={`#${h.id}`} className={i === active ? 'on' : undefined} onClick={() => setOpen(false)}>
              {h.text}
            </a>
          ))}
        </nav>
      </div>
    </div>
  )
}
