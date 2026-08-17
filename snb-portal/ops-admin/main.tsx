import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '../src/ui/styles.css'
import App from '../src/ops-admin/App'
import { reconcileFromCookie } from '../src/auth/tokens'
import { initTheme } from '../src/themeCookie'

// 渲染前同步对账父域 cookie 登录态(照 studio/hub/invoice/raffle-admin main.tsx 的硬性时机)
reconcileFromCookie()
// 主题接线:首帧防闪靠 index.html <head> 的内联 boot 片段,这里负责迁移+聚焦对账。
initTheme()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
