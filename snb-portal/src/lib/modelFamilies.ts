// 把网关返回的模型名翻译成生图工坊 UI 语义。
// 判定基于命名前缀：新增同族模型零改动；接全新家族加一条 FamilySpec。
// 这是「动态清单」（GET /v1/models）与「静态语义」之间唯一的耦合点。
export type ModelRole = 'generation' | 'edit' | 'hidden'
export type SizeMode = 'free' | 'grokSquare'

interface FamilySpec {
  id: string
  matches: (m: string) => boolean
  roleOf: (m: string) => ModelRole
  displayName: (m: string) => string
  /** 该族编辑模型：'self'=用生图模型自身；具体名=统一切该名；null=不支持 edit */
  editModel: 'self' | string | null
  sizeMode: SizeMode
  hasQualityAxis: boolean
}

const FAMILIES: FamilySpec[] = [
  {
    id: 'gpt-image',
    matches: (m) => m.startsWith('gpt-image-'),
    roleOf: () => 'generation',
    displayName: (m) => (m === 'gpt-image-2' ? 'GPT Image' : `GPT Image (${m})`),
    editModel: 'self',
    sizeMode: 'free',
    hasQualityAxis: true,
  },
  {
    id: 'grok-imagine',
    matches: (m) => m.startsWith('grok-imagine'),
    roleOf: (m) => {
      if (m === 'grok-imagine-image' || m === 'grok-imagine-image-quality') return 'generation'
      if (m === 'grok-imagine-edit') return 'edit'
      return 'hidden' // grok-imagine 裸名、grok-imagine-video* 等
    },
    displayName: (m) =>
      m === 'grok-imagine-image' ? 'Grok 快图' : m === 'grok-imagine-image-quality' ? 'Grok 高清' : m,
    editModel: 'grok-imagine-edit',
    sizeMode: 'grokSquare',
    hasQualityAxis: false,
  },
]

function familyOf(model: string): FamilySpec | null {
  return FAMILIES.find((f) => f.matches(model)) ?? null
}

export function roleOf(model: string): ModelRole {
  return familyOf(model)?.roleOf(model) ?? 'hidden'
}

export function isSelectableGenerationModel(model: string): boolean {
  return roleOf(model) === 'generation'
}

export function displayName(model: string): string {
  return familyOf(model)?.displayName(model) ?? model
}

export function editModelFor(model: string): string | null {
  const f = familyOf(model)
  if (!f) return null
  return f.editModel === 'self' ? model : f.editModel
}

export function sizeModeOf(model: string): SizeMode {
  return familyOf(model)?.sizeMode ?? 'free'
}

export function hasQualityAxis(model: string): boolean {
  return familyOf(model)?.hasQualityAxis ?? true
}

/** grok 生图实测只稳定命中方形 1024²/2048²（1K/2K）；其余 size 上游回退 1024²、4K 不支持 */
export const GROK_SQUARE_SIZES = ['1024x1024', '2048x2048'] as const

/** 把任意 size 归一到 grok 支持的方形档；不匹配则默认 2K */
export function normalizeGrokSize(size: string): string {
  return (GROK_SQUARE_SIZES as readonly string[]).includes(size) ? size : '2048x2048'
}
