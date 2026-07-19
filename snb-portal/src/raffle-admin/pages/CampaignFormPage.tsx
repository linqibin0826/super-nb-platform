import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { Alert, Button, Card, Input } from '../../ui'
import { t } from '../../i18n'
import {
  api,
  RaffleApiError,
  type CampaignDetailT,
  type CampaignScalarsT,
  type GateType,
  type GroupT,
  type PrizeKind,
  type PrizeSkeletonT,
  type PrizeT,
  type WeightMode,
} from '../api'
import { Cluster, ErrorBar, Jack, Lamp, Loading, PageHead, RfSelect } from './shared'

function toLocalInput(iso: string): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function toIso(local: string): string {
  return local ? new Date(local).toISOString() : ''
}

const BLANK: CampaignScalarsT = {
  name: '',
  entryOpenAt: new Date().toISOString(),
  entryCloseAt: '',
  drawAt: '',
  gateType: 'RECHARGE',
  gateAmount: 30,
  gateFrom: '2026-06-01T00:00:00.000Z',
  minAccountAgeDays: null,
  weightMode: 'EQUAL',
}

export function CampaignFormPage({ mode }: { mode: 'create' | 'edit' }) {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const cloneFrom = searchParams.get('cloneFrom')
  const navigate = useNavigate()

  const [scalars, setScalars] = useState<CampaignScalarsT>(BLANK)
  const [prizeSkeleton, setPrizeSkeleton] = useState<PrizeSkeletonT[]>([])
  const [detail, setDetail] = useState<CampaignDetailT | null>(null)
  const [loading, setLoading] = useState(mode === 'edit')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [forbidden, setForbidden] = useState(false)

  useEffect(() => {
    if (mode === 'edit' && id) {
      api
        .detail(id)
        .then((d) => {
          setDetail(d)
          setScalars({
            name: d.name,
            entryOpenAt: d.entryOpenAt,
            entryCloseAt: d.entryCloseAt,
            drawAt: d.drawAt,
            gateType: d.gateType,
            gateAmount: d.gateAmount,
            gateFrom: d.gateFrom,
            minAccountAgeDays: d.minAccountAgeDays,
            weightMode: d.weightMode,
          })
          setLoading(false)
        })
        .catch((e) => {
          if (e instanceof RaffleApiError && e.status === 403) setForbidden(true)
          else setError(String(e.message))
          setLoading(false)
        })
    } else if (mode === 'create' && cloneFrom) {
      // 克隆=纯前端操作:拿源期详情预填草稿,时间清空待填,奖品只留骨架(tier/展示名/kind/
      // 排序),payload 不带过来(旧码/口令已消耗或已过期,必须重新生成)。
      api
        .detail(cloneFrom)
        .then((d) => {
          setScalars({
            name: `${d.name}(克隆)`,
            entryOpenAt: '',
            entryCloseAt: '',
            drawAt: '',
            gateType: d.gateType,
            gateAmount: d.gateAmount,
            gateFrom: d.gateFrom,
            minAccountAgeDays: d.minAccountAgeDays,
            weightMode: d.weightMode,
          })
          setPrizeSkeleton(
            d.prizes.map((p) => ({ tier: p.tier, displayName: p.displayName, kind: p.kind, sortOrder: p.sortOrder })),
          )
        })
        .catch((e) => setError(String((e as Error).message)))
    }
  }, [mode, id, cloneFrom])

  const editable = mode === 'create' || detail?.status === 'active'

  // 分组下拉数据源:仅可编辑的编辑页需要;拉不到(admin 通道缺席等)静默降级回手填框
  const [groups, setGroups] = useState<GroupT[] | null>(null)
  useEffect(() => {
    if (mode === 'edit' && detail?.status === 'active' && groups === null) {
      api
        .listGroups()
        .then(setGroups)
        .catch(() => setGroups([]))
    }
  }, [mode, detail, groups])
  const groupOptions = (groups ?? []).map((g) => ({ value: g.id, label: `${g.name}(${g.id})` }))

  // 前端先卡一道与后端 RaffleAdminValidation 同口径的校验,免得整卡提交到后端才报错
  const validateScalars = (): string | null => {
    if (!scalars.name.trim()) return t('raffle.admin.validation.nameRequired')
    if (!scalars.entryOpenAt || !scalars.entryCloseAt || !scalars.drawAt)
      return t('raffle.admin.validation.timesRequired')
    if (Date.parse(scalars.entryOpenAt) >= Date.parse(scalars.entryCloseAt))
      return t('raffle.admin.validation.entryCloseAfterOpen')
    if (Date.parse(scalars.entryCloseAt) >= Date.parse(scalars.drawAt))
      return t('raffle.admin.validation.drawAfterClose')
    if (!(scalars.gateAmount > 0)) return t('raffle.admin.validation.gateAmountPositive')
    if (!scalars.gateFrom) return t('raffle.admin.validation.gateFromRequired')
    if (scalars.minAccountAgeDays != null && scalars.minAccountAgeDays < 0)
      return t('raffle.admin.validation.minAgeNonNegative')
    return null
  }

  const save = async () => {
    const invalid = validateScalars()
    if (invalid) {
      setError(invalid)
      return
    }
    // 克隆骨架行同样卡档位/展示名必填,防空壳垃圾行进库
    if (mode === 'create' && prizeSkeleton.some((p) => !p.tier.trim() || !p.displayName.trim())) {
      setError(t('raffle.admin.validation.prizeTierNameRequired'))
      return
    }
    setSaving(true)
    setError('')
    try {
      if (mode === 'create') {
        const created = await api.create({ ...scalars, prizes: prizeSkeleton })
        navigate(`/admin/campaigns/${created.id}`, { replace: true })
      } else if (id) {
        const updated = await api.update(id, scalars)
        setDetail(updated)
      }
    } catch (e) {
      setError(String((e as Error).message))
    } finally {
      setSaving(false)
    }
  }

  const cancelCampaign = async () => {
    if (!id || !confirm(t('raffle.admin.confirmCancel'))) return
    setSaving(true)
    try {
      await api.cancel(id)
      setDetail(await api.detail(id))
    } catch (e) {
      setError(String((e as Error).message))
    } finally {
      setSaving(false)
    }
  }

  const [prizeBusy, setPrizeBusy] = useState<string | null>(null)
  const [newPrize, setNewPrize] = useState({
    tier: '',
    displayName: '',
    kind: 'ALIPAY_CODE' as PrizeKind,
    payload: '',
  })
  const [redeemForm, setRedeemForm] = useState({
    tier: '',
    displayName: '',
    groupId: 0,
    validityDays: 1,
    count: 1,
  })

  const reloadDetail = async () => {
    if (id) setDetail(await api.detail(id))
  }

  // 排序号不再暴露给站长:新奖品一律自动接在清单末尾(行序即排序)
  const nextSort = () =>
    detail && detail.prizes.length ? Math.max(...detail.prizes.map((p) => p.sortOrder)) + 1 : 0

  const addPrize = async () => {
    if (!id) return
    if (!newPrize.tier.trim() || !newPrize.displayName.trim()) {
      setError(t('raffle.admin.validation.prizeTierNameRequired'))
      return
    }
    // 手填兑换码必须带码值:留空会造出既无码、又无「生成口令」按钮的死行
    if (newPrize.kind === 'REDEEM_CODE' && !newPrize.payload.trim()) {
      setError(t('raffle.admin.validation.redeemPayloadRequired'))
      return
    }
    setPrizeBusy('new')
    setError('')
    try {
      await api.addPrize(id, { ...newPrize, sortOrder: nextSort() })
      setNewPrize({ tier: '', displayName: '', kind: 'ALIPAY_CODE', payload: '' })
      await reloadDetail()
    } catch (e) {
      setError(String((e as Error).message))
    } finally {
      setPrizeBusy(null)
    }
  }

  const deletePrize = async (prizeId: string) => {
    if (!id || !confirm(t('raffle.admin.confirmDeletePrize'))) return
    setPrizeBusy(prizeId)
    try {
      await api.deletePrize(id, prizeId)
      await reloadDetail()
    } catch (e) {
      setError(String((e as Error).message))
    } finally {
      setPrizeBusy(null)
    }
  }

  const generateAlipay = async (existing?: PrizeT) => {
    if (!id) return
    // 从「手动添加」区新建口令奖品时,档位/展示名取自该区输入,同样必填
    if (!existing && (!newPrize.tier.trim() || !newPrize.displayName.trim())) {
      setError(t('raffle.admin.validation.prizeTierNameRequired'))
      return
    }
    setPrizeBusy(existing ? existing.id : 'alipay-new')
    setError('')
    try {
      await api.generateAlipayCode(id, {
        prizeId: existing?.id ?? null,
        tier: existing?.tier ?? newPrize.tier,
        displayName: existing?.displayName ?? newPrize.displayName,
        sortOrder: existing?.sortOrder ?? nextSort(),
      })
      await reloadDetail()
    } catch (e) {
      setError(String((e as Error).message))
    } finally {
      setPrizeBusy(null)
    }
  }

  // 逐行生成:空壳兑换码行只选分组,一行固定出 1 张码、有效期固定 1 天(日卡即本站奖品常态;
  // 非 1 天的少数情况走下方批量生成区的天数字段)
  const [rowGroup, setRowGroup] = useState<Record<string, string>>({})

  const generateRedeemForPrize = async (p: PrizeT) => {
    if (!id) return
    const groupId = Number(rowGroup[p.id] ?? '')
    if (!(groupId > 0)) {
      setError(t('raffle.admin.validation.redeemGroupIdPositive'))
      return
    }
    setPrizeBusy(p.id)
    setError('')
    try {
      await api.generateRedeemCodeForPrize(id, p.id, { groupId, validityDays: 1 })
      await reloadDetail()
    } catch (e) {
      setError(String((e as Error).message))
    } finally {
      setPrizeBusy(null)
    }
  }

  const generateRedeemCodes = async () => {
    if (!id) return
    if (!redeemForm.tier.trim() || !redeemForm.displayName.trim()) {
      setError(t('raffle.admin.validation.prizeTierNameRequired'))
      return
    }
    if (!(redeemForm.groupId > 0)) {
      setError(t('raffle.admin.validation.redeemGroupIdPositive'))
      return
    }
    if (redeemForm.validityDays < 1) {
      setError(t('raffle.admin.validation.redeemValidityMin'))
      return
    }
    // 与后端 GenerateRaffleRedeemCodesHandler 的 1~100 口径一致
    if (redeemForm.count < 1 || redeemForm.count > 100) {
      setError(t('raffle.admin.validation.redeemCountRange'))
      return
    }
    setPrizeBusy('redeem')
    setError('')
    try {
      await api.generateRedeemCodes(id, { ...redeemForm, sortOrderStart: nextSort() })
      await reloadDetail()
    } catch (e) {
      setError(String((e as Error).message))
    } finally {
      setPrizeBusy(null)
    }
  }

  if (forbidden) return <Alert tone="warning">{t('raffle.admin.forbidden')}</Alert>
  if (loading) return <Loading />

  return (
    <>
      <PageHead
        eyebrow={t('raffle.admin.eyebrow')}
        title={mode === 'create' ? t('raffle.admin.newCampaign') : scalars.name}
        sub={detail ? t(`raffle.admin.statuses.${detail.status}`) : undefined}
        status={detail && <Lamp status={detail.status} />}
      />
      {error && (
        <div className="mb-4">
          <ErrorBar msg={error} />
        </div>
      )}
      <Card className="p-6">
        <Input
          label={t('raffle.admin.fields.name')}
          value={scalars.name}
          disabled={!editable}
          onChange={(e) => setScalars({ ...scalars, name: e.target.value })}
        />
        <Cluster label={t('raffle.admin.clusters.timing')}>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <Input
              type="datetime-local"
              label={t('raffle.admin.fields.entryOpenAt')}
              disabled={!editable}
              value={scalars.entryOpenAt ? toLocalInput(scalars.entryOpenAt) : ''}
              onChange={(e) => setScalars({ ...scalars, entryOpenAt: toIso(e.target.value) })}
            />
            <Input
              type="datetime-local"
              label={t('raffle.admin.fields.entryCloseAt')}
              disabled={!editable}
              value={scalars.entryCloseAt ? toLocalInput(scalars.entryCloseAt) : ''}
              onChange={(e) => setScalars({ ...scalars, entryCloseAt: toIso(e.target.value) })}
            />
            <Input
              type="datetime-local"
              label={t('raffle.admin.fields.drawAt')}
              disabled={!editable}
              value={scalars.drawAt ? toLocalInput(scalars.drawAt) : ''}
              onChange={(e) => setScalars({ ...scalars, drawAt: toIso(e.target.value) })}
            />
          </div>
        </Cluster>
        <Cluster label={t('raffle.admin.clusters.gate')}>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <RfSelect
              label={t('raffle.admin.fields.gateType')}
              value={scalars.gateType}
              disabled={!editable}
              onChange={(e) => setScalars({ ...scalars, gateType: e.target.value as GateType })}
            >
              <option value="RECHARGE">{t('raffle.admin.gateTypes.RECHARGE')}</option>
              <option value="SPEND">{t('raffle.admin.gateTypes.SPEND')}</option>
            </RfSelect>
            <Input
              type="number"
              label={t('raffle.admin.fields.gateAmount')}
              disabled={!editable}
              value={scalars.gateAmount}
              onChange={(e) => setScalars({ ...scalars, gateAmount: Number(e.target.value) })}
            />
            <Input
              type="datetime-local"
              label={t('raffle.admin.fields.gateFrom')}
              disabled={!editable}
              value={scalars.gateFrom ? toLocalInput(scalars.gateFrom) : ''}
              onChange={(e) => setScalars({ ...scalars, gateFrom: toIso(e.target.value) })}
            />
          </div>
        </Cluster>
        <Cluster label={t('raffle.admin.clusters.weight')}>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <Input
              type="number"
              min={0}
              label={t('raffle.admin.fields.minAccountAgeDays')}
              hint={t('raffle.admin.fields.minAccountAgeDaysHint')}
              disabled={!editable}
              value={scalars.minAccountAgeDays ?? ''}
              onChange={(e) =>
                setScalars({
                  ...scalars,
                  minAccountAgeDays: e.target.value === '' ? null : Number(e.target.value),
                })
              }
            />
            <RfSelect
              label={t('raffle.admin.fields.weightMode')}
              value={scalars.weightMode}
              disabled={!editable}
              onChange={(e) => setScalars({ ...scalars, weightMode: e.target.value as WeightMode })}
            >
              <option value="EQUAL">{t('raffle.admin.weightModes.EQUAL')}</option>
              <option value="WEIGHTED">{t('raffle.admin.weightModes.WEIGHTED')}</option>
            </RfSelect>
          </div>
        </Cluster>
        <div className="rf-cluster flex items-center gap-3">
          {editable && (
            <Button variant="primary" disabled={saving} onClick={save}>
              {mode === 'create' ? t('raffle.admin.create') : t('raffle.admin.saveChanges')}
            </Button>
          )}
          {mode === 'edit' && detail?.status !== 'cancelled' && (
            <Button variant="secondary" disabled={saving} onClick={cancelCampaign}>
              {t('raffle.admin.cancelCampaign')}
            </Button>
          )}
        </div>
      </Card>

      {mode === 'edit' && detail && (
        <Card className="mt-6 p-6">
          <h2 className="font-display text-lg font-semibold">{t('raffle.admin.prizesTitle')}</h2>
          <div className="mt-4 overflow-hidden rounded-lg border border-snb-hairline">
            <table className="w-full text-[13px]">
              <thead className="bg-snb-well text-left">
                <tr>
                  <th className="rf-plate-label w-20 px-4 py-2.5 font-normal">{t('raffle.admin.fields.tier')}</th>
                  <th className="rf-plate-label px-3 py-2.5 font-normal">{t('raffle.admin.fields.displayName')}</th>
                  <th className="rf-plate-label px-3 py-2.5 font-normal">{t('raffle.admin.fields.payload')}</th>
                  <th className="w-14 px-4 py-2.5" />
                </tr>
              </thead>
              <tbody>
                {detail.prizes.map((p, i) => (
                  <tr key={p.id} className="rf-row border-t border-snb-hairline">
                    {/* 同档位成组:只在组首亮铭牌,行序即台账序 */}
                    <td className="px-4 py-2.5 align-middle">
                      {(i === 0 || detail.prizes[i - 1].tier !== p.tier) && (
                        <span className="rf-tier">{p.tier}</span>
                      )}
                    </td>
                    <td className="px-3 py-2.5 align-middle">{p.displayName}</td>
                    {/* 码值格:已生成=亮点+等宽码值;空壳=生成控件就地放在码值将出现的位置 */}
                    <td className="px-3 py-2 align-middle">
                      <div className="flex items-center gap-2.5">
                        <Jack lit={!!p.payload} />
                        {p.payload ? (
                          <span className="font-mono">{p.payload}</span>
                        ) : editable && p.kind === 'REDEEM_CODE' ? (
                          <>
                            {groupOptions.length > 0 ? (
                              <RfSelect
                                size="sm"
                                className="w-56 flex-none"
                                value={rowGroup[p.id] ?? ''}
                                onChange={(e) => setRowGroup({ ...rowGroup, [p.id]: e.target.value })}
                              >
                                <option value="">{t('raffle.admin.pickGroup')}</option>
                                {groupOptions.map((o) => (
                                  <option key={o.value} value={o.value}>
                                    {o.label}
                                  </option>
                                ))}
                              </RfSelect>
                            ) : (
                              <Input
                                className="w-24 flex-none"
                                type="number"
                                min={1}
                                placeholder={t('raffle.admin.fields.groupId')}
                                value={rowGroup[p.id] ?? ''}
                                onChange={(e) => setRowGroup({ ...rowGroup, [p.id]: e.target.value })}
                              />
                            )}
                            <Button
                              size="xs"
                              variant="secondary"
                              disabled={prizeBusy === p.id}
                              onClick={() => generateRedeemForPrize(p)}
                            >
                              {t('raffle.admin.generateCode')}
                            </Button>
                          </>
                        ) : editable && p.kind === 'ALIPAY_CODE' ? (
                          <Button
                            size="xs"
                            variant="secondary"
                            disabled={prizeBusy === p.id}
                            onClick={() => generateAlipay(p)}
                          >
                            {t('raffle.admin.generatePassphrase')}
                          </Button>
                        ) : (
                          <span className="text-snb-t3">{t('raffle.admin.pendingPayload')}</span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-2.5 text-right align-middle">
                      {editable && (
                        <button
                          type="button"
                          className="rf-del"
                          disabled={prizeBusy === p.id}
                          onClick={() => deletePrize(p.id)}
                        >
                          {t('raffle.admin.delete')}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {editable && (
            <>
              <Cluster label={t('raffle.admin.addPrizeManually')}>
                <div className="grid grid-cols-2 gap-2.5 md:grid-cols-[8rem_1fr_10rem_16rem]">
                  <Input
                    placeholder={t('raffle.admin.fields.tier')}
                    value={newPrize.tier}
                    onChange={(e) => setNewPrize({ ...newPrize, tier: e.target.value })}
                  />
                  <Input
                    placeholder={t('raffle.admin.fields.displayName')}
                    value={newPrize.displayName}
                    onChange={(e) => setNewPrize({ ...newPrize, displayName: e.target.value })}
                  />
                  <RfSelect
                    value={newPrize.kind}
                    onChange={(e) => setNewPrize({ ...newPrize, kind: e.target.value as PrizeKind })}
                  >
                    <option value="ALIPAY_CODE">{t('raffle.admin.kinds.ALIPAY_CODE')}</option>
                    <option value="REDEEM_CODE">{t('raffle.admin.kinds.REDEEM_CODE')}</option>
                  </RfSelect>
                  <Input
                    placeholder={t('raffle.admin.fields.payload')}
                    value={newPrize.payload}
                    onChange={(e) => setNewPrize({ ...newPrize, payload: e.target.value })}
                  />
                </div>
                <div className="flex gap-2">
                  <Button size="sm" variant="secondary" disabled={prizeBusy === 'new'} onClick={addPrize}>
                    {t('raffle.admin.addPrize')}
                  </Button>
                  <Button
                    size="sm"
                    variant="secondary"
                    disabled={prizeBusy === 'alipay-new'}
                    onClick={() => generateAlipay()}
                  >
                    {t('raffle.admin.generatePassphrase')}
                  </Button>
                </div>
              </Cluster>

              <Cluster label={t('raffle.admin.generateRedeemCodes')}>
                <div className="grid grid-cols-2 gap-2.5 md:grid-cols-[8rem_1fr_16rem_6rem_6rem]">
                  <Input
                    placeholder={t('raffle.admin.fields.tier')}
                    value={redeemForm.tier}
                    onChange={(e) => setRedeemForm({ ...redeemForm, tier: e.target.value })}
                  />
                  <Input
                    placeholder={t('raffle.admin.fields.displayName')}
                    value={redeemForm.displayName}
                    onChange={(e) => setRedeemForm({ ...redeemForm, displayName: e.target.value })}
                  />
                  {groupOptions.length > 0 ? (
                    <RfSelect
                      value={redeemForm.groupId ? String(redeemForm.groupId) : ''}
                      onChange={(e) => setRedeemForm({ ...redeemForm, groupId: Number(e.target.value) || 0 })}
                    >
                      <option value="">{t('raffle.admin.pickGroup')}</option>
                      {groupOptions.map((o) => (
                        <option key={o.value} value={o.value}>
                          {o.label}
                        </option>
                      ))}
                    </RfSelect>
                  ) : (
                    <Input
                      type="number"
                      min={1}
                      placeholder={t('raffle.admin.fields.groupId')}
                      value={redeemForm.groupId}
                      onChange={(e) => setRedeemForm({ ...redeemForm, groupId: Number(e.target.value) })}
                    />
                  )}
                  <Input
                    type="number"
                    min={1}
                    placeholder={t('raffle.admin.fields.validityDays')}
                    value={redeemForm.validityDays}
                    onChange={(e) => setRedeemForm({ ...redeemForm, validityDays: Number(e.target.value) })}
                  />
                  <Input
                    type="number"
                    min={1}
                    max={100}
                    placeholder={t('raffle.admin.fields.count')}
                    value={redeemForm.count}
                    onChange={(e) => setRedeemForm({ ...redeemForm, count: Number(e.target.value) })}
                  />
                </div>
                <Button size="sm" variant="primary" disabled={prizeBusy === 'redeem'} onClick={generateRedeemCodes}>
                  {t('raffle.admin.generate')}
                </Button>
              </Cluster>
            </>
          )}
        </Card>
      )}

      {mode === 'create' && cloneFrom && (
        <Card className="mt-6 p-6">
          <h2 className="font-display text-lg font-semibold">{t('raffle.admin.clonedPrizesTitle')}</h2>
          <p className="mt-1 text-sm text-snb-t3">{t('raffle.admin.clonedPrizesHint')}</p>
          <div className="mt-4 space-y-2">
            {prizeSkeleton.map((p, i) => (
              <div key={i} className="flex items-center gap-2.5">
                <Jack lit={false} />
                <Input
                  className="w-32 flex-none"
                  placeholder={t('raffle.admin.fields.tier')}
                  value={p.tier}
                  onChange={(e) =>
                    setPrizeSkeleton(prizeSkeleton.map((x, j) => (j === i ? { ...x, tier: e.target.value } : x)))
                  }
                />
                <Input
                  className="min-w-0 flex-1"
                  placeholder={t('raffle.admin.fields.displayName')}
                  value={p.displayName}
                  onChange={(e) =>
                    setPrizeSkeleton(prizeSkeleton.map((x, j) => (j === i ? { ...x, displayName: e.target.value } : x)))
                  }
                />
                <RfSelect
                  className="w-40 flex-none"
                  value={p.kind}
                  onChange={(e) =>
                    setPrizeSkeleton(
                      prizeSkeleton.map((x, j) => (j === i ? { ...x, kind: e.target.value as PrizeKind } : x)),
                    )
                  }
                >
                  <option value="ALIPAY_CODE">{t('raffle.admin.kinds.ALIPAY_CODE')}</option>
                  <option value="REDEEM_CODE">{t('raffle.admin.kinds.REDEEM_CODE')}</option>
                </RfSelect>
                <button
                  type="button"
                  className="rf-del flex-none"
                  onClick={() => setPrizeSkeleton(prizeSkeleton.filter((_, j) => j !== i))}
                >
                  {t('raffle.admin.delete')}
                </button>
              </div>
            ))}
          </div>
          <div className="mt-3">
            <Button
              size="sm"
              variant="secondary"
              onClick={() =>
                setPrizeSkeleton([
                  ...prizeSkeleton,
                  {
                    tier: '',
                    displayName: '',
                    kind: 'REDEEM_CODE',
                    sortOrder: prizeSkeleton.length ? Math.max(...prizeSkeleton.map((x) => x.sortOrder)) + 1 : 0,
                  },
                ])
              }
            >
              {t('raffle.admin.addPrize')}
            </Button>
          </div>
        </Card>
      )}
    </>
  )
}
