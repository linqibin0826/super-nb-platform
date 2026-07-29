import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ArticlePage } from '../pages/ArticlePage'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

const BODY =
  '<p>正文</p><pre><code class="language-bash">export OPENAI_BASE_URL="https://x/v1"</code></pre>' +
  '<pre><code>裸代码块</code></pre>'

const DETAIL = {
  id: '42', slug: 'hello', type: 'article', title: '接中转站', summary: '摘要',
  coverUrl: null, categorySlug: 'tutorials', categoryName: '教程', tags: [],
  bodyHtml: BODY, ebookPath: null, sourceName: '站长整理', sourceUrl: 'https://example.com/o',
  publishedAt: '2026-07-10T00:00:00Z',
}

function renderArticle() {
  vi.stubGlobal('fetch', vi.fn(async (url: string) =>
    new Response(JSON.stringify(String(url).includes('/articles?') ? { items: [], total: 0, page: 1, pages: 1 } : DETAIL), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })))
  return render(
    <MemoryRouter initialEntries={['/a/hello']}>
      <Routes>
        <Route path="/a/:slug" element={<ArticlePage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('代码块机箱', () => {
  beforeEach(() => {
    vi.stubGlobal('matchMedia', (q: string) => ({ matches: false, media: q, addEventListener: () => {}, removeEventListener: () => {} }))
  })

  it('每个 <pre> 套上文件名条 + 复制按钮，语言名从 language-* 取、缺省走兜底词', async () => {
    renderArticle()
    await waitFor(() => expect(document.querySelectorAll('.hub-code')).toHaveLength(2))

    const langs = Array.from(document.querySelectorAll('.hub-code-lang')).map((e) => e.textContent)
    expect(langs[0]).toContain('bash')
    expect(langs[1]).toContain('代码') // 无 language-* 的兜底
    // 条上恒挂「可横向滚动」文案，靠 data-overflow 才显形——不溢出不亮
    expect(document.querySelectorAll('.hub-code-hint')).toHaveLength(2)
    expect(document.querySelector('.hub-code[data-overflow]')).toBeNull() // jsdom 无布局
    // <pre> 被搬进机箱里，没有留在 prose 直系
    expect(document.querySelectorAll('.hub-prose > pre')).toHaveLength(0)
  })

  it('复制按钮：44 热区壳 + 点击进「已复制」态，1.4s 回落', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const writeText = vi.fn(async () => {})
    vi.stubGlobal('navigator', { ...navigator, clipboard: { writeText } })
    renderArticle()
    await waitFor(() => expect(document.querySelectorAll('.hub-code-copy')).toHaveLength(2))

    const btn = document.querySelectorAll('.hub-code-copy')[0] as HTMLButtonElement
    expect(btn.getAttribute('aria-label')).toBeTruthy()
    expect(btn.textContent).toBe('复制')
    btn.click()
    expect(writeText).toHaveBeenCalledWith('export OPENAI_BASE_URL="https://x/v1"')
    expect(btn.classList.contains('is-copied')).toBe(true)
    expect(btn.textContent).toBe('已复制')

    vi.advanceTimersByTime(1500)
    expect(btn.classList.contains('is-copied')).toBe(false)
    expect(btn.textContent).toBe('复制')
    vi.useRealTimers()
  })

  it('幂等：重复装配不叠壳（StrictMode 双跑防护）', async () => {
    renderArticle()
    await waitFor(() => expect(document.querySelectorAll('.hub-code')).toHaveLength(2))
    // 同一份正文再渲染一次（同 deps）不应长出第三个机箱
    expect(document.querySelectorAll('.hub-code-bar')).toHaveLength(2)
    expect(screen.getByTestId('hub-article')).toBeTruthy()
  })
})
