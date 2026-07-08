import { afterEach, describe, expect, it, vi } from 'vitest'
import { generateImages, ImagesApiError, IMAGES_MODEL } from '../imagesApi'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('generateImages', () => {
  it('成功：锁定模型、带 Bearer、b64 转 dataUrl', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(200, { data: [{ b64_json: 'QUJD' }, { b64_json: 'REVG' }] })
    )
    vi.stubGlobal('fetch', fetchMock)

    const images = await generateImages({
      apiKey: 'sk-test',
      prompt: 'a cat',
      size: '1024x1024',
      n: 2,
      quality: 'medium',
    })

    expect(images).toHaveLength(2)
    expect(images[0]).toEqual({ b64: 'QUJD', dataUrl: 'data:image/png;base64,QUJD' })

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/v1/images/generations')
    expect(init.headers.Authorization).toBe('Bearer sk-test')
    const body = JSON.parse(init.body)
    expect(body).toEqual({
      model: IMAGES_MODEL,
      prompt: 'a cat',
      n: 2,
      size: '1024x1024',
      quality: 'medium',
    })
  })

  it('HTTP 403 → ImagesApiError 且 mapped.key=permissionDenied', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse(403, { error: { message: 'no image permission' } }))
    )
    const err = await generateImages({
      apiKey: 'sk',
      prompt: 'x',
      size: 'auto',
      n: 1,
      quality: 'auto',
    }).catch((e) => e)
    expect(err).toBeInstanceOf(ImagesApiError)
    expect(err.mapped.key).toBe('permissionDenied')
    expect(err.mapped.detail).toBe('no image permission')
  })

  it('200 但没有任何 b64 数据 → 报错（unknown）', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(200, { data: [] })))
    const err = await generateImages({
      apiKey: 'sk',
      prompt: 'x',
      size: '1024x1024',
      n: 1,
      quality: 'medium',
    }).catch((e) => e)
    expect(err).toBeInstanceOf(ImagesApiError)
  })

  it('fetch 抛 AbortError → timeout；抛其他 → network', async () => {
    const abortErr = new DOMException('aborted', 'AbortError')
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(abortErr))
    const e1 = await generateImages({
      apiKey: 'sk',
      prompt: 'x',
      size: '1024x1024',
      n: 1,
      quality: 'medium',
    }).catch((e) => e)
    expect(e1.mapped.key).toBe('timeout')

    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    const e2 = await generateImages({
      apiKey: 'sk',
      prompt: 'x',
      size: '1024x1024',
      n: 1,
      quality: 'medium',
    }).catch((e) => e)
    expect(e2.mapped.key).toBe('network')
  })

  it('带参考图 → POST /v1/images/edits multipart：image[] 逐张、标量字段齐全、不手写 Content-Type', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, { data: [{ b64_json: 'QUJD' }] }))
    vi.stubGlobal('fetch', fetchMock)

    const fileA = new File(['aaa'], 'ref-a.png', { type: 'image/png' })
    const fileB = new File(['bbb'], 'ref-b.jpg', { type: 'image/jpeg' })
    const images = await generateImages({
      apiKey: 'sk-edit',
      prompt: 'make it orange',
      size: '1024x1024',
      n: 2,
      quality: 'high',
      images: [fileA, fileB],
    })

    expect(images).toEqual([{ b64: 'QUJD', dataUrl: 'data:image/png;base64,QUJD' }])

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/v1/images/edits')
    expect(init.method).toBe('POST')
    expect(init.headers.Authorization).toBe('Bearer sk-edit')
    // 不能手写 Content-Type，boundary 交给浏览器
    expect(init.headers['Content-Type']).toBeUndefined()

    const form = init.body as FormData
    expect(form).toBeInstanceOf(FormData)
    const files = form.getAll('image[]') as File[]
    expect(files).toHaveLength(2)
    expect(files.map((f) => f.name)).toEqual(['ref-a.png', 'ref-b.jpg'])
    expect(form.get('model')).toBe(IMAGES_MODEL)
    expect(form.get('prompt')).toBe('make it orange')
    expect(form.get('n')).toBe('2')
    expect(form.get('size')).toBe('1024x1024')
    expect(form.get('quality')).toBe('high')
  })

  it('images 为空数组 → 仍走 /v1/images/generations JSON 老路径（防回归）', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, { data: [{ b64_json: 'QUJD' }] }))
    vi.stubGlobal('fetch', fetchMock)

    await generateImages({
      apiKey: 'sk-test',
      prompt: 'a cat',
      size: 'auto',
      n: 1,
      quality: 'medium',
      images: [],
    })

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/v1/images/generations')
    expect(init.headers['Content-Type']).toBe('application/json')
    expect(JSON.parse(init.body)).toEqual({
      model: IMAGES_MODEL,
      prompt: 'a cat',
      n: 1,
      size: 'auto',
      quality: 'medium',
    })
  })

  it('外部 signal 已 abort → 直接 timeout 错误，不发请求', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const controller = new AbortController()
    controller.abort()
    const err = await generateImages({
      apiKey: 'sk',
      prompt: 'x',
      size: '1024x1024',
      n: 1,
      quality: 'medium',
      signal: controller.signal,
    }).catch((e) => e)
    expect(err).toBeInstanceOf(ImagesApiError)
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
