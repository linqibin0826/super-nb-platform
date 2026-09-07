// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'

// 模块级 t 在 import 时定 locale，须在业务模块导入前钉死 zh（照 EbookLongRead.spec 手法）
vi.hoisted(() => {
  localStorage.setItem('sub2api_locale', 'zh')
})

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
    expect(screen.getAllByText('内容中心').length).toBeGreaterThan(0)
    expect(screen.getByTestId('hub-list')).toBeTruthy()
  })

  it('旧阅读路由 /reader/:slug 重定向文章页（带常规顶栏）', () => {
    render(
      <MemoryRouter initialEntries={['/reader/some-book']}>
        <AppRoutes />
      </MemoryRouter>,
    )
    expect(screen.getByTestId('hub-article')).toBeTruthy() // 落在文章页（loading 态）
    expect(screen.getAllByText('内容中心').length).toBeGreaterThan(0) // AppHeader 已挂载
  })

  it('页脚整理声明全路由可见（整理自互联网 + 侵权联删）', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <AppRoutes />
      </MemoryRouter>,
    )
    const foot = screen.getByTestId('hub-foot')
    expect(foot.textContent).toContain('整理自互联网')
    expect(foot.textContent).toContain('侵权')
  })
})
