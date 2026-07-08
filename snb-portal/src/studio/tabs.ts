// studio 顶部 tab 定义（单一真源）：灵感库 / 我的收藏 / 历史。
export type TabId = 'gallery' | 'favorites' | 'history'

export const TAB_ITEMS: Array<{ id: TabId; labelKey: string }> = [
  { id: 'gallery', labelKey: 'playground.tabs.gallery' },
  { id: 'favorites', labelKey: 'playground.tabs.favorites' },
  { id: 'history', labelKey: 'playground.tabs.history' },
]
