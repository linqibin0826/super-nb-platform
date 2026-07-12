import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { EbookLongRead } from '../EbookLongRead'

// t() 是模块级单例（import 时定 locale）——hoisted 在 import 前钉死 zh，才能断言中文文案
vi.hoisted(() => {
  localStorage.setItem('sub2api_locale', 'zh')
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  localStorage.clear()
})

const BOOK = {
  title: 'OpenAI Codex 从入门到精通',
  subtitle: '面向工程师与产品经理',
  badge: '2026年5月 · v2.0.0',
  author: '花叔',
  metaLines: ['适用版本：CLI 0.130+'],
  totalMinutes: 137,
  chapters: [
    { index: 1, num: null, kind: 'preface', eyebrow: '序', title: '给读者的话', minutes: 5, html: '<p>前言正文</p>' },
    { index: 2, num: '01', kind: 'chapter', eyebrow: '基础', title: 'Codex 是什么', en: 'Five Forms', intro: '不是工具，是一套系统。', minutes: 12, html: '<p>正文</p>' },
    { index: 6, num: '05', kind: 'chapter', eyebrow: '进阶', title: 'AGENTS.md', intro: '怎么干活的契约。', minutes: 10, html: '<ol class="book-steps"><li><p>步骤</p></li></ol>' },
    { index: 13, num: 'B', kind: 'appendix', eyebrow: '附录', title: '定价', minutes: 3, html: '<p>附录正文</p>' },
  ],
}

function stubBook(book: unknown, ok = true) {
  vi.stubGlobal('fetch', vi.fn(async (url: string) =>
    String(url).endsWith('/book.json') && ok
      ? new Response(JSON.stringify(book), { status: 200, headers: { 'Content-Type': 'application/json' } })
      : new Response('nope', { status: 404 })))
}

