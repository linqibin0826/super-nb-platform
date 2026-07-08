import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, cleanup, waitFor } from '@testing-library/react'
import { FavoritesTab } from '../FavoritesTab'
import * as api from '../../lib/galleryApi'
import { t } from '../../i18n'

// ⚠️ 不用 createT(messages, 'zh')：FavoritesTab 内部用的是 i18n 单例 t，
// 其 locale 由 detectLocale() 在模块加载时按 navigator.language 定死，jsdom 下默认落 en——
// 与组件实际渲染的语言脱节会导致断言永远找不到元素。改用同一单例，与
// GalleryTab.spec.tsx / PromptSheet.spec.tsx 等既有测试的做法保持一致。
// vi.hoisted：mock 工厂在文件顶部被提升，authMock 要先于它可用
const authMock = vi.hoisted(() => ({ user: null as { id: number; email: string } | null }))
vi.mock('../../auth/useAuth', () => ({ useAuthUser: () => authMock.user }))

beforeEach(() => {
  // BalancedMasonry 会碰 ResizeObserver / matchMedia，jsdom 都没有
  vi.stubGlobal(
    'ResizeObserver',
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
    }
  )
  vi.stubGlobal('matchMedia', (query: string) => ({
    matches: false,
    media: query,
    addEventListener() {},
    removeEventListener() {},
    addListener() {},
    removeListener() {},
    onchange: null,
    dispatchEvent: () => false,
  }))
  vi.spyOn(api, 'fetchInteractions').mockResolvedValue({ liked: [], favorited: [] })
})

afterEach(() => {
  cleanup()
  authMock.user = null
  vi.restoreAllMocks()
})

const FAV_ITEM = {
  id: '9',
  title: '收藏图',
  imageUrl: 'https://media.super-nb.me/gallery/9.webp',
  imageW: 480,
  imageH: 480,
  authorName: null,
  likeCount: 0,
  favCount: 3,
}

describe('FavoritesTab', () => {
  it('未登录 → 空态引导，不拉收藏', () => {
    const spy = vi.spyOn(api, 'fetchMyFavorites')
    render(<FavoritesTab onApply={vi.fn()} />)
    expect(screen.getByText(t('studio.favorites.loginTitle'))).toBeTruthy()
    expect(spy).not.toHaveBeenCalled()
  })

  it('登录 → 渲染收藏条目', async () => {
    authMock.user = { id: 1, email: 'a@b.c' }
    vi.spyOn(api, 'fetchMyFavorites').mockResolvedValue({
      items: [FAV_ITEM],
      total: 1,
      page: 1,
      pages: 1,
    })
    render(<FavoritesTab onApply={vi.fn()} />)
    expect(await screen.findByText('收藏图')).toBeTruthy()
  })

  it('直接使用 → 取详情全文后 apply（非空 prompt）', async () => {
    authMock.user = { id: 1, email: 'a@b.c' }
    vi.spyOn(api, 'fetchMyFavorites').mockResolvedValue({
      items: [FAV_ITEM],
      total: 1,
      page: 1,
      pages: 1,
    })
    vi.spyOn(api, 'fetchPromptDetail').mockResolvedValue(
      { promptText: '完整提示词' } as unknown as api.PromptDetail
    )
    const onApply = vi.fn()
    render(<FavoritesTab onApply={onApply} />)
    await screen.findByText('收藏图')
    // 卡片两个按钮：藏（aria-label='收藏'）与「直接使用」；按名字取后者，全页唯一
    fireEvent.click(screen.getByRole('button', { name: t('studio.gallery.use') }))
    await waitFor(() => expect(onApply).toHaveBeenCalledWith({ prompt: '完整提示词' }))
  })

  it('卡片点赞按钮调 toggleLike', async () => {
    authMock.user = { id: 1, email: 'a@b.c' }
    vi.spyOn(api, 'fetchMyFavorites').mockResolvedValue({ items: [FAV_ITEM], total: 1, page: 1, pages: 1 })
    const spy = vi.spyOn(api, 'toggleLike').mockResolvedValue({ likeCount: 1, liked: true })
    render(<FavoritesTab onApply={vi.fn()} />)
    await screen.findByText('收藏图')
    fireEvent.click(screen.getByRole('button', { name: t('studio.gallery.like') }))
    await waitFor(() => expect(spy).toHaveBeenCalledWith('9', true))
  })

  it('收藏星标默认点亮（seed，即便回填不返回该 id）', async () => {
    authMock.user = { id: 1, email: 'a@b.c' }
    vi.spyOn(api, 'fetchMyFavorites').mockResolvedValue({ items: [FAV_ITEM], total: 1, page: 1, pages: 1 })
    vi.spyOn(api, 'fetchInteractions').mockResolvedValue({ liked: [], favorited: [] })
    render(<FavoritesTab onApply={vi.fn()} />)
    await screen.findByText('收藏图')
    const star = screen.getByRole('button', { name: t('studio.gallery.save') })
    await waitFor(() => expect(star.getAttribute('aria-pressed')).toBe('true'))
  })

  it('切回激活态 → 重新拉取（修：在灵感库收藏后切回「我的收藏」要刷新，非陈旧挂载）', async () => {
    authMock.user = { id: 1, email: 'a@b.c' }
    const spy = vi
      .spyOn(api, 'fetchMyFavorites')
      .mockResolvedValueOnce({ items: [FAV_ITEM], total: 1, page: 1, pages: 1 })
      .mockResolvedValueOnce({
        items: [FAV_ITEM, { ...FAV_ITEM, id: '20', title: '新收藏' }],
        total: 2,
        page: 1,
        pages: 1,
      })
    const { rerender } = render(<FavoritesTab onApply={vi.fn()} active={true} />)
    await screen.findByText('收藏图')
    expect(spy).toHaveBeenCalledTimes(1)
    // 切走（active=false）：不重复拉取
    rerender(<FavoritesTab onApply={vi.fn()} active={false} />)
    expect(spy).toHaveBeenCalledTimes(1)
    // 切回（active=true）：重新拉取，新收藏出现
    rerender(<FavoritesTab onApply={vi.fn()} active={true} />)
    await waitFor(() => expect(spy).toHaveBeenCalledTimes(2))
    expect(await screen.findByText('新收藏')).toBeTruthy()
  })

  it('分页：点续读拉下一页并追加', async () => {
    authMock.user = { id: 1, email: 'a@b.c' }
    vi.spyOn(api, 'fetchMyFavorites')
      .mockResolvedValueOnce({ items: [FAV_ITEM], total: 2, page: 1, pages: 2 })
      .mockResolvedValueOnce({
        items: [{ ...FAV_ITEM, id: '10', title: '收藏图2' }],
        total: 2,
        page: 2,
        pages: 2,
      })
    render(<FavoritesTab onApply={vi.fn()} />)
    await screen.findByText('收藏图')
    fireEvent.click(screen.getByRole('button', { name: t('studio.gallery.keepBrowsing') }))
    await waitFor(() => expect(screen.getByText('收藏图2')).toBeTruthy())
  })
})
