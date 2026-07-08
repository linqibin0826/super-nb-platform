import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../../auth/tokens', () => ({ getToken: () => 'tok', isExpiringSoon: () => false }))
vi.mock('../../auth/refresh', () => ({ refreshTokens: vi.fn().mockResolvedValue(false) }))

import { createGeneration, listGenerations } from '../generationsApi'

describe('generationsApi', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => vi.unstubAllGlobals())

  it('createGeneration POST camelCase body 到 /gallery/v1/me/generations', async () => {
    ;(fetch as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ id: 'g1', createdAt: 'x' }),
    })
    await createGeneration({
      prompt: 'p', size: '1024x1024', n: 1, quality: 'medium',
      status: 'done', cost: 0.04, elapsedMs: 1000, groupName: 'g', keyId: 7, error: null,
      outputImages: [{ b64: 'QUJD' }], refImages: [{ b64: 'Rjeg', contentType: 'image/png' }],
    })
    const [url, init] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(url).toBe('/gallery/v1/me/generations')
    expect(init.method).toBe('POST')
    const body = JSON.parse(init.body)
    expect(body.elapsedMs).toBe(1000)          // 字段名与平台 DTO 一致，直接透传
    expect(body.outputImages).toEqual([{ b64: 'QUJD' }])
    expect(body.refImages[0].contentType).toBe('image/png')
  })

  it('listGenerations 拉分页信封', async () => {
    ;(fetch as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ items: [{ id: 'g1' }], total: 1, page: 1, pages: 1 }),
    })
    const res = await listGenerations(1)
    expect(res.total).toBe(1)
    expect((fetch as ReturnType<typeof vi.fn>).mock.calls[0][0]).toContain('/me/generations?page=1')
  })
})
