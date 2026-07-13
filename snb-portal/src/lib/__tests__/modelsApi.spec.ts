import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchModels } from '../modelsApi'

afterEach(() => vi.unstubAllGlobals())

describe('fetchModels', () => {
  it('用站内 key 调 /v1/models，提取 data[].id', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ data: [{ id: 'grok-imagine-image' }, { id: 'grok-imagine-edit' }] }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    )
    vi.stubGlobal('fetch', fetchMock)
    const ids = await fetchModels('sk-abc')
    expect(ids).toEqual(['grok-imagine-image', 'grok-imagine-edit'])
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/v1/models')
    expect(init.headers.Authorization).toBe('Bearer sk-abc')
  })

  it('非 200 → 返回空数组（不抛）', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('nope', { status: 403 })))
    expect(await fetchModels('sk')).toEqual([])
  })

  it('网络异常 → 返回空数组（不抛）', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    expect(await fetchModels('sk')).toEqual([])
  })
})
