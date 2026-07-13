import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, cleanup, waitFor } from '@testing-library/react'
import { t } from '../i18n'
import { queueLabel } from '../studio/queueLabel'
import type { GenerationQueue } from '../studio/useGenerationQueue'

// —— App 的重依赖全 mock 掉，只测悬浮栈（托盘/胶囊/票据）的接线 ——
vi.mock('../auth/useAuth', () => ({ useAuthUser: () => ({ id: 1 }) }))
vi.mock('../keys/useEligibleKeys', () => ({
  useEligibleKeys: () => ({ loading: false, keys: [], rates: {} }),
}))
vi.mock('../studio/GalleryTab', () => ({ GalleryTab: () => null }))
vi.mock('../studio/TopBar', () => ({ TopBar: () => null }))

const queueState: GenerationQueue = {
  tasks: [
    {
      id: 't1',
      input: { apiKey: 'sk', keyId: 1, groupName: 'g', model: 'gpt-image-2', prompt: 'a cat', size: '1024x1024', n: 1, quality: 'medium', cost: null },
      status: 'running',
      createdAt: 1000,
      startedAt: 1000,
    },
  ],
  now: 4000,
  runningCount: 1,
  queuedCount: 0,
  finishedCount: 0,
  queueFull: false,
  historyVersion: 0,
  enqueue: vi.fn(),
  cancelTask: vi.fn(),
  retryTask: vi.fn(),
}
vi.mock('../studio/useGenerationQueue', async (orig) => {
  const actual = await orig<typeof import('../studio/useGenerationQueue')>()
  return { ...actual, useGenerationQueue: () => queueState }
})

import App from '../App'

beforeAll(() => {
  vi.stubGlobal(
    'ResizeObserver',
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
    }
  )
  // useTheme 直接调 window.matchMedia，jsdom 没有
  vi.stubGlobal(
    'matchMedia',
    (query: string) => ({
      matches: false,
      media: query,
      addEventListener() {},
      removeEventListener() {},
      addListener() {},
      removeListener() {},
      onchange: null,
      dispatchEvent: () => false,
    })
  )
})

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('生成中托盘被收起后的唤回', () => {
  it('有 running 任务且托盘未开时，出聚合胶囊，点击唤回队列托盘', () => {
    render(<App />)
    // 初始 trayOpen=false（托盘只在点生成时打开），生成中必须给唤回入口
    const chip = screen.getByRole('button', { name: new RegExp(queueLabel(1, 0, 0)) })
    fireEvent.click(chip)
    // 托盘出现：队列标题 + running 展开卡的耗时行（now-startedAt = 3s）
    expect(screen.getByText(t('studio.queue.title'))).toBeTruthy()
    expect(
      screen.getByText(new RegExp(t('playground.results.elapsed', { seconds: 3 })))
    ).toBeTruthy()
  })

  it('生成中托盘也有「收起」钮：收起只藏面板，胶囊立即回来可再唤回', async () => {
    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: new RegExp(queueLabel(1, 0, 0)) }))
    fireEvent.click(screen.getByRole('button', { name: new RegExp(t('studio.results.collapse')) }))
    // 托盘走 AnimatePresence 退场动画，等动画完卸载
    await waitFor(() => expect(screen.queryByText(t('studio.queue.title'))).toBeNull())
    expect(screen.getByRole('button', { name: new RegExp(queueLabel(1, 0, 0)) })).toBeTruthy()
  })
})
