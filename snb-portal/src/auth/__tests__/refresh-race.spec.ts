/**
 * @vitest-environment jsdom
 * @vitest-environment-options {"url": "https://studio.super-nb.me/studio/"}
 *
 * refresh token 一次性轮换下的跨源竞态：输了轮换的那方必须能从 cookie 收养赢家的新 token，
 * 而不是把全家会话清掉。
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { isTombstone, readAuthCookie, writeAuthCookie } from '../cookie'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY, REFRESH_TOKEN_KEY, TOKEN_EXPIRES_AT_KEY } from '../tokens'
import { refreshTokens } from '../refresh'

const USER = { id: 7, email: 'u@x.com' }

function seedLocal(at: string, rt: string, expMs: number) {
  localStorage.setItem(AUTH_TOKEN_KEY, at)
  localStorage.setItem(REFRESH_TOKEN_KEY, rt)
  localStorage.setItem(TOKEN_EXPIRES_AT_KEY, String(expMs))
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(USER))
}

beforeEach(() => {
  localStorage.clear()
  document.cookie = '__Secure-snb_auth=; Domain=.super-nb.me; Path=/; Secure; Max-Age=0'
  vi.restoreAllMocks()
})

describe('刷新协议 × cookie 竞态', () => {
  it('入口对账收养到新 token → 零请求返回 true', async () => {
    seedLocal('at-old', 'rt-old', Date.now() - 1000)
    writeAuthCookie({ at: 'at-new', rt: 'rt-new', exp: Date.now() + 3600_000, user: USER })
    const mock = vi.fn()
    vi.stubGlobal('fetch', mock)
    expect(await refreshTokens()).toBe(true)
    expect(mock).not.toHaveBeenCalled()
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBe('at-new')
    expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBe('rt-new')
  })

  it('刷新被拒但 cookie 已被赢家换新 → 收养返回 true', async () => {
    const exp = Date.now() + 60_000
    seedLocal('at-1', 'rt-1', exp)
    writeAuthCookie({ at: 'at-1', rt: 'rt-1', exp, user: USER })
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation(async () => {
        // 模拟并发赢家：本请求在途时对方已完成轮换并回写 cookie
        writeAuthCookie({ at: 'at-2', rt: 'rt-2', exp: Date.now() + 3600_000, user: USER })
        return new Response('{"code":401}', { status: 401 })
      })
    )
    expect(await refreshTokens()).toBe(true)
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBe('at-2')
    expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBe('rt-2')
  })

  it('彻底死透（cookie 无换新）→ 清会话 + 落墓碑', async () => {
    const exp = Date.now() + 60_000
    seedLocal('at-1', 'rt-1', exp)
    writeAuthCookie({ at: 'at-1', rt: 'rt-1', exp, user: USER })
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{"code":401}', { status: 401 })))
    expect(await refreshTokens()).toBe(false)
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull()
    const got = readAuthCookie()
    expect(got && isTombstone(got)).toBe(true)
  })
})
