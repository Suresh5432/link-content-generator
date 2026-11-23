import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { api } from './api'
import type { WebsiteAnalysis } from './types'

interface WebsitePage {
  id: string
  url: string
  title: string | null
  depth: number | null
  statusCode: number | null
  contentExcerpt: string | null
  fetchedAt: string | null
}

interface WebsiteFinding {
  id: string
  type: string
  name: string
  description: string
  evidence: string
  confidence: string
}

interface InspectionReport {
  analysis: WebsiteAnalysis
  pages: WebsitePage[]
  findings: WebsiteFinding[]
}

const terminalStatuses = new Set(['COMPLETED', 'PARTIALLY_COMPLETED', 'FAILED'])

function parseReport(report: string | null): unknown {
  if (!report) return null
  try {
    return JSON.parse(report) as unknown
  } catch {
    return report
  }
}

function ReportValue({ value }: { value: unknown }) {
  if (value === null || value === undefined) return <p>Unknown or not observed.</p>
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return <p>{String(value)}</p>
  }
  if (Array.isArray(value)) {
    return value.length
      ? <ul>{value.map((item, index) => <li key={index}><ReportValue value={item} /></li>)}</ul>
      : <p>None reported.</p>
  }
  if (typeof value === 'object') {
    return (
      <dl>
        {Object.entries(value).map(([key, item]) => (
          <div key={key}>
            <dt>{key.replace(/([A-Z])/g, ' $1')}</dt>
            <dd><ReportValue value={item} /></dd>
          </div>
        ))}
      </dl>
    )
  }
  return <p>Unknown or not observed.</p>
}

const reportSections = [
  ['overview', 'Website overview'],
  ['purpose', 'Purpose and services'],
  ['functionality', 'Functionality and user activities'],
  ['requestedInformation', 'Information requested'],
  ['sensitiveInformation', 'Sensitive information'],
  ['trackers', 'Tracking and third parties'],
  ['privacy', 'Privacy declarations'],
  ['security', 'Security observations'],
  ['trustAssessment', 'Trust and risk assessment'],
  ['limitations', 'Inspection limitations'],
  ['recommendedPrecautions', 'Recommended precautions'],
] as const

