import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  // 设计系统已 vendor 进 src/ui（🪦 link: 联包退役）；dedupe 仍保留——不 dedupe 会解析出双 React 实例（useId 读 null 崩），
  // 与 vite.config.ts 的 resolve.dedupe 同因同修
  resolve: { dedupe: ['react', 'react-dom'] },
  test: { environment: 'jsdom' },
})
