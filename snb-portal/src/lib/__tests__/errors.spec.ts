import { describe, expect, it } from 'vitest'
import { mapImagesError } from '../errors'

describe('mapImagesError', () => {
  it.each([
    ['timeout', 'timeout'],
    ['network', 'network'],
  ] as const)('传输层 %s → %s', (status, key) => {
    expect(mapImagesError(status, '').key).toBe(key)
  })

  it.each([
    [401, 'unauthorized'],
    [402, 'insufficientBalance'],
    [403, 'permissionDenied'],
    [429, 'rateLimited'],
    [500, 'upstream'],
    [503, 'upstream'],
    [418, 'unknown'],
  ])('HTTP %d → %s', (status, key) => {
    expect(mapImagesError(status, {}).key).toBe(key)
  })

  it('400 + 审核类文案 → contentPolicy', () => {
    const body = { error: { message: 'Your request was rejected by content policy.' } }
    expect(mapImagesError(400, body).key).toBe('contentPolicy')
  })

  it('Adobe 系上游 image_unsafe(生产实锤原文,sub2api 以 400 透出)→ contentPolicy', () => {
    const body = {
      error: {
        message:
          'adobe content rejected: status 451 {"error_code":"image_unsafe","message":"The generated images appear to be unsafe. Try modifying the prompts or the seeds."}',
      },
    }
    expect(mapImagesError(400, body).key).toBe('contentPolicy')
  })

  it('HTTP 451 即使无关键词也 → contentPolicy(451=因法律/政策不可用)', () => {
    expect(mapImagesError(451, { error: { message: 'blocked' } }).key).toBe('contentPolicy')
  })

  it('非 400 的 4xx 带审核类文案也 → contentPolicy(状态码各家不统一)', () => {
    expect(mapImagesError(403, { error: { message: 'content rejected by moderation' } }).key).toBe(
      'contentPolicy'
    )
  })

  it('400 + size 文案 → invalidSize', () => {
    expect(mapImagesError(400, { error: { message: 'invalid size 3840x2160' } }).key).toBe('invalidSize')
  })

  it('400 + invalid_size 文案 → invalidSize', () => {
    expect(mapImagesError(400, { error: { message: 'invalid_size parameter' } }).key).toBe('invalidSize')
  })

  it('400 + resize 文案不误判 → badRequest', () => {
    expect(mapImagesError(400, { error: { message: 'failed to resize buffer' } }).key).toBe('badRequest')
  })

  it('其余 400 → badRequest', () => {
    expect(mapImagesError(400, { error: { message: 'prompt is required' } }).key).toBe('badRequest')
  })

  it('4xx + 余额类文案覆盖状态码 → insufficientBalance', () => {
    const body = { error: { message: 'insufficient balance, please recharge' } }
    expect(mapImagesError(403, body).key).toBe('insufficientBalance')
  })

  it('detail 提取：error.message / error 字符串 / 原始字符串 / 序列化兜底', () => {
    expect(mapImagesError(500, { error: { message: 'boom' } }).detail).toBe('boom')
    expect(mapImagesError(500, { error: 'plain' }).detail).toBe('plain')
    expect(mapImagesError(500, 'raw text').detail).toBe('raw text')
    expect(mapImagesError(500, { foo: 1 }).detail).toBe('{"foo":1}')
  })
})
