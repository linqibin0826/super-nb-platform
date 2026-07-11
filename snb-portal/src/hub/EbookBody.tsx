import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Skeleton } from '../ui'
import { t } from '../i18n'

/** 目录条目：index 从 1 起，即章节路由段 /a/:slug/:index。 */
export interface BookChapter {
  index: number
  title: string
}

type Probe = 'probing' | 'ok' | 'missing'

/**
 * 中和书内 vh 单位。iframe 全高展开后 vh 改随 iframe 自身高度解析：100vh 封面 → 内容更高 →
 * iframe 再拉高 → 正反馈直至浏览器 2^25px 钳制（实测撞过）。两档策略：
 * - min-height 含 vh：整条移除——「满屏页」（封面/封底）塌成内容高，正合普通教程的卷首块观感；
 * - 其它属性含 vh：钉成加载时真视口像素（保守兜底，防塌出裂版）。
 * 注意：iframe 的 CSSRule 属于子 realm，不能 instanceof 主窗口构造器，一律鸭子类型判断。
 */
function neutralizeViewportUnits(doc: Document) {
  const px = window.innerHeight / 100
  const pin = (v: string) => v.replace(/(\d+(?:\.\d+)?)vh/g, (_, n: string) => `${Math.round(Number(n) * px)}px`)

  const fixStyle = (style: CSSStyleDeclaration) => {
    const props: string[] = []
    for (let i = 0; i < style.length; i++) props.push(style.item(i))
    for (const prop of props) {
      const val = style.getPropertyValue(prop)
      if (!/\dvh/.test(val)) continue
      if (prop === 'min-height') style.removeProperty(prop)
      else style.setProperty(prop, pin(val), style.getPropertyPriority(prop))
    }
  }
  const walkRules = (rules: CSSRuleList) => {
    for (const rule of Array.from(rules)) {
      const style = (rule as CSSStyleRule).style
      if (style) fixStyle(style)
      const nested = (rule as CSSMediaRule).cssRules
      if (nested) walkRules(nested)
    }
  }
  for (const sheet of Array.from(doc.styleSheets)) {
    try {
      walkRules(sheet.cssRules)
    } catch {
      /* 外链跨源样式表不可读——自包含书不会有，兜底跳过 */
    }
  }
  for (const el of Array.from(doc.querySelectorAll<HTMLElement>('[style*="vh"]'))) {
    fixStyle(el.style)
  }
}

/**
 * 虚拟分章：书文件保持单 HTML 不动，按 body 直接子元素切章。
 * 排除封面（书首页单独展示）、封底（原作者推广页暂不展示，裁留待站长拍板）、
 * 书内目录页（由站点目录卡替代）；标题取块内第一个 h1-h3。
 */
function extractChapters(doc: Document): { cover: HTMLElement | null; blocks: HTMLElement[]; toc: BookChapter[] } {
  let cover: HTMLElement | null = null
  const blocks: HTMLElement[] = []
  const toc: BookChapter[] = []
  for (const el of Array.from(doc.body.children) as HTMLElement[]) {
    const tag = el.tagName
    if (tag === 'SCRIPT' || tag === 'STYLE' || tag === 'LINK') continue
    if (el.classList.contains('cover')) {
      cover = el
      continue
    }
    if (el.classList.contains('back-page')) continue
    if (el.id === 'toc' || el.querySelector('#toc')) continue
    blocks.push(el)
    const head = el.querySelector('h1, h2, h3')
    toc.push({ index: blocks.length, title: head?.textContent?.trim() || t('hub.book.untitled', { n: blocks.length }) })
  }
  return { cover, blocks, toc }
}

/**
 * 电子书正文（分章阅读）：同源 iframe 一次加载整本，每次只显示一个章节块（切章零请求）。
 * - 书首页（无章号）＝站点目录卡 + 封面纸面卡；章节页＝顶部「← 目录 · k/N」+ 章内容 + 上一/下一章
 * - HEAD 探活：文件缺失给明确提示而非白框
 * - 高度同步：读内容文档 scrollHeight，ResizeObserver 跟随（切章/图片晚到都触发）
 * - 锚点桥接：书内 #hash 指向他章 → 路由切章；同章 → 滚外层页面
 */
