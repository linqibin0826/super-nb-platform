import { useCallback, useEffect, useRef, useState } from 'react'
import { generateImages, ImagesApiError, type GeneratedImage } from '../lib/imagesApi'
import type { MappedImagesError } from '../lib/errors'
import { createGeneration } from '../lib/generationsApi'
import { filesToRefB64 } from '../lib/fileToBase64'
import type { Quality } from '../App'

export const MAX_CONCURRENT = 5
export const MAX_PENDING = 10
export const MAX_FINISHED_KEPT = 10

export interface GenerateInput {
  apiKey: string
  keyId: number
  groupName: string
  /** 用户所选生图模型名 */
  model: string
  prompt: string
  size: string
  n: number
  quality: Quality
  /** 参考图（图生图），透传给 imagesApi：非空走 edits multipart */
  images?: File[]
  /** 本次预估消耗（额度美元），随记录落库；null/缺省 = 未知不落 */
  cost?: number | null
}

export interface Batch {
  images: GeneratedImage[]
  elapsedMs: number
}

export type GenTaskStatus = 'queued' | 'running' | 'done' | 'error'

export interface GenTask {
  id: string
  input: GenerateInput
  status: GenTaskStatus
  createdAt: number
  startedAt?: number
  batch?: Batch
  error?: MappedImagesError
}

export interface GenerationQueue {
  tasks: GenTask[]
  /** 1s ticker（有 running 才转）；elapsed 用 elapsedSecondsOf(task, now) 派生 */
  now: number
  runningCount: number
  queuedCount: number
  finishedCount: number
  queueFull: boolean
  historyVersion: number
  enqueue: (input: GenerateInput) => void
  cancelTask: (id: string) => void
  retryTask: (id: string) => void
}

export function elapsedSecondsOf(task: GenTask, now: number): number {
  if (task.status === 'done' && task.batch) return Math.round(task.batch.elapsedMs / 1000)
  if (task.startedAt === undefined) return 0
  return Math.max(0, Math.round((now - task.startedAt) / 1000))
}

/** 队列 hook：FIFO、并发封顶 MAX_CONCURRENT、未完成上限 MAX_PENDING。
 *  取消（排队/生成中/卸载）一律不写历史；完成/失败照旧写 IndexedDB（旧 useGeneration 同语义）。 */
