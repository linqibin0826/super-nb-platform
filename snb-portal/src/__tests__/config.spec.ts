import { describe, it, expect } from 'vitest'
import { PARENT_DOMAIN, CONSOLE_ORIGIN, isParentDomainHost } from '../config'

describe('config 缺省值 = 现生产值(行为不变)', () => {
  it('父域名缺省 super-nb.me', () => {
    expect(PARENT_DOMAIN).toBe('super-nb.me')
    expect(CONSOLE_ORIGIN).toBe('https://super-nb.me')
  })
  it('isParentDomainHost 认根域与子域、拒他域', () => {
    expect(isParentDomainHost('super-nb.me')).toBe(true)
    expect(isParentDomainHost('studio.super-nb.me')).toBe(true)
    expect(isParentDomainHost('evil.com')).toBe(false)
  })
})
