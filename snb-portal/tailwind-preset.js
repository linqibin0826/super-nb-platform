/** super-nb 品牌 Tailwind preset：零发光网吧 v2（2026-07-27 换装，取代港风霓虹）
 *  语义槽位映射 tokens.css 的 CSS 变量。**双档**：浅色进 `:root`、深色进 `.dark`
 *  （2026-07-29 按原计划补全第二步；双档一直在计划内，07-26 恒暗是分期上线的第一步）。
 *  🚨 色值真源对齐 sub2api fork frontend/tailwind.config.js（safety 阶 / 沥青阶 /
 *  paper），两边逐字同值、改必同步。
 *  🪦 三灯管（霓虹橙/jade/magenta）、管芯 core、五层辉光 tube/glow、tubeOn 点火
 *  随霓虹整条退役——发光是「土」的根因，v2 的强调只有大、亮、橙三样。
 *
 *  ⚠️ 双档纪律：凡两档取值不同的东西一律走 `var(--snb-*)`，**不许在 preset 里写死**。
 *  下面这些固定 hex 阶（primary / asphalt / dark / paper / night）是**深色档的定值**，
 *  只许出现在「图片与深色画布上的元件」以及 `dark:` 前缀里；页面元件一律用 snb.* 语义槽。 */

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
        // 🪦 深色时期的「纸白前景」定值键，**只许用在图片/深色画布上的元件**（Lightbox 等）；
        // 页面上的主按钮与选中态已改走 snb.cta*（两档翻转：深夜纸白 ↔ 白天墨块）
        paper: { DEFAULT: '#EFEBE4', soft: '#EFEBE4', card: '#EFEBE4', hover: '#FFFFFF', press: '#DED8CE' },
        // 夜色底（playground 在用，保名换值）
        night: '#0E1014',
        // 语义槽位：值来自 tokens.css（双档，浅色 :root / 深色 .dark）
        snb: {
          bg: 'rgb(var(--snb-bg) / <alpha-value>)',
          panel: 'rgb(var(--snb-panel) / <alpha-value>)',
          elv: 'rgb(var(--snb-elv) / <alpha-value>)',
          well: 'rgb(var(--snb-well) / <alpha-value>)',
          t1: 'rgb(var(--snb-t1) / <alpha-value>)',
          t2: 'rgb(var(--snb-t2) / <alpha-value>)',
          t3: 'rgb(var(--snb-t3) / <alpha-value>)',
          // 主按钮 / 选中态四态：深夜纸白底沥青字 ↔ 白天墨块底纸白字。
          // 悬停在深色是**提亮**、在浅色是**加深**——「白天里更用力等于更黑」
          cta: 'rgb(var(--snb-cta-bg) / <alpha-value>)',
          'cta-fg': 'rgb(var(--snb-cta-fg) / <alpha-value>)',
          'cta-hover': 'rgb(var(--snb-cta-hover) / <alpha-value>)',
          'cta-press': 'rgb(var(--snb-cta-press) / <alpha-value>)',
          // 状态灯灭态（空心描边 / 灰实心）：深色 #828B96 → 浅色 #86837C
          'lamp-off': 'rgb(var(--snb-lamp-off) / <alpha-value>)',
          hairline: 'var(--snb-hairline)',
          'hairline-strong': 'var(--snb-hairline-strong)',
          'hairline-heavy': 'var(--snb-hairline-heavy)',
          'hairline-hover': 'var(--snb-hairline-hover)', // 兼容别名 = heavy
          // 键帽侧壁：深色是黑半透实边，浅色是实色暖灰 #C9C2B4（读作侧壁不是影子）
          'key-side': 'var(--snb-key-side)',
          // 键帽 1px 描边：⚠️ 必须与 key-side 同为「非 dark: 变体」的普通类，
          // 否则 `dark:border-*` 的 :is(.dark *) 特异度会盖掉后写的 border-b-* 侧壁
          'key-border': 'var(--snb-key-border)',
          // 键帽按到底时收成 1px 的那道边（深 #242A33 / 浅 #C9C2B4，与 app-header.html 同槽）
          'key-active-edge': 'var(--snb-key-active-edge)',
          // 键帽当前位填充：深色 7% 微亮平底 / 浅色抬升面实底（整串色值，不吃 alpha 修饰）
          'key-active': 'var(--snb-key-active-bg)',
          // sticky 顶栏底边（深 hairline / 浅栏位线实色）
          'hdr-line': 'var(--snb-hdr-line)',
          // 浮层遮罩：🚨 两档都是暗的（浅色 = 墨 .32），遮罩变白会让底下页面像没关掉
          mask: 'var(--snb-mask)',
          // 焦点环：深色半透安全橙（原值），浅色实色熔铁橙（半透压纸只有 2.09:1）
          focus: 'var(--snb-focus)',
          // 唯一玻璃例外=sticky 顶栏：半透底 + 栏位线实色（深 #242A33 / 浅 #CFC8B8）
          glass: 'var(--snb-glass-bg)',
          'glass-border': 'var(--snb-glass-border)',
          // 强调只有两个：安全橙 + 功能红（🪦 tangerine/jade/fuchsia/core 已删）
          safety: 'rgb(var(--snb-safety) / <alpha-value>)',
          danger: 'rgb(var(--snb-danger) / <alpha-value>)',
          // 🪦 Alert 旧语义色键：值是**深色定值**，两档不翻。保名防消费方静默失效，
          // 本仓库组件已全部改走 snb-safety / snb-danger（浅色档 #FF5C00 压纸 2.94:1 不达标）
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
        glass: 'var(--snb-shadow-glass)',
        'glass-sm': 'var(--snb-shadow-glass-sm)',
        // GlobalParts v3 底边两族（合成实边非投影，与零发光不冲突）：
        // CTA 族 --snb-edge（深色黑 45% / 浅色墨 30%）——常态 2 / 悬停抬 3 / 按下收 1；
        // 键帽族 --snb-key-side（深色黑 50% / 浅色实色暖灰 #C9C2B4）——常态 3 / 按到底 1。
        // ⚠️ 值改走变量后，深色渲染仍与 07-29 定稿逐字一致（断言挪到 tokens.test.ts）
        'edge-1': '0 1px 0 var(--snb-edge)',
        'edge-2': '0 2px 0 var(--snb-edge)',
        'edge-3': '0 3px 0 var(--snb-edge)',
        key: '0 3px 0 var(--snb-key-side)',
        'key-down': '0 1px 0 var(--snb-key-side)'
      },
      // 动效四档（GlobalParts v3）：按压/常规/开合落定/呼吸；缓动全站一把 ease-snb。
      // 组件里只许用这四档 + ease-snb，不许 duration-200/ease-out 等散档
      transitionDuration: {
        press: '55ms', quick: '120ms', settle: '420ms', breath: '2200ms'
      },
      transitionTimingFunction: {
        snb: 'cubic-bezier(0.2, 0, 0, 1)'
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
        },
        // 导航浮卡入场（GlobalParts v3 §02）：自顶栏底边滑落 + 淡入，420ms 落定档
        snbMenuIn: {
          from: { opacity: '0', transform: 'translateY(-10px)' },
          to: { opacity: '1', transform: 'translateY(0)' }
        }
        // 🪦 tubeOn 点火闪烁已删：那是灯管的行为，新体系里没有灯管
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'scale-in': 'scaleIn 0.2s ease-out',
        'fade-up': 'fadeUp 0.3s ease-out backwards',
        'snb-dot': 'snbDotPulse 2.2s ease-in-out infinite',
        // 浮卡开=420ms 落定（关是 120ms transition，在组件里写）；遮罩同步淡入
        'snb-menu': 'snbMenuIn 420ms cubic-bezier(0.2, 0, 0, 1)',
        'snb-mask': 'fadeIn 420ms cubic-bezier(0.2, 0, 0, 1)'
      }
    }
  }
}
