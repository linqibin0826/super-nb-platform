import { useEffect, useRef } from 'react'

/**
 * 阅读进度条（2026-07-29 定稿）：4px 加粗、下移成顶栏的底边线（AppHeader sticky 高 64）——
 * 橙线在近黑上贴视口顶时几乎看不出来，压在顶栏底边才读得到；
 * 它同时兼任「你在读一篇长文」的状态提示。
 * 按整页滚动比例 scaleX（transform 不触发重排），rAF 节流、不引起 React 重渲染。
 */
export function ReadingProgress() {
  const bar = useRef<HTMLElement>(null)

  useEffect(() => {
    let raf = 0
    const update = () => {
      raf = 0
      const doc = document.documentElement
      const max = doc.scrollHeight - doc.clientHeight
      const ratio = max > 0 ? Math.min(1, doc.scrollTop / max) : 0
      if (bar.current) bar.current.style.transform = `scaleX(${ratio})`
    }
    const onScroll = () => {
      if (!raf) raf = requestAnimationFrame(update)
    }
    update()
    window.addEventListener('scroll', onScroll, { passive: true })
    window.addEventListener('resize', onScroll)
    return () => {
      if (raf) cancelAnimationFrame(raf)
      window.removeEventListener('scroll', onScroll)
      window.removeEventListener('resize', onScroll)
    }
  }, [])

  return (
    <div className="hub-progress" aria-hidden="true" data-testid="hub-progress">
      <i ref={bar} style={{ transform: 'scaleX(0)' }} />
    </div>
  )
}
