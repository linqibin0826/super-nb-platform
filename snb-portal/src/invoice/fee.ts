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

/** 人民币大写(票面「（大写）」栏)。发票大写金额只有中文形态,不随 locale 翻译。 */
export function rmbUpper(cents: number): string {
  const D = '零壹贰叁肆伍陆柒捌玖'
  const U1 = ['', '拾', '佰', '仟']
  const U2 = ['', '万', '亿']
  let yuan = Math.floor(cents / 100)
  const jiao = Math.floor((cents % 100) / 10)
  const fen = cents % 10
  let head = ''
  if (yuan === 0) head = '零圆'
  else {
    const segs: number[] = []
    while (yuan > 0) {
      segs.push(yuan % 10000)
      yuan = Math.floor(yuan / 10000)
    }
    let out = ''
    for (let i = segs.length - 1; i >= 0; i--) {
      const seg = segs[i]
      if (seg === 0) {
        if (out && segs.slice(0, i).some((s) => s > 0)) out += '零'
        continue
      }
      let str = ''
      let pending = false
      for (let p = 3; p >= 0; p--) {
        const d = Math.floor(seg / 10 ** p) % 10
        if (d === 0) {
          if (str) pending = true
        } else {
          if (pending) {
            str += '零'
            pending = false
          }
          str += D[d] + U1[p]
        }
      }
      if (out && seg < 1000) out += '零'
      out += str + U2[i]
    }
    head = out.replace(/零+/g, '零').replace(/零$/, '') + '圆'
  }
  let tail = ''
  if (jiao === 0 && fen === 0) tail = '整'
  else {
    if (jiao > 0) tail += D[jiao] + '角'
    else if (fen > 0) tail += '零'
    if (fen > 0) tail += D[fen] + '分'
  }
  return head + tail
}
