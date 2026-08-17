import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// ops-admin 资产运维台第五入口:root 指到 ops-admin/,独立产物 dist-ops-admin/,与其余四入口完全隔离。
export default defineConfig({
  cacheDir: 'node_modules/.vite-ops-admin', // 五 config 独立缓存防并行 dev 互踩(见 vite.config.ts 注)
  root: 'ops-admin',
  publicDir: 'public',
  base: process.env.OPS_ADMIN_BASE || '/',
  build: {
    outDir: '../dist-ops-admin',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          if (/node_modules\/(react|react-dom|scheduler)\//.test(id)) return 'react'
        },
      },
    },
  },
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 3500,
    proxy: {
      '/api': process.env.PORTAL_API_TARGET || 'http://127.0.0.1:8080',
      '/ops/v1': process.env.OPS_PLATFORM_TARGET || 'http://127.0.0.1:8081',
    },
  },
})
