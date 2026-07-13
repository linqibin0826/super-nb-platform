import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, cleanup } from '@testing-library/react'
import { SpecPanel } from '../SpecPanel'
import { deriveFromSize } from '../spec'
import { t } from '../../../i18n'

const base = {
  spec: deriveFromSize('1024x1024'),
  sizeText: '1024x1024',
  showEmptyState: false,
  hasUser: true,
  n: 1,
  eligible: [],
  selectedKeyId: null,
  updateSpec: vi.fn(),
  onChangeN: vi.fn(),
  onChangeKey: vi.fn(),
  onChangeModel: vi.fn(),
  onChangeSize: vi.fn(),
  selectableModels: ['gpt-image-2', 'grok-imagine-image', 'grok-imagine-image-quality'],
}

afterEach(() => cleanup())

describe('SpecPanel 模型下拉 + 家族显隐', () => {
  it('渲染模型下拉，用家族友好显示名', () => {
    render(<SpecPanel {...base} model="gpt-image-2" />)
    expect(screen.getByText('GPT Image')).toBeTruthy()
    expect(screen.getByText('Grok 快图')).toBeTruthy()
    expect(screen.getByText('Grok 高清')).toBeTruthy()
  })

  it('gpt-image：显示比例/画质档', () => {
    render(<SpecPanel {...base} model="gpt-image-2" />)
    expect(screen.getByText(t('studio.composer.ratio'))).toBeTruthy()
    expect(screen.getByText(t('studio.composer.resolution'))).toBeTruthy()
  })

  it('grok（grokPreset）：隐藏比例/画质档，显示 6 个尺寸预设（含比例）', () => {
    render(<SpecPanel {...base} model="grok-imagine-image" />)
    expect(screen.queryByText(t('studio.composer.ratio'))).toBeNull()
    expect(screen.queryByText(t('studio.composer.resolution'))).toBeNull()
    expect(screen.getByText('1:1 · 1K')).toBeTruthy()
    expect(screen.getByText('1:1 · 2K')).toBeTruthy()
    expect(screen.getByText('3:2 横')).toBeTruthy()
    expect(screen.getByText('16:9 横')).toBeTruthy()
  })
})
