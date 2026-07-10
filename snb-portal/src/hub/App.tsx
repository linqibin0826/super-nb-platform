import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { ThemeScope } from '../ui'
import { useTheme } from '../theme'
import { HubHeader } from './HubHeader'
import { ListPage } from './pages/ListPage'
import { ArticlePage } from './pages/ArticlePage'
import { ReaderPage } from './pages/ReaderPage'

/** 路由树独立导出：测试用 MemoryRouter 包它，生产由下方 App 套 BrowserRouter */
export function AppRoutes() {
  const [theme, toggleTheme] = useTheme()
  return (
    <ThemeScope theme={theme} className="min-h-screen bg-snb-bg text-snb-t1">
      <Routes>
        <Route
          path="/reader/:slug"
          element={<ReaderPage />}
        />
        <Route
          path="/a/:slug"
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
