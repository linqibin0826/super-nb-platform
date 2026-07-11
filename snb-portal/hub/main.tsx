import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '../src/ui/styles.css'
import '../src/hub/hub.css'
import App from '../src/hub/App'
import { reconcileFromCookie } from '../src/auth/tokens'

// 渲染前同步对账父域 cookie 登录态（照 studio main.tsx）
reconcileFromCookie()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
