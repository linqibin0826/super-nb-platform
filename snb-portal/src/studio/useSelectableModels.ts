import { useEffect, useState } from 'react'
import { fetchModels } from '../lib/modelsApi'
import { isSelectableGenerationModel } from '../lib/modelFamilies'

/** 按当前选中 key 拉可选生图模型清单（已用家族表滤掉编辑/隐藏/非生图模型）。
 *  key 变化即重拉；无 key 清空。 */
export function useSelectableModels(apiKey: string | null): { models: string[]; loading: boolean } {
  const [models, setModels] = useState<string[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!apiKey) {
      setModels([])
      return
    }
    let alive = true
    setLoading(true)
    fetchModels(apiKey)
      .then((all) => {
        if (alive) setModels(all.filter(isSelectableGenerationModel))
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [apiKey])

  return { models, loading }
}
