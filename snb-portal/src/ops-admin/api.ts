/** 资产运维台 API 数据层(同源 /ops/v1/admin,生产由 Caddy 反代 snb-platform)。
 * 鉴权照 raffle-admin/api.ts:Bearer + 临期先刷 + 401 刷新重试一次。 */
import { getToken, isExpiringSoon } from '../auth/tokens'
import { refreshTokens } from '../auth/refresh'

const BASE = '/ops/v1/admin'

export class OpsAuthError extends Error {
  constructor() {
    super('未登录或登录已过期')
    this.name = 'OpsAuthError'
  }
}

export class OpsApiError extends Error {
  readonly status: number
  constructor(status: number, detail: string) {
    super(detail)
    this.name = 'OpsApiError'
    this.status = status
  }
}

async function parseError(res: Response): Promise<OpsApiError> {
  // 后端错误统一 RFC 9457 problem+json;detail 缺席时退 title/状态码
  try {
    const body = await res.json()
    return new OpsApiError(res.status, body.detail || body.title || `HTTP ${res.status}`)
  } catch {
    return new OpsApiError(res.status, `HTTP ${res.status}`)
  }
}

export async function opsFetch<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
  if (!retried && isExpiringSoon()) await refreshTokens()
  const token = getToken()
  if (!token) throw new OpsAuthError()
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(init.headers ?? {}),
    },
  })
  if (res.status === 401) {
    if (!retried && (await refreshTokens())) return opsFetch<T>(path, init, true)
    throw new OpsAuthError()
  }
  if (!res.ok) throw await parseError(res)
  if (res.status === 200 && res.headers.get('content-length') === '0') return undefined as T
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

export type AccountStatus = 'ACTIVE' | 'BANNED' | 'UNVERIFIED' | 'ABANDONED'
export type SubService = 'CHATGPT' | 'CLAUDE'
export type SubTier = 'FREE' | 'PLUS' | 'PRO' | 'MAX' | 'TEAM'
export type SubStatus = 'FREE' | 'ACTIVE' | 'EXPIRED' | 'CANCELED' | 'BANNED'
export type RefundStatus = 'NONE' | 'PENDING' | 'APPEALING' | 'REFUNDED' | 'REJECTED'

export interface AccountRow {
  id: string
  email: string
  provider: string | null
  hasPassword: boolean
  recoveryEmail: string | null
  hasRecoveryPassword: boolean
  regYear: string | null
  country: string | null
  owner: string | null
  status: AccountStatus
  source: string | null
  notes: string | null
  createdAt: string
}

export interface SubscriptionRow {
  id: string
  accountId: string
  email: string
  service: SubService
  tier: SubTier | null
  region: string | null
  cardPlatform: string | null
  cardLast4: string | null
  registerIp: string | null
  currentIp: string | null
  registeredAt: string | null
  startedAt: string | null
  nextBillingAt: string | null
  priceUsd: string | null
  status: SubStatus
  sub2apiAccountId: string | null
  sub2apiAccountName: string | null
  bannedAt: string | null
  bannedWhilePaid: boolean | null
  refundStatus: RefundStatus
  refundAmountUsd: string | null
  appealedAt: string | null
  refundResolvedAt: string | null
  refundFollowUpAt: string | null
  refundNotes: string | null
  notes: string | null
}

/** 账号写入参:password/recoveryPassword 为 null=不改(编辑)或不设(新建) */
export type AccountInput = Omit<AccountRow, 'id' | 'hasPassword' | 'hasRecoveryPassword' | 'createdAt'> & {
  password: string | null
  recoveryPassword: string | null
}

export type SubscriptionInput = Omit<SubscriptionRow, 'id' | 'email'>

export interface SecretResult {
  password: string | null
  recoveryPassword: string | null
}

export interface DashboardResult {
  upcomingBilling: SubscriptionRow[]
  refundFollowUps: SubscriptionRow[]
  bannedOpenCount: number
}

export const api = {
  accounts: {
    list: () => opsFetch<AccountRow[]>('/accounts'),
    create: (body: AccountInput) =>
      opsFetch<{ id: string }>('/accounts', { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: AccountInput) =>
      opsFetch<void>(`/accounts/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => opsFetch<void>(`/accounts/${id}`, { method: 'DELETE' }),
    secret: (id: string) => opsFetch<SecretResult>(`/accounts/${id}/secret`),
  },
  subs: {
    list: (accountId?: string) =>
      opsFetch<SubscriptionRow[]>(`/subscriptions${accountId ? `?accountId=${accountId}` : ''}`),
    create: (body: SubscriptionInput) =>
      opsFetch<{ id: string }>('/subscriptions', { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: SubscriptionInput) =>
      opsFetch<void>(`/subscriptions/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => opsFetch<void>(`/subscriptions/${id}`, { method: 'DELETE' }),
  },
  dashboard: () => opsFetch<DashboardResult>('/dashboard'),
}
