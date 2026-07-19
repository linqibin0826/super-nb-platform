import { ThemeScope } from '../ui'
import { useTheme } from '../theme'

export function AppRoutes() {
  const [theme] = useTheme()
  return (
    <ThemeScope theme={theme} className="flex min-h-screen items-center justify-center bg-snb-bg text-snb-t1">
      抽奖管理筹备中
    </ThemeScope>
  )
}

export default function App() {
  return <AppRoutes />
}
