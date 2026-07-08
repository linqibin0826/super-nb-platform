import { useEffect, useRef } from 'react'

/** 生成等待的「余烬画布」：一支赤陶色的画笔在占位卡里游走、留下缓缓退隐的发光笔迹，
 *  余烬颗粒往上飘。还藏了两个小玩具：指针扫过会把余烬拨开，点一下迸出一撮火星——
 *  30~120 秒的等待，给用户一块可以拨弄的画布。
 *  prefers-reduced-motion 或拿不到 2d 上下文（jsdom）时不启动 rAF，
 *  由占位卡里的静态余烬点兜底（TaskCard motion-reduce:block）。 */

interface Ember {
  x: number
  y: number
  r: number
  sway: number
  speed: number
  warm: number
  vx: number
  vy: number
}

interface Spark {
  x: number
  y: number
  vx: number
  vy: number
  life: number
}

/** mulberry32 种子随机：多张卡各画各的、每批不重样 */
function mulberry32(seed: number): () => number {
  let a = seed >>> 0
  return () => {
    a |= 0
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

export function PaintingCanvas({ seed = 1 }: { seed?: number }) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const pointerRef = useRef({ x: 0, y: 0, active: false })
  const sparksRef = useRef<Spark[]>([])

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const rand = mulberry32(seed * 9973 + 7)
    const dpr = Math.min(window.devicePixelRatio || 1, 2)
    let w = 0
    let h = 0
    const resize = () => {
      const rect = canvas.getBoundingClientRect()
      w = rect.width
      h = rect.height
      canvas.width = Math.max(1, Math.round(w * dpr))
      canvas.height = Math.max(1, Math.round(h * dpr))
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    }
    resize()
    const ro = new ResizeObserver(resize)
    ro.observe(canvas)

    const embers: Ember[] = Array.from({ length: 22 }, () => ({
      x: rand(),
      y: rand(),
      r: 0.8 + rand() * 1.6,
      sway: rand() * Math.PI * 2,
      speed: 8 + rand() * 14,
      warm: rand(),
      vx: 0,
      vy: 0,
    }))
    // 画笔游走路径的相位（多正弦合成，永不重复也永不出格）
    const phase = Array.from({ length: 4 }, () => rand() * Math.PI * 2)

    let raf = 0
    let last = performance.now()
    const frame = (now: number) => {
      const dt = Math.min((now - last) / 1000, 0.05)
      last = now

      // 旧笔迹缓缓退隐（往透明擦，不碰卡片底色）
      ctx.globalCompositeOperation = 'destination-out'
      ctx.fillStyle = 'rgba(0, 0, 0, 0.045)'
      ctx.fillRect(0, 0, w, h)
      ctx.globalCompositeOperation = 'lighter'

      // 画笔：柔光晕 + 笔尖亮点
      const t = now / 1000
      const bx = w * (0.5 + 0.34 * Math.sin(t * 0.24 + phase[0]) + 0.11 * Math.sin(t * 0.83 + phase[1]))
      const by = h * (0.5 + 0.33 * Math.sin(t * 0.19 + phase[2]) + 0.12 * Math.sin(t * 0.67 + phase[3]))
      const dab = ctx.createRadialGradient(bx, by, 0, bx, by, 26)
      dab.addColorStop(0, 'rgba(204, 120, 92, 0.16)')
      dab.addColorStop(0.6, 'rgba(204, 120, 92, 0.05)')
      dab.addColorStop(1, 'rgba(204, 120, 92, 0)')
      ctx.fillStyle = dab
      ctx.beginPath()
      ctx.arc(bx, by, 26, 0, Math.PI * 2)
      ctx.fill()
      ctx.fillStyle = 'rgba(255, 224, 204, 0.5)'
      ctx.beginPath()
      ctx.arc(bx, by, 1.6, 0, Math.PI * 2)
      ctx.fill()

      // 余烬：上飘 + 闪烁 + 被指针拨开
      const ptr = pointerRef.current
      for (const e of embers) {
        let ex = e.x * w
        let ey = e.y * h
        if (ptr.active) {
          const dx = ex - ptr.x
          const dy = ey - ptr.y
          const d2 = dx * dx + dy * dy
          if (d2 < 4900 && d2 > 0.01) {
            const d = Math.sqrt(d2)
            e.vx += (dx / d) * 90 * dt
            e.vy += (dy / d) * 90 * dt
          }
        }
        e.vx *= 0.92
        e.vy *= 0.92
        e.sway += dt * 0.9
        ex += (e.vx + Math.sin(e.sway) * 10) * dt
        ey += (e.vy - e.speed) * dt
        if (ey < -4) {
          ey = h + 4
          ex = rand() * w
        }
        if (ex < -4) ex = w + 4
        else if (ex > w + 4) ex = -4
        e.x = ex / w
        e.y = ey / h
        const twinkle = 0.45 + 0.4 * Math.sin(t * 2.1 + e.sway * 3)
        ctx.fillStyle =
          e.warm > 0.6
            ? `rgba(255, 224, 204, ${0.5 * twinkle})`
            : `rgba(204, 120, 92, ${0.65 * twinkle})`
        ctx.beginPath()
        ctx.arc(ex, ey, e.r, 0, Math.PI * 2)
        ctx.fill()
      }

      // 点击迸出的火星：带一点重力，短命
      const sparks = sparksRef.current
      for (let i = sparks.length - 1; i >= 0; i--) {
        const s = sparks[i]
        s.life -= dt
        if (s.life <= 0) {
          sparks.splice(i, 1)
          continue
        }
        s.vy += 60 * dt
        s.x += s.vx * dt
        s.y += s.vy * dt
        ctx.fillStyle = `rgba(255, 224, 204, ${0.8 * Math.max(s.life / 0.9, 0)})`
        ctx.beginPath()
        ctx.arc(s.x, s.y, 1.4, 0, Math.PI * 2)
        ctx.fill()
      }

      raf = requestAnimationFrame(frame)
    }
    raf = requestAnimationFrame(frame)
    return () => {
      cancelAnimationFrame(raf)
      ro.disconnect()
    }
  }, [seed])

  return (
    <canvas
      ref={canvasRef}
      aria-hidden="true"
      className="absolute inset-0 h-full w-full"
      onPointerMove={(e) => {
        const rect = e.currentTarget.getBoundingClientRect()
        pointerRef.current = { x: e.clientX - rect.left, y: e.clientY - rect.top, active: true }
      }}
      onPointerLeave={() => {
        pointerRef.current.active = false
      }}
      onPointerDown={(e) => {
        const rect = e.currentTarget.getBoundingClientRect()
        const x = e.clientX - rect.left
        const y = e.clientY - rect.top
        for (let i = 0; i < 10; i++) {
          const angle = (Math.PI * 2 * i) / 10 + Math.random() * 0.5
          const v = 60 + Math.random() * 90
          sparksRef.current.push({
            x,
            y,
            vx: Math.cos(angle) * v,
            vy: Math.sin(angle) * v - 40,
            life: 0.7 + Math.random() * 0.4,
          })
        }
      }}
    />
  )
}
