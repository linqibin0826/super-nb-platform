// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'

afterEach(cleanup)
import { MemoryRouter } from 'react-router-dom'
import { AppRoutes } from '../App'

// jsdom 缺失的浏览器 API（照 src/__tests__/App.spec.tsx 手法）
vi.stubGlobal('matchMedia', (q: string) => ({
  matches: false,
  media: q,
  addEventListener: () => {},
  removeEventListener: () => {},
}))
vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })

describe('hub AppRoutes', () => {
  it('列表路由渲染顶栏（站名内容中心）与列表骨架', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <AppRoutes />
      </MemoryRouter>,
    )
    expect(screen.getByText('内容中心')).toBeTruthy()
    expect(screen.getByTestId('hub-list')).toBeTruthy()
  })

  it('阅读路由不带常规顶栏（全视口 iframe 布局）', () => {
    render(
      <MemoryRouter initialEntries={['/reader/some-book']}>
        <AppRoutes />
      </MemoryRouter>,
    )
    expect(screen.getByTestId('hub-reader')).toBeTruthy()
    expect(screen.queryByText('创作工坊')).toBeNull() // AppHeader 未挂载
  })
})
