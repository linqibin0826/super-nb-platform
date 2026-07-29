import { useEffect, useRef } from 'react'

/** 生成等待的「走纸描线」：机器表面上一条 1px 级描线向左走纸，像热敏打印机/机房示波器。
 *  双档（2026-07-29）：深夜是沥青底纸白墨，白天是纸底暖墨——整块画布都是我们自己画的，
 *  跟主题走（区别于「用户图片上的信息层两档都不翻」，见 readTone 注释）。
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

/**
 * canvas 不吃 CSS 变量，只能自己读。
 *
 * 🚨 这条 canvas 跟主题走，与「图片上的信息层两档都不翻」不是一回事：
 * 那条纪律保护的是**用户的图**（底下可能是白仪表盘也可能是黑夜景，翻了就瞎）；
 * 这里整块画布都是我们自己画的机器表面，白天档它就该是纸上的墨线。
 *
 * 兜底值 = 深色档原值，逐字不动（拿不到变量时行为与改造前一致）。
 */
function readTone(el: HTMLElement) {
  const cs = getComputedStyle(el)
  const bg = cs.getPropertyValue('--snb-bg').trim() || '14 16 20'
  const ink = cs.getPropertyValue('--snb-t1').trim() || '239 235 228'
  return {
    bg: `rgb(${bg})`,
    ink: `rgb(${ink})`,
    /** 基准中线 / 刻度线：同一支墨的低透明度档 */
    inkAt: (a: number) => `rgb(${ink} / ${a})`,
  }
}

export function PaperTrace({ seed = 1 }: { seed?: number }) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    let tone = readTone(canvas)

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
      ctx.fillStyle = tone.bg
      ctx.fillRect(0, 0, w, h)
    }
    fit()
    const ro = new ResizeObserver(fit)
    ro.observe(canvas)

    // 切档时重读色并重铺底：canvas 里已画的历史线条是旧档的墨，留着会是一段错色残影，
    // 直接当「换一张纸」重开。<html> 上的 .dark 增删是主题契约唯一的 DOM 信号。
    const mo = new MutationObserver(() => {
      tone = readTone(canvas)
      fit()
      prevY = undefined
    })
    mo.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })

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
      ctx.fillStyle = tone.bg
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
      ctx.strokeStyle = tone.inkAt(0.14)
      ctx.lineWidth = 1
      ctx.beginPath()
      ctx.moveTo(w - 1.2, mid)
      ctx.lineTo(w - 0.2, mid)
      ctx.stroke()
      if (x % 48 === 0) {
        ctx.strokeStyle = tone.inkAt(0.1)
        ctx.beginPath()
        ctx.moveTo(w - 0.7, 0)
        ctx.lineTo(w - 0.7, h)
        ctx.stroke()
      }
      if (x % 240 === 0) {
        ctx.strokeStyle = tone.inkAt(0.22)
        ctx.beginPath()
        ctx.moveTo(w - 0.7, h * 0.12)
        ctx.lineTo(w - 0.7, h * 0.88)
        ctx.stroke()
      }

      // 主描线 1.3px：深夜是纸白墨、白天是暖墨
      ctx.strokeStyle = tone.ink
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
      mo.disconnect()
    }
  }, [seed])

  return <canvas ref={canvasRef} aria-hidden="true" className="absolute inset-0 h-full w-full" />
}
