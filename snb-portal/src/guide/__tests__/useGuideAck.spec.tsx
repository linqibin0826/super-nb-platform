import { describe, expect, it, vi, beforeEach } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'
import { useGuideAck } from '../useGuideAck'
import { getGuideAcks, GuideAuthError, postGuideAck } from '../api'

vi.mock('../api', async (importOriginal) => {
  const mod = await importOriginal<typeof import('../api')>()
  return { ...mod, getGuideAcks: vi.fn(), postGuideAck: vi.fn() }
})

const KEY = 'invoice.intro.v1'
const LS = 'snb_guide_ack:' + KEY

beforeEach(() => {
  localStorage.clear()
  vi.mocked(getGuideAcks).mockReset()
  vi.mocked(postGuideAck).mockReset().mockResolvedValue(undefined)
})

describe('useGuideAck 服务端真源 + 本地兜底', () => {
  it('服务端已 ack → 不弹,并写本地缓存', async () => {
    vi.mocked(getGuideAcks).mockResolvedValue([KEY, 'other.v1'])
    const { result } = renderHook(() => useGuideAck(KEY))
    await waitFor(() => expect(localStorage.getItem(LS)).toBe('1'))
    expect(result.current.show).toBe(false)
  })

  it('服务端未 ack 且本地无 → 弹;ack() 后收起+落服务端+落本地', async () => {
    vi.mocked(getGuideAcks).mockResolvedValue([])
    const { result } = renderHook(() => useGuideAck(KEY))
    await waitFor(() => expect(result.current.show).toBe(true))
    act(() => result.current.ack())
    expect(result.current.show).toBe(false)
    expect(postGuideAck).toHaveBeenCalledWith(KEY)
    expect(localStorage.getItem(LS)).toBe('1')
  })

  it('服务端未 ack 但本地读过(未登录时期 dismiss) → 不弹,静默补写服务端', async () => {
    localStorage.setItem(LS, '1')
    vi.mocked(getGuideAcks).mockResolvedValue([])
    const { result } = renderHook(() => useGuideAck(KEY))
    await waitFor(() => expect(postGuideAck).toHaveBeenCalledWith(KEY))
    expect(result.current.show).toBe(false)
  })

  it('未登录(401):本地无 → 弹;本地有 → 不弹', async () => {
    vi.mocked(getGuideAcks).mockRejectedValue(new GuideAuthError())
    const first = renderHook(() => useGuideAck(KEY))
    await waitFor(() => expect(first.result.current.show).toBe(true))

    localStorage.setItem(LS, '1')
    vi.mocked(getGuideAcks).mockRejectedValue(new GuideAuthError())
    const second = renderHook(() => useGuideAck(KEY))
    await new Promise((r) => setTimeout(r, 30))
    expect(second.result.current.show).toBe(false)
  })

  it('网络错误 → 静默不弹', async () => {
    vi.mocked(getGuideAcks).mockRejectedValue(new Error('HTTP 500'))
    const { result } = renderHook(() => useGuideAck(KEY))
    await new Promise((r) => setTimeout(r, 30))
    expect(result.current.show).toBe(false)
  })
})
