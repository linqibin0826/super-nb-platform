/** @type {import('tailwindcss').Config} */
// 品牌色阶/语义 token/字体/阴影/动画走 vendor 进本仓的设计系统 preset（单一信源，避免双源漂移）
import snbPreset from './tailwind-preset.js'

export default {
  presets: [snbPreset],
  // vendor 后设计系统组件源码在 src/ui/，由下方 './src/**' 一并扫描：组件工具类（含 sm:/md: 变体）
  // 与 studio 自身工具类收进同一份本仓样式表、顺序权重一致，不再有「ui 预构建 style.css 的响应式
  // 变体被本仓平铺工具类覆盖」的老坑（QuoteLine 点引线/备注消失的真凶）
  // 四个入口 html 一个都不能漏：漏掉的那个里写工具类会静默失效（今天 invoice/index.html 里
  // 只有 class="dark" 非工具类，所以无症状，但这种坑不会自己报错）
  content: [
    './index.html',
    './hub/index.html',
    './invoice/index.html',
    './raffle-admin/index.html',
    './ops-admin/index.html',
    './src/**/*.{ts,tsx}',
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
