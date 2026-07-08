import type { ImageCostGroup } from './lib/cost'

export interface StudioGroup extends ImageCostGroup {
  id: number
  name: string
  platform: string
  allow_image_generation: boolean
}

export interface StudioApiKey {
  id: number
  name: string
  key: string
  status: string
  group_id: number | null
  group?: StudioGroup | null
}

export interface EligibleKey {
  key: StudioApiKey
  group: StudioGroup
}
