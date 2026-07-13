// GET /v1/models 用「用户站内 key」直调网关（同 imagesApi，不走控制台 JWT 体系）。
// 返回该 key 所在 group 可调度账号 model_mapping key 的并集（后端已现成）。
export async function fetchModels(apiKey: string): Promise<string[]> {
  let res: Response
  try {
    res = await fetch('/v1/models', { headers: { Authorization: `Bearer ${apiKey}` } })
  } catch {
    return []
  }
  if (!res.ok) return []
  const body = (await res.json().catch(() => null)) as { data?: Array<{ id?: string }> } | null
  return (body?.data ?? []).map((m) => m.id).filter((id): id is string => typeof id === 'string')
}
