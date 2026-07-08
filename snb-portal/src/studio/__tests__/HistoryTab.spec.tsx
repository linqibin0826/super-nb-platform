import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, cleanup, waitFor, fireEvent } from '@testing-library/react'

vi.mock('../../lib/generationsApi', () => ({
  listGenerations: vi.fn(),
  getGeneration: vi.fn(),
  deleteGeneration: vi.fn(),
}))
const mockUser = { current: { id: 1 } as { id: number } | null }
vi.mock('../../auth/useAuth', () => ({ useAuthUser: () => mockUser.current }))
vi.mock('../../auth/apiFetch', () => ({ loginUrl: () => 'https://api.super-nb.me/login' }))

import { HistoryTab } from '../HistoryTab'
import { listGenerations, getGeneration } from '../../lib/generationsApi'

const L = listGenerations as ReturnType<typeof vi.fn>
const G = getGeneration as ReturnType<typeof vi.fn>

const noop = () => {}
afterEach(cleanup)
beforeEach(() => {
  vi.clearAllMocks()
  mockUser.current = { id: 1 }
})

describe('HistoryTab', () => {
  it('未登录 → 引导登录', () => {
    mockUser.current = null
    render(<HistoryTab refreshToken={0} onApply={noop} onPreview={noop} onGoGallery={noop} />)
    expect(screen.getByText('Log in to see your history')).toBeTruthy()
  })

  it('登录 → 拉列表渲染行卡', async () => {
    L.mockResolvedValue({
      items: [{ id: 'g1', createdAt: '2026-07-06T10:00:00Z', prompt: 'a cat', size: '1024x1024',
        n: 1, quality: 'medium', status: 'done', cost: 0.04, elapsedMs: 1200,
        error: null, thumbUrl: 'https://fake-r2/gen/1/g1/0.png' }],
      total: 1, page: 1, pages: 1,
    })
    render(<HistoryTab refreshToken={0} onApply={noop} onPreview={noop} onGoGallery={noop} />)
    await waitFor(() => expect(screen.getByText('a cat')).toBeTruthy())
  })

  it('点行卡 → 拉详情、展示参考图区', async () => {
    L.mockResolvedValue({
      items: [{ id: 'g1', createdAt: '2026-07-06T10:00:00Z', prompt: 'a cat', size: '1024x1024',
        n: 1, quality: 'medium', status: 'done', cost: 0.04, elapsedMs: 1200,
        error: null, thumbUrl: 'https://fake-r2/x' }],
      total: 1, page: 1, pages: 1,
    })
    G.mockResolvedValue({
      id: 'g1', createdAt: '2026-07-06T10:00:00Z', prompt: 'a cat', size: '1024x1024', n: 1,
      quality: 'medium', status: 'done', cost: 0.04, elapsedMs: 1200, error: null,
      groupName: 'g', keyId: 7,
      outputImages: [{ url: 'https://fake-r2/o0', width: 1024, height: 1024 }],
      refImages: [{ url: 'https://fake-r2/r0' }],
    })
    render(<HistoryTab refreshToken={0} onApply={noop} onPreview={noop} onGoGallery={noop} />)
    await waitFor(() => expect(screen.getByText('a cat')).toBeTruthy())
    fireEvent.click(screen.getByText('a cat'))
    await waitFor(() => expect(screen.getByText('Reference images')).toBeTruthy())
  })
})
