import { describe, expect, it } from 'vitest'
import { isMpoJpeg } from '../refImage'

// 手工拼最小 JPEG 段序列（marker 走查是纯字节逻辑，无需真图）
function jpeg(...segments: number[][]): Uint8Array {
  return new Uint8Array([0xff, 0xd8, ...segments.flat()])
}

// 构造一个 APP 段：FF <marker> <len 2B> <payload>
function seg(marker: number, payload: number[]): number[] {
  const len = payload.length + 2
  return [0xff, marker, (len >> 8) & 0xff, len & 0xff, ...payload]
}

const MPF = [0x4d, 0x50, 0x46, 0x00] // "MPF\0"
const EXIF = [0x45, 0x78, 0x69, 0x66, 0x00, 0x00] // "Exif\0\0"

describe('isMpoJpeg', () => {
  it('APP2 携带 MPF 标记 → true（裸 MPO）', () => {
    expect(isMpoJpeg(jpeg(seg(0xe2, [...MPF, 1, 2, 3])))).toBe(true)
  })

  it('EXIF(APP1) 之后跟 MPF(APP2) → true（手机直出 JPEG 常态）', () => {
    expect(isMpoJpeg(jpeg(seg(0xe1, [...EXIF, 9, 9]), seg(0xe2, [...MPF])))).toBe(true)
  })

  it('普通 JPEG（APP0/JFIF + 量化表）→ false', () => {
    const jfif = [0x4a, 0x46, 0x49, 0x46, 0x00]
    expect(isMpoJpeg(jpeg(seg(0xe0, jfif), seg(0xdb, [0, 0, 0])))).toBe(false)
  })

  it('APP2 是 ICC 色彩配置而非 MPF → false', () => {
    const icc = [0x49, 0x43, 0x43, 0x5f] // "ICC_"
    expect(isMpoJpeg(jpeg(seg(0xe2, [...icc, 0, 0])))).toBe(false)
  })

  it('走到 SOS 仍未见 MPF → false（不越过图像数据乱扫）', () => {
    expect(isMpoJpeg(jpeg(seg(0xda, [0]), seg(0xe2, [...MPF])))).toBe(false)
  })

  it('非 JPEG 字节（PNG 头/空流）→ false', () => {
    expect(isMpoJpeg(new Uint8Array([0x89, 0x50, 0x4e, 0x47]))).toBe(false)
    expect(isMpoJpeg(new Uint8Array([]))).toBe(false)
  })
})
