/**
 * @vitest-environment jsdom
 * @vitest-environment-options {"url": "https://studio.super-nb.me/studio/"}
 *
 * 环境 URL 必须是 https 的 *.super-nb.me：jsdom 会真校验 Secure（http 下写入被丢）
 * 和 Domain=.super-nb.me（不匹配的域写入被拒），这里连属性一起验。
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  isTombstone,
  readAuthCookie,
  writeAuthCookie,
  writeAuthTombstone,
} from '../cookie'
import {
  AUTH_TOKEN_KEY,
  AUTH_USER_KEY,
  REFRESH_TOKEN_KEY,
  TOKEN_EXPIRES_AT_KEY,
  clearTokens,
  isLoggedIn,
  reconcileFromCookie,
  setTokens,
} from '../tokens'

const SESSION = {
  at: 'at-cookie',
  rt: 'rt-cookie',
  exp: Date.now() + 3600_000,
  user: { id: 7, email: 'u@x.com' },
}

function seedLocal(at: string, rt: string, expMs: number) {
  localStorage.setItem(AUTH_TOKEN_KEY, at)
  localStorage.setItem(REFRESH_TOKEN_KEY, rt)
  localStorage.setItem(TOKEN_EXPIRES_AT_KEY, String(expMs))
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify({ id: 1, email: 'local@x.com' }))
}

function wipeCookie() {
  document.cookie = '__Secure-snb_auth=; Domain=.super-nb.me; Path=/; Secure; Max-Age=0'
}

beforeEach(() => {
  localStorage.clear()
  wipeCookie()
  vi.restoreAllMocks()
})

describe('cookie 契约', () => {
  it('会话写读往返一致', () => {
    writeAuthCookie(SESSION)
    const got = readAuthCookie()
    expect(got).toEqual({ v: 1, ...SESSION })
  })

  it('墓碑写读与判别', () => {
    writeAuthTombstone()
    const got = readAuthCookie()
    expect(got && isTombstone(got)).toBe(true)
  })

  it('脏值/结构不完整一律当不存在', () => {
    document.cookie = '__Secure-snb_auth=%7Bnot-json; Domain=.super-nb.me; Path=/; Secure'
    expect(readAuthCookie()).toBeNull()
    document.cookie =
      '__Secure-snb_auth=' +
      encodeURIComponent(JSON.stringify({ v: 2, at: 'x' })) +
      '; Domain=.super-nb.me; Path=/; Secure'
    expect(readAuthCookie()).toBeNull()
  })
})

describe('reconcileFromCookie', () => {
  it('cookie 会话 + 空本地 → 收养四件套', () => {
    writeAuthCookie(SESSION)
    reconcileFromCookie()
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBe('at-cookie')
    expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBe('rt-cookie')
    expect(localStorage.getItem(TOKEN_EXPIRES_AT_KEY)).toBe(String(SESSION.exp))
    expect(isLoggedIn()).toBe(true)
  })

  it('cookie 墓碑 + 本地会话 → 本地被清、不复活', () => {
    seedLocal('at-old', 'rt-old', Date.now() + 1000)
    writeAuthTombstone()
    reconcileFromCookie()
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull()
    const got = readAuthCookie()
    expect(got && isTombstone(got)).toBe(true)
  })

  it('cookie 缺失 + 本地会话 → 镜像出去（存量迁移）', () => {
    seedLocal('at-local', 'rt-local', 12345)
    reconcileFromCookie()
    const got = readAuthCookie()
    expect(got && !isTombstone(got) && got.at).toBe('at-local')
  })

  it('cookie 更旧（exp 落后本地）→ 本地覆盖 cookie', () => {
    seedLocal('at-new', 'rt-new', Date.now() + 7200_000)
    writeAuthCookie({ ...SESSION, exp: Date.now() + 1000 })
    reconcileFromCookie()
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBe('at-new')
    const got = readAuthCookie()
    expect(got && !isTombstone(got) && got.at).toBe('at-new')
  })

  it('token 相同 → 幂等不动', () => {
    seedLocal('at-cookie', 'rt-cookie', SESSION.exp)
    writeAuthCookie(SESSION)
    reconcileFromCookie()
    expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBe('rt-cookie')
    expect(JSON.parse(localStorage.getItem(AUTH_USER_KEY)!).email).toBe('local@x.com')
  })
})

describe('写通', () => {
  it('setTokens 回写 cookie（唯一真源）', () => {
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify({ id: 3, email: 'a@b.c' }))
    setTokens('at-9', 'rt-9', 3600)
    const got = readAuthCookie()
    expect(got && !isTombstone(got) && got.at).toBe('at-9')
    expect(got && !isTombstone(got) && got.user.id).toBe(3)
  })

  it('clearTokens 落墓碑', () => {
    seedLocal('at-1', 'rt-1', Date.now() + 1000)
    clearTokens()
    const got = readAuthCookie()
    expect(got && isTombstone(got)).toBe(true)
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull()
  })
})
