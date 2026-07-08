// 拷自 sub2api fork feat/image-playground@4f873b56 frontend/src/features/playground/lib/ —— 框架无关，与 fork 同步演进须手动
// 把网关/上游的错误归一成 i18n key（playground.errors.*）+ 可展开的原始 detail。
export interface MappedImagesError {
  key: string
  detail: string
}

export function mapImagesError(
  status: number | 'timeout' | 'network',
  body: unknown
): MappedImagesError {
  const detail = extractDetail(body)
  if (status === 'timeout') return { key: 'timeout', detail }
  if (status === 'network') return { key: 'network', detail }

  const lower = detail.toLowerCase()
  // 余额类文案优先于状态码：各家网关余额不足的状态码不统一（402/403 都见过）
  if (status >= 400 && status < 500 && /insufficient|余额不足|balance/.test(lower)) {
    return { key: 'insufficientBalance', detail }
  }
  // 内容安全拦截同理按文案识别、不依赖状态码：OpenAI 系 400 moderation/content policy，
  // Adobe 系上游 451 image_unsafe/content rejected（sub2api 包一层后以 400 透出）——
  // 漏识别会兜进 badRequest「参数有误」，用户误以为是本站问题（2026-07-07 生产实锤 27 例）
  if (
    (status >= 400 && status < 500 &&
      /moderation|content[ _]policy|content[ _]rejected|safety|unsafe|涉敏/.test(lower)) ||
    status === 451
  ) {
    return { key: 'contentPolicy', detail }
  }
  if (status === 401) return { key: 'unauthorized', detail }
  if (status === 402) return { key: 'insufficientBalance', detail }
  if (status === 403) return { key: 'permissionDenied', detail }
  if (status === 429) return { key: 'rateLimited', detail }
  if (status === 400) {
    if (/(^|[^a-z])size([^a-z]|$)/.test(lower)) return { key: 'invalidSize', detail }
    return { key: 'badRequest', detail }
  }
  if (status >= 500) return { key: 'upstream', detail }
  return { key: 'unknown', detail }
}

function extractDetail(body: unknown): string {
  if (typeof body === 'string') return body
  if (body && typeof body === 'object') {
    const err = (body as Record<string, unknown>).error
    if (err && typeof err === 'object') {
      const message = (err as Record<string, unknown>).message
      if (typeof message === 'string') return message
    }
    if (typeof err === 'string') return err
    try {
      return JSON.stringify(body)
    } catch {
      return String(body)
    }
  }
  return ''
}