export function EbookBody({
  slug,
  title,
  path,
  chapter,
}: {
  slug: string
  title: string
  path: string
  chapter?: number
}) {
  const navigate = useNavigate()
  const [probe, setProbe] = useState<Probe>('probing')
  const [toc, setToc] = useState<BookChapter[] | null>(null)
  const [hasCover, setHasCover] = useState(false)
  const frameRef = useRef<HTMLIFrameElement>(null)
  const bookRef = useRef<{ cover: HTMLElement | null; blocks: HTMLElement[] } | null>(null)
  const chapterRef = useRef(chapter)
  chapterRef.current = chapter
  const syncRef = useRef<(() => void) | null>(null)
  const cleanupRef = useRef<(() => void) | null>(null)

  useEffect(() => {
    let alive = true
    setProbe('probing')
    if (!path) {
      setProbe('missing')
      return
    }
    fetch(path, { method: 'HEAD' })
      .then((res) => alive && setProbe(res.ok ? 'ok' : 'missing'))
      .catch(() => alive && setProbe('missing'))
    return () => {
      alive = false
    }
  }, [path])

  useEffect(() => () => cleanupRef.current?.(), [])

  /** 只显示当前视图对应的块：章节页=第 k 块，书首页=封面。 */
  const applyView = () => {
    const doc = frameRef.current?.contentDocument
    const book = bookRef.current
    if (!doc?.body || !book) return
    const k = chapterRef.current
    const active = k && k >= 1 && k <= book.blocks.length ? book.blocks[k - 1] : book.cover
    for (const el of Array.from(doc.body.children) as HTMLElement[]) {
      ;(el as HTMLElement).style.display = el === active ? '' : 'none'
    }
    syncRef.current?.()
  }

  useEffect(applyView, [chapter, toc])

  const onLoad = () => {
    cleanupRef.current?.()
    const frame = frameRef.current
    const doc = frame?.contentDocument
    if (!frame || !doc?.body) return

    neutralizeViewportUnits(doc)
    const { cover, blocks, toc: chapters } = extractChapters(doc)
    bookRef.current = { cover, blocks }
    setHasCover(!!cover)
    setToc(chapters)

    let raf = 0
    const sync = () => {
      raf = 0
      const h = Math.max(doc.documentElement?.scrollHeight ?? 0, doc.body?.scrollHeight ?? 0)
      // 上限断路器：万一还有未钉住的反馈式单位，宁可截断也不冲到浏览器 2^25 钳制
      if (h > 0 && h < 400_000) frame.style.height = `${h}px`
    }
    const schedule = () => {
      if (!raf) raf = requestAnimationFrame(sync)
    }
    syncRef.current = schedule

    let ro: ResizeObserver | null = null
    if (typeof ResizeObserver !== 'undefined') {
      ro = new ResizeObserver(schedule)
      ro.observe(doc.documentElement)
      if (doc.body) ro.observe(doc.body)
    }

    const onClick = (e: Event) => {
      const target = e.target as Element | null
      const link = target?.closest?.('a[href^="#"]')
      if (!link) return
      const id = decodeURIComponent((link.getAttribute('href') ?? '').slice(1))
      const dest = id ? doc.getElementById(id) : null
      if (!dest) return
      e.preventDefault()
      const book = bookRef.current
      const idx = book ? book.blocks.findIndex((b) => b === dest || b.contains(dest)) : -1
      if (idx >= 0 && idx + 1 !== chapterRef.current) {
        navigate(`/a/${slug}/${idx + 1}`) // 跨章锚点：走路由切章
        return
      }
      const top = frame.getBoundingClientRect().top + window.scrollY + dest.getBoundingClientRect().top - 24
      window.scrollTo(0, Math.max(0, top))
    }
    doc.addEventListener('click', onClick)

    applyView()
    sync()

    cleanupRef.current = () => {
      if (raf) cancelAnimationFrame(raf)
      ro?.disconnect()
      doc.removeEventListener('click', onClick)
      syncRef.current = null
    }
  }

  const total = toc?.length ?? 0
  const valid = chapter !== undefined && chapter >= 1 && chapter <= total
  const current = valid && toc ? toc[chapter - 1] : null
  const prev = current && chapter! > 1 && toc ? toc[chapter! - 2] : null
  const next = current && chapter! < total && toc ? toc[chapter!] : null

  useEffect(() => {
    if (current) document.title = `${current.title} · ${title} · ${t('hub.title')}`
    else if (toc) document.title = `${title} · ${t('hub.title')}`
  }, [current, toc, title])

  if (probe === 'probing') {
    return (
      <div data-testid="hub-ebook-loading">
        <Skeleton className="h-96 w-full rounded-2xl" />
      </div>
    )
  }

  if (probe === 'missing') {
    return (
      <div
        className="rounded-2xl border border-snb-hairline bg-snb-well/60 px-4 py-10 text-center text-sm text-snb-t2"
        data-testid="hub-reader-missing"
      >
        {t('hub.reader.missing')}
      </div>
    )
  }

  return (
    <div data-testid="hub-ebook">
      {current ? (
        <nav key="strip" className="mb-4 flex items-center justify-between text-[13px]" data-testid="hub-book-strip">
          <Link className="text-snb-t3 transition-colors hover:text-snb-t1" to={`/a/${slug}`}>
            ← {t('hub.book.toc')}
          </Link>
          <span className="font-mono text-xs text-snb-t3">
            {chapter} / {total}
          </span>
        </nav>
      ) : (
        <nav
          key="toc"
          className="mb-6 rounded-2xl border border-snb-hairline p-5"
          aria-label={t('hub.book.toc')}
          data-testid="hub-book-toc"
        >
          <h2 className="mb-2 flex items-baseline justify-between text-sm font-semibold text-snb-t1">
            {t('hub.book.toc')}
            {total > 0 && <span className="font-mono text-xs font-normal text-snb-t3">{total}</span>}
          </h2>
          {toc ? (
            <ol className="grid gap-x-6 sm:grid-cols-2">
              {toc.map((ch) => (
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
          ) : (
            <div className="space-y-2 py-1">
              <Skeleton className="h-8" />
              <Skeleton className="h-8" />
              <Skeleton className="h-8" />
            </div>
          )}
        </nav>
      )}

      <figure
        key="book-frame"
        className="relative left-1/2 w-[min(56rem,calc(100vw-2.5rem))] -translate-x-1/2 overflow-hidden rounded-2xl border border-snb-hairline bg-white"
        style={{ display: !current && !hasCover ? 'none' : undefined }}
        data-testid="hub-ebook-frame"
      >
        {/* 书自带完整浅色纸面样式，底色固定白、不随站点暗色 */}
        <iframe
          ref={frameRef}
          title={title}
          src={path}
          onLoad={onLoad}
          scrolling="no"
          className="block w-full border-0"
          style={{ height: '70vh', overflow: 'hidden' }}
        />
      </figure>

      {current && (
        <nav
          key="pager"
          className="mt-8 flex items-center justify-between gap-4 border-t border-snb-hairline pt-5 text-sm"
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
      )}
    </div>
  )
}
