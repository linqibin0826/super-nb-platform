import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  // @super-nb/ui 走 link: 联包，不 dedupe 会解析出双 React 实例（useId 读 null 崩），
  // 与 vite.config.ts 的 resolve.dedupe 同因同修
  resolve: { dedupe: ['react', 'react-dom'] },
  test: { environment: 'jsdom' },
})
