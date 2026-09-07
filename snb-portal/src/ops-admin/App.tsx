import { useEffect, useState } from 'react'
import { BrowserRouter, Navigate, NavLink, Route, Routes } from 'react-router-dom'
import { StatusLamp, ThemeScope, ThemeToggle, cx } from '../ui'
import { DashboardPage } from './pages/DashboardPage'
import { AccountListPage } from './pages/AccountListPage'
import { AccountFormPage } from './pages/AccountFormPage'
import { MONO } from './pages/shared'

/** 外链面板(有确切 URL 的才放,别造假链接;卡台三家 URL 站长给了再加) */
const PANELS: Array<{ label: string; href: string }> = [
  { label: 'Proxy-Cheap', href: 'https://app.proxy-cheap.com/' },
]

/** 胶囊分段的一格（类串与 vendored NavCapsule 逐字同源，改必两边同步）：
 *  当前位=白胶囊(深色为 #3A3A3C)+semibold+1px 3px 12% 投影；非当前位 t2 字 hover 提 t1。
 *  ⚠️ 非当前位用 t2 不用 t3——t3 压灰轨 --snb-well 只有 4.27:1，vendored 件同款注释。 */
function TopLink({ to, end, children }: { to: string; end?: boolean; children: string }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        cx(
          'inline-flex h-[30px] items-center whitespace-nowrap rounded-full px-3.5 text-[13px] font-medium transition-[background-color,color,box-shadow] duration-quick ease-snb-quick focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus',
          isActive
            ? 'bg-snb-key-active font-semibold text-snb-t1 shadow-[0_1px_3px_rgba(0,0,0,0.12)]'
            : 'text-snb-t2 hover:text-snb-t1'
        )
      }
    >
      {children}
    </NavLink>
  )
}

/** 常驻顶条:身份牌 + 胶囊分段两个去处 + 外部面板 + 主题开关。
 *  苹果式 v3:玻璃粘性顶栏(.snb-glass,vendored styles.css 的工具类)+滚动边缘
 *  (静止无线、滚过 8px 才出发丝线与 4% 投影)。
 *  ⚠️ 顶栏带 backdrop-filter ⇒ 它是内部 fixed 后代的包含块。本站顶条内没有浮层，
 *     将来要加下拉必须 absolute + top:100%，绝不能 fixed（三仓库各撞过一次）。 */
function OpsHeader() {
  const [scrolled, setScrolled] = useState(false)
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8)
    onScroll()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])
  return (
    <header
      data-scrolled={scrolled}
      className={cx('snb-glass sticky top-0 z-40', scrolled && 'snb-scrolled')}
    >
      <div className="mx-auto flex h-14 w-full max-w-6xl items-center gap-3 px-4">
        <NavLink
          to="/admin"
          className="flex items-center gap-2.5 rounded-full focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus"
        >
          <StatusLamp state="live" />
          <span className="text-[15px] font-semibold tracking-[-0.01em] text-snb-t1">资产运维台</span>
          <span className={cx('hidden text-[11px] tracking-[0.22em] text-snb-t3 sm:inline', MONO)}>SNB·OPS</span>
        </NavLink>
        <nav className="ml-auto flex items-center gap-2">
          <span className="inline-flex items-center gap-0.5 rounded-full bg-snb-well p-[3px]">
            <TopLink to="/admin" end>
              看板
            </TopLink>
            <TopLink to="/admin/accounts">台账</TopLink>
          </span>
          {PANELS.map((p) => (
            <a
              key={p.href}
              href={p.href}
              target="_blank"
              rel="noreferrer"
              className="hidden h-[30px] items-center rounded-full px-3.5 text-[13px] font-medium text-snb-t3 transition-colors duration-quick ease-snb-quick hover:bg-snb-t1/[0.06] hover:text-snb-t1 focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus sm:inline-flex"
            >
              {p.label} ↗
            </a>
          ))}
          <ThemeToggle className="ml-1" />
        </nav>
      </div>
    </header>
  )
}

export function AppRoutes() {
  // 内部运维台:登录靠父域 cookie SSO(照 raffle-admin),主题跟 snb_theme。
  return (
    <ThemeScope theme="inherit" className="flex min-h-screen flex-col bg-snb-bg text-snb-t1">
      <OpsHeader />
      <div className="mx-auto w-full max-w-6xl flex-1 px-4 py-7">
        <Routes>
          <Route path="/" element={<Navigate to="/admin" replace />} />
          <Route path="/admin" element={<DashboardPage />} />
          <Route path="/admin/accounts" element={<AccountListPage />} />
          <Route path="/admin/accounts/new" element={<AccountFormPage mode="create" />} />
          <Route path="/admin/accounts/:id" element={<AccountFormPage mode="edit" />} />
        </Routes>
      </div>
    </ThemeScope>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}
