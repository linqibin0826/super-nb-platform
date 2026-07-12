import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'
import fs from 'node:fs'
import path from 'node:path'

// 本地 dev 现场转换电子书：调内容仓库管线 transformBook + buildBook，吐与生产同构的 book.json
// （生产由发布管线预转换后 rsync 到 web/hub-books，Caddy /books/* 静态服务）。改转换逻辑刷新即见效。
// 缺省按主树同级布局解析；在 worktree 等异位场景用 HUB_BOOKS_DIR 显式指内容仓库 books/ 目录。
const BOOKS_DIR = process.env.HUB_BOOKS_DIR || path.resolve(__dirname, '../../super-nb-hub-content/books')

function devBooks(): Plugin {
  // 按源文件 mtime 缓存转换结果（jsdom 解析 220K ~百毫秒级，避免每请求重跑）
  const cache = new Map<string, { mtime: number; json: string }>()

  async function loadBook(slug: string): Promise<string | null> {
    const file = path.join(BOOKS_DIR, slug, 'source.html')
    if (!file.startsWith(BOOKS_DIR) || !fs.existsSync(file)) return null
    const libPath = path.join(BOOKS_DIR, '../scripts/lib/books.mjs')
    // 缓存键 = 书源 + 管线代码两者 mtime 取大——改任一侧刷新即生效
    const mtime = Math.max(fs.statSync(file).mtimeMs, fs.statSync(libPath).mtimeMs)
    const hit = cache.get(slug)
    if (hit && hit.mtime === mtime) return hit.json
    // 动态 import 内容仓库管线（依赖按该文件位置从内容仓库 node_modules 解析）；
    // 拼 mtime query 破 ESM 模块缓存
    const libUrl = new URL(`file://${libPath}`)
    libUrl.searchParams.set('v', String(mtime))
    const { transformBook, buildBook } = await import(/* @vite-ignore */ libUrl.href)
    const { meta, chapters } = transformBook(fs.readFileSync(file, 'utf8'))
    const json = JSON.stringify(buildBook(meta, chapters, slug))
    cache.set(slug, { mtime, json })
    return json
  }

  return {
    name: 'dev-hub-books',
    configureServer(server) {
      server.middlewares.use('/books', (req, res, next) => {
        const m = (req.url ?? '').split('?')[0].match(/^\/([a-z0-9-]+)\/book\.json$/)
        if (!m) return next()
        loadBook(m[1])
          .then((json) => {
            if (!json) {
              res.statusCode = 404
              res.end('book not found (dev)')
              return
            }
            res.setHeader('Content-Type', 'application/json; charset=utf-8')
            res.end(json)
          })
          .catch((e) => {
            res.statusCode = 500
            res.end('book transform failed (dev): ' + (e as Error).message)
          })
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
