/** 触发浏览器下载：造一个隐藏 <a download> 并模拟点击（data URL / 普通 URL 都适用）。 */
export function downloadImage(url: string, filename: string): void {
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
}
