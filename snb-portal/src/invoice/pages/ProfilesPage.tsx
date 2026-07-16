import { useEffect, useState } from 'react'
import { Button, Card } from '../../ui'
import { t } from '../../i18n'
import { api, type ProfileT } from '../api'
import { ErrorBar, Loading, PageHead } from './shared'
import { EMPTY_DRAFT, ProfileFormModal, type ProfileDraft } from './ProfileFormModal'

/** 抬头管理(购买方信息):票据单元格卡 + 共享表单弹窗增改 + 删除(带确认)。 */
export function ProfilesPage() {
  const [rows, setRows] = useState<ProfileT[] | null>(null)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState<{ id: string | null; draft: ProfileDraft } | null>(null)

  const load = () => api.profiles().then(setRows).catch((e) => setError(String(e.message)))
  useEffect(() => {
    load()
  }, [])

  const remove = async (id: string) => {
    if (!window.confirm(t('invoice.profiles.confirmDel'))) return
    try {
      await api.deleteProfile(id)
      load()
    } catch (e) {
      setError(String((e as Error).message))
    }
  }

  const head = (
    <PageHead eyebrow={t('invoice.profiles.eyebrow')} title={t('invoice.tabs.profiles')} sub={t('invoice.profiles.sub')} />
  )

  if (!rows) return error ? <>{head}<ErrorBar msg={error} /></> : <>{head}<Loading /></>

  /** 单元格行:有值才成行;mono 控制税号/电话/账号的数字感 */
  const infoRows = (p: ProfileT): { label: string; value: string; mono?: boolean }[] =>
    [
      { label: t('invoice.profiles.taxNo'), value: p.taxNo || t('invoice.profiles.noTax'), mono: !!p.taxNo },
      { label: t('invoice.profiles.regAddress'), value: p.regAddress ?? '', mono: false },
      { label: t('invoice.profiles.regPhone'), value: p.regPhone ?? '', mono: true },
      { label: t('invoice.profiles.bankName'), value: p.bankName ?? '', mono: false },
      { label: t('invoice.profiles.bankAccount'), value: p.bankAccount ?? '', mono: true },
    ].filter((row) => row.value)

  return (
    <>
      {head}
      {error && <div className="mb-4"><ErrorBar msg={error} /></div>}
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {rows.map((p) => (
          <Card key={p.id} className="flex flex-col overflow-hidden p-0">
            <div className="iv-pf-head">
              <b className="min-w-0 flex-1 truncate text-[15px]">{p.title}</b>
              {p.verifiedAt && (
                <span className="iv-stamp-verified" title={t('invoice.profiles.verifiedTip')}>
                  {t('invoice.profiles.verifiedBadge')}
                </span>
              )}
              <span className={`iv-badge ${p.type === 'COMPANY' ? 'co' : 'me'}`}>
                {p.type === 'COMPANY' ? t('invoice.profiles.typeCompany') : t('invoice.profiles.typePersonal')}
              </span>
            </div>
            <div className="flex-1 px-4 py-1.5">
              {infoRows(p).map((row) => (
                <div key={row.label} className="iv-pf-row">
                  <span className="lb">{row.label}</span>
                  <span className={`vl ${row.mono ? 'font-mono text-[12.5px]' : ''} ${row.value === t('invoice.profiles.noTax') ? 'dim' : ''}`}>
                    {row.value}
                  </span>
                </div>
              ))}
            </div>
            <div className="flex justify-end gap-2 border-t border-snb-hairline px-4 py-2.5">
              <Button size="sm" variant="secondary" onClick={() => setEditing({ id: p.id, draft: { ...p } })}>
                {t('invoice.profiles.edit')}
              </Button>
              <Button size="sm" variant="ghost" onClick={() => remove(p.id)}>
                {t('invoice.profiles.del')}
              </Button>
            </div>
          </Card>
        ))}

        <button type="button" className="iv-pf-add" onClick={() => setEditing({ id: null, draft: { ...EMPTY_DRAFT } })}>
          <span className="text-center text-sm">
            <span className="mb-2 block text-3xl font-light leading-none">＋</span>
            {t('invoice.profiles.add')}
          </span>
        </button>
      </div>

      {editing && (
        <ProfileFormModal
          id={editing.id}
          initial={editing.draft}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            load()
          }}
        />
      )}
    </>
  )
}
