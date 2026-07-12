import { useEffect, useId, useRef, useState, type ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { BrandLogo } from '../BrandLogo/BrandLogo'
import { NavCapsule } from '../NavCapsule/NavCapsule'
import { siteNavItems, type SiteKey, type SiteNavItem } from './nav'

export interface AppHeaderProps {
  /** 当前站（导航胶囊高亮 + 缺省副标） */
  site?: SiteKey
  /** 品牌锁定区副标（「控制台/创作工坊/使用指南」等）；不传则只出词标 */
  subtitle?: string
  /**
   * solid（默认）：64px 实底 + hairline 底边，内容站用（明暗随 ThemeScope token 翻转）；
   * hero：80px 透明渐变浮层无底边，首页/活动页顶部用
   */
  variant?: 'solid' | 'hero'
  /** 词标链接（默认主站首页） */
  homeHref?: string
  /** 本地开发/路径部署下覆盖导航链接 */
  resolveHref?: (item: SiteNavItem) => string
  /** 覆盖导航文案（双语站点接 i18n） */
  labelFor?: (item: SiteNavItem) => string
  /** 场景槽（右侧）：顺序规范=工具图标（搜索/主题）在左、账户区（登录注册/余额头像）在最右 */
  children?: ReactNode
  className?: string
}

/**
 * 全站统一 Header（规范 v1，2026-07-05）：三区骨架「品牌锁定 | 驿站胶囊 | 场景槽」。
 * 词标/胶囊全站恒定，场景槽是唯一允许差异的区域。
 * <lg(1024) 胶囊收起、改菜单钮+玻璃下拉浮卡（同一 siteNavItems 数据源，规范 v2 两段式）。
 * 非 React 站点（fork/learn/activity）按 templates/app-header.html 对齐，改必同步。
 */
export function AppHeader({
  site,
  subtitle,
  variant = 'solid',
  homeHref = 'https://super-nb.me/',
  resolveHref,
  labelFor,
  children,
  className,
}: AppHeaderProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const menuId = useId()
  const menuWrapRef = useRef<HTMLDivElement | null>(null)
  const burgerRef = useRef<HTMLButtonElement | null>(null)
  const items = siteNavItems(site, { resolveHref, labelFor })

  // 关闭：Esc / 点浮卡外（菜单钮自身除外，它负责开关）/ 视口抬到 ≥lg
  useEffect(() => {
    if (!menuOpen) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setMenuOpen(false)
    }
    const onDown = (e: PointerEvent) => {
      const target = e.target as Node
      if (menuWrapRef.current?.contains(target) || burgerRef.current?.contains(target)) return
      setMenuOpen(false)
    }
    // matchMedia 在部分环境（jsdom）缺失：拿不到就跳过「抬到 ≥md 自动收起」，不影响开关
    const mq = typeof window.matchMedia === 'function' ? window.matchMedia('(min-width: 1024px)') : null
    const onMq = () => {
      if (mq?.matches) setMenuOpen(false)
    }
    document.addEventListener('keydown', onKey)
    document.addEventListener('pointerdown', onDown)
    mq?.addEventListener('change', onMq)
    return () => {
      document.removeEventListener('keydown', onKey)
      document.removeEventListener('pointerdown', onDown)
      mq?.removeEventListener('change', onMq)
    }
  }, [menuOpen])

  return (
    <header
      className={cx(
        'relative grid grid-cols-[1fr_auto_1fr] items-center px-6 lg:px-10',
        variant === 'solid'
          ? // sticky 毛玻璃：滚动中导航常驻（studio 顶栏验证过的形态，吸收为规范）
            'sticky top-0 z-40 h-16 border-b border-snb-hairline bg-snb-bg/85 backdrop-blur-md'
          : // hero 永远浮在暗色画面上：加 dark 类锁死暗色 token，不随 ThemeScope 翻浅
            'dark h-20 bg-gradient-to-b from-black/45 to-transparent',
        className
      )}
    >
      <a
        href={homeHref}
        className="col-start-1 flex items-baseline gap-2.5 justify-self-start no-underline"
      >
        {/* 词标家族现状：20px、≥768 抬 24（首页/learn 同款） */}
        <BrandLogo className="md:text-2xl" />
        {subtitle && (
          <span className="whitespace-nowrap text-[13px] font-medium tracking-[0.22em] text-snb-t3 max-[639px]:hidden">
            {subtitle}
          </span>
        )}
      </a>
      {/* <lg 隐藏胶囊后不生成 grid item，右槽必须显式钉第三列，否则被自动放进中列 */}
      <NavCapsule
        aria-label="全站导航"
        className="col-start-2 max-lg:hidden"
        items={items}
      />
      <div className="col-start-3 flex items-center gap-2.5 justify-self-end">
        {/* 菜单钮：≥lg 收起；<lg 顶替胶囊，展开右下玻璃浮卡；有促销项时带呼吸点（活动曝光不丢） */}
        <button
          ref={burgerRef}
          type="button"
          className="relative inline-flex h-9 w-9 items-center justify-center rounded-full border border-snb-hairline-strong bg-snb-panel p-0 text-snb-t2 transition-colors hover:border-snb-t3 hover:text-snb-t1 lg:hidden"
          aria-label="打开导航菜单"
          aria-expanded={menuOpen}
          aria-controls={menuId}
          onClick={() => setMenuOpen((v) => !v)}
        >
          {menuOpen ? (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
              <path d="M6 18 18 6M6 6l12 12" />
            </svg>
          ) : (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
              <path d="M3 6h18M3 12h18M3 18h18" />
            </svg>
          )}
          {!menuOpen && items.some((i) => i.dot) && (
            <span
              className="absolute right-1 top-1 h-1.5 w-1.5 rounded-full bg-primary-400 animate-pulse motion-reduce:animate-none"
              aria-hidden="true"
            />
          )}
        </button>
        {children}
      </div>

      {/* <lg 玻璃下拉浮卡（规范 v2：右对齐锚在顶栏下方，与胶囊同玻璃语言） */}
      {menuOpen && (
        <div ref={menuWrapRef} className="lg:hidden">
          <nav
            id={menuId}
            aria-label="全站导航"
            className="absolute right-4 top-[calc(100%+8px)] z-40 min-w-[210px] rounded-2xl border border-snb-hairline bg-snb-bg/95 shadow-glass-sm backdrop-blur-md"
          >
            <ul className="flex flex-col gap-0.5 p-2">
              {items.map((item) => (
                <li key={item.href}>
                  <a
                    href={item.href}
                    target={item.target}
                    aria-current={item.current ? 'page' : undefined}
                    onClick={() => setMenuOpen(false)}
                    className={cx(
                      'flex items-center gap-2.5 rounded-xl px-3.5 py-2.5 text-sm font-medium no-underline transition-colors',
                      item.current
                        ? 'bg-snb-t1/[0.08] text-snb-t1'
                        : item.accent
                          ? 'text-primary-500 hover:bg-primary-500/10'
                          : 'text-snb-t2 hover:bg-snb-t1/[0.06] hover:text-snb-t1'
                    )}
                  >
                    {item.icon}
                    {item.label}
                    {item.dot && (
                      <span className="h-1.5 w-1.5 rounded-full bg-primary-400" aria-hidden="true" />
                    )}
                  </a>
                </li>
              ))}
            </ul>
          </nav>
        </div>
      )}
    </header>
  )
}
