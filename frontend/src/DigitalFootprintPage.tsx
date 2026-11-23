import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, ApiError } from './api'
import type {
  BrowserIntegration, CookieRecord, PersonalDataRecord, PrivacyMetrics, PrivacyPreference,
  PrivacyRequest, PrivacyRequestEvidence, PrivacyRequestHistory, PrivacyRequestStatus, PrivacyRequestType,
  TrackedWebsite, WebsiteAnalysis,
} from './types'
import { privacyRequestStatusLabels, privacyRequestTypeLabels } from './types'

type Tab = 'overview' | 'websites' | 'data' | 'cookies' | 'requests' | 'preferences' | 'assistant' | 'account'
interface Finding { type: string; name: string; description: string | null; evidence: string | null; confidence: string | null }

const requestTypes = Object.keys(privacyRequestTypeLabels) as PrivacyRequestType[]
const dataCategories = ['BASIC_CONTACT', 'ACCOUNT', 'FINANCIAL', 'IDENTITY', 'HEALTH', 'LOCATION', 'BEHAVIOURAL', 'OTHER']
const sources = ['USER_CONFIRMED', 'OBSERVED_ON_WEBSITE', 'DECLARED_IN_POLICY', 'INFERRED_BY_AI', 'UNKNOWN']

function message(error: unknown) { return error instanceof ApiError ? error.message : error instanceof Error ? error.message : 'Something went wrong. Please try again.' }

export function DigitalFootprintPage() {
  const [tab, setTab] = useState<Tab>('overview')
  const [websites, setWebsites] = useState<TrackedWebsite[]>([])
  const [metrics, setMetrics] = useState<PrivacyMetrics | null>(null)
  const [records, setRecords] = useState<PersonalDataRecord[]>([])
  const [cookies, setCookies] = useState<CookieRecord[]>([])
  const [requests, setRequests] = useState<PrivacyRequest[]>([])
  const [preferences, setPreferences] = useState<PrivacyPreference | null>(null)
  const [integrations, setIntegrations] = useState<BrowserIntegration[]>([])
  const [analyses, setAnalyses] = useState<WebsiteAnalysis[]>([])
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')

  const load = useCallback(async () => {
    setBusy(true); setError('')
    try {
      const [sitePage, metricData, recordData, cookieData, requestData, prefData, integrationData, analysisPage] = await Promise.all([
        api.digitalFootprintWebsites(), api.digitalFootprintMetrics(), api.allDataRecords(), api.cookies(), api.privacyRequests(), api.privacyPreferences(), api.browserIntegrations(), api.websiteAnalyses(0, 50),
      ])
      setWebsites(sitePage.content); setMetrics(metricData); setRecords(recordData); setCookies(cookieData); setRequests(requestData); setPreferences(prefData); setIntegrations(integrationData); setAnalyses(analysisPage.content)
    } catch (e) { setError(message(e)) } finally { setBusy(false) }
  }, [])

  useEffect(() => { void load() }, [load])
  useEffect(() => { if (!toast) return; const timer = window.setTimeout(() => setToast(''), 4000); return () => window.clearTimeout(timer) }, [toast])

  const websiteById = useMemo(() => new Map(websites.map(w => [w.id, w])), [websites])

  const assistantContext = useMemo(() => JSON.stringify({
    websites: websites.map(w => ({ name: w.name, domain: w.domain, category: w.category, riskLevel: w.riskLevel, accountStatus: w.accountStatus, dataCategories: w.dataCategories, trackingTechnologies: w.trackingTechnologies })),
    personalData: records.map(r => ({ website: websiteById.get(r.trackedWebsiteId)?.name ?? r.trackedWebsiteId, category: r.dataCategory, type: r.dataType, source: r.source, confirmedByUser: r.confirmedByUser, observedOnWebsite: r.observedOnWebsite, declaredInPolicy: r.declaredInPolicy, inferredByAi: r.inferredByAi })),
    cookies: cookies.map(c => ({ name: c.cookieName, domain: c.domain, category: c.category, trackingRelated: c.trackingRelated })),
    requests: requests.map(r => ({ website: r.trackedWebsiteId ? websiteById.get(r.trackedWebsiteId)?.name : null, type: r.requestType, status: r.status, userVerified: r.userVerified, websiteClaimedDeletion: r.websiteClaimedDeletion })),
  }), [websites, records, cookies, requests, websiteById])

  return (
    <div className="df-page">
      <div className="page-title-row">
        <div><span className="eyebrow">YOUR PRIVACY</span><h1>My Digital Footprint</h1><p>Understand where you shared information, what may be tracked, and how to ask for removal.</p></div>
        <button className="button button-outline button-small" onClick={() => void load()} disabled={busy}>Refresh</button>
      </div>
      <nav className="df-tabs" aria-label="Digital footprint sections">
        {([['overview', 'Dashboard'], ['websites', 'My websites'], ['data', 'Personal data'], ['cookies', 'Cookies & trackers'], ['requests', 'Remove my data'], ['preferences', 'Preferences'], ['assistant', 'Privacy assistant'], ['account', 'Export & delete']] as [Tab, string][]).map(([key, label]) => (
          <button key={key} className={tab === key ? 'df-tab df-tab-active' : 'df-tab'} onClick={() => setTab(key)}>{label}</button>
        ))}
      </nav>
      {error && <div role="alert" className="error-banner"><span>{error}</span><button onClick={() => void load()}>Try again</button></div>}
      {toast && <p role="status" className="df-toast">{toast}</p>}

      {tab === 'overview' && <Overview metrics={metrics} websites={websites} requests={requests} busy={busy} onNavigate={setTab} />}
      {tab === 'websites' && <Websites websites={websites} analyses={analyses} busy={busy} onChanged={() => { setToast('Websites updated.'); void load() }} onError={setError} />}
      {tab === 'data' && <PersonalData websites={websites} records={records} onChanged={() => { setToast('Personal data inventory updated.'); void load() }} onError={setError} />}
      {tab === 'cookies' && <Cookies websites={websites} cookies={cookies} integrations={integrations} onChanged={() => { setToast('Cookie metadata updated.'); void load() }} onError={setError} />}
      {tab === 'requests' && <Requests websites={websites} requests={requests} onChanged={() => { setToast('Privacy request updated.'); void load() }} onError={setError} />}
      {tab === 'preferences' && <Preferences preferences={preferences} onChanged={setPreferences} onError={setError} />}
      {tab === 'assistant' && <Assistant context={assistantContext} onError={setError} />}
      {tab === 'account' && <AccountData onChanged={() => { setToast('Privacy records deleted.'); void load() }} onError={setError} />}
    </div>
  )
}

