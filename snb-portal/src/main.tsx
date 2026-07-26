import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// 设计系统样式须在本地样式之前：先 token 变量、再组件样式、最后 studio 自己的胶水
import './ui/tokens/tokens.css'
import './index.css'
import App from './App.tsx'
import { reconcileFromCookie } from './auth/tokens'
import { purgeLegacyThemeState } from './themeCookie'

// 首帧前跟父域 cookie 对账（同步）：子域名部署下登录态全靠它引导
reconcileFromCookie()
// 全站恒暗：.dark 已硬编码在 index.html 常驻，这里只做一次存量清理
// （删作废的父域 cookie snb_theme + 旧 localStorage 键）
purgeLegacyThemeState()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
