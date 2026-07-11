/** 从管线预渲染 HTML 估算阅读时长（分钟）：CJK 按 ~400 字/分，拉丁按 ~180 词/分，向上取整、至少 1 分钟。 */
export function readingMinutes(html: string): number {
  const text = html
    .replace(/<[^>]+>/g, ' ')
    .replace(/&[a-z#0-9]+;/gi, ' ')
  const cjk = (text.match(/[㐀-䶿一-鿿]/g) ?? []).length
  const words = (text.match(/[A-Za-z0-9]+/g) ?? []).length
  return Math.max(1, Math.ceil(cjk / 400 + words / 180))
}
