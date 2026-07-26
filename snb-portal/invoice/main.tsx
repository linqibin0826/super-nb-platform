import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '../src/ui/styles.css'
import '../src/invoice/invoice.css'
import App from '../src/invoice/App'
import { reconcileFromCookie } from '../src/auth/tokens'
import { purgeLegacyThemeState } from '../src/themeCookie'

// 渲染前同步对账父域 cookie 登录态(照 studio/hub main.tsx)
reconcileFromCookie()
// 全站恒暗：.dark 已硬编码在 index.html 常驻，这里只做一次存量清理
// （删作废的父域 cookie snb_theme + 旧 localStorage 键）
purgeLegacyThemeState()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