export function useGenerationQueue(): GenerationQueue {
  const [tasks, setTasks] = useState<GenTask[]>([])
  const [now, setNow] = useState(() => Date.now())
  const [historyVersion, setHistoryVersion] = useState(0)

  const controllers = useRef(new Map<string, AbortController>())
  const cancelled = useRef(new Set<string>())
  // StrictMode 会双跑 effect：已启动的任务 id 记账，防同一任务起两路请求
  const startedIds = useRef(new Set<string>())

  const runningCount = tasks.filter((task) => task.status === 'running').length
  const queuedCount = tasks.filter((task) => task.status === 'queued').length
  const finishedCount = tasks.length - runningCount - queuedCount
  const queueFull = runningCount + queuedCount >= MAX_PENDING

  const settle = useCallback(
    async (
      task: GenTask,
      patch: Partial<GenTask>,
      images: Array<{ b64: string }>,
      errText: string | undefined,
      startedAt: number
    ) => {
      setTasks((prev) => {
        const next = prev.map((item) => (item.id === task.id ? { ...item, ...patch } : item))
        // 已结束只留最近 MAX_FINISHED_KEPT 条：连 b64 一起清出内存（历史里都有）
        const finished = next.filter((item) => item.status === 'done' || item.status === 'error')
        if (finished.length <= MAX_FINISHED_KEPT) return next
        const dropIds = new Set(finished.slice(0, finished.length - MAX_FINISHED_KEPT).map((item) => item.id))
        return next.filter((item) => !dropIds.has(item.id))
      })
      try {
        const refImages = await filesToRefB64(task.input.images ?? [])
        await createGeneration({
          prompt: task.input.prompt,
          size: task.input.size,
          n: task.input.n,
          quality: task.input.quality,
          status: patch.status === 'done' ? 'done' : 'error',
          cost: task.input.cost ?? null,
          elapsedMs: Date.now() - startedAt,
          groupName: task.input.groupName,
          keyId: task.input.keyId,
          error: errText ?? null,
          outputImages: images,
          refImages,
        })
        setHistoryVersion((v) => v + 1)
      } catch {
        // 保存历史失败（网络 / R2 / 写库）不阻断当前会话：结果卡仍在，只是这条没落库
      }
    },
    []
  )

  const run = useCallback(
    async (task: GenTask, startedAt: number) => {
      const controller = new AbortController()
      controllers.current.set(task.id, controller)
      try {
        const images = await generateImages({
          apiKey: task.input.apiKey,
          model: task.input.model,
          prompt: task.input.prompt,
          size: task.input.size,
          n: task.input.n,
          quality: task.input.quality,
          images: task.input.images,
          signal: controller.signal,
        })
        if (cancelled.current.has(task.id)) return
        await settle(
          task,
          { status: 'done', batch: { images, elapsedMs: Date.now() - startedAt } },
          images.map((image) => ({ b64: image.b64 })),
          undefined,
          startedAt
        )
      } catch (err) {
        if (cancelled.current.has(task.id)) return
        const mapped: MappedImagesError =
          err instanceof ImagesApiError
            ? err.mapped
            : { key: 'unknown', detail: err instanceof Error ? err.message : String(err) }
        await settle(task, { status: 'error', error: mapped }, [], mapped.detail || mapped.key, startedAt)
      } finally {
        controllers.current.delete(task.id)
      }
    },
    [settle]
  )

  // 调度器：有空槽就按 FIFO 把 queued 提为 running 并发请求
  useEffect(() => {
    const capacity = MAX_CONCURRENT - tasks.filter((task) => task.status === 'running').length
    if (capacity <= 0) return
    const toStart = tasks
      .filter((task) => task.status === 'queued' && !startedIds.current.has(task.id))
      .slice(0, capacity)
    if (toStart.length === 0) return
    const startedAt = Date.now()
    for (const task of toStart) startedIds.current.add(task.id)
    setNow(startedAt)
    setTasks((prev) =>
      prev.map((task) =>
        task.status === 'queued' && toStart.some((item) => item.id === task.id)
          ? { ...task, status: 'running', startedAt }
          : task
      )
    )
    for (const task of toStart) void run(task, startedAt)
  }, [tasks, run])

  // 1s ticker：有 running 才转
  useEffect(() => {
    if (runningCount === 0) return
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [runningCount])

  // 卸载视同全部取消：静默 abort，不记假失败历史（旧 useGeneration 同语义）
  useEffect(
    () => () => {
      for (const [id, controller] of controllers.current) {
        cancelled.current.add(id)
        controller.abort()
      }
    },
    []
  )

  const enqueue = useCallback((input: GenerateInput) => {
    setTasks((prev) => {
      const pendingCount = prev.filter((task) => task.status === 'queued' || task.status === 'running').length
      if (pendingCount >= MAX_PENDING) return prev
      return [...prev, { id: crypto.randomUUID(), input, status: 'queued', createdAt: Date.now() }]
    })
  }, [])

  const cancelTask = useCallback((id: string) => {
    cancelled.current.add(id)
    controllers.current.get(id)?.abort()
    controllers.current.delete(id)
    setTasks((prev) => prev.filter((task) => task.id !== id))
  }, [])

  const retryTask = useCallback((id: string) => {
    setTasks((prev) => {
      const target = prev.find((task) => task.id === id)
      if (!target || target.status !== 'error') return prev
      return [
        ...prev.filter((task) => task.id !== id),
        { id: crypto.randomUUID(), input: target.input, status: 'queued' as const, createdAt: Date.now() },
      ]
    })
  }, [])

  return {
    tasks,
    now,
    runningCount,
    queuedCount,
    finishedCount,
    queueFull,
    historyVersion,
    enqueue,
    cancelTask,
    retryTask,
  }
}
