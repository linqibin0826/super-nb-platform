import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// 设计系统样式须在本地样式之前：先 token 变量、再组件层（.snb-glass 等工具类）、最后 studio 自己的胶水。
// ⚠️ 2026-09-07：改为由 index.css 头部 @import './ui/styles.css' 统一引入（它自带 tokens 与三条 @tailwind），
//    本文件不再单独 import tokens.css——否则同一份变量会被打包两遍。
import './index.css'
import App from './App.tsx'
import { reconcileFromCookie } from './auth/tokens'
import { initTheme } from './themeCookie'

// 首帧前跟父域 cookie 对账（同步）：子域名部署下登录态全靠它引导
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
