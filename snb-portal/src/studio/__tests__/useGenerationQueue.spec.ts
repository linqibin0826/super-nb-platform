import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { useGenerationQueue, MAX_CONCURRENT, MAX_PENDING, MAX_FINISHED_KEPT } from '../useGenerationQueue'
import { ImagesApiError, type GeneratedImage } from '../../lib/imagesApi'

vi.mock('../../lib/imagesApi', async (orig) => {
  const actual = await orig<typeof import('../../lib/imagesApi')>()
  return { ...actual, generateImages: vi.fn() }
})
vi.mock('../../lib/generationsApi', () => ({ createGeneration: vi.fn().mockResolvedValue({ id: 'x' }) }))
vi.mock('../../lib/fileToBase64', () => ({ filesToRefB64: vi.fn().mockResolvedValue([]) }))

import { generateImages } from '../../lib/imagesApi'
import { createGeneration } from '../../lib/generationsApi'
import { filesToRefB64 } from '../../lib/fileToBase64'

type Deferred = { resolve: (v: GeneratedImage[]) => void; reject: (e: unknown) => void }
let pending: Deferred[] = []
const IMG: GeneratedImage = { b64: 'QUJD', dataUrl: 'data:image/png;base64,QUJD' }

function makeInput(prompt: string) {
  return { apiKey: 'sk', keyId: 1, groupName: 'g', prompt, size: '1024x1024', n: 1, quality: 'medium' as const, cost: 0.04 }
}

beforeEach(() => {
  vi.clearAllMocks()
  pending = []
  ;(generateImages as ReturnType<typeof vi.fn>).mockImplementation(
    () => new Promise<GeneratedImage[]>((resolve, reject) => { pending.push({ resolve, reject }) })
  )
  ;(createGeneration as ReturnType<typeof vi.fn>).mockResolvedValue({ id: 'x' })
  ;(filesToRefB64 as ReturnType<typeof vi.fn>).mockResolvedValue([])
  if (typeof crypto === 'undefined' || typeof crypto.randomUUID !== 'function') {
    let i = 0
    vi.stubGlobal('crypto', { randomUUID: () => `uuid-${++i}` })
  }
})
afterEach(() => vi.unstubAllGlobals())

