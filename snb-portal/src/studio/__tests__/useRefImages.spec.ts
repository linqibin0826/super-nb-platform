import { describe, it, expect, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { useRefImages, MAX_REFS } from '../useRefImages'

// normalizeRefFile 依赖 canvas/createImageBitmap，测试里直通原文件即可（隔离归一化细节）
vi.mock('../../lib/refImage', () => ({
  normalizeRefFile: vi.fn(async (f: File) => f),
}))

function pngFile(name: string, bytes: number[] = [0x89, 0x50, 0x4e, 0x47]) {
  return new File([new Uint8Array(bytes)], name, { type: 'image/png' })
}

describe('useRefImages 加载态状态机', () => {
  it('addFiles 先落 loading 骨架，读取完成后就地变 ready', async () => {
    const { result } = renderHook(() => useRefImages())
    act(() => result.current.addFiles([pngFile('a.png')]))
    // 同步：立刻出现一枚 loading 占位（选图即有反馈）
    expect(result.current.refs).toHaveLength(1)
    expect(result.current.refs[0].status).toBe('loading')
    expect(result.current.refs[0].url).toBe('')
    expect(result.current.refs[0].file).toBeNull()
    // 异步：读取完成后就地变 ready
    await waitFor(() => expect(result.current.refs[0].status).toBe('ready'))
    expect(result.current.refs[0].url).toMatch(/^data:/)
    expect(result.current.refs[0].file).toBeInstanceOf(File)
  })

  it('过滤掉非图片文件', () => {
    const { result } = renderHook(() => useRefImages())
    const txt = new File(['x'], 'a.txt', { type: 'text/plain' })
    act(() => result.current.addFiles([txt]))
    expect(result.current.refs).toHaveLength(0)
  })

  it('总量（含加载中）截断到 MAX_REFS', async () => {
    const { result } = renderHook(() => useRefImages())
    const many = Array.from({ length: MAX_REFS + 3 }, (_, i) => pngFile(`f${i}.png`))
    act(() => result.current.addFiles(many))
    expect(result.current.refs).toHaveLength(MAX_REFS)
    await waitFor(() => expect(result.current.refs.every((r) => r.status === 'ready')).toBe(true))
  })

  it('已满时再加不超上限', async () => {
    const { result } = renderHook(() => useRefImages())
    act(() => result.current.addFiles(Array.from({ length: MAX_REFS }, (_, i) => pngFile(`f${i}.png`))))
    await waitFor(() => expect(result.current.refs.every((r) => r.status === 'ready')).toBe(true))
    act(() => result.current.addFiles([pngFile('extra.png')]))
    expect(result.current.refs).toHaveLength(MAX_REFS)
  })

  it('remove 删除指定项', async () => {
    const { result } = renderHook(() => useRefImages())
    act(() => result.current.addFiles([pngFile('a.png')]))
    await waitFor(() => expect(result.current.refs[0].status).toBe('ready'))
    const id = result.current.refs[0].id
    act(() => result.current.remove(id))
    expect(result.current.refs).toHaveLength(0)
  })

  it('读取成功记入 recentUploads，同名同大小去重', async () => {
    const { result } = renderHook(() => useRefImages())
    act(() => result.current.addFiles([pngFile('a.png')]))
    await waitFor(() => expect(result.current.recentUploads).toHaveLength(1))
    act(() => result.current.addFiles([pngFile('a.png')]))
    await waitFor(() => expect(result.current.refs).toHaveLength(2))
    expect(result.current.recentUploads).toHaveLength(1) // 同名同大小不重复入列
  })

  it('归一化失败时撤掉骨架', async () => {
    const { normalizeRefFile } = await import('../../lib/refImage')
    ;(normalizeRefFile as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new Error('boom'))
    const { result } = renderHook(() => useRefImages())
    act(() => result.current.addFiles([pngFile('bad.png')]))
    expect(result.current.refs).toHaveLength(1) // 先有骨架
    await waitFor(() => expect(result.current.refs).toHaveLength(0)) // 失败后撤除
  })
})
