import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ThemeScope } from '../ui'
import { DashboardPage } from './pages/DashboardPage'
import { AccountListPage } from './pages/AccountListPage'
import { AccountFormPage } from './pages/AccountFormPage'

export function AppRoutes() {
  // 无站点 Header:内部运维台,登录靠父域 cookie SSO(照 raffle-admin),主题跟 snb_theme。
  return (
    <ThemeScope theme="inherit" className="flex min-h-screen flex-col bg-snb-bg text-snb-t1">
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
