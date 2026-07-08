import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { refreshTokens } from '../refresh'

beforeEach(() => {
  localStorage.clear()
  localStorage.setItem('refresh_token', 'rt-1')
})
afterEach(() => vi.restoreAllMocks())

function okResponse() {
  return new Response(
    JSON.stringify({ code: 0, data: { access_token: 'at-2', refresh_token: 'rt-2', expires_in: 3600 } }),
    { status: 200, headers: { 'Content-Type': 'application/json' } }
  )
}

describe('refreshTokens', () => {
  it('成功：写回三 token，返回 true', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(okResponse()))
    expect(await refreshTokens()).toBe(true)
    expect(localStorage.getItem('auth_token')).toBe('at-2')
    expect(localStorage.getItem('refresh_token')).toBe('rt-2')
  })
  it('失败：清空 token，返回 false', async () => {
    localStorage.setItem('auth_token', 'at-1')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{"code":401}', { status: 401 })))
    expect(await refreshTokens()).toBe(false)
    expect(localStorage.getItem('auth_token')).toBeNull()
  })
  it('单飞：并发两次只发一个请求', async () => {
    const mock = vi.fn().mockResolvedValue(okResponse())
    vi.stubGlobal('fetch', mock)
    await Promise.all([refreshTokens(), refreshTokens()])
    expect(mock).toHaveBeenCalledTimes(1)
  })
  it('无 refresh_token 直接 false 不发请求', async () => {
    localStorage.clear()
    const mock = vi.fn()
    vi.stubGlobal('fetch', mock)
    expect(await refreshTokens()).toBe(false)
    expect(mock).not.toHaveBeenCalled()
  })
})
