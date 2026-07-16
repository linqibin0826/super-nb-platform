import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// invoice 发票中心第三入口:root 指到 invoice/,独立产物 dist-invoice/,与 studio/hub 完全隔离。
export default defineConfig({
  root: 'invoice',
  publicDir: 'public', // 相对 root:invoice/public → dist-invoice 根(favicon/logo 资产自持红线)
  base: process.env.INVOICE_BASE || '/',
  build: {
    outDir: '../dist-invoice',
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
    host: '127.0.0.1', // 本地 SSO cookie 靠 host 一致跨端口共享(studio/hub 同款)
    port: 3300,
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/invoice/v1': process.env.INVOICE_PLATFORM_TARGET || 'http://127.0.0.1:8081',
      '/guide/v1': process.env.INVOICE_PLATFORM_TARGET || 'http://127.0.0.1:8081',
    },
  },
})
