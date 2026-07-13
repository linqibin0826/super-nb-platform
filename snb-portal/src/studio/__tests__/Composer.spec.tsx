import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, cleanup, within } from '@testing-library/react'
import { Composer } from '../Composer'
import { queueLabel } from '../queueLabel'
import { MAX_REFS, type RefImage } from '../useRefImages'
import { createT, t } from '../../i18n'
import { messages } from '../../i18n/messages'

// Composer 只用 user 的真值判断（登录与否），给个非空对象即可
vi.mock('../../auth/useAuth', () => ({
  useAuthUser: () => ({ id: 1 }),
}))

// jsdom 无 ResizeObserver（输入框自动增高依赖），stub 成空实现
beforeAll(() => {
  vi.stubGlobal(
    'ResizeObserver',
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
    }
  )
})

function renderComposer(over: Partial<Parameters<typeof Composer>[0]> = {}) {
  const props = {
    prompt: 'a cat',
    size: '1152x2048',
    n: 1,
    quality: 'medium' as const,
    selectedKeyId: null,
    model: 'gpt-image-2',
    selectableModels: [] as string[],
    onChange: vi.fn(),
    eligible: [],
    rates: {},
    runningCount: 0,
    queuedCount: 0,
    finishedCount: 0,
    queueFull: false,
    canGenerate: true,
    onSubmit: vi.fn(),
    refs: [],
    onAddRefs: vi.fn(),
    onRemoveRef: vi.fn(),
    showEmptyState: false,
    applySignal: 0,
    showTrayChip: false,
    onOpenTray: vi.fn(),
    onCloseTray: vi.fn(),
    recentUploads: [],
    ...over,
  }
  render(<Composer {...props} />)
  return props
}

function expand() {
  fireEvent.click(screen.getByRole('button', { name: t('studio.composer.expand') }))
}

/** 展开开关当前的收起/展开状态（aria-expanded 是同步状态，不受退场动画影响） */
function isExpanded(): boolean {
  const collapse = screen.queryByRole('button', { name: t('studio.composer.collapse') })
  return collapse?.getAttribute('aria-expanded') === 'true'
}

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('Composer 展开态与生成的联动', () => {
  it('点「生成」自动收起展开的配置区', () => {
    const p = renderComposer()
    expand()
    expect(isExpanded()).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: t('studio.editor.generate') }))
    expect(p.onSubmit).toHaveBeenCalledTimes(1)
    expect(isExpanded()).toBe(false)
  })

  it('Cmd+Enter 生成同样收起配置区', () => {
    const p = renderComposer()
    expand()
    fireEvent.keyDown(screen.getByRole('textbox'), { key: 'Enter', metaKey: true })
    expect(p.onSubmit).toHaveBeenCalledTimes(1)
    expect(isExpanded()).toBe(false)
  })

  it('手动展开配置区时通知收起托盘（互斥），收起配置不重复通知', () => {
    const p = renderComposer()
    expand()
    expect(p.onCloseTray).toHaveBeenCalledTimes(1)
    fireEvent.click(screen.getByRole('button', { name: t('studio.composer.collapse') }))
    expect(p.onCloseTray).toHaveBeenCalledTimes(1)
  })
})

describe('队列态下的票据', () => {
  it('有任务在跑时生成按钮仍可用（不再锁死）', () => {
    const p = renderComposer({ runningCount: 3, queuedCount: 1 })
    const btn = screen.getByRole('button', { name: t('studio.editor.generate') }) as HTMLButtonElement
    expect(btn.disabled).toBe(false)
    fireEvent.click(btn)
    expect(p.onSubmit).toHaveBeenCalledTimes(1)
  })

  it('queueFull 时按钮禁用并带满员提示', () => {
    renderComposer({ queueFull: true, canGenerate: false })
    const btn = screen.getByRole('button', { name: t('studio.editor.generate') }) as HTMLButtonElement
    expect(btn.disabled).toBe(true)
    expect(btn.title).toBe(t('studio.queue.fullHint'))
  })

  it('票根胶囊显示聚合文案', () => {
    renderComposer({ showTrayChip: true, runningCount: 2, queuedCount: 1 })
    expect(screen.getByRole('button', { name: new RegExp(queueLabel(2, 1, 0)) })).toBeTruthy()
  })
})

