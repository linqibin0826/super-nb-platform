import {
  REFRESH_TOKEN_KEY,
  clearTokens,
  getToken,
  reconcileFromCookie,
  setTokens,
} from './tokens'

let inflight: Promise<boolean> | null = null

// 单飞：并发调用共享同一次刷新（双应用并发 ≈ 主站多 tab 场景）
export function refreshTokens(): Promise<boolean> {
  inflight ??= doRefresh().finally(() => {
    inflight = null
  })
  return inflight
}

async function doRefresh(): Promise<boolean> {
  // refresh token 一次性轮换：刷新前先跟 cookie 对账——别的源（主站）可能刚刷完。
  // 只有对账真收养到「不同的」token 才短路成功；「本地 token 看着没过期」不能短路，
  // 401 触发的强制刷新场景服务端已宣判 token 死亡，本地时钟不作数
  const before = getToken()
  reconcileFromCookie()
  const adopted = getToken()
  if (adopted && adopted !== before) return true

  const attempted = localStorage.getItem(REFRESH_TOKEN_KEY)
  if (!attempted) return false
  try {
    const res = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: attempted }),
    })
    const body = (await res.json()) as {
      code: number
      data?: { access_token: string; refresh_token: string; expires_in: number }
    }
    if (res.ok && body.code === 0 && body.data) {
      // setTokens 内部会回写 cookie（唯一真源），主站下次对账即收养
      setTokens(body.data.access_token, body.data.refresh_token, body.data.expires_in)
      return true
    }
  } catch {
    // 网络失败与刷新被拒同走下方收养/清理分支（与主站 client.ts 行为一致）
  }
  // 失败可能是输给了并发轮换（对方先刷、旧 rt 已作废）：再对账一次，
  // cookie 里已换新就收养返回成功；确实死透才清会话（clearTokens 落墓碑）
  reconcileFromCookie()
  const current = localStorage.getItem(REFRESH_TOKEN_KEY)
  if (current && current !== attempted) return true
  clearTokens()
  return false
}
