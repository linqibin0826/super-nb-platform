import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '../ui/styles.css'
import './hub.css'
import App from './App'
import { reconcileFromCookie } from '../auth/tokens'

// 渲染前同步对账父域 cookie 登录态（照 studio main.tsx）
reconcileFromCookie()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
