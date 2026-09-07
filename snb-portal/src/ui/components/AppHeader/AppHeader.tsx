import { useEffect, useId, useRef, useState, type ReactNode } from 'react'
import { cx } from '../../lib/cx'
import { BrandLogo } from '../BrandLogo/BrandLogo'
import { NavCapsule } from '../NavCapsule/NavCapsule'
import { ThemeToggle } from '../ThemeToggle/ThemeToggle'
import { siteNavItems, type SiteKey, type SiteNavItem } from './nav'

export interface AppHeaderProps {
  /** 当前站（导航胶囊高亮 + 缺省副标） */
  site?: SiteKey
  /** 品牌锁定区副标（「控制台/创作工坊/新手指南」等）；不传则只出词标 */
  subtitle?: string
  /**
   * solid（默认）：桌面 64 / 手机 60 玻璃粘性顶栏 + 滚动边缘，内容站用；
   * hero：桌面 80 / 手机 60 透明渐变浮层无底边，首页/活动页顶部用
   */
  variant?: 'solid' | 'hero'
  /** 词标链接（默认主站首页） */
  homeHref?: string
  /** 本地开发/路径部署下覆盖导航链接 */
  resolveHref?: (item: SiteNavItem) => string
  /** 覆盖导航文案（双语站点接 i18n） */
  labelFor?: (item: SiteNavItem) => string
  /**
   * 场景槽（右侧）：顺序规范=工具图标（搜索）在左、账户区（余额头像）在最右。
   * <1024 时「登录」必须收进浮卡（给场景槽里的登录项加 `max-lg:hidden`），
   * 顶栏上只留一枚 CTA + 菜单钮。
   */
  children?: ReactNode
  /**
   * 手机浮卡底部：整宽 CTA + 整宽次按钮「登录」。
   * 用 lib/cta 的 `ctaAnchorClass` / `secondaryAnchorClass` 配 `w-full` 写。
   * 业务地址（注册页/登录页）由各站自己给，设计系统不写死。
   */
  menuFooter?: ReactNode
  /**
   * 主题开关「浅色 / 深色」（浅色档值表 §05）。默认 `false`——存量消费方零改动。
   *
   * 打开后开关渲染在**场景槽最前**：访客态即「登录」左边（与定稿一致）。
   * ⚠️ 已登录态定稿要求放在**余额与头像之间**，那是 `HeaderAccount` 内部；要严格照稿
   * 就把本 prop 关掉，自己在 `children` 里 `<HeaderAccount/>` 前后插 `<ThemeToggle/>`。
   */
  themeToggle?: boolean
  className?: string
}

/**
 * 全站统一 Header（苹果式 v3）：三区骨架
 * 「品牌锁定 | 胶囊分段 | 场景槽」——**flex 两端对齐；胶囊分段 `mx-auto flex-none`
 * 贴合内容居中；<lg 收起**
 * （🪦 grid 三列已退役：768 平板下会把整条顶栏挤到左边、右侧空 37%）。
 * 桌面 ≥1024 高 64 / 内边距 24；手机 <1024 高 60 / 内边距 16，
 * 菜单钮 44×44 永远贴最右（拇指角），「登录」收进浮卡。
 * 非 React 站点（fork/learn/activity）按 templates/app-header.html 对齐，改必同步。
 *
 * 🚨 **1024 是按「没有侧栏的壳」定的断点**（站长 2026-07-30 拍板的例外规则）：
 *    带常驻侧栏的外壳整体提高一档——可用宽得先减掉侧栏。已知例外一处：
 *    sub2api fork `layout/AppHeader.vue`（控制台，侧栏 280px）走 **xl(1280)**。
 *    本组件与其余消费方都没有侧栏，仍走 1024。细节见 templates/app-header.html 同段。
 */
