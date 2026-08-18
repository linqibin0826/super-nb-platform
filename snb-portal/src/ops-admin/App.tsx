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

function TopLink({ to, end, children }: { to: string; end?: boolean; children: string }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        cx(
          'rounded-lg px-3 py-1.5 text-sm transition-colors duration-quick ease-snb focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus',
          isActive
            ? 'bg-black/[0.05] font-medium text-snb-t1 dark:bg-white/[0.07]'
            : 'text-snb-t2 hover:bg-snb-t1/[0.06] hover:text-snb-t1'
        )
      }
    >
      {children}
    </NavLink>
  )
}

/** 常驻顶条:身份牌 + 两个去处 + 外部面板 + 灯开关。
 *  ⚠️ 不用 backdrop-filter(顶栏 blur 会把内部 fixed 浮层的包含块拐走,三仓库各撞过一次),实底即可。 */
function OpsHeader() {
  return (
    <header className="sticky top-0 z-40 border-b border-snb-hairline bg-snb-bg">
      <div className="mx-auto flex h-14 w-full max-w-6xl items-center gap-3 px-4">
        <NavLink
          to="/admin"
          className="flex items-center gap-2.5 rounded-lg focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus"
        >
          <StatusLamp state="live" />
          <span className="text-[15px] font-semibold text-snb-t1">资产运维台</span>
          <span className={cx('hidden text-[11px] tracking-[0.22em] text-snb-t3 sm:inline', MONO)}>SNB·OPS</span>
        </NavLink>
        <nav className="ml-auto flex items-center gap-1">
          <TopLink to="/admin" end>
            看板
          </TopLink>
          <TopLink to="/admin/accounts">台账</TopLink>
          {PANELS.map((p) => (
            <a
              key={p.href}
              href={p.href}
              target="_blank"
              rel="noreferrer"
              className="hidden rounded-lg px-3 py-1.5 text-sm text-snb-t2 transition-colors duration-quick ease-snb hover:bg-snb-t1/[0.06] hover:text-snb-t1 focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus sm:inline"
            >
              {p.label} ↗
            </a>
          ))}
          <ThemeToggle className="ml-1 h-9 w-9" />
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
