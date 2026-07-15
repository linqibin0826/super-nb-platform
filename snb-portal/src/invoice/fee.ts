/** 手续费预估(镜像后端 FeePolicy,常量以 GET /orders 下发为准,不硬编码)。全程用「分」防浮点。 */
export interface FeeConstants {
  minTotal: number
  freeThreshold: number
  feeRate: number
}

export function selectedTotalCents(
  orders: { orderId: string; amount: number }[],
  selected: Set<string>,
): number {
  return orders
    .filter((o) => selected.has(o.orderId))
    .reduce((sum, o) => sum + Math.round(o.amount * 100), 0)
}

export function feeCents(totalCents: number, c: FeeConstants): number {
  if (totalCents >= Math.round(c.freeThreshold * 100)) return 0
  return Math.round(totalCents * c.feeRate)
}

export function fmtYuan(cents: number): string {
  return (cents / 100).toFixed(2)
}