describe('分辨率档：无效档显示但禁用（不隐藏）', () => {
  it('宽屏(9:16)：1K 显示但禁用，2K/4K 可用', () => {
    renderComposer({ size: '1152x2048' }) // 9:16 2K
    fireEvent.click(screen.getByRole('button', { name: t('studio.composer.expand') }))
    const res = within(screen.getByRole('radiogroup', { name: t('studio.composer.resolution') }))
    expect((res.getByRole('radio', { name: '1K' }) as HTMLButtonElement).disabled).toBe(true)
    expect((res.getByRole('radio', { name: '2K' }) as HTMLButtonElement).disabled).toBe(false)
    expect((res.getByRole('radio', { name: '4K' }) as HTMLButtonElement).disabled).toBe(false)
  })

  it('从宽屏 4K 切到方形：4K 变禁用（仍显示），钳制后发有效 2K 而非越界 3840×3840', () => {
    const p = renderComposer({ size: '2160x3840' }) // 9:16 4K（有效）
    fireEvent.click(screen.getByRole('button', { name: t('studio.composer.expand') }))
    const ratioGroup = within(screen.getByRole('radiogroup', { name: t('studio.composer.ratio') }))
    fireEvent.click(ratioGroup.getByRole('radio', { name: '1:1' }))
    const res = within(screen.getByRole('radiogroup', { name: t('studio.composer.resolution') }))
    expect((res.getByRole('radio', { name: '4K' }) as HTMLButtonElement).disabled).toBe(true) // 方形 4K 超上限、禁用
    expect((res.getByRole('radio', { name: '1K' }) as HTMLButtonElement).disabled).toBe(false) // 方形 1K 可用
    const sizes = (p.onChange as ReturnType<typeof vi.fn>).mock.calls.map((c) => c[0].size).filter(Boolean)
    expect(sizes).toContain('2048x2048') // 1:1 2K
    expect(sizes).not.toContain('3840x3840') // 绝不发越界尺寸
  })
})

describe('去掉 auto（上游 400 不认）', () => {
  it('配置区不再渲染「自动」按钮', () => {
    renderComposer()
    fireEvent.click(screen.getByRole('button', { name: t('studio.composer.expand') }))
    expect(screen.queryByRole('radio', { name: '自动' })).toBeNull()
  })
})

describe('custom 非法尺寸禁用生成', () => {
  function toCustom(w: string, h: string) {
    fireEvent.click(screen.getByRole('button', { name: t('studio.composer.expand') }))
    fireEvent.click(screen.getByRole('radio', { name: t('studio.composer.ratioCustom') }))
    const [wi, hi] = screen.getAllByRole('spinbutton')
    fireEvent.change(wi, { target: { value: w } })
    fireEvent.change(hi, { target: { value: h } })
  }
  it('非16倍数（1000×1000）→ 生成禁用', () => {
    renderComposer()
    toCustom('1000', '1000')
    expect((screen.getByRole('button', { name: t('studio.editor.generate') }) as HTMLButtonElement).disabled).toBe(true)
  })
  it('合法（2048×1344）→ 生成可用', () => {
    renderComposer()
    toCustom('2048', '1344')
    expect((screen.getByRole('button', { name: t('studio.editor.generate') }) as HTMLButtonElement).disabled).toBe(false)
  })
  it('非法尺寸显示约束提示', () => {
    renderComposer()
    toCustom('1000', '1000')
    expect(screen.getByText(t('studio.composer.customInvalid'))).toBeTruthy()
  })
})

describe('尺寸档命名', () => {
  it('中文尺寸档标签为「分辨率」（不再是「画质」）', () => {
    const tZh = createT(messages, 'zh')
    expect(tZh('studio.composer.resolution')).toBe('分辨率')
  })
})

function ready(id: string): RefImage {
  return { id, status: 'ready', url: `data:image/png;base64,AAAA${id}`, file: new File(['x'], `${id}.png`, { type: 'image/png' }), name: `${id}.png` }
}

describe('参考图样片行', () => {
  it('有参考图时下沉展示样片行 + 行尾 N/上限 计数', () => {
    renderComposer({ refs: [ready('a'), ready('b')] })
    expect(screen.getByText(`2 / ${MAX_REFS}`)).toBeTruthy()
    expect(screen.getAllByRole('button', { name: t('studio.editor.refRemove') })).toHaveLength(2)
  })

  it('点样片角标删除回调 onRemoveRef(id)', () => {
    const p = renderComposer({ refs: [ready('a')] })
    fireEvent.click(screen.getByRole('button', { name: t('studio.editor.refRemove') }))
    expect(p.onRemoveRef).toHaveBeenCalledWith('a')
  })

  it('加载中的样片只显骨架、不出删除键（还没 File）', () => {
    const loading: RefImage = { id: 'x', status: 'loading', url: '', file: null, name: 'x.png' }
    renderComposer({ refs: [loading] })
    expect(screen.queryByRole('button', { name: t('studio.editor.refRemove') })).toBeNull()
    expect(screen.getByText(`1 / ${MAX_REFS}`)).toBeTruthy()
  })

  it('满 MAX_REFS 张时入口禁用并给满额提示', () => {
    const full = Array.from({ length: MAX_REFS }, (_, i) => ready(`r${i}`))
    renderComposer({ refs: full })
    const entry = screen.getByRole('button', { name: t('studio.composer.pickerTitle') }) as HTMLButtonElement
    expect(entry.disabled).toBe(true)
    expect(entry.title).toBe(t('studio.composer.refFull', { max: MAX_REFS }))
  })
})
