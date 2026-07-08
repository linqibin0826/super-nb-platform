import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'
import fs from 'node:fs'
import path from 'node:path'

// 本地 dev 直读同级 super-nb-gallery 仓库（生产由 Caddy /playground-gallery/* 静态路由提供）
const GALLERY_DIR = path.resolve(__dirname, '../super-nb-gallery')
const MIME: Record<string, string> = {
  '.json': 'application/json',
  '.webp': 'image/webp',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
}

function devGallery(): Plugin {
  return {
    name: 'dev-gallery',
    configureServer(server) {
      server.middlewares.use('/playground-gallery', (req, res, next) => {
        const rel = decodeURIComponent((req.url ?? '/').split('?')[0])
        const file = path.join(GALLERY_DIR, rel)
        if (!file.startsWith(GALLERY_DIR) || !fs.existsSync(file) || !fs.statSync(file).isFile()) {
          return next()
        }
        res.setHeader('Content-Type', MIME[path.extname(file)] ?? 'application/octet-stream')
        fs.createReadStream(file).pipe(res)
      })
    },
  }
}

// 本地联调活动页：/activity/ 直接吐 activity.html（新 /activity/v1 契约版），与 studio 同源，
// 共享 localStorage 登录态（auth_token）；/activity/v1/* 落到下方 proxy 转发 snb-platform。
// 生产形态由 Caddy 负责静态页与反代，这段只服务本地测试环境。
const ACTIVITY_HTML =
  process.env.ACTIVITY_HTML ||
  path.resolve(__dirname, '../ai-relay/deployment/files/activity-svc/static/activity.html')

function devActivity(): Plugin {
  return {
    name: 'dev-activity',
    configureServer(server) {
      server.middlewares.use('/activity', (req, res, next) => {
        const rel = (req.url ?? '/').split('?')[0]
        if (rel !== '/' && rel !== '' && rel !== '/index.html') return next() // /activity/v1/* 走 proxy
        if (!fs.existsSync(ACTIVITY_HTML)) return next()
        res.setHeader('Content-Type', 'text/html; charset=utf-8')
        fs.createReadStream(ACTIVITY_HTML).pipe(res)
      })
    },
  }
}

export default defineConfig({
  // 路径部署（api.super-nb.me/studio）默认 /studio/；子域名部署构建时 STUDIO_BASE=/ 覆盖
  base: process.env.STUDIO_BASE || '/studio/',
  // ⚠️ @super-nb/ui 走 link: 联包，它的 react 会解析到自己 node_modules 的物理副本——
  // 生产 Rollup 会把两份 React 都打进去（双实例 → hooks dispatcher 为 null，白屏 useId 崩），
  // dev 有 optimizeDeps 兜底看不出来。dedupe 强制全部归到本仓副本。
  resolve: { dedupe: ['react', 'react-dom'] },
  build: {
    rollupOptions: {
      output: {
        // vendor 分包：react/motion 各自独立，发版只业务 chunk 变哈希，回头客命中长缓存。
        // @super-nb/ui 不显式分组：它是 link: 源码包、Rollup 按引用自然归并，强分组反而可能与
        // dedupe 后的 react 引用纠缠（双实例白屏红线）。
        manualChunks(id) {
          if (/node_modules\/(react|react-dom|scheduler)\//.test(id)) return 'react'
          if (/node_modules\/motion\//.test(id)) return 'motion'
        },
      },
    },
  },
  plugins: [react(), devGallery(), devActivity()],
  server: {
    // 显式绑 127.0.0.1：裸 localhost 会只绑 ::1，而本地 SSO 靠「127.0.0.1 host-only cookie
    // 跨端口共享」（本地 sub2api 控制台 :8080 登录 → studio :3100 接上），主机名必须一致
    host: '127.0.0.1',
    port: 3100,
    // 代理必须显式 127.0.0.1（裸 localhost 有 IPv6 坑）
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      // 生成链路可单独指到假上游演示/验证（本地栈渠道是真上游 key，绝不真调上游红线）：
      // STUDIO_V1_TARGET=http://127.0.0.1:8180 pnpm dev
      '/v1': process.env.STUDIO_V1_TARGET || 'http://127.0.0.1:8080',
      // 灵感库 + 活动中心 → snb-platform（本地 bootRun :8081；生产由 Caddy 反代 /gallery/v1、/activity/v1）
      '/gallery': process.env.STUDIO_PLATFORM_TARGET || 'http://127.0.0.1:8081',
      '/activity/v1': process.env.STUDIO_PLATFORM_TARGET || 'http://127.0.0.1:8081',
    },
  },
})