function renderBook(opts: { slug?: string; part?: string } = {}) {
  const slug = opts.slug ?? 'codex-complete-guide-zh'
  const entry = opts.part ? `/a/${slug}/${opts.part}` : `/a/${slug}`
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path="/a/:slug/:part?" element={<EbookLongRead slug={slug} path={`books/${slug}`} />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('EbookLongRead（节目单方向·第六稿）', () => {
  beforeEach(() => {
    vi.stubGlobal('matchMedia', (q: string) => ({ matches: false, media: q, addEventListener: () => {}, removeEventListener: () => {} }))
    vi.stubGlobal('IntersectionObserver', class { observe() {} disconnect() {} })
  })

  it('目录：刊头（点题带/署名/统计）+ 幕间标题带真实统计 + 每讲一行（序号/钩子/时长墨条）', async () => {
    stubBook(BOOK)
    renderBook()
    await waitFor(() => expect(screen.getByTestId('hub-book-index')).toBeTruthy())
    // 刊头：bespoke 点题带；署名/参数行已拆（出处挪页底）
    expect(document.querySelector('.hub-forms')?.textContent).toContain('CLI')
    expect(document.querySelector('.hub-colophon')).toBeNull()
    // 页底出处：改编自 + 作者 X 链接
    const credit = screen.getByTestId('hub-book-credit')
    expect(credit.textContent).toContain('改编自 花叔 的原作')
    expect(credit.textContent).toContain('原文请查看作者的 X 主页')
    expect(credit.querySelector('a')!.getAttribute('href')).toBe('https://x.com/AlchainHust')
    expect(credit.querySelector('a')!.getAttribute('rel')).toContain('noopener')
    // 幕间标题：开篇/基础篇/进阶篇/附录，带统计
    const actNames = Array.from(document.querySelectorAll('.hub-act .name')).map((n) => n.textContent)
    expect(actNames).toEqual(['开篇', '基础篇', '进阶篇', '附录'])
    expect(Array.from(document.querySelectorAll('.hub-act')).map((n) => n.textContent).join(' ')).toContain('第 1 讲 · 12 分钟')
    // 行：4 行链到各自独立页；讲行有钩子，序/附录紧凑行无钩子
    const rows = Array.from(screen.getByTestId('hub-book-index').querySelectorAll('a.hub-row'))
    expect(rows).toHaveLength(4)
    expect(rows.map((a) => a.getAttribute('href'))).toEqual(
      ['/a/codex-complete-guide-zh/1', '/a/codex-complete-guide-zh/2', '/a/codex-complete-guide-zh/6', '/a/codex-complete-guide-zh/13'])
    expect(rows[1].textContent).toContain('01')
    expect(rows[1].textContent).toContain('不是工具，是一套系统。')
    expect(rows[1].querySelector('.rtrack i')).toBeTruthy()
    expect(rows[0].classList.contains('compact')).toBe(true)
    expect(rows[3].classList.contains('compact')).toBe(true)
    // 新访客 CTA：从第 1 讲开始
    expect(screen.getByText(/从第 1 讲开始读|Start with Lesson 1/).closest('a')!.getAttribute('href')).toBe('/a/codex-complete-guide-zh/1')
  })

  it('目录续读态：CTA 变「继续读·第 N 讲」、当前行高亮、读过行标已读', async () => {
    localStorage.setItem('hub-book-pos:codex-complete-guide-zh', JSON.stringify({ index: 6, at: 0.3 }))
    localStorage.setItem('hub-book-read:codex-complete-guide-zh', JSON.stringify([1, 2]))
    stubBook(BOOK)
    renderBook()
    await waitFor(() => expect(screen.getByTestId('hub-book-resume')).toBeTruthy())
    const cta = screen.getByTestId('hub-book-resume')
    expect(cta.textContent).toContain('第 5 讲')
    expect(cta.getAttribute('href')).toBe('/a/codex-complete-guide-zh/6')
    // 行状态
    const rows = Array.from(screen.getByTestId('hub-book-index').querySelectorAll('a.hub-row'))
    expect(rows[2].classList.contains('resume')).toBe(true)
    expect(rows[2].textContent).toContain('上次读到')
    expect(rows[1].classList.contains('read')).toBe(true)
    expect(rows[1].textContent).toContain('已读')
    // 从头开始的 ghost 链到第一篇
    expect(screen.getByText(/从头开始|Start over/).closest('a')!.getAttribute('href')).toBe('/a/codex-complete-guide-zh/1')
  })

  it('讲次页：顶条刻度 + 讲头眉标 + 正文 + 下一讲邀请块 + 底部上一讲', async () => {
    localStorage.setItem('hub-book-read:codex-complete-guide-zh', JSON.stringify([1, 2]))
    stubBook(BOOK)
    renderBook({ part: '6' }) // 第 5 讲 AGENTS.md
    await waitFor(() => expect(screen.getByTestId('hub-book-parttop')).toBeTruthy())
    // 顶条：全部 N 篇 + 刻度（4 格，当前是第 3 格，读过 2 格）
    const top = screen.getByTestId('hub-book-parttop')
    expect(top.textContent).toContain('全部 4 篇')
    const ticks = Array.from(top.querySelectorAll('.ticks i'))
    expect(ticks).toHaveLength(4)
    expect(ticks[2].classList.contains('cur')).toBe(true)
    expect(ticks[0].classList.contains('done')).toBe(true)
    // 讲头
    expect(document.querySelector('.hub-eyebrow')?.textContent).toContain('进阶篇 · 第 5 讲')
    expect(document.querySelector('.hub-sec-title')?.textContent).toBe('AGENTS.md')
    expect(document.querySelector('.hub-sec-intro')?.textContent).toBe('怎么干活的契约。')
    expect(document.querySelector('.hub-prose .book-steps')).toBeTruthy()
    // 下一讲块：下一项是附录 → 「接下来」；标题 B · 定价
    const nav = screen.getByTestId('hub-book-partnav')
    expect(nav.textContent).toContain('接下来')
    const nextLink = nav.querySelector('a.next-link')!
    expect(nextLink.getAttribute('href')).toBe('/a/codex-complete-guide-zh/13')
    expect(nextLink.textContent).toContain('B · 定价')
    // 底部：上一讲 + 全部 N 篇
    const foot = document.querySelector('.hub-part-foot')!
    expect(foot.textContent).toContain('01 · Codex 是什么')
    expect(document.title).toContain('第 5 讲 AGENTS.md')
  })

  it('末讲：下一讲块变「全书读完 · 回到全部 N 篇」', async () => {
    stubBook(BOOK)
    renderBook({ part: '13' })
    await waitFor(() => expect(screen.getByTestId('hub-book-partnav')).toBeTruthy())
    const nav = screen.getByTestId('hub-book-partnav')
    expect(nav.textContent).toContain('全书读完')
    expect(nav.querySelector('a.next-link')!.getAttribute('href')).toBe('/a/codex-complete-guide-zh')
  })

  it('机制舞台 bespoke：codex 05 讲注入 AGENTS.md 发现链；别的书不注入', async () => {
    stubBook(BOOK)
    renderBook({ part: '6' })
    await waitFor(() => expect(screen.getByTestId('hub-book-stage')).toBeTruthy())

    cleanup()
    stubBook(BOOK)
    renderBook({ slug: 'other-book', part: '6' })
    await waitFor(() => expect(screen.getByTestId('hub-book-parttop')).toBeTruthy())
    expect(screen.queryByTestId('hub-book-stage')).toBeNull()
  })

  it('进讲即写进度（换讲滚动比归零、同讲保留）', async () => {
    localStorage.setItem('hub-book-pos:codex-complete-guide-zh', JSON.stringify({ index: 13, at: 0.6 }))
    stubBook(BOOK)
    renderBook({ part: '13' })
    await waitFor(() => expect(screen.getByTestId('hub-book-parttop')).toBeTruthy())
    // 同讲重进：保留旧滚动比
    expect(JSON.parse(localStorage.getItem('hub-book-pos:codex-complete-guide-zh')!)).toEqual({ index: 13, at: 0.6 })

    cleanup()
    stubBook(BOOK)
    renderBook({ part: '2' })
    await waitFor(() => expect(screen.getByTestId('hub-book-parttop')).toBeTruthy())
    // 换讲：位置切换、滚动比归零
    expect(JSON.parse(localStorage.getItem('hub-book-pos:codex-complete-guide-zh')!)).toEqual({ index: 2, at: 0 })
  })

  it('无效 part 重定向回目录', async () => {
    stubBook(BOOK)
    renderBook({ part: '99' })
    await waitFor(() => expect(screen.getByTestId('hub-book-index')).toBeTruthy())
  })

  it('book.json 缺失显示内容缺失提示', async () => {
    stubBook(BOOK, false)
    renderBook()
    await waitFor(() => expect(screen.getByTestId('hub-reader-missing')).toBeTruthy())
  })
})
