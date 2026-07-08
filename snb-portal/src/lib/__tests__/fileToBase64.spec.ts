import { describe, expect, it } from 'vitest'
import { filesToRefB64 } from '../fileToBase64'

describe('filesToRefB64', () => {
  it('把 File[] 转成 { b64, contentType }[]，b64 不含 data: 前缀', async () => {
    const file = new File([new Uint8Array([1, 2, 3])], 'ref.png', { type: 'image/png' })
    const out = await filesToRefB64([file])
    expect(out).toHaveLength(1)
    expect(out[0].contentType).toBe('image/png')
    // atob(b64) 应还原 3 字节
    expect(atob(out[0].b64).length).toBe(3)
    expect(out[0].b64).not.toContain(',') // 已剥离 "data:...;base64," 头
  })

  it('空数组直接返回空', async () => {
    expect(await filesToRefB64([])).toEqual([])
  })
})
