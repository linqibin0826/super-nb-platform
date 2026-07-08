import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'
import { useInteractions, type InteractionSeed } from '../useInteractions'
import * as api from '../../lib/galleryApi'

afterEach(() => vi.restoreAllMocks())

describe('useInteractions', () => {
  it('未登录 toggle → onRequireLogin，不发请求', async () => {
    const spy = vi.spyOn(api, 'toggleLike')
    const onLogin = vi.fn()
    const { result } = renderHook(() => useInteractions(['1'], false, onLogin))
    await act(async () => { await result.current.toggle('like', '1') })
    expect(onLogin).toHaveBeenCalledOnce()
    expect(spy).not.toHaveBeenCalled()
  })

  it('乐观点赞：即时填充，成功后用服务端计数校正', async () => {
    vi.spyOn(api, 'fetchInteractions').mockResolvedValue({ liked: [], favorited: [] })
    vi.spyOn(api, 'toggleLike').mockResolvedValue({ likeCount: 6, liked: true })
    const { result } = renderHook(() => useInteractions(['7'], true, vi.fn()))
    await act(async () => { await result.current.toggle('like', '7') })
    expect(result.current.liked.has('7')).toBe(true)
    expect(result.current.likeCounts.get('7')).toBe(6)
  })

  it('点赞失败 → 回滚填充态', async () => {
    vi.spyOn(api, 'fetchInteractions').mockResolvedValue({ liked: [], favorited: [] })
    vi.spyOn(api, 'toggleLike').mockRejectedValue(new Error('boom'))
    const { result } = renderHook(() => useInteractions(['7'], true, vi.fn()))
    await act(async () => { await result.current.toggle('like', '7') })
    expect(result.current.liked.has('7')).toBe(false)
  })

  it('挂载回填本批 my-state', async () => {
    vi.spyOn(api, 'fetchInteractions').mockResolvedValue({ liked: ['1'], favorited: ['2'] })
    const { result } = renderHook(() => useInteractions(['1', '2'], true, vi.fn()))
    await waitFor(() => expect(result.current.liked.has('1')).toBe(true))
    expect(result.current.favorited.has('2')).toBe(true)
  })

  it('seed 初始已知：种子里的收藏首帧即点亮', () => {
    vi.spyOn(api, 'fetchInteractions').mockResolvedValue({ liked: [], favorited: [] })
    const { result } = renderHook(() =>
      useInteractions(['9'], true, vi.fn(), { favorited: ['9'] })
    )
    expect(result.current.favorited.has('9')).toBe(true) // 回填前即为真
  })

  it('增量回填：ids 增长只请求新增的 id，不重复请求旧的', async () => {
    const spy = vi.spyOn(api, 'fetchInteractions').mockResolvedValue({ liked: [], favorited: [] })
    const { rerender } = renderHook(({ ids }) => useInteractions(ids, true, vi.fn()), {
      initialProps: { ids: ['1', '2'] },
    })
    await waitFor(() => expect(spy).toHaveBeenCalledWith(['1', '2']))
    rerender({ ids: ['1', '2', '3'] })
    await waitFor(() => expect(spy).toHaveBeenCalledWith(['3'])) // 只请求新增的 3，不是 [1,2,3]
  })

  it('回填不覆盖已 toggle 掉的 id（防竞态复活）', async () => {
    // 一个可控 promise：先 toggle 掉，再让回填带着旧「已赞」值落地
    let resolveFill: (v: { liked: string[]; favorited: string[] }) => void = () => {}
    vi.spyOn(api, 'fetchInteractions').mockReturnValue(
      new Promise((res) => {
        resolveFill = res
      }) as Promise<{ liked: string[]; favorited: string[] }>
    )
    vi.spyOn(api, 'toggleLike').mockResolvedValue({ likeCount: 0, liked: false })
    const { result } = renderHook(() => useInteractions(['7'], true, vi.fn(), { liked: ['7'] }))
    // 用户取消赞（7 本是种子点亮）
    await act(async () => {
      await result.current.toggle('like', '7')
    })
    expect(result.current.liked.has('7')).toBe(false)
    // 在途回填此刻才落地、仍带旧「7 已赞」——不得把 7 复活
    await act(async () => {
      resolveFill({ liked: ['7'], favorited: [] })
    })
    expect(result.current.liked.has('7')).toBe(false)
  })

  it('seed 异步增长：后加入 seed 的 id 也点亮', async () => {
    vi.spyOn(api, 'fetchInteractions').mockResolvedValue({ liked: [], favorited: [] })
    const { result, rerender } = renderHook(
      ({ seed }: { seed: InteractionSeed }) => useInteractions(['9', '10'], true, vi.fn(), seed),
      { initialProps: { seed: { favorited: ['9'] } } }
    )
    expect(result.current.favorited.has('9')).toBe(true)
    expect(result.current.favorited.has('10')).toBe(false)
    rerender({ seed: { favorited: ['9', '10'] } })
    await waitFor(() => expect(result.current.favorited.has('10')).toBe(true))
  })

  it('回填被后续 ids 变化超车：迟到的成功响应仍应用、不丢弃', async () => {
    let resolveFirst: (v: { liked: string[]; favorited: string[] }) => void = () => {}
    const spy = vi.spyOn(api, 'fetchInteractions').mockImplementation((reqIds: string[]) => {
      if (reqIds.join(',') === '1,2') {
        return new Promise((res) => {
          resolveFirst = res
        }) as Promise<{ liked: string[]; favorited: string[] }>
      }
      return Promise.resolve({ liked: [], favorited: [] })
    })
    const { result, rerender } = renderHook(
      ({ ids }: { ids: string[] }) => useInteractions(ids, true, vi.fn()),
      { initialProps: { ids: ['1', '2'] } }
    )
    rerender({ ids: ['1', '2', '3'] })
    await waitFor(() => expect(spy).toHaveBeenCalledWith(['3']))
    await act(async () => {
      resolveFirst({ liked: ['1'], favorited: [] })
    })
    expect(result.current.liked.has('1')).toBe(true)
  })
})
