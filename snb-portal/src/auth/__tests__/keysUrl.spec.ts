/**
 * @vitest-environment jsdom
 * @vitest-environment-options {"url": "https://studio.super-nb.me/"}
 *
 * 子域名部署下 keysUrl() 必须给控制台绝对地址：studio 自己的 Caddy 站点块对 /keys
 * 没有路由，会 try_files 兜底吞成本站 SPA，绝到不了控制台真正的 Keys 页
 *（2026-07-05 报的 bug：没有密钥时点「去密钥页看看」跳回本站而非控制台）。
 */
import { describe, expect, it } from 'vitest'
import { keysUrl } from '../apiFetch'

describe('keysUrl（子域名部署）', () => {
  it('studio.super-nb.me 下返回控制台绝对地址，而非本站相对路径', () => {
    expect(keysUrl()).toBe('https://super-nb.me/keys')
  })
})
