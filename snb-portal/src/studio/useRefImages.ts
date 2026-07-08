import { useCallback, useState } from 'react'
import { normalizeRefFile } from '../lib/refImage'

/** 参考图：选图即以 loading 骨架占位落位，归一化+读取完成后就地替换为 ready。
 *  url 是 FileReader 的 data:URL（线上 CSP img-src 无 blob:，禁用 createObjectURL）。 */
export interface RefImage {
  id: string
  status: 'loading' | 'ready'
  /** ready 时是 data URL；loading 时为空串 */
  url: string
  /** ready 时是归一化后的 File；loading 时为 null */
  file: File | null
  /** 文件名（loading 态也已知，用于去重与无障碍标签） */
  name: string
}

export const MAX_REFS = 10

/** 参考图集合的状态机：当前已选 refs（含加载中骨架）+ 会话内「最近上传」留存。
 *  抽成 hook 便于单测加载态流转（选图即 loading → 完成 ready / 失败撤除 / 截断到上限）。 */
export function useRefImages() {
  const [refs, setRefs] = useState<RefImage[]>([])
  // 本次会话上传过的参考图（含已从当前选择移除的），供选择器「最近上传」一键复用
  const [recentUploads, setRecentUploads] = useState<RefImage[]>([])

  /** 只收 image/*；总量（含加载中）截断到 MAX_REFS。
   *  入队前过 normalizeRefFile：手机直出的 MPO 味 JPEG / gif 等上游不收的格式就地重编码。
   *  useCallback 挂 refs.length：既让粘贴监听拿到稳定引用，又保证「剩余名额」始终是最新值。 */
  const addFiles = useCallback((files: FileList | File[]): void => {
    const images = Array.from(files).filter((f) => f.type.startsWith('image/'))
    const room = Math.max(0, MAX_REFS - refs.length)
    const accepted = images.slice(0, room)
    if (accepted.length === 0) return

    // 先落骨架占位：选图当下就有明确反馈，不再干等 normalize+读取那段黑箱
    const placeholders: RefImage[] = accepted.map((file) => ({
      id: crypto.randomUUID(),
      status: 'loading',
      url: '',
      file: null,
      name: file.name,
    }))
    setRefs((prev) => [...prev, ...placeholders])

    accepted.forEach((file, i) => {
      const id = placeholders[i].id
      const drop = () => setRefs((cur) => cur.filter((r) => r.id !== id))
      void normalizeRefFile(file)
        .then(
          (safe) =>
            new Promise<void>((resolve, reject) => {
              const reader = new FileReader()
              reader.onload = () => {
                const url = reader.result
                if (typeof url !== 'string') return reject(new Error('read failed'))
                // 就地把这枚骨架替换为真图（位次不变，其余占位/真图不受影响）
                setRefs((cur) =>
                  cur.map((r) => (r.id === id ? { id, status: 'ready' as const, url, file: safe, name: safe.name } : r))
                )
                // 记入会话「最近上传」：按 名字:大小 去重（从选择器复用同一张不重复入列），最多留 12 张
                setRecentUploads((prev) => {
                  const key = `${safe.name}:${safe.size}`
                  if (prev.some((r) => `${r.name}:${r.file?.size}` === key)) return prev
                  return [
                    { id: crypto.randomUUID(), status: 'ready' as const, url, file: safe, name: safe.name },
                    ...prev,
                  ].slice(0, 12)
                })
                resolve()
              }
              reader.onerror = () => reject(new Error('read error'))
              reader.readAsDataURL(safe)
            })
        )
        .catch(drop) // 归一化/读取失败：撤掉骨架（维持修复前「失败即无此图」的行为）
    })
  }, [refs.length])

  const remove = useCallback((id: string): void => {
    setRefs((prev) => prev.filter((r) => r.id !== id))
  }, [])

  return { refs, recentUploads, addFiles, remove }
}
