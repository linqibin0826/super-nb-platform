import { BrowserRouter, Navigate, Route, Routes, useParams } from 'react-router-dom'
import { ThemeScope } from '../ui'
import { useTheme } from '../theme'
import { HubHeader } from './HubHeader'
import { HubFooter } from './HubFooter'
import { ListPage } from './pages/ListPage'
import { ArticlePage } from './pages/ArticlePage'

/** 旧阅读页路由兼容：电子书已并入文章页版式（2026-07-11 站长拍板），/reader/:slug 一律重定向 /a/:slug。 */
function ReaderRedirect() {
  const { slug = '' } = useParams()
  return <Navigate to={`/a/${slug}`} replace />
}

/** 路由树独立导出：测试用 MemoryRouter 包它，生产由下方 App 套 BrowserRouter */
export function AppRoutes() {
  const [theme, toggleTheme] = useTheme()
  return (
    <ThemeScope theme={theme} className="flex min-h-screen flex-col bg-snb-bg text-snb-t1">
      {/* 页脚钉底：路由内容撑 flex-1，短页（空态/加载态）页脚也贴底不悬空 */}
      <div className="flex-1">
        <Routes>
          <Route path="/reader/:slug" element={<ReaderRedirect />} />
          <Route
            path="/a/:slug/:part?"
            element={
              <>
                <HubHeader theme={theme} onToggleTheme={toggleTheme} />
                <ArticlePage />
              </>
            }
          />
          <Route
            path="/"
            element={
              <>
                <HubHeader theme={theme} onToggleTheme={toggleTheme} />
                <ListPage />
              </>
            }
          />
        </Routes>
      </div>
      <HubFooter />
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
