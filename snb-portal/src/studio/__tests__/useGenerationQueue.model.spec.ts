import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { useGenerationQueue, type GenerateInput } from '../useGenerationQueue'

// 只验证 model 透传：mock imagesApi 与历史落库
vi.mock('../../lib/imagesApi', () => ({
  generateImages: vi.fn().mockResolvedValue([{ b64: 'QUJD', dataUrl: 'data:image/png;base64,QUJD' }]),
  ImagesApiError: class extends Error {},
}))
vi.mock('../../lib/generationsApi', () => ({ createGeneration: vi.fn().mockResolvedValue(undefined) }))
vi.mock('../../lib/fileToBase64', () => ({ filesToRefB64: vi.fn().mockResolvedValue([]) }))

import { generateImages } from '../../lib/imagesApi'

afterEach(() => vi.clearAllMocks())

const baseInput: GenerateInput = {
  apiKey: 'sk',
  keyId: 1,
  groupName: 'g',
  model: 'grok-imagine-image',
  prompt: 'a cat',
  size: '1024x1024',
  n: 1,
  quality: 'auto',
}

describe('useGenerationQueue model 透传', () => {
  it('enqueue 的 model 传到 generateImages', async () => {
    const { result } = renderHook(() => useGenerationQueue())
    act(() => result.current.enqueue(baseInput))
    await waitFor(() =>
      expect(generateImages).toHaveBeenCalledWith(expect.objectContaining({ model: 'grok-imagine-image' }))
    )
  })
})
