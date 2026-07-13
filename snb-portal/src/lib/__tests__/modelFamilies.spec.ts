import { describe, expect, it } from 'vitest'
import {
  roleOf,
  isSelectableGenerationModel,
  displayName,
  editModelFor,
  sizeModeOf,
  hasQualityAxis,
  normalizeGrokSize,
} from '../modelFamilies'

describe('modelFamilies', () => {
  it('gpt-image 族：generation 角色、edit 用自身、尺寸自由、有画质轴', () => {
    expect(roleOf('gpt-image-2')).toBe('generation')
    expect(isSelectableGenerationModel('gpt-image-2')).toBe(true)
    expect(editModelFor('gpt-image-2')).toBe('gpt-image-2')
    expect(sizeModeOf('gpt-image-2')).toBe('free')
    expect(hasQualityAxis('gpt-image-2')).toBe(true)
    expect(displayName('gpt-image-2')).toBe('GPT Image')
  })

  it('grok 生图：快图/高清进下拉、edit 统一切 grok-imagine-edit、固定 1024、无画质轴', () => {
    expect(roleOf('grok-imagine-image')).toBe('generation')
    expect(roleOf('grok-imagine-image-quality')).toBe('generation')
    expect(displayName('grok-imagine-image')).toBe('Grok 快图')
    expect(displayName('grok-imagine-image-quality')).toBe('Grok 高清')
    expect(editModelFor('grok-imagine-image')).toBe('grok-imagine-edit')
    expect(editModelFor('grok-imagine-image-quality')).toBe('grok-imagine-edit')
    expect(sizeModeOf('grok-imagine-image')).toBe('grokSquare')
    expect(hasQualityAxis('grok-imagine-image')).toBe(false)
  })

  it('grok-imagine-edit 是编辑角色、不进下拉', () => {
    expect(roleOf('grok-imagine-edit')).toBe('edit')
    expect(isSelectableGenerationModel('grok-imagine-edit')).toBe(false)
  })

  it('grok 裸名 / 视频 → hidden，不进下拉', () => {
    expect(roleOf('grok-imagine')).toBe('hidden')
    expect(roleOf('grok-imagine-video')).toBe('hidden')
    expect(isSelectableGenerationModel('grok-imagine')).toBe(false)
  })

  it('非生图家族（gpt-5.x 文本）→ hidden、editModelFor=null、默认能力兜底', () => {
    expect(roleOf('gpt-5.5')).toBe('hidden')
    expect(isSelectableGenerationModel('gpt-5.5')).toBe(false)
    expect(editModelFor('gpt-5.5')).toBeNull()
    expect(sizeModeOf('gpt-5.5')).toBe('free')
    expect(hasQualityAxis('gpt-5.5')).toBe(true)
    expect(displayName('gpt-5.5')).toBe('gpt-5.5')
  })

  it('normalizeGrokSize：命中方形档保留，其余归 2K', () => {
    expect(normalizeGrokSize('1024x1024')).toBe('1024x1024')
    expect(normalizeGrokSize('2048x2048')).toBe('2048x2048')
    expect(normalizeGrokSize('1152x2048')).toBe('2048x2048')
    expect(normalizeGrokSize('3840x2160')).toBe('2048x2048')
  })
})
