import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, keysUrl } from '../apiFetch'

beforeEach(() => {
  localStorage.clear()
  localStorage.setItem('auth_token', 'at-1')
  localStorage.setItem('auth_user', '{"id":1,"email":"a@b.c"}')
  localStorage.setItem('refresh_token', 'rt-1')
  localStorage.setItem('token_expires_at', String(Date.now() + 3_600_000))
})
afterEach(() => vi.restoreAllMocks())

const ok = (data: unknown) =>
  new Response(JSON.stringify({ code: 0, message: 'ok', data }), { status: 200 })

describe('apiFetch', () => {
  it('带 Bearer 并解包 data', async () => {
    const mock = vi.fn().mockResolvedValue(ok({ v: 1 }))
    vi.stubGlobal('fetch', mock)
    expect(await apiFetch<{ v: number }>('/keys')).toEqual({ v: 1 })
    const [url, init] = mock.mock.calls[0]
    expect(url).toBe('/api/v1/keys')
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer at-1')
  })
  it('code!==0 抛 ApiError', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 7, message: 'boom' }), { status: 200 })
    ))
    await expect(apiFetch('/keys')).rejects.toThrowError(ApiError)
  })
  it('401 → refresh 成功 → 用新 token 重试一次', async () => {
    const mock = vi.fn()
      .mockResolvedValueOnce(new Response('{"code":401}', { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        code: 0, data: { access_token: 'at-2', refresh_token: 'rt-2', expires_in: 3600 },
      }), { status: 200 }))
      .mockResolvedValueOnce(ok({ v: 2 }))
    vi.stubGlobal('fetch', mock)
    expect(await apiFetch<{ v: number }>('/keys')).toEqual({ v: 2 })
    expect((mock.mock.calls[2][1].headers as Record<string, string>).Authorization).toBe('Bearer at-2')
  })
  it('401 → refresh 失败 → 抛错且 token 已清', async () => {
    const mock = vi.fn()
      .mockResolvedValueOnce(new Response('{"code":401}', { status: 401 }))
      .mockResolvedValueOnce(new Response('{"code":401}', { status: 401 }))
    vi.stubGlobal('fetch', mock)
    await expect(apiFetch('/keys')).rejects.toThrowError(ApiError)
    expect(localStorage.getItem('auth_token')).toBeNull()
  })
})

describe('keysUrl（同源/本地部署）', () => {
  it('非 super-nb.me 子域名下返回本站相对路径', () => {
    expect(keysUrl()).toBe('/keys')
  })
})
