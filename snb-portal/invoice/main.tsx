import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '../src/ui/styles.css'
import '../src/invoice/invoice.css'
import App from '../src/invoice/App'
import { reconcileFromCookie } from '../src/auth/tokens'
import { initTheme } from '../src/themeCookie'

// 渲染前同步对账父域 cookie 登录态(照 studio/hub main.tsx)
reconcileFromCookie()
// 主题接线：迁移旧键 → 落地当前档 → 挂聚焦对账（跨源没有 storage 事件，
// 用户在别的子站切了档，本站只能靠重新获得焦点时重读 cookie 跟上）。
// 首帧防闪不靠这里，靠 index.html <head> 里的内联 boot 片段。
initTheme()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
