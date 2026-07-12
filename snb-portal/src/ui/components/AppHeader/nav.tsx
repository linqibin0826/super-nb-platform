import type { ReactNode } from 'react'
import type { NavCapsuleItem } from '../NavCapsule/NavCapsule'

/**
 * 全站导航的唯一真源（Header 规范 v2，2026-07-12 站长拍板；spec 见 ai-relay
 * docs/superpowers/specs/2026-07-12-header-nav-v2-design.md）。
 * 四项固定：控制台 / 创作工坊 / 内容中心 / 活动（促销描边+呼吸点）；
 * 「使用指南」入口收进内容中心（hub 首页常驻「使用手册」直达位），新站不再进顶栏。
 * 响应式两段：≥1024 胶囊 / <1024 菜单钮+玻璃下拉浮卡（唯一例外：help 站保留 VitePress 三档）。
 * 非 React 消费方（fork Vue / learn VitePress / activity 静态页）按 templates/app-header.html
 * 模板抄写——项目/顺序/链接改动必须与本常量同步（契约互指）。
 */
export type SiteKey = 'console' | 'help' | 'studio' | 'hub' | 'activity' | 'home'

export interface SiteNavItem {
  key: SiteKey
  label: string
  /** 各站绝对地址（同源站点也可整页跳转，跨子域必须绝对） */
  href: string
  icon: ReactNode
  /** 促销强调（赤陶描边 + 呼吸点） */
  promo?: boolean
}

function Icon({ children }: { children: ReactNode }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width="15"
      height="15"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {children}
    </svg>
  )
}

export const SITE_NAV_ITEMS: SiteNavItem[] = [
  {
    key: 'console',
    label: '控制台',
    href: 'https://super-nb.me/dashboard',
    icon: (
      <Icon>
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </Icon>
    ),
  },
  {
    key: 'studio',
    label: '创作工坊',
    href: 'https://studio.super-nb.me/',
    icon: (
      <Icon>
        <path d="M12 19l7-7 3 3-7 7-3-3z" />
        <path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z" />
      </Icon>
    ),
  },
  {
    key: 'hub',
    label: '内容中心',
    href: 'https://hub.super-nb.me/',
    icon: (
      <Icon>
        <path d="M4 22h16a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2H8a2 2 0 0 0-2 2v16a2 2 0 0 1-2 2Zm0 0a2 2 0 0 1-2-2v-9c0-1.1.9-2 2-2h2" />
        <path d="M18 14h-8" />
        <path d="M15 18h-5" />
        <path d="M10 6h8v4h-8V6z" />
      </Icon>
    ),
  },
  {
    key: 'activity',
    label: '活动',
    // 2026-07-12 起指活动中心(/activity/all/,registry.json 驱动),不再直挂开卡页
    href: 'https://super-nb.me/activity/all/',
    promo: true,
    icon: (
      <Icon>
        <rect x="3" y="8" width="18" height="4" rx="1" />
        <path d="M12 8v13" />
        <path d="M19 12v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7" />
        <path d="M7.5 8a2.5 2.5 0 0 1 0-5C11 3 12 8 12 8m4.5 0a2.5 2.5 0 0 0 0-5C13 3 12 8 12 8" />
      </Icon>
    ),
  },
]

export interface SiteNavOptions {
  /** 本地开发/路径部署覆盖链接（如 studio dev 指向本地代理） */
  resolveHref?: (item: SiteNavItem) => string
  /** 覆盖文案（双语站点接 i18n；缺省用常量中文） */
  labelFor?: (item: SiteNavItem) => string
}

/** 生成 NavCapsule items：标出当前站、促销项转 accent+dot。 */
export function siteNavItems(active?: SiteKey, opts: SiteNavOptions = {}): NavCapsuleItem[] {
  return SITE_NAV_ITEMS.map((item) => ({
    label: opts.labelFor ? opts.labelFor(item) : item.label,
    href: opts.resolveHref ? opts.resolveHref(item) : item.href,
    icon: item.icon,
    current: item.key === active,
    accent: item.promo,
    dot: item.promo,
  }))
}
