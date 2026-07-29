import { useLayoutEffect, type RefObject } from 'react'
import { th } from './hubMessages'

/**
 * 代码块装配器（2026-07-29 杂志架改版）：给 .hub-prose 里的每个 <pre> 套上
 * 「文件名条 + 复制按钮」的机箱壳（视觉规格照定稿：条高 44 / 按钮视觉 28、热区 44 /
 * 已复制态纸白填充 1.4s 回落 / 长代码不折行横向滚动并标注）。
 *
 * 正文是管线预渲染 HTML（dangerouslySetInnerHTML），React 不管理其内部节点，
 * 这里做一次性 DOM 装配：幂等（data-codeblock 标记防 StrictMode 双跑），
 * html 变更时 React 重设 innerHTML、壳随旧节点一起消亡，效果里重新装配。
 *
 * 🚨 调用方铁律：`dangerouslySetInnerHTML` 的那个 `{ __html }` 对象**必须 useMemo 稳住身份**。
 * React 19 的 updateProperties 按「prop 值身份变了没」决定要不要重设，
 * 命中后 setProp 无条件 `domElement.innerHTML = __html`（react-dom-client 20407 行附近，
 * 不比字符串值）——每渲染新建一次字面量对象，就等于每次重渲染都把这里装配好的机箱壳
 * 连同 <pre> 一起冲掉，效果又不重跑（deps 没变），最后什么都不剩。React 18 比的是字符串值，
 * 同样的写法没事，升到 19 才炸；2026-07-29 落地时踩过一次。
 *
 * 语法高亮分档只做外观层（hub.css 四档亮度映射 hljs-* / prism token.*），
 * 真正的着色 token 由内容管线（super-nb-hub-content 构建侧）生成。
 */
export function useCodeBlocks(ref: RefObject<HTMLElement | null>, deps: unknown[]): void {
  // useLayoutEffect 而非 useEffect：装配是纯 DOM 改写，放到 paint 之后会先闪一帧裸 <pre>；
  // 顺带 scrollWidth 量溢出本来就要同步布局
  useLayoutEffect(() => {
    const root = ref.current
    if (!root) return
    const pres = Array.from(root.querySelectorAll('pre'))
    for (const pre of pres) {
      if (pre.closest('[data-codeblock]')) continue // 幂等：已装配过
      const parent = pre.parentNode
      if (!parent) continue

      const figure = document.createElement('figure')
      figure.className = 'hub-code'
      figure.setAttribute('data-codeblock', '')

      const bar = document.createElement('figcaption')
      bar.className = 'hub-code-bar'

      const label = document.createElement('span')
      label.className = 'hub-code-lang'
      label.textContent = langOf(pre) ?? th('art.codeFallback')
      const hint = document.createElement('span')
      hint.className = 'hub-code-hint'
      hint.textContent = ` · ${th('art.scrollHint')}`
      label.appendChild(hint)

      const button = document.createElement('button')
      button.type = 'button'
      button.className = 'hub-code-copy'
      button.setAttribute('aria-label', th('art.copyAria'))
      const face = document.createElement('span')
      face.textContent = th('art.copy')
      button.appendChild(face)

      let timer: ReturnType<typeof setTimeout> | undefined
      button.addEventListener('click', () => {
        // innerText 保行；jsdom 没实现它，退 textContent 兜底
        const text = pre.innerText || pre.textContent || ''
        if (navigator.clipboard?.writeText) navigator.clipboard.writeText(text).catch(() => {})
        button.classList.add('is-copied')
        face.textContent = th('art.copied')
        clearTimeout(timer)
        timer = setTimeout(() => {
          button.classList.remove('is-copied')
          face.textContent = th('art.copy')
        }, 1400)
      })

      bar.appendChild(label)
      bar.appendChild(button)
      parent.insertBefore(figure, pre)
      figure.appendChild(bar)
      figure.appendChild(pre)

      // 溢出才亮「可横向滚动」（装配时测一次；窗口缩放的边缘情况不追）
      if (pre.scrollWidth > pre.clientWidth + 2) figure.setAttribute('data-overflow', '')
    }
    // 壳与 <pre> 同生共死（innerHTML 重设即整体消亡），无需显式清理
    // eslint-disable-next-line react-hooks/exhaustive-deps -- 依赖由调用方按正文内容传入
  }, deps)
}

/** 从 <pre><code class="language-xxx"> 或 <pre class="language-xxx"> 提语言名。 */
function langOf(pre: HTMLPreElement): string | null {
  const cls = `${pre.className} ${pre.querySelector('code')?.className ?? ''}`
  const m = cls.match(/language-([a-z0-9+#-]+)/i)
  return m ? m[1] : null
}
