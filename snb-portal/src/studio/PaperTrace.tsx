import { useEffect, useRef } from 'react'

/** 生成等待的「走纸描线」：沥青底上一条 1px 级纸白描线向左走纸，像热敏打印机/机房示波器。
 *  设计定稿（StudioWaiting 2026-07-29）：零发光零粒子零加法混合——无 shadowBlur、
 *  无渐变、无 globalCompositeOperation:lighter；线的振幅随渐近进度收敛（越接近完工线越稳）。
 *  prefers-reduced-motion 或拿不到 2d 上下文（jsdom）时不启动 rAF，
 *  由占位卡里的静态状态点兜底（TaskCard motion-reduce:block）。 */

/** mulberry32 种子随机：多张占位卡各走各的相位，每批不重样 */
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

const BG = '#0E1014'
const INK = '#EFEBE4'

export function PaperTrace({ seed = 1 }: { seed?: number }) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const rand = mulberry32(seed * 9973 + 7)
    const phase = [rand() * Math.PI * 2, rand() * Math.PI * 2, rand() * Math.PI * 2]
    const dpr = Math.min(window.devicePixelRatio || 1, 2)
    let w = 0
    let h = 0
    const fit = () => {
      const rect = canvas.getBoundingClientRect()
      w = rect.width
      h = rect.height
      canvas.width = Math.max(1, Math.round(w * dpr))
      canvas.height = Math.max(1, Math.round(h * dpr))
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      ctx.fillStyle = BG
      ctx.fillRect(0, 0, w, h)
    }
    fit()
    const ro = new ResizeObserver(fit)
    ro.observe(canvas)

    const t0 = Date.now()
    let x = 0
    let prevY: number | undefined
    let raf = 0
    const frame = () => {
      raf = requestAnimationFrame(frame)
      if (w <= 0 || h <= 0) return
      // 整幅左移 1px（物理像素域拷贝），右缘补底色后画新一段
      ctx.setTransform(1, 0, 0, 1, 0, 0)
      ctx.drawImage(canvas, -1 * dpr, 0)
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      ctx.fillStyle = BG
      ctx.fillRect(w - 1.2, 0, 1.4, h)

      // 振幅随渐近进度收敛（与进度条同一条 92% 逼近曲线）：越接近完工，线越稳
      const p = Math.min(0.92, 0.92 * (1 - Math.exp(-((Date.now() - t0) / 1000) / 45)))
      const amp = h * 0.34 * (1 - p * 0.72)
      const mid = h / 2
      const v =
        Math.sin(x / 21 + phase[0]) * 0.55 +
        Math.sin(x / 7.3 + phase[1]) * 0.28 +
        Math.sin(x / 3.1 + phase[2]) * 0.17
      const y = mid + v * amp

      // 基准中线（淡）+ 周期刻度线（更淡）——全部平色描线，无任何光晕
      ctx.strokeStyle = 'rgba(239,235,228,0.14)'
      ctx.lineWidth = 1
      ctx.beginPath()
      ctx.moveTo(w - 1.2, mid)
      ctx.lineTo(w - 0.2, mid)
      ctx.stroke()
      if (x % 48 === 0) {
        ctx.strokeStyle = 'rgba(239,235,228,0.10)'
        ctx.beginPath()
        ctx.moveTo(w - 0.7, 0)
        ctx.lineTo(w - 0.7, h)
        ctx.stroke()
      }
      if (x % 240 === 0) {
        ctx.strokeStyle = 'rgba(239,235,228,0.22)'
        ctx.beginPath()
        ctx.moveTo(w - 0.7, h * 0.12)
        ctx.lineTo(w - 0.7, h * 0.88)
        ctx.stroke()
      }

      // 主描线：纸白 1.3px
      ctx.strokeStyle = INK
      ctx.lineWidth = 1.3
      ctx.beginPath()
      ctx.moveTo(w - 1.2, prevY === undefined ? y : prevY)
      ctx.lineTo(w - 0.2, y)
      ctx.stroke()
      prevY = y
      x += 1
    }
    raf = requestAnimationFrame(frame)
    return () => {
      cancelAnimationFrame(raf)
      ro.disconnect()
    }
  }, [seed])

  return <canvas ref={canvasRef} aria-hidden="true" className="absolute inset-0 h-full w-full" />
}
