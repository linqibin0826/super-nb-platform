/** 发票 API 数据层(同源 /invoice/v1,生产由 Caddy 反代 snb-platform)。
 * 鉴权照 generationsApi:Bearer + 临期先刷 + 401 刷新重试一次。 */
import { getToken, isExpiringSoon } from '../auth/tokens'
import { refreshTokens } from '../auth/refresh'

const BASE = '/invoice/v1'

export class InvoiceAuthError extends Error {
  constructor() {
    super('未登录或登录已过期')
    this.name = 'InvoiceAuthError'
  }
}

export class InvoiceApiError extends Error {
  readonly status: number
  constructor(status: number, detail: string) {
    super(detail)
    this.name = 'InvoiceApiError'
    this.status = status
  }
}

async function parseError(res: Response): Promise<InvoiceApiError> {
  // 后端错误统一 RFC 9457 problem+json;detail 缺席时退 title/状态码
  try {
    const body = await res.json()
    return new InvoiceApiError(res.status, body.detail || body.title || `HTTP ${res.status}`)
  } catch {
    return new InvoiceApiError(res.status, `HTTP ${res.status}`)
  }
}

export async function invoiceFetch<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
  if (!retried && isExpiringSoon()) await refreshTokens()
  const token = getToken()
  if (!token) throw new InvoiceAuthError()
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(init.headers ?? {}),
    },
  })
  if (res.status === 401) {
    if (!retried && (await refreshTokens())) return invoiceFetch<T>(path, init, true)
    throw new InvoiceAuthError()
  }
  if (!res.ok) throw await parseError(res)
  if (res.status === 200 && res.headers.get('content-length') === '0') return undefined as T
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

/** 表单上传(multipart 不能带 Content-Type: application/json) */
export async function invoiceUpload(path: string, form: FormData): Promise<void> {
  if (isExpiringSoon()) await refreshTokens()
  const token = getToken()
  if (!token) throw new InvoiceAuthError()
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  })
  if (!res.ok) throw await parseError(res)
}

/** 鉴权下载:fetch blob + objectURL 触发保存(直接 <a href> 带不上 Bearer 头) */
export async function downloadPdf(path: string, filename: string): Promise<void> {
  if (isExpiringSoon()) await refreshTokens()
  const token = getToken()
  if (!token) throw new InvoiceAuthError()
  const res = await fetch(`${BASE}${path}`, { headers: { Authorization: `Bearer ${token}` } })
  if (!res.ok) throw await parseError(res)
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

export interface ProfileT {
  id: string
  type: 'COMPANY' | 'PERSONAL'
  title: string
  taxNo: string | null
  regAddress: string | null
  regPhone: string | null
  bankName: string | null
  bankAccount: string | null
  /** 核验章:名称+税号与官方开票档案一致的时刻;null=未核验(服务端判定,只读) */
  verifiedAt: string | null
}

/** 官方开票档案(第三方核验返回,字段可能残缺) */
export interface RegistryOfficialT {
  name: string | null
  taxNo: string | null
  address: string | null
  phone: string | null
  bankName: string | null
  bankAccount: string | null
}

export interface OrderT {
  orderId: string
  orderNo: string
  amount: number
  completedAt: string
}

export interface OverviewT {
  orders: OrderT[]
  billableTotal: number
  balance: number
  minTotal: number
  freeThreshold: number
  feeRate: number
}

export type RequestStatus = 'PENDING' | 'INVOICING' | 'ISSUED' | 'REJECTED' | 'CANCELLED'

export interface RequestT {
  id: string
  requestNo: string
  amount: number
  fee: number
  status: RequestStatus
  profileTitle: string
  remark: string | null
  rejectReason: string | null
  createdAt: string
  issuedAt: string | null
}

export interface AdminRowT {
  id: string
  requestNo: string
  userId: string
  email: string
  amount: number
  fee: number
  status: RequestStatus
  createdAt: string
}

export interface AdminPageT {
  items: AdminRowT[]
  total: number
}

export interface AdminDetailT extends Omit<AdminRowT, 'email'> {
  email: string
  profileType: 'COMPANY' | 'PERSONAL'
  profileTitle: string
  profileTaxNo: string | null
  profileRegAddress: string | null
  profileRegPhone: string | null
  profileBankName: string | null
  profileBankAccount: string | null
  profileVerifiedAt: string | null
  remark: string | null
  rejectReason: string | null
  feeChargedAt: string | null
  issuedAt: string | null
  orders: OrderT[]
}

export const api = {
  profiles: () => invoiceFetch<ProfileT[]>('/profiles'),
  createProfile: (body: Omit<ProfileT, 'id' | 'verifiedAt'>) =>
    invoiceFetch<{ id: string }>('/profiles', { method: 'POST', body: JSON.stringify(body) }),
  updateProfile: (id: string, body: Omit<ProfileT, 'id' | 'verifiedAt'>) =>
    invoiceFetch<void>(`/profiles/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  registryLookup: (name: string) =>
    invoiceFetch<{ found: boolean; official: RegistryOfficialT | null }>('/registry/lookup', {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),
  deleteProfile: (id: string) => invoiceFetch<void>(`/profiles/${id}`, { method: 'DELETE' }),
  orders: () => invoiceFetch<OverviewT>('/orders'),
  createRequest: (orderIds: string[], profileId: string, remark: string) =>
    invoiceFetch<{ id: string; requestNo: string; amount: number; fee: number }>('/requests', {
      method: 'POST',
      body: JSON.stringify({ orderIds, profileId, remark: remark || null }),
    }),
  requests: () => invoiceFetch<RequestT[]>('/requests'),
  cancelRequest: (id: string) => invoiceFetch<void>(`/requests/${id}/cancel`, { method: 'POST' }),
  adminPage: (status: string, page: number) =>
    invoiceFetch<AdminPageT>(`/admin/requests?status=${status}&page=${page}&size=20`),
  adminDetail: (id: string) => invoiceFetch<AdminDetailT>(`/admin/requests/${id}`),
  adminCharge: (id: string) =>
    invoiceFetch<{ status: string }>(`/admin/requests/${id}/charge`, { method: 'POST' }),
  adminReject: (id: string, reason: string, refundFee: boolean) =>
    invoiceFetch<void>(`/admin/requests/${id}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason, refundFee }),
    }),
}
