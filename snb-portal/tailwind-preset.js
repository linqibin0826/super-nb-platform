/** super-nb 品牌 Tailwind preset：苹果式 v3（2026-09-07 换代，取代零发光网吧 v2）
 *  语义槽位映射 tokens.css 的 CSS 变量。**双档**：浅色进 `:root`、深色进 `.dark`。
 *  spec：ai-relay docs/superpowers/specs/2026-09-07-apple-glass-visual-language-design.md
 *
 *  ⚠️ 双档纪律：凡两档取值不同的东西一律走 `var(--snb-*)`，**不许在 preset 里写死**。
 *  下面这些固定 hex 阶（primary / asphalt / dark / paper / night）是**定值**，只许出现在
 *  「图片与深色画布上的元件」以及 `dark:` 前缀里；页面元件一律用 snb.* 语义槽。
 *  🪦 键帽底边（edge-*、key、key-down）保名：值退化成发丝线，只为不让消费方静默失效。 */

/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // 赤陶橙阶：400 = 深色强调 #E08F72，500 = 品牌原色 #CC785C，600 = 浅色强调 #A85A3F
        primary: {
          50: '#FCF1EC', 100: '#F8E1D6', 200: '#F2C3B0', 300: '#EBA58C',
          400: '#E08F72', 500: '#CC785C', 600: '#A85A3F', 700: '#874633',
          800: '#6B3828', 900: '#4D2A1E', 950: '#2E1912'
        },
        white: '#FFFFFF',
        // 深色画布定值（保名）：apple 深色
        asphalt: { DEFAULT: '#000000', lit: '#1D1D1F', plate: '#161617' },
        // 中性阶（保名换值，apple 灰阶）：500→600 断崖仍是字色/面色分界
        dark: {
          50: '#F5F5F7', 100: '#E8E8ED', 200: '#D2D2D7', 300: '#AEAEB2',
          400: '#A1A1A6', 500: '#86868B', 600: '#3A3A3C', 700: '#2C2C2E',
          800: '#1D1D1F', 900: '#161617', 950: '#000000'
        },
        accent: {
          50: '#f8fafc', 100: '#f1f5f9', 200: '#e2e8f0', 300: '#cbd5e1',
          400: '#94a3b8', 500: '#64748b', 600: '#475569', 700: '#334155',
          800: '#1e293b', 900: '#0f172a', 950: '#020617'
        },
        // 🪦 深色画布上的「浅色前景」定值键（Lightbox 等），页面元件走 snb.cta*
        paper: { DEFAULT: '#F5F5F7', soft: '#F5F5F7', card: '#FFFFFF', hover: '#FFFFFF', press: '#E8E8ED' },
        night: '#000000',
        snb: {
          bg: 'rgb(var(--snb-bg) / <alpha-value>)',
          panel: 'rgb(var(--snb-panel) / <alpha-value>)',
          elv: 'rgb(var(--snb-elv) / <alpha-value>)',
          well: 'rgb(var(--snb-well) / <alpha-value>)',
          t1: 'rgb(var(--snb-t1) / <alpha-value>)',
          t2: 'rgb(var(--snb-t2) / <alpha-value>)',
          t3: 'rgb(var(--snb-t3) / <alpha-value>)',
          t4: 'rgb(var(--snb-t4) / <alpha-value>)',
          cta: 'rgb(var(--snb-cta-bg) / <alpha-value>)',
          'cta-fg': 'rgb(var(--snb-cta-fg) / <alpha-value>)',
          'cta-hover': 'rgb(var(--snb-cta-hover) / <alpha-value>)',
          'cta-press': 'rgb(var(--snb-cta-press) / <alpha-value>)',
          'lamp-off': 'rgb(var(--snb-lamp-off) / <alpha-value>)',
          hairline: 'var(--snb-hairline)',
          'hairline-strong': 'var(--snb-hairline-strong)',
          'hairline-heavy': 'var(--snb-hairline-heavy)',
          'hairline-hover': 'var(--snb-hairline-hover)',
          'key-side': 'var(--snb-key-side)',
          'key-border': 'var(--snb-key-border)',
          'key-active-edge': 'var(--snb-key-active-edge)',
          'key-active': 'var(--snb-key-active-bg)',
          'hdr-line': 'var(--snb-hdr-line)',
          mask: 'var(--snb-mask)',
          focus: 'var(--snb-focus)',
          glass: 'var(--snb-glass-bg)',
          'glass-border': 'var(--snb-glass-border)',
          safety: 'rgb(var(--snb-safety) / <alpha-value>)',
          brand: 'rgb(var(--snb-brand) / <alpha-value>)',
          danger: 'rgb(var(--snb-danger) / <alpha-value>)',
          live: 'rgb(var(--snb-live) / <alpha-value>)',
          'live-ink': 'rgb(var(--snb-live-ink) / <alpha-value>)',
          // Badge/StatCard/Alert 标题在 10% 染色底与 well 上的字色（同 live-ink 先例）
          'safety-ink': 'rgb(var(--snb-safety-ink) / <alpha-value>)',
          'danger-ink': 'rgb(var(--snb-danger-ink) / <alpha-value>)',
          // 🪦 Alert 旧语义色键保名，值改指语义槽、随档翻——不再是定值
          amber: 'rgb(var(--snb-safety) / <alpha-value>)',
          ember: 'rgb(var(--snb-danger) / <alpha-value>)'
        }
      },
      fontFamily: {
        // 全站系统栈；sign 保名 = sans（🪦 v2 招牌字随网吧退役）
        sans: [
          '-apple-system', 'BlinkMacSystemFont', 'SF Pro Text', 'SF Pro Display', 'Helvetica Neue',
          'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'sans-serif'
        ],
        sign: [
          '-apple-system', 'BlinkMacSystemFont', 'SF Pro Text', 'SF Pro Display', 'Helvetica Neue',
          'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'sans-serif'
        ],
        mono: ['ui-monospace', 'SF Mono', 'Menlo', 'monospace']
      },
      boxShadow: {
        card: 'var(--snb-shadow-card)',
        'card-hover': 'var(--snb-shadow-card-hover)',
        glass: 'var(--snb-shadow-glass)',
        'glass-sm': 'var(--snb-shadow-glass-sm)',
        cta: 'var(--snb-shadow-cta)',
        // 🪦 键帽底边保名降级：全部退化成 1px 发丝线（消费方不静默失效）
        'edge-1': '0 1px 0 var(--snb-edge)',
        'edge-2': '0 1px 0 var(--snb-edge)',
        'edge-3': '0 1px 0 var(--snb-edge)',
        key: '0 1px 0 var(--snb-key-side)',
        'key-down': '0 1px 0 var(--snb-key-side)'
      },
      transitionDuration: {
        press: '100ms', quick: '200ms', settle: '400ms', breath: '2200ms'
      },
      transitionTimingFunction: {
        snb: 'cubic-bezier(0.32, 0.72, 0, 1)',
        'snb-quick': 'cubic-bezier(0.25, 1, 0.5, 1)'
      },
      borderRadius: {
        chip: 'var(--snb-r-chip)',
        lg: 'var(--snb-r-thumb)',
        xl: 'var(--snb-r-sheet)',
        '2xl': 'var(--snb-r-card)',
        '3xl': 'var(--snb-r-panel)',
        '4xl': 'var(--snb-r-hero)',
        glass: 'var(--snb-r-panel)'
      },
      keyframes: {
        fadeIn: { '0%': { opacity: '0' }, '100%': { opacity: '1' } },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' }
        },
        scaleIn: {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' }
        },
        fadeUp: {
          from: { opacity: '0', transform: 'translateY(10px)' },
          to: { opacity: '1', transform: 'translateY(0)' }
        },
        // 在线点：绿色扩散环（透明度归零的 ring，不是光晕）
        snbDotPulse: {
          '0%, 100%': { boxShadow: '0 0 0 0 rgb(var(--snb-live) / .5)' },
          '70%': { boxShadow: '0 0 0 7px rgb(var(--snb-live) / 0)' }
        },
        snbMenuIn: {
          from: { opacity: '0', transform: 'translateY(-8px)' },
          to: { opacity: '1', transform: 'translateY(0)' }
        },
        // 浮层物化：blur + scale + opacity 一起动（motion.md §5）
        snbMaterialize: {
          from: { opacity: '0', transform: 'translateY(12px) scale(0.98)' },
          to: { opacity: '1', transform: 'none' }
        }
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'scale-in': 'scaleIn 0.2s ease-out',
        'fade-up': 'fadeUp 0.3s ease-out backwards',
        'snb-dot': 'snbDotPulse 2.2s ease-in-out infinite',
        'snb-menu': 'snbMenuIn 400ms cubic-bezier(0.32, 0.72, 0, 1)',
        'snb-mask': 'fadeIn 250ms ease-out',
        'snb-materialize': 'snbMaterialize 400ms cubic-bezier(0.32, 0.72, 0, 1)'
      }
    }
  }
}
