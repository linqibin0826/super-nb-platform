import { useState } from 'react'
import { BrowserRouter, NavLink, Route, Routes, useLocation } from 'react-router-dom'
import { ThemeScope } from '../ui'
import { t } from '../i18n'
import { ti } from './copy'
import { FirstVisitGuide } from './FirstVisitGuide'
import { HomeBar } from './HomeBar'
import { useGuideAck } from '../guide/useGuideAck'
import { ApplyPage } from './pages/ApplyPage'
import { RequestsPage } from './pages/RequestsPage'
import { ProfilesPage } from './pages/ProfilesPage'
import { AdminPage } from './pages/AdminPage'

/** 站内二级导航(申请/我的申请/抬头;admin 不放导航,直链 /admin)。
 *  柜台页(/admin)不渲染——那是站长在控制台里嵌的管理入口,用户端页签混进去不像话。
 *  热区:手机整宽三等分直接 44 高;桌面视觉胶囊 32 + 上下透明内边距 = 44(2026-07-29 定稿)。
 *  右侧常驻「再看一遍开票须知」——须知单读过之后入口不消失,只在「申请开票」这条路由出现。 */
function SubNav({ onOpenGuide }: { onOpenGuide: () => void }) {
  const { pathname } = useLocation()
  if (pathname.startsWith('/admin')) return null
  const tabs = [
    { to: '/', label: t('invoice.tabs.apply') },
    { to: '/requests', label: t('invoice.tabs.requests') },
    { to: '/profiles', label: t('invoice.tabs.profiles') },
  ]
  return (
    <div className="mx-auto flex w-full max-w-6xl flex-wrap items-center justify-between gap-2 px-4 pt-6">
      <nav className="grid w-full grid-cols-3 gap-1.5 sm:flex sm:w-auto sm:gap-1">
        {tabs.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            end={tab.to === '/'}
            className="flex h-11 items-center justify-center sm:h-auto sm:justify-start sm:py-1.5"
          >
            {({ isActive }) => (
              <span
                className={`flex h-full w-full items-center justify-center rounded-full px-4 text-[13.5px] transition-colors sm:h-8 sm:w-auto ${
                  isActive
                    ? 'bg-paper font-semibold text-asphalt'
                    : 'border border-snb-hairline-strong text-snb-t2 hover:border-[rgba(239,235,228,0.3)] hover:text-snb-t1'
                }`}
              >
                {tab.label}
              </span>
            )}
          </NavLink>
        ))}
      </nav>
      {pathname === '/' && (
        <button
          type="button"
          className="flex h-11 items-center rounded-lg border border-snb-hairline-strong px-4 text-[13.5px] text-snb-t1 transition-colors hover:border-[rgba(239,235,228,0.3)] hover:bg-snb-panel"
          onClick={onOpenGuide}
        >
          {ti('invoice.guide.reopen')}
        </button>
      )}
    </div>
  )
}

export function AppRoutes() {
  // 无站点 Header:本站以 iframe 嵌进控制台(2026-07-17 站长拍板),站头/主题开关由宿主提供;
  // 独立标签页形态才补一条 40px 回家条(HomeBar 自己判定容器)。
  const guide = useGuideAck('invoice.intro.v1')
  const [reopen, setReopen] = useState(false)
  const { pathname } = useLocation()
  // 须知单只挡「申请开票」这一条路由(2026-07-29 定稿):我的申请 / 抬头管理不再被拦
  const onApply = pathname === '/'
  return (
    <ThemeScope theme="dark" className="flex min-h-screen flex-col bg-snb-bg text-snb-t1">
      {onApply && (guide.show || reopen) && (
        <FirstVisitGuide
          onConfirm={() => {
            setReopen(false)
            guide.ack()
          }}
          onSkip={() => {
            setReopen(false)
            guide.hide()
          }}
        />
      )}
      <HomeBar />
      <SubNav onOpenGuide={() => setReopen(true)} />
      <div className="mx-auto w-full max-w-6xl flex-1 px-4 py-7">
        <Routes>
          <Route path="/" element={<ApplyPage />} />
          <Route path="/requests" element={<RequestsPage />} />
          <Route path="/profiles" element={<ProfilesPage />} />
          <Route path="/admin" element={<AdminPage />} />
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