describe('useGenerationQueue 调度', () => {
  it('入队即跑：单任务直接 running', async () => {
    const { result } = renderHook(() => useGenerationQueue())
    await act(async () => result.current.enqueue(makeInput('p1')))
    expect(result.current.tasks).toHaveLength(1)
    expect(result.current.tasks[0].status).toBe('running')
    expect(result.current.runningCount).toBe(1)
    expect(generateImages).toHaveBeenCalledTimes(1)
  })

  it('并发封顶 5，第 6 个排队；释放槽位后 FIFO 递补', async () => {
    const { result } = renderHook(() => useGenerationQueue())
    await act(async () => {
      for (let i = 1; i <= 6; i++) result.current.enqueue(makeInput(`p${i}`))
    })
    expect(result.current.runningCount).toBe(MAX_CONCURRENT)
    expect(result.current.queuedCount).toBe(1)
    expect(result.current.tasks[5].status).toBe('queued')
    await act(async () => { pending[0].resolve([IMG]) })
    expect(result.current.runningCount).toBe(MAX_CONCURRENT)
    expect(result.current.queuedCount).toBe(0)
    const lastCall = (generateImages as ReturnType<typeof vi.fn>).mock.calls[5][0]
    expect(lastCall.prompt).toBe('p6')
  })

  it('未完成上限 10：第 11 个入队被忽略，queueFull 置位', async () => {
    const { result } = renderHook(() => useGenerationQueue())
    await act(async () => {
      for (let i = 1; i <= 11; i++) result.current.enqueue(makeInput(`p${i}`))
    })
    expect(result.current.tasks).toHaveLength(MAX_PENDING)
    expect(result.current.queueFull).toBe(true)
  })

  it('取消排队任务：移出列表，不写历史', async () => {
    const { result } = renderHook(() => useGenerationQueue())
    await act(async () => {
      for (let i = 1; i <= 6; i++) result.current.enqueue(makeInput(`p${i}`))
    })
    const queuedId = result.current.tasks[5].id
    await act(async () => result.current.cancelTask(queuedId))
    expect(result.current.tasks).toHaveLength(5)
    expect(createGeneration).not.toHaveBeenCalled()
  })

  it('取消生成中任务：移出列表，请求后续 reject 不写历史不复活', async () => {
    const { result } = renderHook(() => useGenerationQueue())
    await act(async () => result.current.enqueue(makeInput('p1')))
    const id = result.current.tasks[0].id
    await act(async () => result.current.cancelTask(id))
    expect(result.current.tasks).toHaveLength(0)
    await act(async () => { pending[0].reject(new ImagesApiError({ key: 'timeout', detail: '' })) })
    expect(result.current.tasks).toHaveLength(0)
    expect(createGeneration).not.toHaveBeenCalled()
  })

  it('完成：task 落 batch，createGeneration 记 done + cost，historyVersion 递增', async () => {
    const { result } = renderHook(() => useGenerationQueue())
    await act(async () => result.current.enqueue(makeInput('p1')))
    await act(async () => { pending[0].resolve([IMG]) })
    const task = result.current.tasks[0]
    expect(task.status).toBe('done')
    expect(task.batch?.images).toHaveLength(1)
    expect(result.current.historyVersion).toBe(1)
    const [arg] = (createGeneration as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(arg.status).toBe('done')
    expect(arg.cost).toBe(0.04)
    expect(arg.outputImages).toEqual([{ b64: 'QUJD' }])
    expect(arg.refImages).toEqual([])
  })

  it('图生图：把输入参考图 File 转 b64 带进 createGeneration', async () => {
    const file = new File([new Uint8Array([1])], 'r.png', { type: 'image/png' })
    ;(filesToRefB64 as ReturnType<typeof vi.fn>).mockResolvedValueOnce([{ b64: 'Rk8=', contentType: 'image/png' }])
    const { result } = renderHook(() => useGenerationQueue())
    await act(async () => result.current.enqueue({ ...makeInput('p1'), images: [file] }))
    await act(async () => { pending[0].resolve([IMG]) })
    expect(filesToRefB64).toHaveBeenCalledWith([file])
    const [arg] = (createGeneration as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(arg.refImages).toEqual([{ b64: 'Rk8=', contentType: 'image/png' }])
  })

  it('失败：error 映射落卡，createGeneration 记 error', async () => {
    const { result } = renderHook(() => useGenerationQueue())
    await act(async () => result.current.enqueue(makeInput('p1')))
    await act(async () => { pending[0].reject(new ImagesApiError({ key: 'permissionDenied', detail: 'no perm' })) })
    const task = result.current.tasks[0]
    expect(task.status).toBe('error')
    expect(task.error?.key).toBe('permissionDenied')
    const [arg] = (createGeneration as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(arg.status).toBe('error')
  })

  it('非 ImagesApiError 异常归一为 unknown', async () => {
    const { result } = renderHook(() => useGenerationQueue())
    await act(async () => result.current.enqueue(makeInput('p1')))
    await act(async () => { pending[0].reject(new Error('boom')) })
    expect(result.current.tasks[0].error?.key).toBe('unknown')
    expect(result.current.tasks[0].error?.detail).toBe('boom')
  })

  it('重试：失败卡换成同 input 的新任务，追加到栈底', async () => {
    const { result } = renderHook(() => useGenerationQueue())
    await act(async () => result.current.enqueue(makeInput('p1')))
    await act(async () => { pending[0].reject(new ImagesApiError({ key: 'upstream', detail: '' })) })
    const failedId = result.current.tasks[0].id
    await act(async () => result.current.retryTask(failedId))
    expect(result.current.tasks).toHaveLength(1)
    expect(result.current.tasks[0].id).not.toBe(failedId)
    expect(result.current.tasks[0].input.prompt).toBe('p1')
    expect(result.current.tasks[0].status).toBe('running')
    expect(generateImages).toHaveBeenCalledTimes(2)
  })

  it('卸载：生成中任务静默取消，不写历史', async () => {
    const { result, unmount } = renderHook(() => useGenerationQueue())
    await act(async () => result.current.enqueue(makeInput('p1')))
    unmount()
    await act(async () => { pending[0].reject(new ImagesApiError({ key: 'timeout', detail: '' })) })
    expect(createGeneration).not.toHaveBeenCalled()
  })

  it('已结束保留上限 10：第 11 个完成时淘汰最旧的', async () => {
    const { result } = renderHook(() => useGenerationQueue())
    for (let i = 1; i <= MAX_FINISHED_KEPT + 1; i++) {
      await act(async () => result.current.enqueue(makeInput(`p${i}`)))
      await act(async () => { pending[i - 1].resolve([IMG]) })
    }
    expect(result.current.finishedCount).toBe(MAX_FINISHED_KEPT)
    expect(result.current.tasks[0].input.prompt).toBe('p2') // p1 被淘汰
    expect(createGeneration).toHaveBeenCalledTimes(MAX_FINISHED_KEPT + 1) // 历史一条不少
  })
})
