import { beforeEach, describe, expect, it } from 'vitest'
import {
  clearTokens, getToken, getUserSnapshot, isExpiringSoon, setTokens,
} from '../tokens'

beforeEach(() => localStorage.clear())

describe('tokens', () => {
  it('setTokens 写三 key，getToken 读回', () => {
    setTokens('at', 'rt', 3600)
    expect(getToken()).toBe('at')
    expect(localStorage.getItem('refresh_token')).toBe('rt')
    const at = Number(localStorage.getItem('token_expires_at'))
    expect(at).toBeGreaterThan(Date.now() + 3500 * 1000)
  })
  it('isExpiringSoon：临期 60s 内为 true，无值为 false', () => {
    expect(isExpiringSoon()).toBe(false)
    localStorage.setItem('token_expires_at', String(Date.now() + 30_000))
    expect(isExpiringSoon()).toBe(true)
    localStorage.setItem('token_expires_at', String(Date.now() + 300_000))
    expect(isExpiringSoon()).toBe(false)
  })
  it('getUserSnapshot：raw 未变返回同一引用（快照稳定）', () => {
    localStorage.setItem('auth_user', JSON.stringify({ id: 1, email: 'a@b.c' }))
    const first = getUserSnapshot()
    expect(first).toEqual({ id: 1, email: 'a@b.c' })
    expect(getUserSnapshot()).toBe(first)
    clearTokens()
    expect(getUserSnapshot()).toBeNull()
  })
})
