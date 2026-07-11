import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'
import fs from 'node:fs'
import path from 'node:path'

// 本地 dev 直读 super-nb/ 下内容仓库的电子书原始 HTML（生产由 Caddy /books/* 从 web/hub-books 静态服务）。
// 照 vite.config.ts 的 devGallery 先例；dev 吐的是未清洗的 source.html，与生产清洗版仅差字体 @import。
// 缺省按主树同级布局解析；在 worktree 等异位场景用 HUB_BOOKS_DIR 显式指内容仓库 books/ 目录。
const BOOKS_DIR = process.env.HUB_BOOKS_DIR || path.resolve(__dirname, '../../super-nb-hub-content/books')

function devBooks(): Plugin {
  return {
    name: 'dev-hub-books',
    configureServer(server) {
      server.middlewares.use('/books', (req, res, next) => {
        const m = (req.url ?? '').split('?')[0].match(/^\/([a-z0-9-]+)\.html$/)
        if (!m) return next()
        const file = path.join(BOOKS_DIR, m[1], 'source.html')
        if (!file.startsWith(BOOKS_DIR) || !fs.existsSync(file)) {
          res.statusCode = 404
          res.end('book not found (dev)')
          return
        }
        res.setHeader('Content-Type', 'text/html; charset=utf-8')
        fs.createReadStream(file).pipe(res)
      })
    },
  }
}

// hub 内容中心第二入口：root 指到 hub/（hub/index.html 即入口），独立产物 dist-hub/，
// 与 studio 的 vite.config.ts 完全隔离——两入口各自 tree-shake，互不打包对方。
export default defineConfig({
  root: 'hub',
  publicDir: 'public', // 相对 root：hub/public → dist-hub 根（favicon/logo 资产自持红线）
  base: process.env.HUB_BASE || '/',
  build: {
    outDir: '../dist-hub',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        // vendor 分包口径与 studio 一致：react 独立 chunk，发版只业务 chunk 变哈希
        manualChunks(id: string) {
          if (/node_modules\/(react|react-dom|scheduler)\//.test(id)) return 'react'
          if (/node_modules\/motion\//.test(id)) return 'motion'
        },
      },
    },
  },
  plugins: [react(), devBooks()],
  server: {
    host: '127.0.0.1', // 与 studio 同理：本地 SSO cookie 靠 host 一致跨端口共享
    port: 3200,
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/content/v1': process.env.HUB_PLATFORM_TARGET || 'http://127.0.0.1:8081',
    },
  },
})
