// 参考图归一化：上游（OpenAI images）只收 png/jpeg/webp，且按字节嗅探真实格式。
// 手机直出的 JPEG 常带 APP2「MPF」多帧标记（三星/小米/索尼景深、连拍），浏览器当普通
// image/jpeg 渲染没问题，但上游会认成 MPO 拒收（"Unsupported image format: mpo"）。
// 入队时：JPEG 嗅 MPF → 命中用 canvas 重编码成干净 JPEG（顺带剥 EXIF）；
// png/webp/干净 jpeg 原样直传；其余 image/*（gif/bmp/tiff…）一律转 PNG。

/** 嗅探 JPEG 字节流里的 APP2「MPF\0」段（MPO 特征）。非 JPEG / 无标记返回 false。
 *  只需要走到 SOS（图像数据开始）之前——APP 段都在头部。 */
export function isMpoJpeg(bytes: Uint8Array): boolean {
  if (bytes.length < 4 || bytes[0] !== 0xff || bytes[1] !== 0xd8) return false
  let offset = 2
  while (offset + 4 <= bytes.length) {
    if (bytes[offset] !== 0xff) return false
    const marker = bytes[offset + 1]
    // SOS（FFDA）之后是压缩数据；EOI（FFD9）结束——都不会再有 APP 段
    if (marker === 0xda || marker === 0xd9) return false
    const length = (bytes[offset + 2] << 8) | bytes[offset + 3]
    if (length < 2) return false
    if (marker === 0xe2 && length >= 6) {
      const p = offset + 4
      if (
        p + 4 <= bytes.length &&
        bytes[p] === 0x4d && // M
        bytes[p + 1] === 0x50 && // P
        bytes[p + 2] === 0x46 && // F
        bytes[p + 3] === 0x00
      ) {
        return true
      }
    }
    offset += 2 + length
  }
  return false
}

/** APP 段全在 SOS 之前，单段至多 64KB；读 1MB 前缀足够覆盖再大的 EXIF/XMP 头 */
const SNIFF_PREFIX_BYTES = 1024 * 1024

const PASS_THROUGH_TYPES = new Set(['image/png', 'image/webp'])

async function transcode(file: File, mime: 'image/jpeg' | 'image/png'): Promise<File> {
  // from-image：按 EXIF 方向摆正后再画，重编码结果与用户看到的预览一致
  const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' })
  try {
    const canvas = document.createElement('canvas')
    canvas.width = bitmap.width
    canvas.height = bitmap.height
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('canvas 2d unavailable')
    ctx.drawImage(bitmap, 0, 0)
    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, mime, mime === 'image/jpeg' ? 0.92 : undefined)
    )
    if (!blob) throw new Error('toBlob failed')
    const ext = mime === 'image/jpeg' ? 'jpg' : 'png'
    const base = file.name.replace(/\.[^.]+$/, '') || 'ref'
    return new File([blob], `${base}.${ext}`, { type: mime })
  } finally {
    bitmap.close()
  }
}

/** 归一化参考图：保证送上游的一定是 png/webp/干净 jpeg。
 *  任一步失败（解码不了等）回退原文件——维持修复前行为，让上游错误照常可见。 */
export async function normalizeRefFile(file: File): Promise<File> {
  try {
    if (PASS_THROUGH_TYPES.has(file.type)) return file
    if (file.type === 'image/jpeg') {
      const prefix = await file.slice(0, SNIFF_PREFIX_BYTES).arrayBuffer()
      return isMpoJpeg(new Uint8Array(prefix)) ? await transcode(file, 'image/jpeg') : file
    }
    // 其余 image/*（gif/bmp/tiff/avif…）上游不收，统一转 PNG（保 alpha）
    return await transcode(file, 'image/png')
  } catch {
    return file
  }
}
