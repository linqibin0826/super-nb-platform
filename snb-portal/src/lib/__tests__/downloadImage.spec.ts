import { afterEach, describe, expect, it, vi } from 'vitest'
import { downloadImage } from '../downloadImage'

describe('downloadImage', () => {
  afterEach(() => vi.restoreAllMocks())

  it('创建 <a> 设置正确的 href/download 并触发一次点击', () => {
    let capturedHref = ''
    let capturedDownload = ''
    const clickSpy = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(function (this: HTMLAnchorElement) {
        capturedHref = this.href
        capturedDownload = this.download
      })

    downloadImage('data:image/png;base64,AAAA', 'snb-img-1.png')

    expect(capturedHref).toBe('data:image/png;base64,AAAA')
    expect(capturedDownload).toBe('snb-img-1.png')
    expect(clickSpy).toHaveBeenCalledTimes(1)
  })
})
