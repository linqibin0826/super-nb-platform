import type { EligibleKey, StudioApiKey, StudioGroup } from '../types'

// 判定逻辑对齐 fork useEligibleKeys.ts：分组优先 /groups/available 完整版，key 内嵌兜底
export function filterEligibleKeys(keys: StudioApiKey[], groups: StudioGroup[]): EligibleKey[] {
  const byId = new Map(groups.map((group) => [group.id, group]))
  return keys
    .map((key) => {
      const group = (key.group_id !== null ? byId.get(key.group_id) : undefined) ?? key.group ?? null
      return group ? { key, group } : null
    })
    .filter(
      (entry): entry is EligibleKey =>
        entry !== null &&
        entry.key.status === 'active' &&
        entry.group.platform === 'openai' &&
        entry.group.allow_image_generation
    )
}