export function AppHeader({
  site,
  subtitle,
  variant = 'solid',
  homeHref = 'https://super-nb.me/',
  resolveHref,
  labelFor,
  children,
  menuFooter,
  themeToggle = false,
  className,
}: AppHeaderProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  // 滚动边缘：静止时顶栏与底平齐无线，内容滚到下面（>8px）才出发丝线 + 4% 投影（.snb-scrolled）
  const [scrolled, setScrolled] = useState(false)
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8)
    onScroll()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])
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
    // matchMedia 在部分环境（jsdom）缺失：拿不到就跳过「抬到 ≥lg 自动收起」，不影响开关
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
      data-scrolled={scrolled}
      className={cx(
        // 三区弹性：两端对齐；胶囊分段 mx-auto flex-none 贴合内容居中；<lg 收起
        'relative flex items-center justify-between gap-4 px-4 lg:gap-6 lg:px-6',
        variant === 'solid'
          ? // 玻璃粘性顶栏（.snb-glass 定义在 styles.css，全站允许毛玻璃滤镜的三处之一）。
            // 静止无底线；滚过 8px 挂 .snb-scrolled 才出 --snb-hdr-line 发丝线 + 4% 投影。
            // ⚠️ .snb-scrolled 必须与 .snb-glass 同挂一个元素才生效。
            cx('snb-glass sticky top-0 z-40 h-[60px] lg:h-16', scrolled && 'snb-scrolled')
          : // hero = **浮在暗色画面上**的顶栏（图片/视频背景之上），加 dark 类锁死暗色 token。
            // 🚨 这里的「不翻」与主题无关，跟图片上的信息层是同一条纪律：底下是一张暗画面，
            //    白天把字翻成墨就直接瞎。用它的前提是**该处确实有暗底画面**——
            //    没有暗底就别用 hero，用 solid。
            // ⚠️ 2026-07-29 起本仓无消费方（活动页 Hero 视频已全部撤掉，fork 首页用自己的
            //    PublicShell）；留着是给将来真有暗景的场合用，不是「门面恒暗」的依据。
            'dark h-[60px] bg-gradient-to-b from-black/45 to-transparent lg:h-20',
        className
      )}
    >
      <a href={homeHref} className="flex flex-none items-baseline gap-2.5 no-underline">
        {/* 词标家族现状：20px、≥768 抬 24（首页/learn 同款） */}
        <BrandLogo className="md:text-2xl" />
        {subtitle && (
          <span className="whitespace-nowrap text-[13px] font-medium tracking-[0.22em] text-snb-t3 max-[639px]:hidden">
            {subtitle}
          </span>
        )}
      </a>
      {/* 胶囊分段贴合内容、auto 居中；<lg 收起，两端自动贴边
          （⚠️ 不能给 flex-1：根元素现在是 rounded-full bg-snb-well 灰轨，会横贯整条顶栏；
           与 templates/app-header.html 的 .snb-hdr__capsule{flex:none;margin:0 auto} 一致） */}
      <NavCapsule aria-label="全站导航" className="mx-auto flex-none max-lg:hidden" items={items} />
      <div className="flex flex-none items-center gap-2.5">
        {/* 主题开关：场景槽最前 = 访客态「登录」左边（定稿位置）。默认不出，见 props 注释 */}
        {themeToggle && <ThemeToggle />}
        {children}
        {/* 菜单钮：44×44 实热区、hairline-strong 描边，**永远排在场景槽最后**
            （拇指角在右下，v2 时期夹在字标与登录之间像挂在 logo 上的徽章）；
            ≥lg 收起，胶囊分段常驻。有促销项时带 6px 小点（活动曝光不丢） */}
        <button
          ref={burgerRef}
          type="button"
          className="relative inline-flex h-11 w-11 flex-none items-center justify-center rounded-full border border-snb-hairline-strong bg-transparent p-0 text-snb-t1 transition-all duration-quick ease-snb-quick hover:bg-snb-well aria-expanded:bg-snb-well lg:hidden"
          aria-label={menuOpen ? '关闭导航菜单' : '打开导航菜单'}
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
            <span aria-hidden="true" className="absolute right-2 top-2 h-1.5 w-1.5 rounded-full bg-snb-safety" />
          )}
        </button>
      </div>

      {/* <lg 导航浮卡（玻璃浮层 .snb-glass）：
          自顶栏底边整宽滑落 + 淡入 400ms 落定档；下垫纯色遮罩（无模糊无发光），点遮罩即关 */}
      {menuOpen && (
        <div ref={menuWrapRef} className="lg:hidden">
          {/* ⚠️ 遮罩必须 absolute 挂在顶栏下沿，**不能用 fixed**：顶栏带毛玻璃滤镜（backdrop 系滤镜），
              该滤镜会让自己成为 fixed 后代的包含块，`fixed inset-x-0 bottom-0`
              会被算进 60px 高的顶栏里、塌成零高（实测过，遮罩整块消失）。
              🚨 定位这一条与主题无关，浅色化时也**绝不能**改回 fixed + top:Npx + bottom:0。
              🚨 遮罩颜色走 --snb-mask：**两档都是暗的**（浅色 = 黑 .40）——
              白天档不等于遮罩变白，遮罩变白会让底下页面看着像没关掉 */}
          <div
            aria-hidden="true"
            onClick={() => setMenuOpen(false)}
            className="absolute inset-x-0 top-full z-30 h-screen animate-snb-mask bg-snb-mask motion-reduce:animate-none"
          />
          <nav
            id={menuId}
            aria-label="全站导航"
            className="snb-glass absolute inset-x-0 top-full z-40 animate-snb-menu border-b border-snb-hairline px-2.5 pb-3.5 pt-2.5 shadow-glass motion-reduce:animate-none"
          >
            <ul className="m-0 flex list-none flex-col p-0">
              {items.map((item) => (
                <li key={item.href}>
                  <a
                    href={item.href}
                    target={item.target}
                    aria-current={item.current ? 'page' : undefined}
                    onClick={() => setMenuOpen(false)}
                    className={cx(
                      // 条目高 52 = 整行热区（🪦 v2 的 210px 右锚小浮卡退役）
                      'flex h-[52px] items-center gap-2.5 rounded-xl px-3.5 text-[15px] no-underline',
                      item.current ? 'bg-snb-key-active font-semibold text-snb-t1' : 'text-snb-t2'
                    )}
                  >
                    {item.icon}
                    {item.label}
                    {item.dot && <span aria-hidden="true" className="h-1.5 w-1.5 flex-none rounded-full bg-snb-safety" />}
                  </a>
                </li>
              ))}
            </ul>
            {menuFooter && (
              <>
                <div className="mx-1 my-2.5 border-t border-snb-hairline" />
                <div className="flex flex-col gap-2.5 px-1">{menuFooter}</div>
              </>
            )}
          </nav>
        </div>
      )}
    </header>
  )
}
