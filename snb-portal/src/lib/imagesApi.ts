// 拷自 sub2api fork feat/image-playground@4f873b56 frontend/src/features/playground/lib/ —— 框架无关，与 fork 同步演进须手动
// 直接持用户站内 key 调网关（不是控制台 apiClient 的 JWT 体系）。
// 只用 data: URL（线上 CSP img-src 含 data: 不含 blob:）。
import { mapImagesError, type MappedImagesError } from './errors'

export const IMAGES_MODEL = 'gpt-image-2'
const DEFAULT_TIMEOUT_MS = 300_000

export interface GenerateImagesParams {
  apiKey: string
  prompt: string
  /** 'auto' 会原样传给后端 */
  size: string
  n: number
  quality: 'low' | 'medium' | 'high' | 'auto'
  /** 参考图（图生图）。非空走 /v1/images/edits multipart；空/缺省走 /v1/images/generations JSON 老路径 */
  images?: File[]
  /** 外部取消（如「取消」按钮）；与内部超时任一触发都中止请求 */
  signal?: AbortSignal
  timeoutMs?: number
}

export interface GeneratedImage {
  b64: string
  /** 可直接绑 <img :src> */
  dataUrl: string
}

export class ImagesApiError extends Error {
  readonly mapped: MappedImagesError

  constructor(mapped: MappedImagesError) {
    super(mapped.detail || mapped.key)
    this.name = 'ImagesApiError'
    this.mapped = mapped
  }
}

// 图生图 /v1/images/edits 后端结论（sub2api 私有 fork，2026-07-04 核对）：
// - 路由：backend/internal/server/routes/gateway.go:118 在 /v1 组注册 POST /images/edits → OpenAIGateway.Images
//   （gateway.go:200 另有根级 /images/edits 别名，不用）。
// - 图片字段名：backend/internal/service/openai_images.go:356 —— 文件 part 接受 `image` 或任意 `image[` 前缀
//   （即 `image[]` 也命中），可重复出现，多图 = 多个同名 part；`mask`（:343）可选，本站不用。
// - 标量字段：model(:372) / prompt(:375) / size(:377) / n(:388，须正整数字符串) / quality(:394)
//   全部按 multipart 表单值读取，与 JSON 路径字段同名。
// - 上限：无显式张数上限（仅要求 ≥1 张，openai_images.go:433）；单 part 上限 20MB（:41 openAIImageMaxUploadPartSize）。
// - 转发方式：网关把原始 multipart body 透传上游，只按渠道映射重写 model 字段
//   （openai_images.go:787 rewriteOpenAIImagesMultipartModel 重组时其余 part 原样保留），
//   所以字段名会原样到达上游——选 `image[]`（OpenAI SDK 多图惯例，且命中后端 `image[` 前缀判断）。
function buildRequest(params: GenerateImagesParams, signal: AbortSignal): { url: string; init: RequestInit } {
  if (params.images && params.images.length > 0) {
    const form = new FormData()
    form.append('model', IMAGES_MODEL)
    form.append('prompt', params.prompt)
    form.append('n', String(params.n))
    form.append('size', params.size)
    form.append('quality', params.quality)
    for (const file of params.images) {
      form.append('image[]', file, file.name)
    }
    return {
      url: '/v1/images/edits',
      init: {
        method: 'POST',
        // 不手写 Content-Type：浏览器自动带 multipart boundary
        headers: { Authorization: `Bearer ${params.apiKey}` },
        body: form,
        signal,
      },
    }
  }
  // 文生图老路径：行为逐字不变
  return {
    url: '/v1/images/generations',
    init: {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${params.apiKey}`,
      },
      body: JSON.stringify({
        model: IMAGES_MODEL,
        prompt: params.prompt,
        n: params.n,
        size: params.size,
        quality: params.quality,
      }),
      signal,
    },
  }
}

export async function generateImages(params: GenerateImagesParams): Promise<GeneratedImage[]> {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), params.timeoutMs ?? DEFAULT_TIMEOUT_MS)
  if (params.signal) {
    if (params.signal.aborted) {
      clearTimeout(timeout)
      throw new ImagesApiError(mapImagesError('timeout', ''))
    }
    params.signal.addEventListener('abort', () => controller.abort(), { once: true })
  }

  const { url, init } = buildRequest(params, controller.signal)

  let response: Response
  try {
    response = await fetch(url, init)
  } catch (err) {
    clearTimeout(timeout)
    if ((err as any)?.name === 'AbortError') {
      throw new ImagesApiError(mapImagesError('timeout', ''))
    }
    throw new ImagesApiError(mapImagesError('network', err instanceof Error ? err.message : ''))
  }
  clearTimeout(timeout)

  const text = await response.text()
  let body: unknown = text
  try {
    body = JSON.parse(text)
  } catch {
    // 保留原始文本作为 detail
  }

  if (!response.ok) {
    throw new ImagesApiError(mapImagesError(response.status, body))
  }

  const data = (body as { data?: Array<{ b64_json?: string }> }).data ?? []
  const images = data
    .filter((item): item is { b64_json: string } => typeof item.b64_json === 'string' && item.b64_json.length > 0)
    .map((item) => ({ b64: item.b64_json, dataUrl: `data:image/png;base64,${item.b64_json}` }))

  if (images.length === 0) {
    throw new ImagesApiError(mapImagesError(response.status, body))
  }
  return images
}
