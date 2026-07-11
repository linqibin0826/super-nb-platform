import { useEffect, useRef, useState } from 'react'
import { Skeleton } from '../ui'
import { t } from '../i18n'

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
 * 电子书正文：同源 iframe 以内容全高嵌入页面流（外层唯一滚动条），视觉上即普通教程正文。
 * - HEAD 探活：文件缺失给明确提示而非白框
 * - 高度同步：onLoad 后读内容文档 scrollHeight，ResizeObserver 跟随（图片/字体晚到再长高）
 * - 锚点桥接：iframe 全高展开后内部无滚动空间，书内目录 #hash 点击改为滚动外层页面
 */
export function EbookBody({ title, path }: { title: string; path: string }) {
  const [probe, setProbe] = useState<Probe>('probing')
  const frameRef = useRef<HTMLIFrameElement>(null)
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

  const onLoad = () => {
    cleanupRef.current?.()
    const frame = frameRef.current
    const doc = frame?.contentDocument
    if (!frame || !doc) return

    neutralizeViewportUnits(doc)

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
    sync()

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
      const top = frame.getBoundingClientRect().top + window.scrollY + dest.getBoundingClientRect().top - 24
      window.scrollTo(0, Math.max(0, top))
    }
    doc.addEventListener('click', onClick)

    cleanupRef.current = () => {
      if (raf) cancelAnimationFrame(raf)
      ro?.disconnect()
      doc.removeEventListener('click', onClick)
    }
  }

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
    <figure
      className="relative left-1/2 w-[min(56rem,calc(100vw-2.5rem))] -translate-x-1/2 overflow-hidden rounded-2xl border border-snb-hairline bg-white"
      data-testid="hub-ebook"
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
  )
}
