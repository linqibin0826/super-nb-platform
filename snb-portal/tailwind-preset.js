/** super-nb 品牌 Tailwind preset：零发光网吧 v2（2026-07-27 换装，取代港风霓虹）
 *  语义槽位映射 tokens.css 的 CSS 变量（恒暗单主题，:root 与 .dark 合并声明）。
 *  🚨 色值真源对齐 sub2api fork frontend/tailwind.config.js（safety 阶 / 沥青阶 /
 *  paper），两边逐字同值、改必同步。
 *  🪦 三灯管（霓虹橙/jade/magenta）、管芯 core、五层辉光 tube/glow、tubeOn 点火
 *  随霓虹整条退役——发光是「土」的根因，v2 的强调只有大、亮、橙三样。 */

/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // 工业安全橙：全站唯一强调色（fork primary 阶同值）
        primary: {
          50: '#FFF3EB', 100: '#FFE4D3', 200: '#FFC7A6', 300: '#FFA372',
          400: '#FF8342', 500: '#FF5C00', 600: '#DB4F00', 700: '#B04000',
          800: '#853000', 900: '#5C2100', 950: '#331200'
        },
        // 主字纸米白：覆盖 white 一次到位落实「主字不是纯白」纪律（fork 同款）
        white: '#EFEBE4',
        // 沥青底（偏青，不是纯黑；fork asphalt 同值）
        asphalt: { DEFAULT: '#0E1014', lit: '#12151B', plate: '#10131A' },
        // 中性阶：换值保名——MasonryCard/Alert/Skeleton/NavCapsule 都在用
        // bg-dark-900 / border-l-dark-500 等，改名会让它们静默失效。
        // 值 = fork 的 --ink 沥青阶（hex 形式），⚠️ 500→600 断崖是故意的：
        // 比 faint(500) 更暗的字不许存在，600 起是面色不是字色
        dark: {
          50: '#F6F4EF', 100: '#EFEBE4', 200: '#CACFD5', 300: '#B2B8C0',
          400: '#A0A7B0', 500: '#828B96', 600: '#2E3540', 700: '#242A33',
          800: '#171A20', 900: '#12151B', 950: '#0E1014'
        },
        // 辅助色（中性图表色，不承载品牌，随主题换装不动）
        accent: {
          50: '#f8fafc', 100: '#f1f5f9', 200: '#e2e8f0', 300: '#cbd5e1',
          400: '#94a3b8', 500: '#64748b', 600: '#475569', 700: '#334155',
          800: '#1e293b', 900: '#0f172a', 950: '#020617'
        },
        // 恒暗后 paper 语义=「亮色前景」（NavCapsule 用 text-paper，键名保留）
        paper: { DEFAULT: '#EFEBE4', soft: '#EFEBE4', card: '#EFEBE4' },
        // 夜色底（playground 在用，保名换值）
        night: '#0E1014',
        // 语义槽位：值来自 tokens.css
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
          // 强调只有两个：安全橙 + 功能红（🪦 tangerine/jade/fuchsia/core 已删）
          safety: 'rgb(var(--snb-safety) / <alpha-value>)',
          danger: 'rgb(var(--snb-danger) / <alpha-value>)',
          // Alert 的 warning/danger 语义色（键名保留，值收进 v2 双强调——
          // v2 色板没有黄：警告就是安全橙，危险就是功能红）
          amber: '#FF5C00',
          ember: '#E5484D'
        }
      },
      fontFamily: {
        // 招牌：几何无衬线（Jost = Futura 开源复刻，自托管子集，spec §9.2）
        // 🚨 v2 纪律：Jost 只给字标与数字
        sign: ['Jost', 'Futura', 'Avenir Next', 'Century Gothic', 'Helvetica Neue', 'Arial', 'sans-serif'],
        sans: [
          'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto',
          'Helvetica Neue', 'Arial', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'sans-serif'
        ],
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace']
        // ⚠️ display（Georgia 衬线）已删除：编辑风遗产。
        //    所有 font-display 使用点须改 font-mono（数字）或 font-sans（中文）。
      },
      boxShadow: {
        card: 'var(--snb-shadow-card)',
        'card-hover': 'var(--snb-shadow-card-hover)',
        // 🪦 tube / glow / glow-jade 辉光随零发光纪律退役。
        // glass 两键保名：纯黑投影非辉光，浮层在用（删键会静默不生成类）
        glass: '0 8px 32px rgba(0, 0, 0, 0.45)',
        'glass-sm': '0 4px 16px rgba(0, 0, 0, 0.35)'
      },
      // 🪦 gradient-primary 已删：霓虹渐变，v2 主按钮是纸白平钮不是橙渐变
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
        // 呼吸点：安全橙扩散环（透明度归零的 ring，不是光晕）。
        // 全站唯一会动的点——数据永远不闪，会动的只有状态灯
        snbDotPulse: {
          '0%, 100%': { boxShadow: '0 0 0 0 rgb(var(--snb-safety) / .5)' },
          '50%': { boxShadow: '0 0 0 5px rgb(var(--snb-safety) / 0)' }
        }
        // 🪦 tubeOn 点火闪烁已删：那是灯管的行为，新体系里没有灯管
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
