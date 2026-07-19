/** 抽奖管理 API 数据层(同源 /activity/v1/admin/raffle,生产由 Caddy 反代 snb-platform)。
 * 鉴权照 invoice/api.ts:Bearer + 临期先刷 + 401 刷新重试一次。 */
import { getToken, isExpiringSoon } from '../auth/tokens'
import { refreshTokens } from '../auth/refresh'

const BASE = '/activity/v1/admin/raffle'

export class RaffleAuthError extends Error {
  constructor() {
    super('未登录或登录已过期')
    this.name = 'RaffleAuthError'
  }
}

export class RaffleApiError extends Error {
  readonly status: number
  constructor(status: number, detail: string) {
    super(detail)
    this.name = 'RaffleApiError'
    this.status = status
  }
}

async function parseError(res: Response): Promise<RaffleApiError> {
  // 后端错误统一 RFC 9457 problem+json;detail 缺席时退 title/状态码
  try {
    const body = await res.json()
    return new RaffleApiError(res.status, body.detail || body.title || `HTTP ${res.status}`)
  } catch {
    return new RaffleApiError(res.status, `HTTP ${res.status}`)
  }
}

export async function raffleFetch<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
  if (!retried && isExpiringSoon()) await refreshTokens()
  const token = getToken()
  if (!token) throw new RaffleAuthError()
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(init.headers ?? {}),
    },
  })
  if (res.status === 401) {
    if (!retried && (await refreshTokens())) return raffleFetch<T>(path, init, true)
    throw new RaffleAuthError()
  }
  if (!res.ok) throw await parseError(res)
  if (res.status === 200 && res.headers.get('content-length') === '0') return undefined as T
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

export type CampaignStatus = 'active' | 'drawn' | 'cancelled'
export type GateType = 'RECHARGE' | 'SPEND'
export type WeightMode = 'EQUAL' | 'WEIGHTED'
export type PrizeKind = 'REDEEM_CODE' | 'ALIPAY_CODE'

export interface CampaignSummaryT {
  id: string
  name: string
  status: CampaignStatus
  entryOpenAt: string
  entryCloseAt: string
  drawAt: string
  gateType: GateType
  gateAmount: number
  prizeCount: number
}

export interface PrizeT {
  id: string
  tier: string
  displayName: string
  kind: PrizeKind
  payload: string
  sortOrder: number
  winnerUserId: string | null
  assignedAt: string | null
}

export interface CampaignDetailT {
  id: string
  name: string
  status: CampaignStatus
  entryOpenAt: string
  entryCloseAt: string
  drawAt: string
  gateType: GateType
  gateAmount: number
  gateFrom: string
  minAccountAgeDays: number | null
  weightMode: WeightMode
  drawnAt: string | null
  entrantCountAtDraw: number | null
  disqualifiedCount: number | null
  prizes: PrizeT[]
}

export interface GroupT {
  id: string
  name: string
}

export interface PrizeSkeletonT {
  tier: string
  displayName: string
  kind: PrizeKind
  sortOrder: number
}

export interface CampaignScalarsT {
  name: string
  entryOpenAt: string
  entryCloseAt: string
  drawAt: string
  gateType: GateType
  gateAmount: number
  gateFrom: string
  minAccountAgeDays: number | null
  weightMode: WeightMode
}

export const api = {
  list: () => raffleFetch<CampaignSummaryT[]>('/campaigns'),
  listGroups: () => raffleFetch<GroupT[]>('/subscription-groups'),
  detail: (id: string) => raffleFetch<CampaignDetailT>(`/campaigns/${id}`),
  create: (body: CampaignScalarsT & { prizes: PrizeSkeletonT[] }) =>
    raffleFetch<CampaignDetailT>('/campaigns', { method: 'POST', body: JSON.stringify(body) }),
  update: (id: string, body: CampaignScalarsT) =>
    raffleFetch<CampaignDetailT>(`/campaigns/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  cancel: (id: string) => raffleFetch<void>(`/campaigns/${id}/cancel`, { method: 'POST' }),
  addPrize: (
    id: string,
    body: { tier: string; displayName: string; kind: PrizeKind; payload: string; sortOrder: number },
  ) => raffleFetch<PrizeT>(`/campaigns/${id}/prizes`, { method: 'POST', body: JSON.stringify(body) }),
  updatePrize: (
    id: string,
    prizeId: string,
    body: { tier: string; displayName: string; kind: PrizeKind; payload: string; sortOrder: number },
  ) =>
    raffleFetch<PrizeT>(`/campaigns/${id}/prizes/${prizeId}`, { method: 'PUT', body: JSON.stringify(body) }),
  deletePrize: (id: string, prizeId: string) =>
    raffleFetch<void>(`/campaigns/${id}/prizes/${prizeId}`, { method: 'DELETE' }),
  generateRedeemCodes: (
    id: string,
    body: {
      tier: string
      displayName: string
      groupId: number
      validityDays: number
      count: number
      sortOrderStart: number
    },
  ) =>
    raffleFetch<PrizeT[]>(`/campaigns/${id}/prizes/generate-redeem-codes`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  generateRedeemCodeForPrize: (id: string, prizeId: string, body: { groupId: number; validityDays: number }) =>
    raffleFetch<PrizeT>(`/campaigns/${id}/prizes/${prizeId}/generate-redeem-code`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  generateAlipayCode: (
    id: string,
    body: { prizeId: string | null; tier: string; displayName: string; sortOrder: number },
  ) =>
    raffleFetch<PrizeT>(`/campaigns/${id}/prizes/generate-alipay-code`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
}
