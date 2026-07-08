import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, cleanup } from '@testing-library/react'
import { TaskCard } from '../TaskCard'
import { t } from '../../i18n'
import { SIZE_PRESETS } from '../../lib/sizes'
import type { GenTask } from '../useGenerationQueue'

/** 造一个 IHDR 头写死宽高的 PNG base64，用于验证「按真实尺寸定比例」 */
function pngHeadB64(w: number, h: number): string {
  const be = (n: number) => [(n >>> 24) & 255, (n >>> 16) & 255, (n >>> 8) & 255, n & 255]
  const bytes = [
    0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
    0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52,
    ...be(w),
    ...be(h),
  ]
  return btoa(String.fromCharCode(...bytes))
}

const baseInput = {
  apiKey: 'sk', keyId: 1, groupName: 'g', prompt: 'a lighthouse at dusk',
  size: '1024x1024', n: 1, quality: 'medium' as const, cost: 0.04,
}

function makeTask(over: Partial<GenTask>): GenTask {
  return { id: 't1', input: baseInput, status: 'queued', createdAt: 1000, ...over }
}

function renderCard(task: GenTask, over: Partial<Parameters<typeof TaskCard>[0]> = {}) {
  const props = {
    task, expanded: false, now: 61_000, position: null as number | null, canRetry: true,
    onToggle: vi.fn(), onCancel: vi.fn(), onRetry: vi.fn(), onPreview: vi.fn(),
    ...over,
  }
  render(<TaskCard {...props} />)
  return props
}

afterEach(() => cleanup())

describe('TaskCard 收起行', () => {
  it('queued：位次徽标 + 取消钮', () => {
    const p = renderCard(makeTask({ status: 'queued' }), { position: 2 })
    expect(screen.getByText(t('studio.queue.position', { pos: 2 }))).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: t('studio.queue.cancel') }))
    expect(p.onCancel).toHaveBeenCalledTimes(1)
  })

  it('running：m:ss 计时 + 取消钮', () => {
    renderCard(makeTask({ status: 'running', startedAt: 1000 }))
    expect(screen.getByText('1:00')).toBeTruthy() // (61000-1000)/1000 = 60s
    expect(screen.getByRole('button', { name: t('studio.queue.cancel') })).toBeTruthy()
  })

  it('error：失败短语 + 重试钮（queueFull 时禁用）', () => {
    const p = renderCard(makeTask({ status: 'error', error: { key: 'upstream', detail: '' } }))
    fireEvent.click(screen.getByRole('button', { name: t('studio.queue.retry') }))
    expect(p.onRetry).toHaveBeenCalledTimes(1)
    cleanup()
    renderCard(makeTask({ status: 'error', error: { key: 'upstream', detail: '' } }), { canRetry: false })
    expect((screen.getByRole('button', { name: t('studio.queue.retry') }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('done 收起：缩略图条最多 4 枚', () => {
    const images = Array.from({ length: 6 }, (_, i) => ({ b64: 'QUJD', dataUrl: `data:image/png;base64,IMG${i}` }))
    renderCard(makeTask({ status: 'done', batch: { images, elapsedMs: 83_000 } }))
    expect(screen.getAllByRole('img')).toHaveLength(4)
  })

  it('点行头触发 onToggle', () => {
    const p = renderCard(makeTask({ status: 'queued' }), { position: 1 })
    fireEvent.click(screen.getByRole('button', { name: baseInput.prompt }))
    expect(p.onToggle).toHaveBeenCalledTimes(1)
  })
})

describe('TaskCard 展开体', () => {
  it('queued 展开：等待文案，不挂画布', () => {
    renderCard(makeTask({ status: 'queued' }), { expanded: true, position: 1 })
    expect(screen.getByText(t('studio.queue.waiting'))).toBeTruthy()
  })

  it('running 展开：耗时行 + 阶段文案', () => {
    renderCard(makeTask({ status: 'running', startedAt: 1000 }), { expanded: true })
    expect(screen.getByText(new RegExp(t('playground.results.elapsed', { seconds: 60 })))).toBeTruthy()
  })

  it('done 展开：结果图可点开大图（整批 url + 正确 index）', () => {
    const images = [
      { b64: 'QUJD', dataUrl: 'data:image/png;base64,QUJD' },
      { b64: 'RUZH', dataUrl: 'data:image/png;base64,RUZH' },
    ]
    const p = renderCard(makeTask({ status: 'done', batch: { images, elapsedMs: 83_000 } }), { expanded: true })
    fireEvent.click(screen.getAllByRole('img')[1])
    expect(p.onPreview).toHaveBeenCalledWith(
      ['data:image/png;base64,QUJD', 'data:image/png;base64,RUZH'],
      1
    )
  })

  it('error 展开：错误文案 + 重试', () => {
    renderCard(makeTask({ status: 'error', error: { key: 'insufficientBalance', detail: 'x' } }), { expanded: true })
    expect(screen.getByText(t('playground.errors.insufficientBalance'))).toBeTruthy()
  })

  it('done 展开：按图片真实尺寸定比例，不用请求 size 硬裁（2026-07-05 生产坑）', () => {
    // 请求 1:1 3840，上游实际返回 1536x1024（3:2）→ MasonryCard 该拿真实 3:2，不是请求的 1:1
    const b64 = pngHeadB64(1536, 1024)
    renderCard(
      makeTask({
        status: 'done',
        input: { ...baseInput, size: '3840x3840' },
        batch: { images: [{ b64, dataUrl: `data:image/png;base64,${b64}` }], elapsedMs: 1000 },
      }),
      { expanded: true }
    )
    const img = screen.getByRole('img')
    expect((img.parentElement as HTMLElement).style.aspectRatio).toBe('1536 / 1024')
  })

  it('done meta 显示实际返回张数，而非请求 n（上游可能少给）', () => {
    const b64 = pngHeadB64(1024, 1024)
    renderCard(
      makeTask({
        status: 'done',
        input: { ...baseInput, n: 2, size: '1024x1024' },
        batch: { images: [{ b64, dataUrl: `data:image/png;base64,${b64}` }], elapsedMs: 1000 },
      }),
      { expanded: true }
    )
    const sizeLabel = t(SIZE_PRESETS.find((p) => p.value === '1024x1024')!.labelKey)
    const meta = t('studio.results.meta', {
      n: 1,
      size: sizeLabel,
      quality: t('playground.form.qualitiesShort.medium'),
    })
    expect(screen.getByText(meta)).toBeTruthy()
  })
})
