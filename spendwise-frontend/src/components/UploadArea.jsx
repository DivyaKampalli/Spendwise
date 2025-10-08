// spendwise-frontend/src/components/UploadArea.jsx
import { useEffect, useMemo, useRef, useState } from 'react'
import { previewImport, commitImport, listCategories } from '../api'

const GROUPS = ['ESSENTIAL','SURPLUS','DEBT']

export default function UploadArea({ month, onCommitted }) {
  const [file, setFile] = useState(null)
  const [fileKey, setFileKey] = useState(0)
  const fileInputRef = useRef(null)

  const [preview, setPreview] = useState(null)
  const [overrides, setOverrides] = useState({})           // hash -> category name
  const [groupOverrides, setGroupOverrides] = useState({}) // hash -> ESSENTIAL|SURPLUS|DEBT
  const [excluded, setExcluded] = useState({})             // hash -> true means EXCLUDED

  const descRef = useRef({}) // hash -> current description text (uncontrolled inputs)

  const [statementType, setStatementType] = useState('debit')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  const [categories, setCategories] = useState([])

  useEffect(() => {
    listCategories().then(d => setCategories(d.categories || [])).catch(() => setCategories([]))
  }, [])

  const rows = preview?.rows || []
  const eligibleCount = useMemo(
    () => rows.filter(r => r.inTargetMonth && !excluded[r.hash]).length,
    [rows, excluded]
  )

  function resetFileInput() {
    setFile(null)
    if (fileInputRef.current) fileInputRef.current.value = ''
    setFileKey(k => k + 1)
  }
  function resetState() {
    setPreview(null)
    setOverrides({})
    setGroupOverrides({})
    setExcluded({})
    descRef.current = {}
    setMessage('')
    resetFileInput()
  }

  async function doPreview() {
    if (!file || !month) { setMessage('Select a month and choose a CSV.'); return }
    setMessage(''); setLoading(true)
    try {
      const data = await previewImport(file, month, statementType)
      setPreview(data)
      setOverrides({})
      setGroupOverrides({})
      // initialize description ref and default exclusions
      const initExcluded = {}
      for (const r of (data.rows || [])) {
        descRef.current[r.hash] = r.description || ''
        if (!r.inTargetMonth) initExcluded[r.hash] = true
      }
      setExcluded(initExcluded)
    } catch (e) {
      setMessage(e.message || 'Preview failed')
    } finally { setLoading(false) }
  }

  async function doCommit() {
    if (!file || !month) return
    setLoading(true); setMessage('')
    try {
      const excludeHashes = Object.entries(excluded).filter(([,v]) => v).map(([h]) => h)

      // Build description overrides only where text differs from original preview
      const descOverrides = {}
      for (const r of rows) {
        const cur = (descRef.current[r.hash] ?? '')
        if ((r.description || '') !== cur) descOverrides[r.hash] = cur
      }

      const res = await commitImport(
        file, month, overrides, statementType, excludeHashes, descOverrides, groupOverrides
      )
      setMessage(`Inserted ${res.inserted}, skipped ${res.skipped}.`)
      resetState(); onCommitted?.(month)
    } catch (e) {
      setMessage(e.message || 'Commit failed')
    } finally { setLoading(false) }
  }

  // helpers
  function currentCategoryName(row) {
    return overrides[row.hash] ?? row.suggestedCategory ?? 'Uncategorized'
  }
  function categoryMetaByName(name) {
    return categories.find(c => c.name.toLowerCase() === (name || '').toLowerCase())
  }

  function onChangeCategory(row, name) {
    const suggested = row.suggestedCategory ?? 'Uncategorized'
    setOverrides(prev => {
      const next = { ...prev }
      if (!name || name === suggested) delete next[row.hash]
      else next[row.hash] = name
      return next
    })
    const meta = categoryMetaByName(name)
    if (meta?.isIncome) {
      setGroupOverrides(prev => { const n = { ...prev }; delete n[row.hash]; return n })
    }
  }

  function CategorySelect({ row }) {
    const value = currentCategoryName(row)
    const grouped = {
      INCOME:    categories.filter(c =>  c.isIncome),
      ESSENTIAL: categories.filter(c => !c.isIncome && c.group === 'ESSENTIAL'),
      SURPLUS:   categories.filter(c => !c.isIncome && (c.group === 'SURPLUS' || c.group == null)),
      DEBT:      categories.filter(c => !c.isIncome && c.group === 'DEBT'),
    }
    // Ensure the current value exists in options (in case list hasn’t loaded it yet)
    const inAnyGroup = Object.values(grouped).some(arr => arr.some(c => c.name.toLowerCase() === value.toLowerCase()))
    return (
      <select className="select" value={value} onChange={e => onChangeCategory(row, e.target.value)}>
        {!inAnyGroup && value && <option value={value}>{value}</option>}
        {grouped.INCOME.length > 0 && (
          <optgroup label="INCOME">
            {grouped.INCOME.map(c => <option key={c.id} value={c.name}>{c.name}</option>)}
          </optgroup>
        )}
        <optgroup label="ESSENTIAL">
          {grouped.ESSENTIAL.map(c => <option key={c.id} value={c.name}>{c.name}</option>)}
        </optgroup>
        <optgroup label="SURPLUS">
          {grouped.SURPLUS.map(c => <option key={c.id} value={c.name}>{c.name}</option>)}
        </optgroup>
        <optgroup label="DEBT">
          {grouped.DEBT.map(c => <option key={c.id} value={c.name}>{c.name}</option>)}
        </optgroup>
      </select>
    )
  }

  function GroupSelect({ row }) {
    const catName = currentCategoryName(row)
    const meta = categoryMetaByName(catName)
    const disabled = meta?.isIncome === true
    const value = (groupOverrides[row.hash] ??
                  (meta && !meta.isIncome ? (meta.group || 'SURPLUS') :
                   (row.categoryGroup || 'SURPLUS')))
    function onChange(e) {
      setGroupOverrides(prev => ({ ...prev, [row.hash]: e.target.value }))
    }
    return (
      <select className="select" value={value} onChange={onChange} disabled={disabled} title={disabled ? 'Not applicable for Income' : ''}>
        {GROUPS.map(g => <option key={g} value={g}>{g.charAt(0) + g.slice(1).toLowerCase()}</option>)}
      </select>
    )
  }

  function DescriptionInput({ row }) {
    // Uncontrolled input: defaultValue + update ref; no re-render on each keystroke
    return (
      <input
        className="input"
        style={{ width: 320 }}
        defaultValue={descRef.current[row.hash] ?? row.description ?? ''}
        onChange={e => { descRef.current[row.hash] = e.target.value }}
      />
    )
  }

  return (
    <div className="card">
      <div className="flex-spread">
        <div className="h2">Upload Statement</div>
        <div className="small">CSV only</div>
      </div>

      <div className="row" style={{marginTop:8}}>
        <label className="small">Statement:</label>
        <label>
          <input type="radio" name="st" value="debit"
                 checked={statementType === 'debit'}
                 onChange={() => setStatementType('debit')} />
          &nbsp;Debit card (charges negative)
        </label>
        <label>
          <input type="radio" name="st" value="credit"
                 checked={statementType === 'credit'}
                 onChange={() => setStatementType('credit')} />
          &nbsp;Credit card (flip all signs)
        </label>
      </div>

      <div className="row" style={{marginTop:8}}>
        <input
          key={fileKey}
          ref={fileInputRef}
          type="file"
          accept=".csv"
          onClick={e => { e.currentTarget.value = null }}
          onChange={e => setFile(e.target.files?.[0] || null)}
        />
        <button onClick={doPreview} disabled={!file || !month || loading}>Preview</button>
        <button className="secondary" onClick={resetState} disabled={loading}>Reset</button>
      </div>

      {message && (<div style={{marginTop:8}} className="small">{message}</div>)}

      {rows.length > 0 && (
        <>
          <div className="hr" />
          <div className="flex-spread">
            <div className="h2">Preview</div>
            <div className="small">{rows.length} rows · Eligible to insert: {eligibleCount}</div>
          </div>
          <div style={{overflowX:'auto'}}>
            <table className="table">
              <thead>
                <tr>
                  <th>Include</th>
                  <th>Date</th>
                  <th>Description</th>
                  <th>Amount</th>
                  <th>Category</th>
                  <th>Group</th>
                  <th>In Month</th>
                </tr>
              </thead>
              <tbody>
                {rows.map(r => {
                  const included = !(excluded[r.hash] ?? false)
                  return (
                    <tr key={r.hash}>
                      <td>
                        <input
                          type="checkbox"
                          checked={included}
                          onChange={e => {
                            const include = e.target.checked
                            setExcluded(prev => ({ ...prev, [r.hash]: !include }))
                          }}
                        />
                      </td>
                      <td>{r.date}</td>
                      <td><DescriptionInput row={r} /></td>
                      <td>{r.amount}</td>
                      <td><CategorySelect row={r} /></td>
                      <td><GroupSelect row={r} /></td>
                      <td>{r.inTargetMonth ? <span className="badge ok">yes</span> : <span className="badge warn">no</span>}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          <div className="row" style={{marginTop:12}}>
            <button onClick={doCommit} disabled={eligibleCount === 0 || loading}>
              Commit {eligibleCount} rows
            </button>
          </div>
        </>
      )}
    </div>
  )
}
