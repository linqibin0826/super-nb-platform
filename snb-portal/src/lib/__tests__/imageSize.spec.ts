import { describe, expect, it } from 'vitest'
import { pngDimensions } from '../imageSize'

/** 造一个只含签名 + IHDR 头的 PNG base64（前 24 字节即够解尺寸） */
function pngHeadB64(w: number, h: number): string {
  const be = (n: number) => [(n >>> 24) & 255, (n >>> 16) & 255, (n >>> 8) & 255, n & 255]
  const bytes = [
    0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, // 签名
    0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52, // IHDR 长度 + "IHDR"
    ...be(w),
    ...be(h),
  ]
  return btoa(String.fromCharCode(...bytes))
}

describe('pngDimensions', () => {
  it('从 PNG 头解出宽高（3:2 横版）', () => {
    expect(pngDimensions(pngHeadB64(1536, 1024))).toEqual({ width: 1536, height: 1024 })
  })
  it('大尺寸不溢出（超 16 位）', () => {
    expect(pngDimensions(pngHeadB64(3840, 2160))).toEqual({ width: 3840, height: 2160 })
  })
  it('长 base64 只取头部也能解（真实图片有后续数据）', () => {
    expect(pngDimensions(pngHeadB64(1024, 1536) + 'AAAABBBBCCCCDDDD')).toEqual({ width: 1024, height: 1536 })
  })
  it('非 PNG 返回 null', () => {
    expect(pngDimensions(btoa('not a png at all!!!!!!!!!!'))).toBeNull()
  })
  it('空/垃圾返回 null 不抛', () => {
    expect(pngDimensions('')).toBeNull()
    expect(pngDimensions('@@@')).toBeNull()
  })
})
