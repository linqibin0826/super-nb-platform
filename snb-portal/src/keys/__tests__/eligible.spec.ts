import { describe, expect, it } from 'vitest'
import { filterEligibleKeys } from '../eligible'
import type { StudioApiKey, StudioGroup } from '../../types'

const g = (over: Partial<StudioGroup>): StudioGroup =>
  ({ id: 1, name: 'G', platform: 'openai', allow_image_generation: true, ...over }) as StudioGroup
const k = (over: Partial<StudioApiKey>): StudioApiKey =>
  ({ id: 1, name: 'K', key: 'sk-x', status: 'active', group_id: 1, ...over })

describe('filterEligibleKeys', () => {
  it('active+openai+开生图 才入选', () => {
    const groups = [g({}), g({ id: 2, platform: 'anthropic' }), g({ id: 3, allow_image_generation: false })]
    const keys = [
      k({}),
      k({ id: 2, group_id: 2 }),
      k({ id: 3, group_id: 3 }),
      k({ id: 4, status: 'disabled' }),
    ]
    expect(filterEligibleKeys(keys, groups).map((e) => e.key.id)).toEqual([1])
  })
  it('available 分组缺失时回退 key 内嵌 group', () => {
    const keys = [k({ group_id: 99, group: g({ id: 99 }) })]
    expect(filterEligibleKeys(keys, [])).toHaveLength(1)
  })
  it('无分组信息的 key 被剔除', () => {
    expect(filterEligibleKeys([k({ group_id: null, group: null })], [])).toHaveLength(0)
  })
})