export function WebsiteAnalysisPage() {
  const [url, setUrl] = useState('')
  const [items, setItems] = useState<WebsiteAnalysis[]>([])
  const [selected, setSelected] = useState<string | null>(null)
  const [pollVersion, setPollVersion] = useState(0)
  const [analysis, setAnalysis] = useState<WebsiteAnalysis | null>(null)
  const [inspection, setInspection] = useState<InspectionReport | null>(null)
  const [aiReport, setAiReport] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [error, setError] = useState('')
  const selectedRef = useRef<string | null>(null)

  const load = useCallback(async () => {
    const page = await api.websiteAnalyses(0, 20)
    setItems(page.content)
  }, [])

  useEffect(() => {
    load().catch((e: unknown) => setError(String(e)))
  }, [load])

  const loadInspection = useCallback(async (id: string) => {
    const [report, findings] = await Promise.all([
      api.websiteAnalysisReport(id) as Promise<Omit<InspectionReport, 'findings'>>,
      api.websiteAnalysisFindings(id) as Promise<WebsiteFinding[]>,
    ])
    return { ...report, findings } as InspectionReport
  }, [])

  const generateReport = useCallback(async (id: string) => {
    setGenerating(true)
    setError('')
    try {
      const generated = await api.websiteAnalysisAiReport(id)
      const updated = await api.websiteAnalysis(id)
      if (selectedRef.current === id) {
        setAiReport(parseReport(generated))
        setAnalysis(updated)
      }
      await load()
    } catch (e) {
      setError(`AI report could not be generated: ${String(e)}`)
    } finally {
      setGenerating(false)
    }
  }, [load])

  useEffect(() => {
    if (!selected) return
    let active = true
    let timer: ReturnType<typeof setTimeout> | undefined

    const poll = async () => {
      try {
        const current = await api.websiteAnalysis(selected)
        if (!active) return
        setAnalysis(current)
        if (terminalStatuses.has(current.status)) {
          const complete = await loadInspection(selected)
          if (!active) return
          setInspection(complete)
          const parsed = parseReport(complete.analysis.report)
          setAiReport(typeof parsed === 'string' ? null : parsed)
          if (!complete.analysis.report || typeof parsed === 'string') await generateReport(selected)
          await load()
          return
        }
        timer = setTimeout(poll, 1800)
      } catch (e) {
        if (active) setError(String(e))
      }
    }

    poll().catch((e: unknown) => { if (active) setError(String(e)) })
    return () => {
      active = false
      if (timer) clearTimeout(timer)
    }
  }, [selected, pollVersion, generateReport, load, loadInspection])

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!url.trim()) return
    setBusy(true)
    setError('')
    try {
      const created = await api.createWebsiteAnalysis(url.trim())
      setUrl('')
      selectedRef.current = created.id
      setSelected(created.id)
      setPollVersion((version) => version + 1)
      setAnalysis(created)
      setInspection(null)
      setAiReport(null)
      await load()
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const open = (id: string) => {
    selectedRef.current = id
    setSelected(id)
    setPollVersion((version) => version + 1)
    setAnalysis(null)
    setInspection(null)
    setAiReport(null)
    setError('')
  }

  const retry = async (id: string) => {
    setError('')
    try {
      await api.retryWebsiteAnalysis(id)
      open(id)
      await load()
    } catch (e) {
      setError(String(e))
    }
  }

  return (
    <main>
      <h1>Website analysis</h1>
      <p>Public, static inspection only. No forms are submitted and no credentials are collected.</p>
      <form onSubmit={submit}>
        <label htmlFor="website-analysis-url">Website URL</label>
        <input
          id="website-analysis-url"
          type="url"
          required
          value={url}
          onChange={(event) => setUrl(event.target.value)}
          placeholder="https://example.com"
        />
        <button type="submit" disabled={busy}>{busy ? 'Submitting…' : 'Analyze website'}</button>
      </form>

      {error && <p role="alert">{error}</p>}

      <h2>Recent analyses</h2>
      <ul>
        {items.map((item) => (
          <li key={item.id}>
            <button type="button" onClick={() => open(item.id)}>{item.title || item.url}</button>
            {' — '}{item.status}{' — '}Risk: {item.riskLevel || 'pending'}
            {item.confidence !== null && ` (${item.confidence}% confidence)`}
            {(item.status === 'FAILED' || item.status === 'PARTIALLY_COMPLETED') && (
              <button type="button" onClick={() => retry(item.id)}>Retry</button>
            )}
          </li>
        ))}
      </ul>

      {selected && analysis && (
        <section aria-live="polite">
          <h2>{analysis.title || analysis.domain || analysis.url}</h2>
          <p>Status: {analysis.status}</p>
          {!terminalStatuses.has(analysis.status) && <p>Inspecting public pages…</p>}
          {analysis.errorMessage && <p>{analysis.errorMessage}</p>}
          {analysis.riskLevel && (
            <p>Risk assessment: {analysis.riskLevel} ({analysis.confidence ?? 'unknown'}% confidence).
              {' '}This is an evidence-based estimate from the observed pages, not a guarantee of safety. See the trust assessment and evidence below.
            </p>
          )}

          {aiReport !== null && (
            <section>
              <h3>Website intelligence report</h3>
              <p>AI interpretation of collected evidence; consult the observed findings below for source details.</p>
              {typeof aiReport === 'string'
                ? <p>{aiReport}</p>
                : reportSections.map(([key, title]) => {
                  if (typeof aiReport !== 'object' || aiReport === null || !(key in aiReport)) return null
                  return <section key={key}><h4>{title}</h4><ReportValue value={aiReport[key as keyof typeof aiReport]} /></section>
                })}
            </section>
          )}
          {terminalStatuses.has(analysis.status) && !aiReport && (
            <button type="button" onClick={() => generateReport(selected)} disabled={generating}>
              {generating ? 'Generating report…' : 'Generate AI report'}
            </button>
          )}

          {inspection && (
            <>
              <h3>Observed pages ({inspection.pages.length})</h3>
              <ul>
                {inspection.pages.map((page) => (
                  <li key={page.id}>
                    <a href={page.url} target="_blank" rel="noreferrer">{page.title || page.url}</a>
                    {' — HTTP '}{page.statusCode ?? 'unknown'}
                    {page.contentExcerpt && <p>{page.contentExcerpt}</p>}
                  </li>
                ))}
              </ul>
              <h3>Evidence and observations ({inspection.findings.length})</h3>
              <ul>
                {inspection.findings.map((finding) => (
                  <li key={finding.id}>
                    <strong>{finding.type}: {finding.name}</strong> — {finding.description}
                    {finding.evidence && <p>Evidence: {finding.evidence}</p>}
                    <small>Confidence: {finding.confidence}</small>
                  </li>
                ))}
              </ul>
              <p>Observed facts are distinct from AI interpretations and website policy declarations. Uninspected behavior remains unknown.</p>
            </>
          )}
        </section>
      )}
    </main>
  )
}
