import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// 设计系统样式须在本地样式之前：先 token 变量、再组件样式、最后 studio 自己的胶水
import '@super-nb/ui/tokens.css'
import '@super-nb/ui/style.css'
import './index.css'
import App from './App.tsx'
import { reconcileFromCookie } from './auth/tokens'

// 首帧前跟父域 cookie 对账（同步）：子域名部署下登录态全靠它引导
reconcileFromCookie()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
