/** @type {import('tailwindcss').Config} */
// 品牌色阶/语义 token/字体/阴影/动画统一走 @super-nb/ui 的 preset（单一信源，避免双源漂移）
import snbPreset from '@super-nb/ui/tailwind-preset'

export default {
  presets: [snbPreset],
  // 必须扫描 @super-nb/ui 产物：组件用到的工具类（含 sm:/md: 变体）要收进本仓这份「顺序正确
  // 且最后加载」的样式表——只靠 ui 预构建 style.css 时，其响应式变体会被本仓后加载的同权重
  // 平铺工具类（如 .hidden）覆盖（QuoteLine 点引线/备注消失的真凶）
  content: ['./index.html', './src/**/*.{ts,tsx}', './node_modules/@super-nb/ui/dist/super-nb-ui.js'],
  theme: {
    extend: {},
  },
  plugins: [],
}