function Overview({ metrics, websites, requests, busy, onNavigate }: { metrics: PrivacyMetrics | null; websites: TrackedWebsite[]; requests: PrivacyRequest[]; busy: boolean; onNavigate: (tab: Tab) => void }) {
  if (busy && !metrics) return <p className="df-muted">Loading your privacy dashboard…</p>
  const pending = requests.filter(r => ['SUBMITTED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'NEEDS_VERIFICATION'].includes(r.status))
  return <div className="df-stack">
    <section className="df-metric-grid">
      <Metric label="Tracked websites" value={metrics?.totalWebsites ?? websites.length} />
      <Metric label="With sensitive data" value={metrics?.sensitiveWebsites ?? 0} tone="warn" />
      <Metric label="With trackers" value={metrics?.websitesWithTrackers ?? 0} tone="warn" />
      <Metric label="Pending deletions" value={metrics?.pendingDeletions ?? 0} tone="warn" />
      <Metric label="Completed deletions" value={metrics?.completedDeletions ?? 0} tone="good" />
      <Metric label="Outstanding actions" value={metrics?.outstandingActions ?? 0} />
    </section>
    <section className="panel df-panel">
      <div className="panel-heading"><div><h2>What to do next</h2><p>Clear, small steps — not legal advice.</p></div></div>
      <ul className="df-next-list">
        {pending.length > 0 && <li><b>{pending.length} request(s) awaiting a response.</b> Record website responses and follow up when a reminder is due. <button className="df-link" onClick={() => onNavigate('requests')}>Review requests</button></li>}
        {(metrics?.sensitiveWebsites ?? 0) > 0 && <li><b>Sensitive categories are recorded.</b> Confirm whether each record was shared by you, observed, or only declared in a policy. <button className="df-link" onClick={() => onNavigate('data')}>Open inventory</button></li>}
        {(metrics?.trackerCookies ?? 0) > 0 && <li><b>{metrics?.trackerCookies} tracking cookies</b> were synced as metadata. Remove them in your browser with the extension. <button className="df-link" onClick={() => onNavigate('cookies')}>Manage cookies</button></li>}
        <li><b>Verify before you trust.</b> A website's deletion claim is evidence of its claim, not proof that every copy is erased. Mark completion only after you verify. <button className="df-link" onClick={() => onNavigate('requests')}>Open requests</button></li>
      </ul>
    </section>
    <p className="df-disclaimer">Link Context stores category-level records you create. It does not store passwords, cookie values, session tokens, full card numbers or identity numbers.</p>
  </div>
}

function Metric({ label, value, tone }: { label: string; value: number; tone?: 'good' | 'warn' }) {
  return <div className={`df-metric ${tone ? `df-metric-${tone}` : ''}`}><span>{label}</span><strong>{value.toLocaleString()}</strong></div>
}

function Websites({ websites, analyses, busy, onChanged, onError }: { websites: TrackedWebsite[]; analyses: WebsiteAnalysis[]; busy: boolean; onChanged: () => void; onError: (m: string) => void }) {
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [risk, setRisk] = useState('')
  const [deletion, setDeletion] = useState('')
  const [sort, setSort] = useState('recent')
  const [form, setForm] = useState({ name: '', domain: '', url: '', category: '' })
  const [analysisId, setAnalysisId] = useState('')
  const [working, setWorking] = useState(false)

  const visible = useMemo(() => {
    const q = search.trim().toLowerCase()
    const filtered = websites.filter(w => (!q || w.name.toLowerCase().includes(q) || w.domain.toLowerCase().includes(q))
      && (!category || w.category?.toUpperCase() === category) && (!risk || w.riskLevel?.toUpperCase() === risk)
      && (!deletion || w.deletionRequestStatus?.toUpperCase() === deletion))
    if (sort === 'risk') return [...filtered].sort((a, b) => rank(b.riskLevel) - rank(a.riskLevel))
    return [...filtered].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  }, [websites, search, category, risk, deletion, sort])

  async function addManual() {
    if (!form.name.trim() || !form.domain.trim() || !form.url.trim()) { onError('Name, domain and URL are required.'); return }
    setWorking(true)
    try { await api.createDigitalFootprintWebsite({ ...form, category: form.category || undefined }); setForm({ name: '', domain: '', url: '', category: '' }); onChanged() }
    catch (e) { onError(message(e)) } finally { setWorking(false) }
  }

  async function importAnalysis() {
    const analysis = analyses.find(a => a.id === analysisId)
    if (!analysis) { onError('Choose a completed website analysis to import.'); return }
    setWorking(true)
    try {
      const findings = await api.websiteAnalysisFindings(analysis.id) as Finding[]
      const trackers = [...new Set(findings.filter(f => f.type === 'TRACKER').map(f => f.name))]
      const cats = [...new Set(findings.filter(f => f.type === 'INPUT_FIELD').map(f => f.description ?? f.name))]
      const domain = analysis.domain ?? new URL(analysis.url).hostname
      await api.importDigitalFootprintWebsite({
        analysisId: analysis.id, name: analysis.title || domain, domain, url: analysis.url,
        category: analysis.category ?? undefined, riskLevel: analysis.riskLevel ?? undefined,
        dataCategories: cats.length ? cats.join(', ') : undefined, trackingTechnologies: trackers.length ? trackers.join(', ') : undefined,
      })
      onChanged()
    } catch (e) { onError(message(e)) } finally { setWorking(false) }
  }

  async function update(id: string, field: string, value: string) {
    try { await api.updateDigitalFootprintWebsite(id, { [field]: value || null }); onChanged() }
    catch (e) { onError(message(e)) }
  }

  async function discover(website: TrackedWebsite) {
    if (!website.fromAnalysisId) { onError('This website was not imported from an analysis, so there is nothing to discover.'); return }
    setWorking(true)
    try {
      const findings = await api.websiteAnalysisFindings(website.fromAnalysisId) as Finding[]
      const pick = (type: string) => findings.find(f => f.type === type)?.evidence ?? undefined
      await api.updatePrivacyContact(website.id, {
        privacyPolicyUrl: pick('PRIVACY_POLICY'), deletionPolicyUrl: pick('DATA_DELETION_PAGE'),
        accessRequestUrl: pick('DATA_ACCESS_PAGE'), accountDeletionUrl: pick('ACCOUNT_DELETION_PAGE'),
        cookieManagementUrl: pick('COOKIE_MANAGEMENT_PAGE'), consentManagementUrl: pick('CONSENT_MANAGEMENT_PAGE'),
        privacyContactEmail: findings.find(f => f.type === 'PRIVACY_CONTACT_EMAIL')?.name,
        dpoContact: pick('DPO_CONTACT'), sourceUrls: JSON.stringify(findings.filter(f => f.type.startsWith('PRIVACY') || f.type.endsWith('_PAGE')).map(f => f.evidence)),
      })
      await api.updateDigitalFootprintWebsite(website.id, { privacyPolicyUrl: pick('PRIVACY_POLICY') ?? null, deletionPolicyUrl: pick('DATA_DELETION_PAGE') ?? null })
      onChanged()
    } catch (e) { onError(message(e)) } finally { setWorking(false) }
  }

  return <div className="df-stack">
    <section className="panel df-panel">
      <div className="panel-heading"><div><h2>Add a website</h2><p>Add manually, or import an existing Website Intelligence analysis.</p></div></div>
      <div className="df-form-grid">
        <label className="form-label">Name<input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="Example Bank" /></label>
        <label className="form-label">Domain<input value={form.domain} onChange={e => setForm({ ...form, domain: e.target.value })} placeholder="example.com" /></label>
        <label className="form-label">URL<input value={form.url} onChange={e => setForm({ ...form, url: e.target.value })} placeholder="https://example.com" /></label>
        <label className="form-label">Category<input value={form.category} onChange={e => setForm({ ...form, category: e.target.value })} placeholder="Finance" /></label>
      </div>
      <button className="button button-dark button-small" onClick={() => void addManual()} disabled={working}>Add website</button>
      <hr className="df-rule" />
      <div className="df-inline">
        <label className="form-label df-grow">Import from analysis
          <select value={analysisId} onChange={e => setAnalysisId(e.target.value)}>
            <option value="">Choose an analysis…</option>
            {analyses.map(a => <option key={a.id} value={a.id}>{a.title || a.domain || a.url} — {a.status}</option>)}
          </select>
        </label>
        <button className="button button-outline button-small" onClick={() => void importAnalysis()} disabled={working || !analysisId}>Import</button>
      </div>
    </section>

    <div className="toolbar df-toolbar">
      <input className="search-input" aria-label="Search websites" placeholder="Search websites" value={search} onChange={e => setSearch(e.target.value)} />
      <select aria-label="Filter category" value={category} onChange={e => setCategory(e.target.value)}><option value="">All categories</option>{[...new Set(websites.map(w => w.category).filter(Boolean))].map(c => <option key={c!} value={c!}>{c}</option>)}</select>
      <select aria-label="Filter risk" value={risk} onChange={e => setRisk(e.target.value)}><option value="">All risk levels</option>{['HIGH', 'MEDIUM', 'LOW', 'UNKNOWN'].map(r => <option key={r} value={r}>{r}</option>)}</select>
      <select aria-label="Filter deletion status" value={deletion} onChange={e => setDeletion(e.target.value)}><option value="">All deletion states</option>{['DRAFT', 'SUBMITTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'].map(s => <option key={s} value={s}>{s}</option>)}</select>
      <select aria-label="Sort websites" value={sort} onChange={e => setSort(e.target.value)}><option value="recent">Recently added</option><option value="risk">Risk level</option></select>
    </div>

    {busy ? <p className="df-muted">Loading websites…</p> : visible.length === 0 ? <div className="empty-state empty-card"><h3>No websites yet</h3><p>Add a website or import a completed analysis to start your footprint.</p></div> : <div className="df-stack">
      {visible.map(w => <section key={w.id} className="panel df-site-card">
        <div className="df-site-head">
          <div className="df-site-identity">
            <span className="df-site-logo" aria-hidden="true">{(w.name || w.domain || '?').slice(0, 1).toUpperCase()}</span>
            <div><h3>{w.name}</h3><p className="df-muted">{w.domain} · {w.category || 'Uncategorised'} · risk {w.riskLevel || 'unknown'}</p></div>
          </div>
          <div className="df-row-actions">
            {w.fromAnalysisId && <button className="button button-outline button-small" onClick={() => void discover(w)} disabled={working}>Discover privacy contacts</button>}
            <button className="button button-delete button-small" onClick={async () => { if (!confirm(`Remove ${w.name} from your dashboard? This does not delete data held by the website.`)) return; try { await api.deleteDigitalFootprintWebsite(w.id); onChanged() } catch (e) { onError(message(e)) } }}>Remove</button>
          </div>
        </div>
        <div className="df-form-grid">
          <label className="form-label">Risk<select value={w.riskLevel ?? ''} onChange={e => void update(w.id, 'riskLevel', e.target.value)}><option value="">Unknown</option>{['HIGH', 'MEDIUM', 'LOW'].map(r => <option key={r} value={r}>{r}</option>)}</select></label>
          <label className="form-label">Account status<input defaultValue={w.accountStatus ?? ''} onBlur={e => { if (e.target.value !== (w.accountStatus ?? '')) void update(w.id, 'accountStatus', e.target.value) }} placeholder="active / closed" /></label>
          <label className="form-label">Privacy policy URL<input defaultValue={w.privacyPolicyUrl ?? ''} onBlur={e => { if (e.target.value !== (w.privacyPolicyUrl ?? '')) void update(w.id, 'privacyPolicyUrl', e.target.value) }} placeholder="https://…" /></label>
          <label className="form-label">Deletion policy URL<input defaultValue={w.deletionPolicyUrl ?? ''} onBlur={e => { if (e.target.value !== (w.deletionPolicyUrl ?? '')) void update(w.id, 'deletionPolicyUrl', e.target.value) }} placeholder="https://…" /></label>
        </div>
        <div className="df-badges">
          {w.dataCategories && <span className="df-badge">Data: {w.dataCategories}</span>}
          {w.trackingTechnologies && <span className="df-badge df-badge-warn">Trackers: {w.trackingTechnologies}</span>}
          {w.deletionRequestStatus && <span className="df-badge">Deletion: {w.deletionRequestStatus}</span>}
          <span className="df-badge df-badge-muted">First recorded {formatDate(w.firstRecordedAt ?? w.createdAt)}</span>
        </div>
      </section>)}
    </div>}
  </div>
}

function PersonalData({ websites, records, onChanged, onError }: { websites: TrackedWebsite[]; records: PersonalDataRecord[]; onChanged: () => void; onError: (m: string) => void }) {
  const [websiteId, setWebsiteId] = useState('')
  const [form, setForm] = useState({ dataCategory: 'BASIC_CONTACT', dataType: '', description: '', source: 'USER_CONFIRMED', dateShared: '', evidenceSource: '' })
  const filtered = records.filter(r => !websiteId || r.trackedWebsiteId === websiteId)
  const byId = new Map(websites.map(w => [w.id, w]))

  async function add() {
    if (!websiteId) { onError('Choose a website first.'); return }
    if (!form.dataType.trim()) { onError('Data type is required (for example: email address).'); return }
    try {
      await api.createDataRecord(websiteId, { dataCategory: form.dataCategory, dataType: form.dataType, description: form.description || undefined, source: form.source, dateShared: form.dateShared || undefined, evidenceSource: form.evidenceSource || undefined })
      setForm({ ...form, dataType: '', description: '', dateShared: '', evidenceSource: '' }); onChanged()
    } catch (e) { onError(message(e)) }
  }

  return <div className="df-stack">
    <section className="panel df-panel">
      <div className="panel-heading"><div><h2>Record what you shared</h2><p>Record the category, not the secret. Never enter a password, full card number or ID number.</p></div></div>
      <div className="df-form-grid">
        <label className="form-label">Website<select value={websiteId} onChange={e => setWebsiteId(e.target.value)}><option value="">Choose…</option>{websites.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}</select></label>
        <label className="form-label">Category<select value={form.dataCategory} onChange={e => setForm({ ...form, dataCategory: e.target.value })}>{dataCategories.map(c => <option key={c} value={c}>{c}</option>)}</select></label>
        <label className="form-label">Type<input value={form.dataType} onChange={e => setForm({ ...form, dataType: e.target.value })} placeholder="email address" /></label>
        <label className="form-label">Evidence source<select value={form.source} onChange={e => setForm({ ...form, source: e.target.value })}>{sources.map(s => <option key={s} value={s}>{s.replaceAll('_', ' ').toLowerCase()}</option>)}</select></label>
        <label className="form-label">Date shared<input type="date" value={form.dateShared} onChange={e => setForm({ ...form, dateShared: e.target.value })} /></label>
        <label className="form-label">Notes (no secrets)<input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="Optional description" /></label>
      </div>
      <button className="button button-dark button-small" onClick={() => void add()}>Add record</button>
    </section>

    <div className="toolbar df-toolbar"><select aria-label="Filter by website" value={websiteId} onChange={e => setWebsiteId(e.target.value)}><option value="">All websites</option>{websites.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}</select></div>
    {filtered.length === 0 ? <div className="empty-state empty-card"><h3>No personal data recorded</h3><p>Add categories above. AI inferences are labelled and never treated as confirmed facts.</p></div> : <div className="df-stack">{filtered.map(r => <article key={r.id} className="panel df-record">
      <div className="df-site-head"><div><h3>{r.dataType} <span className="df-muted">· {r.dataCategory}</span></h3><p className="df-muted">{byId.get(r.trackedWebsiteId)?.name ?? 'Website'} · {sourceLabel(r.source)}</p></div><button className="button button-delete button-small" onClick={async () => { try { await api.deleteDataRecord(r.id); onChanged() } catch (e) { onError(message(e)) } }}>Delete</button></div>
      {r.description && <p>{r.description}</p>}
      <div className="df-badges">
        {r.confirmedByUser && <span className="df-badge df-badge-good">Confirmed by you</span>}
        {r.observedOnWebsite && <span className="df-badge">Observed on website</span>}
        {r.declaredInPolicy && <span className="df-badge">Declared in policy</span>}
        {r.inferredByAi && <span className="df-badge df-badge-warn">AI inference — unconfirmed</span>}
        {r.dateShared && <span className="df-badge df-badge-muted">Shared {formatDate(r.dateShared)}</span>}
        {r.evidenceSource && <span className="df-badge df-badge-muted">Evidence: {r.evidenceSource}</span>}
      </div>
    </article>)}</div>}
  </div>
}

function Cookies({ websites, cookies, integrations, onChanged, onError }: { websites: TrackedWebsite[]; cookies: CookieRecord[]; integrations: BrowserIntegration[]; onChanged: () => void; onError: (m: string) => void }) {
  const [domain, setDomain] = useState('')
  const [onlyTracking, setOnlyTracking] = useState(false)
  const visible = cookies.filter(c => (!domain || c.domain === domain) && (!onlyTracking || c.trackingRelated))
  const domains = [...new Set(cookies.map(c => c.domain))].sort()
  const siteFor = (cookieDomain: string) => websites.find(w => cookieDomain === w.domain || cookieDomain.endsWith(`.${w.domain}`) || w.domain.endsWith(`.${cookieDomain}`))

  async function remove(cookie: CookieRecord) {
    if (!confirm(`Remove the stored metadata for “${cookie.cookieName}”? This does not delete the cookie from your browser. Use the extension or your browser settings to delete it.`)) return
    try { const result = await api.deleteCookieMetadata(cookie.id); onChanged(); alert(result.message) } catch (e) { onError(message(e)) }
  }

  return <div className="df-stack">
    <section className="panel df-panel">
      <div className="panel-heading"><div><h2>Browser extension</h2><p>Cookies are browser- and device-specific. This page shows metadata only — never cookie values.</p></div></div>
      <p className="df-muted">Install the Link Context browser extension to sync cookie <b>metadata</b> (name, domain, category, flags) for domains you allow. The extension never uploads cookie values, session identifiers or tokens.</p>
      <div className="df-badges">{integrations.length === 0 ? <span className="df-badge df-badge-muted">No browser integration registered yet</span> : integrations.map(i => <span key={i.id} className="df-badge">{i.browserName || 'Browser'} · {i.deviceLabel || i.installationId} · last sync {i.lastSyncAt ? formatDate(i.lastSyncAt) : 'never'}</span>)}</div>
      <details className="df-details"><summary>Manual mode: delete cookies without the extension</summary><ol><li>Open your browser settings → Privacy and security → Cookies and site data.</li><li>Search for the website's domain and remove its cookies.</li><li>This logs you out of that site and resets its preferences. It does not delete your account on the server.</li></ol></details>
    </section>

    <div className="toolbar df-toolbar">
      <select aria-label="Filter cookie domain" value={domain} onChange={e => setDomain(e.target.value)}><option value="">All domains</option>{domains.map(d => <option key={d} value={d}>{d}</option>)}</select>
      <label className="df-check"><input type="checkbox" checked={onlyTracking} onChange={e => setOnlyTracking(e.target.checked)} /> Tracking only</label>
    </div>
    {visible.length === 0 ? <div className="empty-state empty-card"><h3>No cookie metadata</h3><p>Register the browser extension and sync permitted domains to see cookie categories here.</p></div> : <div className="df-table-wrap"><table className="df-table"><thead><tr><th>Cookie</th><th>Domain</th><th>Party</th><th>Category</th><th>Flags</th><th>Expires</th><th></th></tr></thead><tbody>{visible.map(c => <tr key={c.id}>
      <td>{c.cookieName}{c.trackingRelated && <span className="df-badge df-badge-warn">tracking</span>}</td>
      <td>{c.domain}{siteFor(c.domain) && <span className="df-muted"> · {siteFor(c.domain)!.name}</span>}</td>
      <td>{c.firstParty ? 'First-party' : 'Third-party'}</td>
      <td>{c.category}</td>
      <td>{[c.secure && 'Secure', c.httpOnly && 'HttpOnly', c.sameSite && `SameSite=${c.sameSite}`].filter(Boolean).join(', ') || '—'}</td>
      <td>{c.expiration ? formatDate(c.expiration) : 'Session / unknown'}</td>
      <td><button className="button button-outline button-small" onClick={() => void remove(c)}>Remove metadata</button></td>
    </tr>)}</tbody></table></div>}
    <p className="df-disclaimer">Deleting a cookie can sign you out or reset preferences. Clearing browser storage never deletes server-side account information.</p>
  </div>
}

function Requests({ websites, requests, onChanged, onError }: { websites: TrackedWebsite[]; requests: PrivacyRequest[]; onChanged: () => void; onError: (m: string) => void }) {
  const [form, setForm] = useState({ trackedWebsiteId: '', requestType: 'DELETION' as PrivacyRequestType, dataCategories: '', privacyContactEmail: '', privacyContactUrl: '', preferredContact: '', legalBasis: '' })
  const [selectedId, setSelectedId] = useState('')
  const [submissionMethod, setSubmissionMethod] = useState('MANUAL')
  const [externalReference, setExternalReference] = useState('')
  const [response, setResponse] = useState('')
  const [claimed, setClaimed] = useState(false)
  const [evidence, setEvidence] = useState<PrivacyRequestEvidence[]>([])
  const [history, setHistory] = useState<PrivacyRequestHistory[]>([])
  const [drafting, setDrafting] = useState(false)
  const selected = requests.find(r => r.id === selectedId) ?? null
  const siteName = (id: string | null) => id ? websites.find(w => w.id === id)?.name ?? 'Website' : 'No website'

  async function create() {
    try {
      const created = await api.createPrivacyRequest({ trackedWebsiteId: form.trackedWebsiteId || undefined, requestType: form.requestType, dataCategories: form.dataCategories || undefined, privacyContactEmail: form.privacyContactEmail || undefined, privacyContactUrl: form.privacyContactUrl || undefined, preferredContact: form.preferredContact || undefined, legalBasis: form.legalBasis || undefined })
      setSelectedId(created.id); onChanged()
    } catch (e) { onError(message(e)) }
  }

  async function createWithAi() {
    const site = websites.find(w => w.id === form.trackedWebsiteId)
    if (!site) { onError('Choose a website so the AI draft has context.'); return }
    setDrafting(true)
    try {
      const draft = await api.privacyRequestDraft({ requestType: form.requestType, websiteName: site.name, dataCategories: form.dataCategories || undefined })
      const created = await api.createPrivacyRequest({ trackedWebsiteId: form.trackedWebsiteId || undefined, requestType: form.requestType, dataCategories: form.dataCategories || undefined, privacyContactEmail: form.privacyContactEmail || undefined, privacyContactUrl: form.privacyContactUrl || undefined, preferredContact: form.preferredContact || undefined, legalBasis: form.legalBasis || undefined, requestMessage: draft.message })
      setSelectedId(created.id); onChanged()
    } catch (e) { onError(message(e)) } finally { setDrafting(false) }
  }

  async function loadDetail(id: string) {
    setSelectedId(id)
    try { const [ev, hi] = await Promise.all([api.privacyRequestEvidence(id), api.privacyRequestHistory(id)]); setEvidence(ev); setHistory(hi) } catch (e) { onError(message(e)) }
  }

  async function act(fn: () => Promise<PrivacyRequest>) {
    try { const updated = await fn(); await loadDetail(updated.id); onChanged() } catch (e) { onError(message(e)) }
  }

  return <div className="df-stack">
    <section className="panel df-panel">
      <div className="panel-heading"><div><h2>Prepare a privacy request</h2><p>You review and approve the exact message before anything is submitted.</p></div></div>
      <div className="df-form-grid">
        <label className="form-label">Website<select value={form.trackedWebsiteId} onChange={e => setForm({ ...form, trackedWebsiteId: e.target.value })}><option value="">No specific website</option>{websites.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}</select></label>
        <label className="form-label">Request type<select value={form.requestType} onChange={e => setForm({ ...form, requestType: e.target.value as PrivacyRequestType })}>{requestTypes.map(t => <option key={t} value={t}>{privacyRequestTypeLabels[t]}</option>)}</select></label>
        <label className="form-label">Data categories<input value={form.dataCategories} onChange={e => setForm({ ...form, dataCategories: e.target.value })} placeholder="email, address" /></label>
        <label className="form-label">Privacy email<input value={form.privacyContactEmail} onChange={e => setForm({ ...form, privacyContactEmail: e.target.value })} placeholder="privacy@example.com" /></label>
        <label className="form-label">Privacy request URL<input value={form.privacyContactUrl} onChange={e => setForm({ ...form, privacyContactUrl: e.target.value })} placeholder="https://…" /></label>
        <label className="form-label">Your contact<input value={form.preferredContact} onChange={e => setForm({ ...form, preferredContact: e.target.value })} placeholder="email to reply to" /></label>
      </div>
      <div className="df-inline">
        <button className="button button-dark button-small" onClick={() => void create()}>Prepare draft</button>
        <button className="button button-outline button-small" disabled={drafting} onClick={() => void createWithAi()}>{drafting ? 'Drafting…' : 'Draft with AI'}</button>
      </div>
      <p className="df-muted">AI drafts are suggestions only. Review and edit the exact message before approval; nothing is submitted automatically.</p>
    </section>

    <div className="df-request-layout">
      <div className="df-request-list">
        {requests.length === 0 ? <p className="df-muted">No requests yet.</p> : requests.map(r => <button key={r.id} className={`df-request-item ${selectedId === r.id ? 'df-request-active' : ''}`} onClick={() => void loadDetail(r.id)}>
          <span><b>{privacyRequestTypeLabels[r.requestType]}</b><small>{siteName(r.trackedWebsiteId)} · {formatDate(r.createdAt)}</small></span>
          <span className={`df-status df-status-${r.status.toLowerCase()}`}>{privacyRequestStatusLabels[r.status]}</span>
        </button>)}
      </div>
      <div className="df-request-detail">
        {!selected ? <p className="df-muted">Select a request to review, approve and track it.</p> : <section className="panel df-panel">
          <div className="df-site-head"><div><h2>{privacyRequestTypeLabels[selected.requestType]}</h2><p className="df-muted">{siteName(selected.trackedWebsiteId)} · {privacyRequestStatusLabels[selected.status]}</p></div><a className="button button-outline button-small" href={selected.privacyContactUrl || selected.privacyContactEmail ? (selected.privacyContactUrl || `mailto:${selected.privacyContactEmail}`) : '#'} target="_blank" rel="noreferrer" onClick={e => { if (!selected.privacyContactUrl && !selected.privacyContactEmail) e.preventDefault() }}>Open official channel</a></div>
          <label className="form-label">Message (review before approval)<textarea key={selected.id} rows={10} defaultValue={selected.requestMessage ?? ''} onBlur={async e => { if (e.target.value === (selected.requestMessage ?? '')) return; try { await api.updatePrivacyRequest(selected.id, { requestMessage: e.target.value }); onChanged() } catch (err) { onError(message(err)) } }} /></label>
          <div className="df-badges">
            {selected.approvedAt && <span className="df-badge df-badge-good">Approved {formatDate(selected.approvedAt)}</span>}
            {selected.submittedAt && <span className="df-badge">Submitted {formatDate(selected.submittedAt)}</span>}
            {selected.websiteClaimedDeletion && <span className="df-badge df-badge-warn">Website claims deletion</span>}
            {selected.userVerified && <span className="df-badge df-badge-good">You verified completion</span>}
            {selected.externalReference && <span className="df-badge df-badge-muted">Ref: {selected.externalReference}</span>}
            {selected.expectedResponseDate && <span className="df-badge df-badge-muted">Expected by {formatDate(selected.expectedResponseDate)}</span>}
          </div>
          <div className="df-row-actions df-actions-wrap">
            {selected.status === 'DRAFT' && <button className="button button-outline button-small" onClick={() => void act(() => api.updatePrivacyRequest(selected.id, {}))}>Mark ready for review</button>}
            {['DRAFT', 'READY_FOR_REVIEW', 'AWAITING_USER_APPROVAL'].includes(selected.status) && <button className="button button-dark button-small" onClick={() => void act(() => api.approvePrivacyRequest(selected.id))}>Approve exact message</button>}
            {selected.status === 'AWAITING_USER_APPROVAL' && <div className="df-inline">
              <select aria-label="Submission method" value={submissionMethod} onChange={e => setSubmissionMethod(e.target.value)}><option value="MANUAL">Manual / official form</option><option value="EMAIL">Email</option><option value="API">Official API</option></select>
              <input aria-label="External reference" placeholder="Reference (optional)" value={externalReference} onChange={e => setExternalReference(e.target.value)} />
              <button className="button button-dark button-small" onClick={() => void act(() => api.submitPrivacyRequest(selected.id, { submissionMethod, externalReference: externalReference || undefined }))}>I submitted this request</button>
            </div>}
            {!['COMPLETED', 'CANCELLED', 'REJECTED', 'EXPIRED'].includes(selected.status) && <button className="button button-outline button-small" onClick={() => void act(() => api.cancelPrivacyRequest(selected.id))}>Cancel</button>}
          </div>
          <hr className="df-rule" />
          <div className="df-form-grid">
            <label className="form-label">Record website response<textarea rows={3} value={response} onChange={e => setResponse(e.target.value)} placeholder="Paste the website's reply (no secrets)" /></label>
            <div className="df-stack">
              <label className="df-check"><input type="checkbox" checked={claimed} onChange={e => setClaimed(e.target.checked)} /> The website claims deletion is complete</label>
              <button className="button button-outline button-small" onClick={() => void act(() => api.recordPrivacyResponse(selected.id, response, claimed))}>Save response</button>
              <button className="button button-outline button-small" onClick={() => void act(() => api.verifyPrivacyRequest(selected.id, true))}>I independently verified completion</button>
            </div>
          </div>
          <details className="df-details"><summary>Status history ({history.length})</summary><ul className="df-history">{history.map(h => <li key={h.id}><b>{privacyRequestStatusLabels[h.toStatus as PrivacyRequestStatus] ?? h.toStatus}</b> · {formatDate(h.createdAt)}{h.note ? ` — ${h.note}` : ''}</li>)}</ul></details>
          <details className="df-details"><summary>Evidence ({evidence.length})</summary><ul className="df-history">{evidence.map(e => <li key={e.id}><b>{e.type}</b> · {formatDate(e.createdAt)}{e.description ? ` — ${e.description}` : ''}{e.referenceUrl ? <> · <a href={e.referenceUrl} target="_blank" rel="noreferrer">link</a></> : null}</li>)}</ul></details>
        </section>}
      </div>
    </div>
    <p className="df-disclaimer">Link Context never submits a request without your explicit approval and never bypasses CAPTCHA, login or identity verification. A website's confirmation is evidence of its claim, not proof every copy was erased.</p>
  </div>
}

function Preferences({ preferences, onChanged, onError }: { preferences: PrivacyPreference | null; onChanged: (p: PrivacyPreference) => void; onError: (m: string) => void }) {
  const rows: [keyof PrivacyPreference, string][] = [
    ['analyticsCookies', 'Analytics cookies'], ['advertisingCookies', 'Advertising cookies'],
    ['marketingCommunications', 'Marketing communications'], ['thirdPartyTracking', 'Third-party tracking'],
    ['personalizedAdvertising', 'Personalised advertising'], ['dataSharing', 'Data sharing'],
  ]
  async function toggle(key: keyof PrivacyPreference) {
    if (!preferences) return
    try { onChanged(await api.updatePrivacyPreferences({ [key]: !preferences[key] })) } catch (e) { onError(message(e)) }
  }
  if (!preferences) return <p className="df-muted">Loading preferences…</p>
  return <div className="df-stack">
    <section className="panel df-panel">
      <div className="panel-heading"><div><h2>Your tracking preferences</h2><p>These are your preferences in Link Context. They do not automatically change another website's consent.</p></div></div>
      {rows.map(([key, label]) => <div key={key} className="toggle-row"><span><b>{label}</b><small>Allow {label.toLowerCase()} where a supported integration exists.</small></span><button role="switch" aria-checked={Boolean(preferences[key])} aria-label={label} className={`switch ${preferences[key] ? 'switch-on' : ''}`} onClick={() => void toggle(key)}><i /></button></div>)}
    </section>
    <p className="df-disclaimer">Only synchronise preferences with a website when an official integration supports it and you authorise it. Use each website's consent interface for changes on that site.</p>
  </div>
}

function Assistant({ context, onError }: { context: string; onError: (m: string) => void }) {
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState('')
  const [busy, setBusy] = useState(false)
  async function ask() {
    if (!question.trim()) { onError('Ask a question first.'); return }
    setBusy(true)
    try { setAnswer((await api.privacyAssistant(question, context)).answer) } catch (e) { onError(message(e)) } finally { setBusy(false) }
  }
  return <div className="df-stack">
    <section className="panel df-panel">
      <div className="panel-heading"><div><h2>Ask about your footprint</h2><p>Answers use only the records loaded in this dashboard. AI inferences are never treated as facts.</p></div></div>
      <div className="df-prompt-row"><input className="search-input" placeholder="Which websites have my email address?" value={question} onChange={e => setQuestion(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') void ask() }} /><button className="button button-dark button-small" onClick={() => void ask()} disabled={busy}>{busy ? 'Thinking…' : 'Ask'}</button></div>
      <div className="df-badges"><button className="df-link" onClick={() => setQuestion('What should I remove first?')}>What should I remove first?</button><button className="df-link" onClick={() => setQuestion('Which websites have pending deletion requests?')}>Which requests are pending?</button><button className="df-link" onClick={() => setQuestion('Which websites use advertising trackers?')}>Which sites track me?</button></div>
      {answer && <div className="df-answer"><h3>Answer</h3><p className="df-pre">{answer}</p></div>}
    </section>
    <p className="df-disclaimer">The assistant never invents records and never exposes another user's data. Do not paste secrets into questions.</p>
  </div>
}

function AccountData({ onChanged, onError }: { onChanged: () => void; onError: (m: string) => void }) {
  const [busy, setBusy] = useState(false)
  async function exportData() {
    setBusy(true)
    try {
      const data = await api.exportDigitalFootprint()
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a'); a.href = url; a.download = `link-context-digital-footprint-${new Date().toISOString().slice(0, 10)}.json`; a.click(); URL.revokeObjectURL(url)
    } catch (e) { onError(message(e)) } finally { setBusy(false) }
  }
  async function deleteAll() {
    if (!confirm('Delete all Link Context privacy records? This cannot be undone. It does not delete data held by third-party websites.')) return
    setBusy(true)
    try { await api.deleteDigitalFootprintAccountData(); onChanged() } catch (e) { onError(message(e)) } finally { setBusy(false) }
  }
  return <div className="df-stack">
    <section className="panel df-panel">
      <div className="panel-heading"><div><h2>Export your privacy records</h2><p>Download websites, inventory, cookie metadata, preferences and request history as JSON.</p></div></div>
      <button className="button button-dark button-small" onClick={() => void exportData()} disabled={busy}>Export JSON</button>
    </section>
    <section className="panel df-panel">
      <div className="panel-heading"><div><h2>Delete your Link Context privacy records</h2><p>Removes footprint records from Link Context. It does not delete your account profile or data held by other websites.</p></div></div>
      <button className="button button-delete button-small" onClick={() => void deleteAll()} disabled={busy}>Delete privacy records</button>
    </section>
  </div>
}

function rank(risk: string | null) { return risk === 'HIGH' ? 3 : risk === 'MEDIUM' ? 2 : risk === 'LOW' ? 1 : 0 }
function sourceLabel(source: string) { return source.replaceAll('_', ' ').toLowerCase() }
function formatDate(value: string | null) { if (!value) return ''; const d = new Date(value); return Number.isNaN(d.valueOf()) ? '' : new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', year: 'numeric' }).format(d) }
