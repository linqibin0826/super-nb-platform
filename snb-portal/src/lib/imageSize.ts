/** 从 PNG base64 头部同步解出真实像素尺寸（imagesApi 一律输出 data:image/png）。
 *  用途：上游（gpt-image 系）不认我们请求的 size（如 1:1 4K），实际返回图尺寸/比例常与请求不符；
 *  展示必须按真实尺寸定比例，否则 MasonryCard 的 object-cover 会按错误比例裁切两侧（2026-07-05 生产坑）。
 *  非 PNG / 解析失败返回 null，调用方回退请求 size。
 *  前 24 字节 = 8 字节签名 + IHDR(长度4 + "IHDR"4) + width4 + height4，正好对应前 32 个 base64 字符。 */
export function pngDimensions(b64: string): { width: number; height: number } | null {
  try {
    const head = atob(b64.slice(0, 32))
    if (head.length < 24) return null
    if (head.charCodeAt(0) !== 0x89 || head.charCodeAt(1) !== 0x50) return null // PNG 签名
    const u32 = (o: number) =>
      head.charCodeAt(o) * 0x1000000 +
      (head.charCodeAt(o + 1) << 16) +
      (head.charCodeAt(o + 2) << 8) +
      head.charCodeAt(o + 3)
    const width = u32(16)
    const height = u32(20)
    return width > 0 && height > 0 ? { width, height } : null
  } catch {
    return null
  }
}
