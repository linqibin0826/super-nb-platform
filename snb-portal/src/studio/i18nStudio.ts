// studio 本地文案层（2026-07-29 画图机位改版）：
// 共享 src/i18n 是三站（studio/hub/invoice）合用文件、本轮并行改造中约定只读——
// 新增与改词的 studio 文案全部收在这里，st() 先查本地字典、查不到回落共享 t()。
// ⚠️ 八段等待文案是设计定稿逐字终稿（机房腔，节奏与「越往后越接近完工」的递进不许动）。
import type { LocaleDict } from '../i18n/core'
import { locale, t } from '../i18n'

const STUDIO_LOCAL: Record<'zh' | 'en', LocaleDict> = {
  zh: {
    studio: {
      hero: {
        subtitle: '逛灵感库不用登录，出图和收藏才要开卡。',
        libCount: '灵感库 5778 条素材',
        queueLight: '生图队列 · 同时最多跑 {n} 个',
      },
      gallery: {
        // 排序四档按真源口径改名：featured=收录默认序
        sortFeatured: '默认',
        sortNewest: '最新',
        sortLikes: '最赞',
        sortFavs: '最多收藏',
      },
      filters: {
        searchPlaceholder: '搜标题、搜提示词——不用登录',
        all: '全部',
        moreStyles: '更多筛选 · 风格 ▾',
        lessStyles: '收起风格 ▴',
        styleNote: '风格轴有货的就这几档；主体轴还全是空的，先不摆出来占地方。',
        moreCats: '+{n} 类',
        lessCats: '收起 ▴',
      },
      wall: {
        ccNote: '署名常显是硬要求：源库按 CC BY 4.0 收录，不署名就不能用。',
      },
      // 空态/错误态配对（公用件规范 v3）：请求成功且空 → 空态；失败/超时/解析失败 → 错误态，
      // 二者不可互换。空态按钮指向「能把这里填满的那个动作」，不许只写「暂无数据」。
      empty: {
        title: '这一筛没剩下什么',
        body: '换个类目，或者把搜索词删掉——全库 5778 条，总有一张能用。',
        action: '清掉筛选，回全部',
      },
      wallError: {
        title: '灵感库没拉下来',
        body: '刚才请求素材列表失败了，是机房这边的问题；你的收藏和创作记录都还在。',
        action: '再试一次',
      },
      big: {
        counter: '第 {n} 张 · 本次筛选',
        promptTitle: '提示词全文',
        promptHint: '可选可复制',
        use: '直接使用 · 填进创作票据',
        copiedFull: '已复制到剪贴板',
        guestNote: '提示词随便看随便抄，不用登录。要点赞收藏或者照这张出图，得先开卡上机。',
        prev: '上一张',
        next: '下一张',
        close: '关闭',
      },
      // 八段走纸文案 · 机房腔终稿（7 秒一换，越往后越接近完工）
      results: {
        stage1: '机器自检，风扇转起来了…',
        stage2: '显存吃满，开始铺第一遍…',
        stage3: '描线成形，别碰键盘…',
        stage4: '光影往哪儿落，显卡在算…',
        stage5: '挪去一台更闲的卡，继续…',
        stage6: '退一帧看整体，像素对齐…',
        stage7: '最后一遍校色，快好了…',
        stage8: '写盘出图中，马上给你…',
      },
      wait: {
        paperLabel: '出图记录纸 · 走纸中',
        traceNote: '这条线只是机器在走纸，不代表剩余时间',
        step: '第 {step} / 8 段',
        note: '进度是估的，到不了 100——真出图了直接跳图',
        stayHint: '站内随便逛不影响这单；但别关标签页、别刷新——出图是这台浏览器在盯着的，页面一关这单就当场取消。',
      },
      rail: {
        note: '票据在自己的柜台格子里，永不压图。一次生成通常 30–120 秒。',
      },
      guestBoard: {
        badge: '规格牌 · 访客视图',
        lead: '先看清有得选、按用量算钱，再决定要不要充。',
        modelsTitle: '能选什么模型',
        modelsBody: '这里会列出你能用的生图模型——清单是拿你自己的 Key 去问接口拿的，所以没上机之前谁也看不到。',
        sizesTitle: '出什么尺寸',
        sizesAuto: '自动 / 自定义宽高',
        sizesBody1: '先挑比例，再挑画质档 ',
        sizesBody2: '——两样合起来才是最后出图的尺寸，计费也按这个档走。',
        costTitle: '大概花多少',
        costFormula1: '分组档位单价 × 张数 × 分组倍率',
        costFormula2: '按官方价计 · 充值 1:1 不看汇率',
        costBody: '出图前票据上会写清这一单预估多少钱，一分不差再撕票。分组是登录后才知道的，所以这儿只给算法不给数。',
        cta: '开卡上机',
        ctaNote: '开卡建 Key 全程免费，不充值不计费。登录后这三格会填上真的模型清单和你这一单的预估。',
      },
    },
  },
  en: {
    studio: {
      hero: {
        subtitle: 'Browse the wall without signing in — generating and saving need a card.',
        libCount: 'Library · 5,778 prompts',
        queueLight: 'Queue · up to {n} at once',
      },
      gallery: {
        sortFeatured: 'Default',
        sortNewest: 'Newest',
        sortLikes: 'Most liked',
        sortFavs: 'Most saved',
      },
      filters: {
        searchPlaceholder: 'Search titles & prompts — no sign-in needed',
        all: 'All',
        moreStyles: 'More filters · Style ▾',
        lessStyles: 'Hide styles ▴',
        styleNote: 'Only these style bins are stocked; the subject axis is still empty, so it stays off the shelf.',
        moreCats: '+{n} more',
        lessCats: 'Less ▴',
      },
      wall: {
        ccNote: 'Attribution stays on: the source library is CC BY 4.0 — no credit, no use.',
      },
      empty: {
        title: 'That filter left nothing',
        body: 'Try another bin, or clear the search box — 5,778 prompts in the library, one of them fits.',
        action: 'Clear filters',
      },
      wallError: {
        title: "The library didn't load",
        body: 'The request for the prompt list failed on our side. Your saves and your history are untouched.',
        action: 'Try again',
      },
      big: {
        counter: '#{n} · this filter',
        promptTitle: 'Full prompt',
        promptHint: 'Select & copy freely',
        use: 'Use it · drop into the ticket',
        copiedFull: 'Copied to clipboard',
        guestNote: 'Prompts are free to read and copy — no sign-in. Liking, saving, or generating from this one needs a card.',
        prev: 'Previous',
        next: 'Next',
        close: 'Close',
      },
      results: {
        stage1: 'Self-test done, fans spinning up…',
        stage2: 'VRAM maxed, laying down the first pass…',
        stage3: 'Lines taking shape — hands off the keyboard…',
        stage4: 'Working out where the light lands, GPU crunching…',
        stage5: 'Shifted to a freer card, carrying on…',
        stage6: 'Stepping back a frame, snapping pixels to grid…',
        stage7: 'One last color pass, nearly there…',
        stage8: 'Writing to disk, coming right up…',
      },
      wait: {
        paperLabel: 'Print log · feeding',
        traceNote: 'The trace just means the machine is feeding paper — not a countdown',
        step: 'Line {step} / 8',
        note: 'The bar is a guess and never hits 100 — when the image lands, it just lands',
        stayHint: 'Browsing the site is fine, but do not close or refresh this tab — this browser is minding the job; close it and the order is cancelled on the spot.',
      },
      rail: {
        note: 'The ticket keeps to its own counter slot and never covers the wall. A run usually takes 30–120 s.',
      },
      guestBoard: {
        badge: 'Spec board · guest view',
        lead: 'See what is on offer and how usage is priced, before you put money in.',
        modelsTitle: 'Which models',
        modelsBody: 'Your selectable image models will be listed here — the list is fetched with your own key, so nobody sees it before getting on a station.',
        sizesTitle: 'Which sizes',
        sizesAuto: 'Auto / custom W×H',
        sizesBody1: 'Pick a ratio, then a resolution tier ',
        sizesBody2: ' — together they make the final output size, and billing follows the same tier.',
        costTitle: 'Rough cost',
        costFormula1: 'group tier price × count × group multiplier',
        costFormula2: 'official rates · top-ups land 1:1, no FX',
        costBody: 'Before a run, the ticket spells out the estimate for this order — torn only at that exact price. Your group is known after sign-in, so here you get the formula, not a number.',
        cta: 'Open a card',
        ctaNote: 'Opening a card and creating keys is free — no top-up, no charge. Once signed in these three slots fill with your real model list and estimate.',
      },
    },
  },
}

function lookup(dict: LocaleDict, key: string): string | undefined {
  let node: string | LocaleDict | undefined = dict
  for (const part of key.split('.')) {
    node = typeof node === 'object' ? node[part] : undefined
  }
  return typeof node === 'string' ? node : undefined
}

/** studio 本地翻译：先查本地覆盖层，缺席回落共享 t()（参数占位语法与共享层一致） */
export function st(key: string, params?: Record<string, string | number>): string {
  const local = lookup(STUDIO_LOCAL[locale], key)
  if (local === undefined) return t(key, params)
  if (!params) return local
  return local.replace(/\{(\w+)\}/g, (_, name: string) => String(params[name] ?? ''))
}
