/** 发票中心本地文案分片(2026-07-29 Claude Design 定稿落地新增的词条)。
 *
 *  ⚠️ 这是一块**临时分片**,不是新体例:本轮多路并行落地里 `src/i18n/messages.ts`
 *  是共享文件(同仓另有改 studio / hub 的活儿在同文件里加键),按目录纪律本轮不动它。
 *  分片复用共享的 createT + locale,键名与 messages.ts 的 `invoice.*` 一一对应,
 *  并行落地收口后应整体并回 messages.ts、删掉本文件与 `ti` 的调用。
 *
 *  用法:老词条继续 `t('invoice.…')`,本轮新增的走 `ti('invoice.…')`。 */
import { createT, locale, type Locale, type LocaleDict } from '../i18n'

const dict: Record<Locale, LocaleDict> = {
  zh: {
    invoice: {
      /** 独立标签页形态的回家条(嵌入控制台 iframe 时整条不渲染) */
      homebar: {
        note: '发票中心 · 独立窗口',
        back: '← 回我的机位',
        login: '登录',
      },
      /** 访客态:三个页签同一副壳,只换骨架与末句 */
      guest: {
        badge: '访客视图 · {tab} · 数据未接通',
        cta: '登录后开票',
        apply:
          '登录后这里排的是你真实付款的充网费订单：勾上要开的，票面会当场算出合计、大写金额和手续费。',
        requests:
          '登录后这里是你每一张申请：待受理 / 开票中 / 已开票 / 已驳回 / 已撤回，盖着章，开出来的 PDF 在这儿下载。',
        profiles:
          '登录后这里存你的开票抬头，最多 10 个：整段资料粘进来就能自动认出，公司名点一下「核验」还能盖上「已核验」章。',
      },
      guide: {
        later: '稍后再看',
        close: '关闭须知',
        reopen: '再看一遍开票须知',
        exitNote:
          '「知道了」= 服务端永久记住，换浏览器也不再弹。「稍后再看」和 ✕ / Esc = 只跳过这一次，下次还会弹。',
      },
      apply: {
        profilePickHint: '抬头就在票面上选；＋ 是就地开浮层填，填完直接落回这张票——不跳页、不丢勾选',
        addProfileChip: '＋ 新增抬头',
        ordFoot: '一笔订单只能开一次，开过即从这里消失；撤回申请后订单会回来。共 {n} 笔可开票订单。',
      },
    },
  },
  en: {
    invoice: {
      homebar: {
        note: 'Invoice desk · standalone window',
        back: '← Back to My Station',
        login: 'Sign in',
      },
      guest: {
        badge: 'Guest view · {tab} · data not connected',
        cta: 'Sign in to invoice',
        apply:
          'Once you sign in, this lists the top-up orders you actually paid for: tick the ones to invoice and the ticket totals them on the spot — figures, amount in words and the fee.',
        requests:
          'Once you sign in, every request sits here: pending / issuing / issued / rejected / withdrawn, stamped, with the issued PDF ready to download.',
        profiles:
          'Once you sign in, your invoice titles live here, up to 10: paste a whole block of details and it reads itself in; hit “Verify” on a company name to earn the “Verified” stamp.',
      },
      guide: {
        later: 'Later',
        close: 'Close the notice',
        reopen: 'Read the invoicing notice again',
        exitNote:
          '“Got it” is remembered server-side — it will not pop up again, not even in another browser. “Later” and ✕ / Esc skip this once only; it comes back next time.',
      },
      apply: {
        profilePickHint:
          'Pick the title right on the ticket; ＋ opens the form in place — once filled in it drops straight back onto this ticket, no page hop, no lost ticks',
        addProfileChip: '＋ New title',
        ordFoot:
          'Each order can be invoiced once — invoiced ones drop off this list; withdraw a request and they come back. {n} invoiceable orders in total.',
      },
    },
  },
}

export const ti = createT(dict, locale)
