/** super-nb 品牌 Tailwind preset：港风霓虹网咖（2026-07-26 换装）
 *  语义槽位映射 tokens.css 的 CSS 变量（恒暗单主题，:root 与 .dark 合并声明）。
 *  spec: ai-relay/docs/superpowers/specs/2026-07-26-wangba-visual-language-design.md */

/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // 主灯管：霓虹橙（钱）——取代赤陶橙 #CC785C
        primary: {
          50: '#FFF1EA', 100: '#FFDFCF', 200: '#FFBE9F', 300: '#FF9C6F',
          400: '#FF843F', 500: '#FF6B35', 600: '#E5551F', 700: '#BC4315',
          800: '#93340F', 900: '#6B250A', 950: '#3D1404'
        },
        // 副灯管：青玉（量）
        jade: {
          50: '#E6FFFB', 100: '#B8FFF3', 200: '#7AFAE7', 300: '#3DEFD8',
          400: '#12E2CB', 500: '#00E5C7', 600: '#00B8A0', 700: '#008D7B',
          800: '#00655A', 900: '#00423B', 950: '#00221E'
        },
        // 点缀：品红（仪式）
        // ⚠️ 命名为 magenta 而非 fuchsia——Tailwind 3 内置已有 fuchsia 色阶，
        //    同名定义会静默覆盖它，让既有的 bg-fuchsia-* 用法变色且不报错
        magenta: {
          50: '#FFEBFB', 100: '#FFD1F5', 200: '#FFA3EB', 300: '#FF75E1',
          400: '#FF52DA', 500: '#FF3DDB', 600: '#DB1FB6', 700: '#AB158D',
          800: '#7C0F66', 900: '#530A44', 950: '#2C0524'
        },
        // 管芯过曝白：所有灯管字本身用它（发光纪律①）
        core: '#FFF4EA',
        // 湿沥青夜
        asphalt: { DEFAULT: '#070910', lit: '#0D111A', plate: '#090C13' },
        // 中性阶：原暖褐 dark 阶换值保名——MasonryCard/Alert/Skeleton/NavCapsule
        // 都在用 bg-dark-900 / border-l-dark-500 等，改名会让它们静默失效
        dark: {
          50: '#EDEFF4', 100: '#D6DAE4', 200: '#AEB5C6', 300: '#828CA3',
          400: '#5D6780', 500: '#434C61', 600: '#333A4A', 700: '#252B38',
          800: '#181D27', 900: '#0D111A', 950: '#070910'
        },
        // 辅助色（中性图表色，不承载品牌，随主题换装不动）
        accent: {
          50: '#f8fafc', 100: '#f1f5f9', 200: '#e2e8f0', 300: '#cbd5e1',
          400: '#94a3b8', 500: '#64748b', 600: '#475569', 700: '#334155',
          800: '#1e293b', 900: '#0f172a', 950: '#020617'
        },
        // 恒暗后 paper 不再是奶油纸，语义变成「亮色前景」（NavCapsule 用 text-paper）
        paper: { DEFAULT: '#F3E7DA', soft: '#FFF4EA', card: '#FFF4EA' },
        // 夜色底
        night: '#070910',
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
          tangerine: 'rgb(var(--snb-tangerine) / <alpha-value>)',
          jade: 'rgb(var(--snb-jade) / <alpha-value>)',
          fuchsia: 'rgb(var(--snb-fuchsia) / <alpha-value>)',
          core: 'rgb(var(--snb-core) / <alpha-value>)',
          // Alert 的 warning/danger 语义色（键名保留，值换霓虹告警档）
          amber: '#FFB020',
          ember: '#FF3B5C'
        }
      },
      fontFamily: {
        // 招牌：几何无衬线（Jost = Futura 开源复刻，自托管子集 SUPERNB 七字母，spec §9.2）
        sign: ['Jost', 'Futura', 'Avenir Next', 'Century Gothic', 'Helvetica Neue', 'Arial', 'sans-serif'],
        sans: [
          'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto',
          'Helvetica Neue', 'Arial', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'sans-serif'
        ],
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace']
        // ⚠️ display（Georgia 衬线）已删除：编辑风遗产，与霓虹冲突最大。
        //    所有 font-display 使用点须改 font-mono（数字）或 font-sans（中文）。
      },
      boxShadow: {
        card: 'var(--snb-shadow-card)',
        'card-hover': 'var(--snb-shadow-card-hover)',
        // Hero 级五层衰减辉光（发光纪律②）——全页仅一处
        tube:
          '0 0 3px rgb(var(--snb-tangerine) / 1), 0 0 10px rgb(var(--snb-tangerine) / .9), 0 0 26px rgb(var(--snb-tangerine) / .62), 0 0 58px rgb(var(--snb-tangerine) / .36), 0 0 116px rgb(var(--snb-tangerine) / .18)',
        // 次级两层辉光
        glow: '0 0 8px rgb(var(--snb-tangerine) / .85), 0 0 24px rgb(var(--snb-tangerine) / .42)',
        'glow-jade': '0 0 8px rgb(var(--snb-jade) / .85), 0 0 24px rgb(var(--snb-jade) / .42)',
        // 玻璃卡阴影（键名保留——GlassCard/AppHeader/NavCapsule 都在用，
        // 删键会让 Tailwind 静默不生成类且不报错）。恒暗后改纯黑阴影。
        glass: '0 8px 32px rgba(0, 0, 0, 0.45)',
        'glass-sm': '0 4px 16px rgba(0, 0, 0, 0.35)'
      },
      backgroundImage: {
        'gradient-primary': 'linear-gradient(135deg, #FF6B35 0%, #E5551F 100%)'
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
        // 呼吸点：霓虹橙。唯一消费方 NavCapsule 配的是 bg-primary-400 橙色圆点，
        // 且语义是「活动促销」属钱那一档——光晕必须与点同色，否则橙点发青光
        snbDotPulse: {
          '0%, 100%': { boxShadow: '0 0 0 0 rgb(var(--snb-tangerine) / .55)' },
          '50%': { boxShadow: '0 0 0 5px rgb(var(--snb-tangerine) / 0)' }
        },
        // 开机点亮：灯管通电过冲闪烁两次后稳定（spec §3.5）
        tubeOn: {
          '0%': { opacity: '.1' },
          '22%': { opacity: '1' },
          '30%': { opacity: '.25' },
          '38%': { opacity: '1' },
          '46%': { opacity: '.5' },
          '100%': { opacity: '1' }
        }
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'scale-in': 'scaleIn 0.2s ease-out',
        'fade-up': 'fadeUp 0.3s ease-out backwards',
        'snb-dot': 'snbDotPulse 2.4s ease-in-out infinite',
        'tube-on': 'tubeOn 1.15s both'
      }
    }
  }
}
