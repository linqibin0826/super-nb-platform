import { useEffect, useState } from 'react'
import { Badge, Button, Card, Input, Modal } from '../../ui'
import { t } from '../../i18n'
import { api, type ProfileT } from '../api'
import { ErrorBar, Loading } from './shared'

type Draft = Omit<ProfileT, 'id'>

const EMPTY: Draft = {
  type: 'COMPANY',
  title: '',
  taxNo: '',
  regAddress: '',
  regPhone: '',
  bankName: '',
  bankAccount: '',
}

/** 抬头管理:列表 + 弹窗增改 + 删除(带确认)。 */
export function ProfilesPage() {
  const [rows, setRows] = useState<ProfileT[] | null>(null)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState<{ id: string | null; draft: Draft } | null>(null)
  const [saving, setSaving] = useState(false)

  const load = () => api.profiles().then(setRows).catch((e) => setError(String(e.message)))
  useEffect(() => {
    load()
  }, [])

  const save = async () => {
    if (!editing) return
    setSaving(true)
    setError('')
    try {
      if (editing.id) await api.updateProfile(editing.id, editing.draft)
      else await api.createProfile(editing.draft)
      setEditing(null)
      load()
    } catch (e) {
      setError(String((e as Error).message))
    } finally {
      setSaving(false)
    }
  }

  const remove = async (id: string) => {
    if (!window.confirm(t('invoice.profiles.confirmDel'))) return
    try {
      await api.deleteProfile(id)
      load()
    } catch (e) {
      setError(String((e as Error).message))
    }
  }

  const set = (patch: Partial<Draft>) =>
    setEditing((cur) => (cur ? { ...cur, draft: { ...cur.draft, ...patch } } : cur))

  if (!rows) return error ? <ErrorBar msg={error} /> : <Loading />

  const fields: { key: keyof Draft; label: string; companyOnly?: boolean }[] = [
    { key: 'taxNo', label: t('invoice.profiles.taxNo'), companyOnly: true },
    { key: 'regAddress', label: t('invoice.profiles.regAddress'), companyOnly: true },
    { key: 'regPhone', label: t('invoice.profiles.regPhone'), companyOnly: true },
    { key: 'bankName', label: t('invoice.profiles.bankName'), companyOnly: true },
    { key: 'bankAccount', label: t('invoice.profiles.bankAccount'), companyOnly: true },
  ]

  return (
    <div className="space-y-3">
      {error && <ErrorBar msg={error} />}
      <Button variant="primary" onClick={() => setEditing({ id: null, draft: { ...EMPTY } })}>
        {t('invoice.profiles.add')}
      </Button>
      {rows.length === 0 && <Card className="p-8 text-center text-snb-t2">{t('invoice.profiles.empty')}</Card>}
      {rows.map((p) => (
        <Card key={p.id} className="flex items-center gap-3 p-4">
          <Badge tone={p.type === 'COMPANY' ? 'primary' : 'gray'}>
            {p.type === 'COMPANY' ? t('invoice.profiles.typeCompany') : t('invoice.profiles.typePersonal')}
          </Badge>
          <div className="flex-1">
            <div>{p.title}</div>
            {p.taxNo && <div className="font-mono text-xs text-snb-t2">{p.taxNo}</div>}
          </div>
          <Button size="sm" onClick={() => setEditing({ id: p.id, draft: { ...p } })}>
            {t('invoice.profiles.edit')}
          </Button>
          <Button size="sm" onClick={() => remove(p.id)}>{t('invoice.profiles.del')}</Button>
        </Card>
      ))}

      {editing && (
        <Modal open onClose={() => setEditing(null)} title={t('invoice.profiles.add')}>
          <div className="space-y-3">
            <div className="flex gap-2">
              {(['COMPANY', 'PERSONAL'] as const).map((type) => (
                <Button
                  key={type}
                  size="sm"
                  variant={editing.draft.type === type ? 'primary' : undefined}
                  onClick={() => set({ type })}
                >
                  {type === 'COMPANY' ? t('invoice.profiles.typeCompany') : t('invoice.profiles.typePersonal')}
                </Button>
              ))}
            </div>
            <Input
              value={editing.draft.title}
              onChange={(e) => set({ title: e.target.value })}
              placeholder={t('invoice.profiles.title')}
            />
            {fields
              .filter((f) => !f.companyOnly || editing.draft.type === 'COMPANY')
              .map((f) => (
                <Input
                  key={f.key}
                  value={editing.draft[f.key] ?? ''}
                  onChange={(e) => set({ [f.key]: e.target.value } as Partial<Draft>)}
                  placeholder={f.label}
                />
              ))}
            <div className="flex justify-end gap-2">
              <Button onClick={() => setEditing(null)}>{t('invoice.profiles.cancel')}</Button>
              <Button variant="primary" disabled={saving} onClick={save}>
                {t('invoice.profiles.save')}
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}
