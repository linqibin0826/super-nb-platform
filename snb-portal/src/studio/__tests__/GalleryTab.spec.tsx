import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, cleanup, waitFor } from '@testing-library/react'
import { t } from '../../i18n'
import type { PromptListItem } from '../../lib/galleryApi'

vi.mock('../../lib/galleryApi', async (orig) => {
  const actual = await orig<typeof import('../../lib/galleryApi')>()
  return {
    ...actual,
    fetchPrompts: vi.fn(),
    fetchCategories: vi.fn(),
    // 默认解析空 my-state：GalleryTab 接入 useInteractions 后，任何渲染（含既有「页尾续读区」
    // 3 条测试，因下面加了登录态 useAuth mock 而 enabled）都会在挂载时触发一次回填，
    // 不给默认值会让 fetchInteractions() 返回 undefined、.then 抛 TypeError 打挂全文件。
    fetchInteractions: vi.fn().mockResolvedValue({ liked: [], favorited: [] }),
    toggleLike: vi.fn(),
    toggleFavorite: vi.fn(),
  }
})
import { fetchPrompts, fetchCategories, toggleLike } from '../../lib/galleryApi'
import { GalleryTab } from '../GalleryTab'

vi.mock('../../auth/useAuth', () => ({ useAuthUser: () => ({ id: 1, email: 'a@b.c' }) }))

function makeItems(offset: number, n: number): PromptListItem[] {
  return Array.from({ length: n }, (_, i) => ({
    id: String(offset + i + 1),
    title: `作品 ${offset + i + 1}`,
    imageUrl: `https://media.super-nb.me/gallery/${offset + i + 1}.webp`,
    imageW: 480,
    imageH: 640,
    authorName: null,
    likeCount: 0,
    favCount: 0,
  }))
}

beforeAll(() => {
  // BalancedMasonry 直接调 ResizeObserver / window.matchMedia，jsdom 都没有
  vi.stubGlobal(
    'ResizeObserver',
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
    }
  )
  vi.stubGlobal(
    'matchMedia',
    (query: string) => ({
      matches: false,
      media: query,
      addEventListener() {},
      removeEventListener() {},
      addListener() {},
      removeListener() {},
      onchange: null,
      dispatchEvent: () => false,
    })
  )
  vi.stubGlobal(
    'IntersectionObserver',
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
      takeRecords() {
        return []
      }
    }
  )
})

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('页尾续读区', () => {
  it('还有下页时常驻：计数行 + 「继续往下看」按钮（不依赖自动预算状态）', async () => {
    vi.mocked(fetchCategories).mockResolvedValue({ scene: [], style: [], subject: [] })
    vi.mocked(fetchPrompts).mockResolvedValue({
      items: makeItems(0, 24),
      total: 5778,
      page: 1,
      pages: 241,
    })
    render(<GalleryTab onApply={vi.fn()} />)
    const btn = await screen.findByRole('button', { name: t('studio.gallery.keepBrowsing') })
    expect(btn).toBeTruthy()
    expect(
      screen.getByText(t('studio.gallery.progress', { seen: 24, left: 5778 - 24 }))
    ).toBeTruthy()
  })

  it('点按钮拉下一页，计数行跟着涨', async () => {
    vi.mocked(fetchCategories).mockResolvedValue({ scene: [], style: [], subject: [] })
    vi.mocked(fetchPrompts)
      .mockResolvedValueOnce({ items: makeItems(0, 24), total: 5778, page: 1, pages: 241 })
      .mockResolvedValueOnce({ items: makeItems(24, 24), total: 5778, page: 2, pages: 241 })
    render(<GalleryTab onApply={vi.fn()} />)
    const btn = await screen.findByRole('button', { name: t('studio.gallery.keepBrowsing') })
    fireEvent.click(btn)
    await waitFor(() =>
      expect(
        screen.getByText(t('studio.gallery.progress', { seen: 48, left: 5778 - 48 }))
      ).toBeTruthy()
    )
    expect(vi.mocked(fetchPrompts).mock.calls[1][0]).toMatchObject({ page: 2 })
  })

  it('最后一页：出到底提示、不再出续读按钮', async () => {
    vi.mocked(fetchCategories).mockResolvedValue({ scene: [], style: [], subject: [] })
    vi.mocked(fetchPrompts).mockResolvedValue({
      items: makeItems(0, 10),
      total: 10,
      page: 1,
      pages: 1,
    })
    render(<GalleryTab onApply={vi.fn()} />)
    expect(await screen.findByText(t('studio.gallery.atEnd'))).toBeTruthy()
    expect(screen.queryByRole('button', { name: t('studio.gallery.keepBrowsing') })).toBeNull()
  })
})

describe('排序 + 交互', () => {
  beforeEach(() => {
    vi.mocked(fetchCategories).mockResolvedValue({ scene: [], style: [], subject: [] })
    vi.mocked(fetchPrompts).mockResolvedValue({
      items: makeItems(0, 1),
      total: 1,
      page: 1,
      pages: 1,
    })
  })

  it('点赞最多 / 收藏最多 两档已可点（不再置灰）', async () => {
    render(<GalleryTab onApply={vi.fn()} />)
    await screen.findByText('作品 1')
    const likes = screen.getByRole('button', { name: t('studio.gallery.sortLikes') }) as HTMLButtonElement
    const favs = screen.getByRole('button', { name: t('studio.gallery.sortFavs') }) as HTMLButtonElement
    expect(likes.disabled).toBe(false)
    expect(favs.disabled).toBe(false)
  })

  it('点「点赞最多」→ 以 sort=likes 重新拉列表', async () => {
    render(<GalleryTab onApply={vi.fn()} />)
    await screen.findByText('作品 1')
    fireEvent.click(screen.getByRole('button', { name: t('studio.gallery.sortLikes') }))
    await waitFor(() =>
      expect(vi.mocked(fetchPrompts).mock.calls.some((c) => c[0]?.sort === 'likes')).toBe(true)
    )
  })

  it('卡片赞按钮 → 调 toggleLike(id, true)', async () => {
    vi.mocked(toggleLike).mockResolvedValue({ likeCount: 1, liked: true })
    render(<GalleryTab onApply={vi.fn()} />)
    await screen.findByText('作品 1')
    // 单条数据 → 全页唯一的「点赞」按钮（aria-label='点赞'，与排序档「点赞最多」名字不同）
    fireEvent.click(screen.getByRole('button', { name: t('studio.gallery.like') }))
    await waitFor(() => expect(vi.mocked(toggleLike)).toHaveBeenCalledWith('1', true))
  })
})
