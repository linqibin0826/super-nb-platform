/** super-nb 品牌 Tailwind preset：原始色阶蒸馏自 sub2api fork frontend/tailwind.config.js（逐字），
 *  语义槽位映射 tokens.css 的 CSS 变量（:root 浅色 / .dark 暗色）。 */

/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // 赤陶橙主色（Anthropic 风）
        primary: {
          50: '#FBF4F1', 100: '#F6E5DD', 200: '#EECABA', 300: '#E2A88F',
          400: '#D78D6F', 500: '#CC785C', 600: '#B5634A', 700: '#97503C',
          800: '#7A4231', 900: '#65392B', 950: '#371C14'
        },
        // 暖褐中性阶（浅色当墨字，暗色当面层）
        dark: {
          50: '#FAF7F4', 100: '#F0EAE4', 200: '#DDD2C8', 300: '#C2B3A5',
          400: '#99887A', 500: '#736356', 600: '#544840', 700: '#3B332C',
          800: '#272019', 900: '#1B1611', 950: '#110D09'
        },
        // 辅助色（深蓝灰，图表用）
        accent: {
          50: '#f8fafc', 100: '#f1f5f9', 200: '#e2e8f0', 300: '#cbd5e1',
          400: '#94a3b8', 500: '#64748b', 600: '#475569', 700: '#334155',
          800: '#1e293b', 900: '#0f172a', 950: '#020617'
        },
        // 编辑风奶油纸面
        paper: { DEFAULT: '#F0EEE6', soft: '#F5F4EE', card: '#FAF9F5' },
        // 夜色底（learn/studio 已统一）
        night: '#17110D',
        // 语义槽位：值随 :root/.dark 翻转（tokens.css）
        snb: {
          bg: 'rgb(var(--snb-bg) / <alpha-value>)',
          panel: 'rgb(var(--snb-panel) / <alpha-value>)',
          elv: 'rgb(var(--snb-elv) / <alpha-value>)',
          well: 'rgb(var(--snb-well) / <alpha-value>)',
          t1: 'rgb(var(--snb-t1) / <alpha-value>)',
          t2: 'rgb(var(--snb-t2) / <alpha-value>)',
          t3: 'rgb(var(--snb-t3) / <alpha-value>)',
          hairline: 'var(--snb-hairline)',
          'hairline-strong': 'var(--snb-hairline-strong)',
          // 余烬暖色语义（learn）
          amber: '#D9A35C',
          ember: '#E0604C'
        }
      },
      fontFamily: {
        sans: [
          'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto',
          'Helvetica Neue', 'Arial', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'sans-serif'
        ],
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace'],
        display: ['Georgia', 'Songti SC', 'STSong', 'Noto Serif SC', 'serif']
      },
      boxShadow: {
        // card 双主题（浅=fork 暖褐系 / 暗=studio 黑系），走变量
        card: 'var(--snb-shadow-card)',
        'card-hover': 'var(--snb-shadow-card-hover)',
        glass: '0 8px 32px rgba(70, 50, 38, 0.10)',
        'glass-sm': '0 4px 16px rgba(70, 50, 38, 0.08)',
        glow: '0 0 20px rgba(204, 120, 92, 0.25)',
        'glow-lg': '0 0 40px rgba(204, 120, 92, 0.35)'
      },
      backgroundImage: {
        'gradient-primary': 'linear-gradient(135deg, #CC785C 0%, #B5634A 100%)'
      },
      borderRadius: {
        lg: '0.625rem', xl: '0.875rem', '2xl': '1.25rem', '3xl': '1.75rem', '4xl': '2rem',
        glass: '18px'
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
        snbDotPulse: {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(204, 120, 92, 0.5)' },
          '50%': { boxShadow: '0 0 0 5px rgba(204, 120, 92, 0)' }
        }
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'scale-in': 'scaleIn 0.2s ease-out',
        'fade-up': 'fadeUp 0.3s ease-out backwards',
        'snb-dot': 'snbDotPulse 2.2s ease-in-out infinite'
      }
    }
  }
}
