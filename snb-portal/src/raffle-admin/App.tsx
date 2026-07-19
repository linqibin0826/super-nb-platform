import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ThemeScope } from '../ui'
import { useTheme } from '../theme'
import { CampaignListPage } from './pages/CampaignListPage'
import { CampaignFormPage } from './pages/CampaignFormPage'

export function AppRoutes() {
  // 无站点 Header:本站以 iframe 嵌进 sub2api 后台自定义菜单(照抄 invoice 的 2026-07-17
  // 模式),站头/主题开关由宿主提供;主题跟父域 cookie snb_theme,登录靠父域 cookie SSO,
  // 不依赖 sub2api 往 iframe src 上拼的 token= 参数(设计稿 §2)。
  const [theme] = useTheme()
  return (
    <ThemeScope theme={theme} className="flex min-h-screen flex-col bg-snb-bg text-snb-t1">
      <div className="mx-auto w-full max-w-6xl flex-1 px-4 py-7">
        <Routes>
          <Route path="/" element={<Navigate to="/admin" replace />} />
          <Route path="/admin" element={<CampaignListPage />} />
          <Route path="/admin/campaigns/new" element={<CampaignFormPage mode="create" />} />
          <Route path="/admin/campaigns/:id" element={<CampaignFormPage mode="edit" />} />
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
