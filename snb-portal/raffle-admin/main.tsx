import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '../src/ui/styles.css'
import '../src/raffle-admin/raffle-admin.css'
import App from '../src/raffle-admin/App'
import { reconcileFromCookie } from '../src/auth/tokens'

// 渲染前同步对账父域 cookie 登录态(照 studio/hub/invoice main.tsx 的硬性时机)
reconcileFromCookie()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
