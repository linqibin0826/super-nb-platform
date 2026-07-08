import type { ReactNode } from 'react'
import type { NavCapsuleItem } from '../NavCapsule/NavCapsule'

/**
 * 全站导航的唯一真源（Header 规范 v1，2026-07-05 站长拍板）。
 * 四项固定：控制台 / 使用指南 / 创作工坊 / 充值活动（促销描边+呼吸点）。
 * 非 React 消费方（fork Vue / learn VitePress / activity 静态页）按 templates/app-header.html
 * 模板抄写——项目/顺序/链接改动必须与本常量同步（契约互指）。
 */
export type SiteKey = 'console' | 'help' | 'studio' | 'activity' | 'home'

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
    key: 'help',
    label: '使用指南',
    // 2026-07-05 迁子域名（原 api.super-nb.me/help/ 由主站 301 兜底）
    href: 'https://help.super-nb.me/',
    icon: (
      <Icon>
        <path d="M2 4h7a3 3 0 0 1 3 3v13a2 2 0 0 0-2-2H2z" />
        <path d="M22 4h-7a3 3 0 0 0-3 3v13a2 2 0 0 1 2-2h8z" />
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
    key: 'activity',
    label: '充值活动',
    href: 'https://super-nb.me/activity/',
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
