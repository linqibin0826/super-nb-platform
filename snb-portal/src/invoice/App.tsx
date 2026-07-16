import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import { ThemeScope } from '../ui'
import { useTheme } from '../theme'
import { t } from '../i18n'
import { InvoiceHeader } from './InvoiceHeader'
import { FirstVisitGuide } from './FirstVisitGuide'
import { useGuideAck } from '../guide/useGuideAck'
import { ApplyPage } from './pages/ApplyPage'
import { RequestsPage } from './pages/RequestsPage'
import { ProfilesPage } from './pages/ProfilesPage'
import { AdminPage } from './pages/AdminPage'

/** 站内二级导航(申请/我的申请/抬头;admin 不放导航,直链 /admin) */
function SubNav() {
  const tabs = [
    { to: '/', label: t('invoice.tabs.apply') },
    { to: '/requests', label: t('invoice.tabs.requests') },
    { to: '/profiles', label: t('invoice.tabs.profiles') },
  ]
  return (
    <nav className="mx-auto flex w-full max-w-6xl gap-1.5 px-4 pt-6">
      {tabs.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          end={tab.to === '/'}
          className={({ isActive }) =>
            `rounded-full px-4 py-1.5 text-sm transition-colors ${
              isActive
                ? 'bg-snb-panel font-semibold text-snb-t1 shadow-card'
                : 'text-snb-t2 hover:bg-snb-t1/5 hover:text-snb-t1'
            }`
          }
        >
          {tab.label}
        </NavLink>
      ))}
    </nav>
  )
}

export function AppRoutes() {
  const [theme, toggleTheme] = useTheme()
  const guide = useGuideAck('invoice.intro.v1')
  return (
    <ThemeScope theme={theme} className="flex min-h-screen flex-col bg-snb-bg text-snb-t1">
      {guide.show && <FirstVisitGuide onDismiss={guide.ack} />}
      <InvoiceHeader theme={theme} onToggleTheme={toggleTheme} />
      <SubNav />
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
