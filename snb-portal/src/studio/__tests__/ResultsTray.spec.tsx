import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, cleanup } from '@testing-library/react'
import { ResultsTray } from '../ResultsTray'
import { queueLabel } from '../queueLabel'
import { t } from '../../i18n'
import type { GenerationQueue, GenTask } from '../useGenerationQueue'

const baseInput = {
  apiKey: 'sk', keyId: 1, groupName: 'g', model: 'gpt-image-2', prompt: 'p',
  size: '1024x1024', n: 1, quality: 'medium' as const, cost: null,
}

function makeTask(id: string, over: Partial<GenTask> = {}): GenTask {
  return { id, input: { ...baseInput, prompt: `prompt-${id}` }, status: 'running', createdAt: 1, startedAt: 1, ...over }
}

function makeQueue(tasks: GenTask[], over: Partial<GenerationQueue> = {}): GenerationQueue {
  const running = tasks.filter((task) => task.status === 'running').length
  const queued = tasks.filter((task) => task.status === 'queued').length
  return {
    tasks, now: 5000,
    runningCount: running, queuedCount: queued, finishedCount: tasks.length - running - queued,
    queueFull: false, historyVersion: 0,
    enqueue: vi.fn(), cancelTask: vi.fn(), retryTask: vi.fn(),
    ...over,
  }
}

function renderTray(queue: GenerationQueue) {
  const onClose = vi.fn()
  const view = render(<ResultsTray queue={queue} onPreview={vi.fn()} onClose={onClose} />)
  return { onClose, view }
}

afterEach(() => cleanup())

describe('ResultsTray 票据栈', () => {
  it('节头：标题 + 聚合 meta + 收起钮', () => {
    const queue = makeQueue([makeTask('a'), makeTask('b', { status: 'queued', startedAt: undefined })])
    const { onClose } = renderTray(queue)
    expect(screen.getByText(t('studio.queue.title'))).toBeTruthy()
    expect(screen.getByText(queueLabel(1, 1, 0))).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: new RegExp(t('studio.results.collapse')) }))
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('默认展开最新任务（栈底）', () => {
    const queue = makeQueue([makeTask('a'), makeTask('b', { status: 'queued', startedAt: undefined })])
    renderTray(queue)
    const rows = screen.getAllByRole('button', { name: /prompt-/ })
    expect(rows[0].getAttribute('aria-expanded')).toBe('false') // a
    expect(rows[1].getAttribute('aria-expanded')).toBe('true') // b（最新）
  })

  it('点旧卡行头切换展开（互斥）', () => {
    const queue = makeQueue([makeTask('a'), makeTask('b', { status: 'queued', startedAt: undefined })])
    renderTray(queue)
    fireEvent.click(screen.getByRole('button', { name: 'prompt-a' }))
    const rows = screen.getAllByRole('button', { name: /prompt-/ })
    expect(rows[0].getAttribute('aria-expanded')).toBe('true')
    expect(rows[1].getAttribute('aria-expanded')).toBe('false')
  })

  it('点展开中的卡行头可收起——允许全部收起（站长 2026-07-05 反馈）', () => {
    const queue = makeQueue([makeTask('a'), makeTask('b', { status: 'queued', startedAt: undefined })])
    renderTray(queue)
    fireEvent.click(screen.getByRole('button', { name: 'prompt-b' })) // b 默认展开，点它收起
    const rows = screen.getAllByRole('button', { name: /prompt-/ })
    expect(rows[0].getAttribute('aria-expanded')).toBe('false')
    expect(rows[1].getAttribute('aria-expanded')).toBe('false')
  })

  it('queued 卡位次按排队序号计（1 起）', () => {
    const queue = makeQueue([
      makeTask('a'),
      makeTask('b', { status: 'queued', startedAt: undefined }),
      makeTask('c', { status: 'queued', startedAt: undefined }),
    ])
    renderTray(queue)
    expect(screen.getAllByText(t('studio.queue.position', { pos: 1 })).length).toBeGreaterThan(0)
    expect(screen.getAllByText(t('studio.queue.position', { pos: 2 })).length).toBeGreaterThan(0)
  })

  it('取消/重试透传 queue 方法', () => {
    const queue = makeQueue([
      makeTask('a', { status: 'error', startedAt: undefined, error: { key: 'upstream', detail: '' } }),
      makeTask('b'),
    ])
    renderTray(queue)
    fireEvent.click(screen.getByRole('button', { name: t('studio.queue.cancel') }))
    expect(queue.cancelTask).toHaveBeenCalledWith('b')
    fireEvent.click(screen.getAllByRole('button', { name: t('studio.queue.retry') })[0])
    expect(queue.retryTask).toHaveBeenCalledWith('a')
  })
})
