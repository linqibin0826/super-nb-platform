import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// raffle-admin 抽奖管理第四入口:root 指到 raffle-admin/,独立产物 dist-raffle-admin/,与 studio/hub/invoice 完全隔离。
export default defineConfig({
  root: 'raffle-admin',
  publicDir: 'public',
  base: process.env.RAFFLE_ADMIN_BASE || '/',
  build: {
    outDir: '../dist-raffle-admin',
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
    port: 3400,
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/activity/v1': process.env.RAFFLE_PLATFORM_TARGET || 'http://127.0.0.1:8081',
    },
  },
})
