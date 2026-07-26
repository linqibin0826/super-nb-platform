import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '../src/ui/styles.css'
import '../src/raffle-admin/raffle-admin.css'
import App from '../src/raffle-admin/App'
import { reconcileFromCookie } from '../src/auth/tokens'
import { purgeLegacyThemeState } from '../src/themeCookie'

// 渲染前同步对账父域 cookie 登录态(照 studio/hub/invoice main.tsx 的硬性时机)
reconcileFromCookie()
// 全站恒暗：.dark 已硬编码在 index.html 常驻，这里只做一次存量清理
// （删作废的父域 cookie snb_theme + 旧 localStorage 键）
purgeLegacyThemeState()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
