import { useEffect, useRef, useState } from 'react'

/**
 * 机制舞台（bespoke，仅 codex 书 §05）：AGENTS.md 发现链随滚动进入视口逐级点亮。
 * 评审定调——为这本书单独定制，不是每本电子书自动继承的通用功能。
 */
export function AgentsDiscoveryStage() {
  const ref = useRef<HTMLDivElement>(null)
  const [play, setPlay] = useState(false)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    const io = new IntersectionObserver(
      (ents) => {
        if (ents.some((e) => e.isIntersecting)) {
          setPlay(true)
          io.disconnect()
        }
      },
      { threshold: 0.4 },
    )
    io.observe(el)
    return () => io.disconnect()
  }, [])

  return (
    <figure ref={ref} className={`hub-stage${play ? ' is-play' : ''}`} data-testid="hub-book-stage">
      <figcaption className="hub-stage-top">
        <span className="t">Codex 如何找到你的 AGENTS.md</span>
        <span className="tag">滚动播放</span>
      </figcaption>
      <div className="hub-tree">
        <div className="lvl">
          <span className="cap">① 全局级</span>
          <div><code>~/.codex/</code> <span className="hit">AGENTS.override.md</span> → AGENTS.md</div>
        </div>
        <div className="lvl">
          <span className="cap">② 项目级</span>
          <div><code>project-root/</code> <span className="hit">AGENTS.md</span></div>
          <div className="indent"><code>src/</code> <span className="hit">AGENTS.md</span> ← 越近越优先</div>
        </div>
        <div className="lvl">
          <span className="cap">③ 合并成一条指令链</span>
          <div>从根到当前目录顺序拼接，后出现的<span className="hit">覆盖</span>前面的</div>
        </div>
      </div>
      <div className="hub-stage-foot">合并后总大小上限默认 32KB（<code>project_doc_max_bytes</code>）；超出后面的文件被跳过。</div>
    </figure>
  )
}
