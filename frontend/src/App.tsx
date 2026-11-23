import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import type { ReactNode, RefObject } from 'react'
import { Link, Navigate, Route, Routes, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ArrowLeft, ArrowRight, Bell, BookOpen, Check, CircleHelp, ExternalLink, FileText, Globe2, LayoutDashboard, LoaderCircle, LogOut, Mail, Menu, Plus, Settings2, ShieldCheck, Sparkles, Trash2, UserPlus, UserRound, X } from 'lucide-react'
import keycloak, { initializeKeycloak } from './auth'
import { api, ApiError } from './api'
import type { Link as LinkRecord, Notification, NotificationPreferences, Page, Profile, Summary } from './types'
import { isFailed, statusLabels } from './types'
import { WebsiteAnalysisPage } from './WebsiteAnalysisPage'
import { DigitalFootprintPage } from './DigitalFootprintPage'

interface AuthState { ready: boolean; authenticated: boolean; name: string; email: string; tokenParsed?: Record<string, unknown> }
const AuthContext = createContext<AuthState>({ ready: false, authenticated: false, name: '', email: '' })
const useAuth = () => useContext(AuthContext)
type AuthFlowStarter = (url: string) => void
interface AuthFlowContextValue { start: AuthFlowStarter; url: string; error: string; close: () => void; frame: RefObject<HTMLIFrameElement> }
const AuthFlowContext = createContext<AuthFlowContextValue>({ start: () => undefined, url: '', error: '', close: () => undefined, frame: { current: null } })
const useAuthFlow = () => useContext(AuthFlowContext)

function notifyAuthFrame(authenticated: boolean) {
  if (window.parent === window) return false
  const params = new URLSearchParams(window.location.search)
  const flow = params.has('loggedOut') ? 'logout' : params.has('authComplete') ? 'login' : null
  if (!flow) return false
  window.parent.postMessage({ type: 'link-context-auth-frame-complete', flow, success: flow === 'logout' ? !authenticated : authenticated }, window.location.origin)
  return true
}

export default function App() {
  const [auth, setAuth] = useState<AuthState>({ ready: false, authenticated: false, name: '', email: '' })
  const [authUrl, setAuthUrl] = useState('')
  const [authFlowError, setAuthFlowError] = useState('')
  const authFrame = useRef<HTMLIFrameElement>(null)
  const [search] = useSearchParams()
  useEffect(() => {
    const onMessage = (event: MessageEvent) => {
      if (event.origin !== window.location.origin || event.source !== authFrame.current?.contentWindow) return
      const message = event.data as { type?: string; flow?: 'login' | 'logout'; success?: boolean }
      if (message.type !== 'link-context-auth-frame-complete' || !message.flow) return
      setAuthUrl('')
      if (!message.success) {
        setAuthFlowError('Sign-in could not be completed. Please try again.')
        return
      }
      window.location.replace(message.flow === 'logout' ? '/login' : '/login?authComplete=1')
    }
    window.addEventListener('message', onMessage)
    let active = true
    initializeKeycloak()
      .then(authenticated => { if (!active || notifyAuthFrame(authenticated)) return; setAuth({ ready: true, authenticated, name: String(keycloak.tokenParsed?.name ?? keycloak.tokenParsed?.preferred_username ?? ''), email: String(keycloak.tokenParsed?.email ?? ''), tokenParsed: keycloak.tokenParsed }) })
      .catch(() => { if (!active || notifyAuthFrame(false)) return; setAuth({ ready: true, authenticated: false, name: '', email: '' }) })
    return () => { active = false; window.removeEventListener('message', onMessage) }
  }, [])

  const authFlow: AuthFlowContextValue = {
    url: authUrl,
    error: authFlowError,
    frame: authFrame,
    close: () => setAuthUrl(''),
    start: url => {
      try {
        const target = new URL(url, window.location.origin)
        if (target.origin !== window.location.origin) throw new Error('Sign-in is unavailable because the secure login page is not on this site.')
        setAuthFlowError('')
        setAuthUrl(target.href)
      } catch (error) {
        setAuthFlowError(error instanceof Error ? error.message : 'Unable to open the secure sign-in page.')
        throw error
      }
    },
  }

  return <AuthContext.Provider value={auth}><AuthFlowContext.Provider value={authFlow}><Routes>
    <Route path="/login" element={auth.ready && auth.authenticated && search.get('authComplete') === '1' ? <Navigate to="/" replace /> : auth.ready ? <LoginPage /> : <LoadingScreen />} />
    <Route path="/signin" element={<Navigate to="/login" replace />} />
    <Route path="/forgot-password" element={auth.ready ? <ForgotPasswordPage /> : <LoadingScreen />} />
    <Route path="/forgetpassword" element={auth.ready ? <ForgotPasswordPage /> : <LoadingScreen />} />
    <Route path="/signup" element={auth.ready ? auth.authenticated ? <Navigate to="/" replace /> : <SignUpPage /> : <LoadingScreen />} />
    <Route path="/logout" element={<Navigate to="/login" replace />} />
    <Route path="/*" element={!auth.ready ? <LoadingScreen /> : !auth.authenticated ? <Navigate to="/login" replace /> : <AppShell />} />
  </Routes></AuthFlowContext.Provider></AuthContext.Provider>
}

function LoadingScreen() { return <div className="center-screen"><LoaderCircle className="spin" /><span>Getting your workspace ready…</span></div> }
function LoginPage() {
  const auth = useAuth()
  const { start: startAuthFlow } = useAuthFlow()
  const [search] = useSearchParams()
  const sessionExpired = search.has('sessionExpired')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  async function authenticate() {
    setPending(true)
    setError('')
    try {
      if (auth.authenticated) startAuthFlow(keycloak.createLogoutUrl({ redirectUri: `${window.location.origin}/login?loggedOut=1` }))
      else startAuthFlow(await keycloak.createLoginUrl({ redirectUri: `${window.location.origin}/login?authComplete=1` }))
    }
    catch (reason) { setError(reason instanceof Error ? reason.message : auth.authenticated ? 'Unable to sign out.' : 'Unable to start sign-in.') }
    finally { setPending(false) }
  }
  const heading = auth.authenticated ? 'You’re signed in.' : search.has('loggedOut') ? 'You’re signed out.' : sessionExpired ? 'Your session expired.' : 'Welcome back.'
  const description = auth.authenticated ? `Signed in as ${auth.email || auth.name || 'your Link Context account'}.` : search.has('loggedOut') ? 'Your session has ended. Come back whenever you’re ready.' : sessionExpired ? 'Sign in again to continue to your workspace.' : 'Sign in to pick up where you left off.'
  return <AuthFrame variant="welcome"><span className="eyebrow">{auth.authenticated ? 'YOUR ACCOUNT' : 'YOUR READING SPACE'}</span><h2>{heading}</h2><p>{description}</p><button className="button button-dark button-wide" disabled={pending} onClick={() => void authenticate()}>{pending ? auth.authenticated ? 'Signing out…' : 'Waiting for sign-in…' : auth.authenticated ? 'Sign out' : sessionExpired ? 'Sign in again' : 'Continue to sign in'} {auth.authenticated ? <LogOut size={16} /> : <ArrowRight size={16} />}</button>{error && <p role="alert">{error}</p>}{auth.authenticated ? <div className="secure-note"><ShieldCheck size={15} /> Sign out securely from this page</div> : <><div className="secure-note"><ShieldCheck size={15} /> Secure sign-in for your account</div><div className="auth-links"><Link to="/forgot-password">Forgot password?</Link><Link to="/signup">Create an account</Link></div></>}</AuthFrame>
}

function ForgotPasswordPage() {
  const { start: startAuthFlow } = useAuthFlow()
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  async function continueToKeycloak(event: React.FormEvent) {
    event.preventDefault()
    setPending(true)
    setError('')
    try { startAuthFlow(await keycloak.createLoginUrl({ loginHint: email.trim() || undefined, redirectUri: `${window.location.origin}/login?authComplete=1` })) }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to start password recovery.') }
    finally { setPending(false) }
  }
  return <AuthFrame variant="recovery"><Link className="auth-back" to="/login"><ArrowLeft size={14} /> Back to sign in</Link><span className="auth-form-icon"><Mail size={18} /></span><span className="eyebrow">ACCOUNT RECOVERY</span><h2>Reset your password.</h2><p>We’ll guide you through a secure password reset. Your password is never entered or stored here.</p><form className="auth-form" onSubmit={continueToKeycloak}><label className="form-label">Email address <input type="email" autoComplete="email" value={email} onChange={event => setEmail(event.target.value)} placeholder="you@example.com" /></label><button className="button button-dark button-wide" disabled={pending} type="submit">{pending ? 'Waiting for sign-in…' : 'Continue to password reset'} <ArrowRight size={16} /></button></form>{error && <p role="alert">{error}</p>}<div className="auth-helper">On the secure sign-in page, choose <b>Forgot password?</b> to request a reset email.</div></AuthFrame>
}

function SignUpPage() {
  const { start: startAuthFlow } = useAuthFlow()
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  async function register() {
    setPending(true)
    setError('')
    try { startAuthFlow(await keycloak.createRegisterUrl({ redirectUri: `${window.location.origin}/login?authComplete=1` })) }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to start registration.') }
    finally { setPending(false) }
  }
  return <AuthFrame><Link className="auth-back" to="/login"><ArrowLeft size={14} /> Back</Link><span className="auth-form-icon"><UserPlus size={18} /></span><span className="eyebrow">GET STARTED</span><h2>Create your account.</h2><p>Set up your Link Context account securely.</p><button className="button button-dark button-wide" disabled={pending} onClick={() => void register()}><UserPlus size={16} /> {pending ? 'Waiting for registration…' : 'Create account'} <ArrowRight size={16} /></button>{error && <p role="alert">{error}</p>}<div className="auth-links auth-links-column"><span>Already have an account? <Link to="/login">Sign in</Link></span></div></AuthFrame>
}

function AuthFrame({ children, variant = 'default' }: { children: ReactNode; variant?: 'default' | 'welcome' | 'recovery' }) {
  const flow = useAuthFlow()
  return <main className={`login-page auth-page auth-${variant}`}><div className="login-art"><div className="brand-mark"><Sparkles size={19} /></div><p className="eyebrow">A calmer way to read</p><h1>Make the internet<br />make sense.</h1><p className="login-copy">Save the links that matter. Get the context you need, without the clutter.</p><div className="art-card"><div className="art-icon"><Globe2 size={20} /></div><div><strong>One link, a clearer picture</strong><span>Key ideas, ready when you are.</span></div><span className="art-pill">READY</span></div><div className="art-decoration"><span /><span /><span /></div></div><div className="login-panel"><Link className="brand" to="/login"><span className="brand-mark"><Sparkles size={17} /></span><span>link<span className="brand-light">context</span></span></Link><section className="login-content auth-content">{children}{flow.error && <p role="alert">{flow.error}</p>}</section><footer>Secure account access for Link Context.</footer></div>{flow.url && <div className="auth-frame-overlay" role="dialog" aria-modal="true" aria-label="Secure sign in"><button className="auth-frame-close" onClick={flow.close} aria-label="Close sign-in">Close</button><iframe ref={flow.frame} title="Link Context secure sign-in" src={flow.url} /></div>}</main>
}

const navItems = [{ to: '/', label: 'Overview', icon: LayoutDashboard }, { to: '/links', label: 'My links', icon: Globe2 }, { to: '/summaries', label: 'Summaries', icon: FileText }, { to: '/digital-footprint', label: 'My footprint', icon: ShieldCheck }, { to: '/notifications', label: 'Notifications', icon: Bell }, { to: '/profile', label: 'Profile', icon: UserRound }]
function AppShell() {
  const auth = useAuth(), location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false), [unread, setUnread] = useState(0), [toast, setToast] = useState('')
  const loadUnread = useCallback(() => api.unreadCount().then(result => setUnread(result.count)).catch(() => undefined), [])
  useEffect(() => { void loadUnread() }, [location.pathname, loadUnread])
  useEffect(() => { if (!toast) return; const timer = window.setTimeout(() => setToast(''), 3800); return () => window.clearTimeout(timer) }, [toast])
  const activeLabel = navItems.find(item => item.to === location.pathname)?.label ?? (location.pathname.startsWith('/links/') ? 'Link details' : 'Summary')
  return <div className="app-layout"><aside className={`sidebar ${mobileOpen ? 'sidebar-open' : ''}`}><Link className="brand side-brand" to="/"><span className="brand-mark"><Sparkles size={17} /></span><span>link<span className="brand-light">context</span></span></Link><div className="side-label">WORKSPACE</div><nav>{navItems.map(item => <Link onClick={() => setMobileOpen(false)} key={item.to} className={`nav-link ${location.pathname === item.to || (item.to !== '/' && location.pathname.startsWith(item.to)) ? 'nav-active' : ''}`} to={item.to}><item.icon size={18} /><span>{item.label}</span>{item.to === '/notifications' && unread > 0 && <span className="nav-count">{unread}</span>}</Link>)}</nav><div className="sidebar-bottom"><div className="help-card"><div className="help-icon"><CircleHelp size={16} /></div><strong>A little help?</strong><p>Your links are private and only visible to you.</p></div><Link className="account-row" to="/login"><span className="avatar">{(auth.name || auth.email || 'Y').slice(0, 1).toUpperCase()}</span><span className="account-name"><b>{auth.name || 'Your account'}</b><small>{auth.email || 'Signed in'}</small></span><LogOut size={16} /></Link></div></aside>{mobileOpen && <button className="mobile-scrim" aria-label="Close menu" onClick={() => setMobileOpen(false)} />}<main className="main-area"><header className="topbar"><button className="icon-button menu-button" aria-label="Open menu" onClick={() => setMobileOpen(true)}><Menu size={19} /></button><div className="breadcrumbs"><span>Workspace</span><span className="crumb-slash">/</span><b>{activeLabel}</b></div><div className="topbar-actions"><Link className="icon-button notification-button" aria-label="Notifications" to="/notifications"><Bell size={18} />{unread > 0 && <i />}</Link><span className="topbar-divider" /><span className="topbar-date">YOUR PERSONAL LIBRARY</span></div></header><div className="page-container"><Routes><Route path="/" element={<Dashboard onToast={setToast} />} /><Route path="/links" element={<LinksPage onToast={setToast} />} /><Route path="/links/:id" element={<LinkDetail onToast={setToast} />} /><Route path="/summaries" element={<SummariesPage />} /><Route path="/summaries/:id" element={<SummaryDetail />} /><Route path="/notifications" element={<NotificationsPage onUnread={loadUnread} />} /><Route path="/profile" element={<ProfilePage />} />
<Route path="/website-analyses" element={<WebsiteAnalysisPage />} />
<Route path="/digital-footprint" element={<DigitalFootprintPage />} /><Route path="*" element={<NotFound />} /></Routes></div></main>{toast && <div role="status" className="toast"><Check size={17} />{toast}<button aria-label="Dismiss" onClick={() => setToast('')}><X size={15} /></button></div>}</div>
}

function Greeting() { const auth = useAuth(); const first = auth.name.split(' ')[0]; const hour = new Date().getHours(); return <>{hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening'}{first ? `, ${first}` : ''}.</> }
function Dashboard({ onToast }: { onToast: (message: string) => void }) {
  const [links, setLinks] = useState<LinkRecord[]>([]), [summaries, setSummaries] = useState<Summary[]>([]), [counts, setCounts] = useState({ links: 0, ready: 0, reading: 0 }), [busy, setBusy] = useState(true), [error, setError] = useState('')
  const refresh = () => { setBusy(true); setError(''); Promise.all([api.links(0, 5), api.summaries(0, 4)]).then(([linkPage, summaryPage]) => { setLinks(linkPage.content); setSummaries(summaryPage.content); setCounts({ links: linkPage.totalElements, ready: summaryPage.totalElements, reading: linkPage.content.filter(link => !isFailed(link.status) && link.status !== 'COMPLETED').length }) }).catch(e => setError(errorMessage(e))).finally(() => setBusy(false)) }
  useEffect(() => { refresh() }, [])
  return <><section className="welcome-row"><div><span className="eyebrow">YOUR PERSONAL LIBRARY</span><h1><Greeting /></h1><p>Keep what matters. Catch up when it suits you.</p></div><SubmitLink onCreated={link => { onToast('Your link is in the reading queue.'); refresh(); void link }} compact={false} /></section>{error && <ErrorBanner message={error} onRetry={refresh} />}<section className="stats-grid"><StatCard icon={<Globe2 size={18} />} label="Links saved" value={counts.links} tone="mint" /><StatCard icon={<BookOpen size={18} />} label="Summaries ready" value={counts.ready} tone="lavender" /><StatCard icon={<LoaderCircle size={18} />} label="In progress" value={counts.reading} tone="peach" /></section><div className="content-grid"><section className="panel recent-panel"><div className="panel-heading"><div><h2>Recently saved</h2><p>Your latest links, all in one place.</p></div><Link className="text-link" to="/links">See all <ArrowRight size={15} /></Link></div>{busy ? <LoadingInline /> : links.length ? <div className="link-list">{links.slice(0, 4).map(link => <LinkRow key={link.id} link={link} />)}</div> : <EmptyState icon={<Globe2 />} title="Your reading list starts here" body="Save an article or page and its summary will show up here." action={<Link to="/links" className="button button-outline button-small"><Plus size={15} /> Add a link</Link>} />}</section><section className="panel summary-panel"><div className="panel-heading"><div><h2>Ready to revisit</h2><p>Fresh context, whenever you need it.</p></div></div>{busy ? <LoadingInline /> : summaries.length ? <div className="summary-list">{summaries.slice(0, 3).map(item => <SummaryRow key={item.id} summary={item} />)}</div> : <EmptyState icon={<FileText />} title="No summaries yet" body="Once a link is processed, your key takeaways will be waiting here." />}</section></div><div className="privacy-note"><ShieldCheck size={16} /><span><b>Your library, your space.</b> Only you can see the links and summaries you save.</span></div></>
}
function StatCard({ icon, label, value, tone }: { icon: ReactNode; label: string; value: number; tone: string }) { return <div className="stat-card"><span className={`stat-icon ${tone}`}>{icon}</span><div><span>{label}</span><strong>{value.toLocaleString()}</strong></div></div> }
function SubmitLink({ onCreated, compact = true }: { onCreated: (link: LinkRecord) => void; compact?: boolean }) {
  const [url, setUrl] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState('')
  async function submit(event: React.FormEvent) { event.preventDefault(); setError(''); let parsed: URL; try { parsed = new URL(url.trim()) } catch { setError('Enter a complete web address, like https://example.com/article.'); return }; if (!['http:', 'https:'].includes(parsed.protocol)) { setError('Only http and https links can be saved.'); return }; setBusy(true); try { const link = await api.createLink(parsed.toString()); setUrl(''); onCreated(link) } catch (e) { setError(errorMessage(e)) } finally { setBusy(false) } }
  return <form className={`submit-form ${compact ? 'submit-compact' : 'submit-hero'}`} onSubmit={submit}><div className="submit-field"><Globe2 size={18} /><input aria-label="Link URL" type="url" placeholder="Paste a link to get started…" value={url} onChange={e => setUrl(e.target.value)} required maxLength={2048} /><button className="button button-dark" disabled={busy}>{busy ? <LoaderCircle className="spin" size={16} /> : <><span>Save link</span><ArrowRight size={15} /></>}</button></div>{error && <p role="alert" className="field-error">{error}</p>}</form>
}

function LinksPage({ onToast }: { onToast: (message: string) => void }) {
  const [data, setData] = useState<Page<LinkRecord> | null>(null), [busy, setBusy] = useState(true), [error, setError] = useState(''), [filter, setFilter] = useState('ALL'), [search, setSearch] = useState(''), [deleteTarget, setDeleteTarget] = useState<LinkRecord | null>(null), [deleting, setDeleting] = useState(false), [deleteError, setDeleteError] = useState('')
  const load = useCallback((selectedFilter: string) => {
    setBusy(true); setError('')
    const request = selectedFilter === 'READY' ? api.links(0, 100, 'COMPLETED') : selectedFilter === 'EXTRACTION_FAILED' ? api.links(0, 100, 'EXTRACTION_FAILED') : api.links(0, 100)
    request.then(setData).catch(e => setError(errorMessage(e))).finally(() => setBusy(false))
  }, [])
  useEffect(() => { load(filter) }, [filter, load])
  async function deleteSelectedLink() {
    if (!deleteTarget) return
    setDeleting(true); setDeleteError('')
    try {
      await api.deleteLink(deleteTarget.id)
      setData(current => {
        if (!current) return current
        const totalElements = Math.max(0, current.totalElements - 1)
        return { ...current, content: current.content.filter(link => link.id !== deleteTarget.id), totalElements, totalPages: Math.ceil(totalElements / current.size) }
      })
      onToast('Link deleted from your library.')
      setDeleteTarget(null)
    } catch (e) { setDeleteError(errorMessage(e)) }
    finally { setDeleting(false) }
  }
  const filtered = (data?.content ?? []).filter(link => (filter === 'ALL' || (filter === 'READY' ? link.status === 'COMPLETED' : filter === 'EXTRACTION_FAILED' ? link.status === 'EXTRACTION_FAILED' : isFailed(link.status))) && link.url.toLowerCase().includes(search.toLowerCase()))
  const emptyTitle = search ? 'No matching links' : filter === 'EXTRACTION_FAILED' ? 'No extraction failures' : filter === 'FAILED' ? 'No links need attention' : 'Nothing saved here yet'
  const emptyBody = search ? 'Try a different search or clear the search field.' : filter === 'EXTRACTION_FAILED' ? 'Links that could not be read will appear here, with a link to their details and a retry option.' : filter === 'FAILED' ? 'Links with extraction or summary issues will appear here.' : 'Paste a link above to start building your personal reading list.'
  return <><PageTitle eyebrow="YOUR COLLECTION" title="My links" subtitle="Everything you’ve saved, with its progress at a glance." action={<SubmitLink onCreated={link => { onToast('Your link is in the reading queue.'); setData(current => current ? { ...current, content: [link, ...current.content] } : current) }} />} /><div className="toolbar"><div className="filter-tabs">{[['ALL', 'All links'], ['READY', 'Ready'], ['EXTRACTION_FAILED', 'Extraction failed'], ['FAILED', 'Needs attention']].map(([key, label]) => <button key={key} className={filter === key ? 'filter-active' : ''} onClick={() => setFilter(key)}>{label}</button>)}</div><input className="search-input" placeholder="Search saved links" aria-label="Search links" value={search} onChange={e => setSearch(e.target.value)} /></div>{error && <ErrorBanner message={error} onRetry={() => load(filter)} />}{busy ? <LoadingCard /> : filtered.length ? <div className="panel link-page-list">{filtered.map(link => <LinkRow key={link.id} link={link} onDelete={() => { setDeleteError(''); setDeleteTarget(link) }} />)}</div> : <EmptyState className="empty-card" icon={<Globe2 />} title={emptyTitle} body={emptyBody} />}{deleteTarget && <DeleteLinkDialog link={deleteTarget} busy={deleting} error={deleteError} onCancel={() => { if (!deleting) setDeleteTarget(null) }} onConfirm={deleteSelectedLink} />}</>
}
function LinkRow({ link, onDelete }: { link: LinkRecord; onDelete?: () => void }) { const hostname = safeHostname(link.url); return <div className="link-row"><span className="favicon"><Globe2 size={16} /></span><div className="row-main"><Link to={`/links/${link.id}`} className="row-title" title={link.url}>{hostname}<ExternalLink size={12} /></Link><span className="row-subtitle">{link.url}</span></div><StatusBadge status={link.status} /><span className="row-date">{formatDate(link.createdAt)}</span>{onDelete && <button className="row-action row-delete" aria-label={`Delete ${hostname}`} title="Delete link" onClick={onDelete}><Trash2 size={16} /></button>}<Link to={`/links/${link.id}`} className="row-arrow" aria-label={`View ${hostname}`}><ArrowRight size={17} /></Link></div> }
function StatusBadge({ status }: { status: LinkRecord['status'] }) { return <span className={`status-badge ${status === 'COMPLETED' ? 'status-ready' : isFailed(status) ? 'status-failed' : 'status-pending'}`}><i />{statusLabels[status] ?? status}</span> }
function DeleteLinkDialog({ link, busy, error, onCancel, onConfirm }: { link: LinkRecord; busy: boolean; error: string; onCancel: () => void; onConfirm: () => void }) {
  return <div className="dialog-backdrop"><section className="delete-dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-link-title" aria-describedby="delete-link-description"><span className="delete-dialog-icon"><Trash2 size={18} /></span><h2 id="delete-link-title">Delete this link?</h2><p id="delete-link-description"><strong>{safeHostname(link.url)}</strong> will be removed from your library. Any generated summary remains in Summaries.</p>{error && <p className="delete-dialog-error" role="alert">{error}</p>}<div className="delete-dialog-actions"><button className="button button-outline" onClick={onCancel} disabled={busy}>Cancel</button><button className="button button-delete" onClick={onConfirm} disabled={busy}>{busy ? <><LoaderCircle size={14} className="spin" /> Deleting…</> : <><Trash2 size={14} /> Delete link</>}</button></div></section></div>
}
function LinkDetail({ onToast }: { onToast: (message: string) => void }) { const { id = '' } = useParamsSafe(); const navigate = useNavigate(); const [link, setLink] = useState<LinkRecord | null>(null), [summary, setSummary] = useState<Summary | null>(null), [busy, setBusy] = useState(true), [error, setError] = useState(''), [retrying, setRetrying] = useState(false), [deleteOpen, setDeleteOpen] = useState(false), [deleting, setDeleting] = useState(false), [deleteError, setDeleteError] = useState('')
  const load = useCallback(() => { setBusy(true); setError(''); Promise.all([api.link(id), api.summaryForLink(id).catch(e => e instanceof ApiError && e.status === 404 ? null : Promise.reject(e))]).then(([l, s]) => { setLink(l); setSummary(s) }).catch(e => setError(errorMessage(e))).finally(() => setBusy(false)) }, [id])
  useEffect(() => { load() }, [load])
  async function retry() { setRetrying(true); try { setLink(await api.retryLink(id)); onToast('We’ve added this link back to the queue.') } catch (e) { setError(errorMessage(e)) } finally { setRetrying(false) } }
  async function deleteCurrentLink() {
    setDeleting(true); setDeleteError('')
    try { await api.deleteLink(id); onToast('Link deleted from your library.'); navigate('/links') }
    catch (e) { setDeleteError(errorMessage(e)) }
    finally { setDeleting(false) }
  }
  if (busy) return <LoadingCard />; if (error || !link) return <ErrorBanner message={error || 'This link could not be found.'} onRetry={load} />
  const failedTitle = link.status === 'EXTRACTION_FAILED' ? 'We couldn’t read this page' : link.status === 'SUMMARIZATION_FAILED' ? 'We couldn’t prepare the summary' : 'This link needs another try'
  const failedBody = link.status === 'EXTRACTION_FAILED' ? 'The page content could not be extracted. You can retry processing this link.' : link.status === 'SUMMARIZATION_FAILED' ? 'The page was read, but its summary could not be prepared. Retry processing to try again.' : 'Something interrupted the process. You can add it back to the queue and we’ll try again.'
  return <><Link to="/links" className="back-link">← Back to my links</Link><div className="detail-header"><div className="detail-kicker"><StatusBadge status={link.status} /><span>Saved {formatDate(link.createdAt)}</span></div><h1>{safeHostname(link.url)}</h1><a className="detail-url" href={link.url} target="_blank" rel="noreferrer">{link.url}<ExternalLink size={14} /></a><div className="detail-actions">{isFailed(link.status) && <button className="button button-outline" disabled={retrying || deleting} onClick={retry}>{retrying ? <LoaderCircle size={15} className="spin" /> : 'Try again'}</button>}<button className="button button-delete" onClick={() => { setDeleteError(''); setDeleteOpen(true) }} disabled={deleting}><Trash2 size={14} /> Delete link</button></div></div>{summary ? <SummaryCard summary={summary} /> : <div className="panel processing-card"><span className="processing-mark">{isFailed(link.status) ? <CircleHelp /> : <LoaderCircle className="spin" />}</span><h2>{isFailed(link.status) ? failedTitle : 'We’re gathering the useful bits'}</h2><p>{isFailed(link.status) ? failedBody : 'We’re reading the page and preparing a clear, concise summary. This page will be ready to revisit as soon as it’s done.'}</p></div>}{deleteOpen && <DeleteLinkDialog link={link} busy={deleting} error={deleteError} onCancel={() => { if (!deleting) setDeleteOpen(false) }} onConfirm={deleteCurrentLink} />}</>
}
function useParamsSafe() { return useParams() }

function SummariesPage() { const [data, setData] = useState<Summary[]>([]), [busy, setBusy] = useState(true), [error, setError] = useState(''); const load = () => { setBusy(true); api.summaries(0, 100).then(result => setData(result.content)).catch(e => setError(errorMessage(e))).finally(() => setBusy(false)) }; useEffect(() => { load() }, [])
  return <><PageTitle eyebrow="YOUR TAKEAWAYS" title="Summaries" subtitle="The useful parts of what you’ve been reading." />{error && <ErrorBanner message={error} onRetry={load} />}{busy ? <LoadingCard /> : data.length ? <div className="summary-grid">{data.map(summary => <SummaryTile key={summary.id} summary={summary} />)}</div> : <EmptyState className="empty-card" icon={<FileText />} title="Your summaries will live here" body="Save a link and we’ll turn it into a short, useful read." action={<Link to="/links" className="button button-dark button-small"><Plus size={15} /> Save a link</Link>} />}</>
}
function SummaryRow({ summary }: { summary: Summary }) { return <Link className="summary-row" to={`/summaries/${summary.id}`}><span className="summary-row-icon"><FileText size={17} /></span><span className="summary-row-copy"><b>{summary.title || safeHostname(summary.linkId)}</b><small>{summary.readingTimeMinutes} min read · {formatDate(summary.createdAt)}</small></span><ArrowRight size={16} /></Link> }
function SummaryTile({ summary }: { summary: Summary }) { return <Link className="panel summary-tile" to={`/summaries/${summary.id}`}><span className="tile-date">{formatDate(summary.createdAt)}</span><h2>{summary.title || 'A useful read'}</h2><p>{summary.summary}</p><div className="takeaway-count"><Sparkles size={14} /> {summary.keyTakeaways.length} key takeaways <ArrowRight size={15} /></div></Link> }
function SummaryCard({ summary }: { summary: Summary }) { return <article className="panel full-summary"><div className="summary-meta"><span className="eyebrow">A CLEARER PICTURE</span><span>{summary.readingTimeMinutes} min read · {summary.wordCount.toLocaleString()} words</span></div><h2>{summary.title || 'Summary'}</h2><p className="summary-lede">{summary.summary}</p>{summary.keyTakeaways.length > 0 && <><div className="section-rule" /><h3>Key takeaways</h3><ul className="takeaway-list">{summary.keyTakeaways.map((item, index) => <li key={index}><span>{String(index + 1).padStart(2, '0')}</span>{item}</li>)}</ul></>}<div className="summary-footer"><span>Created {formatDate(summary.createdAt)}</span><span>Prepared with {summary.aiProvider}</span></div></article> }
function SummaryDetail() { const { id = '' } = useParamsSafe(); const [summary, setSummary] = useState<Summary | null>(null), [error, setError] = useState(''); useEffect(() => { api.summary(id).then(setSummary).catch(e => setError(errorMessage(e))) }, [id]); return <>{error ? <ErrorBanner message={error} onRetry={() => { setError(''); api.summary(id).then(setSummary).catch(e => setError(errorMessage(e))) }} /> : !summary ? <LoadingCard /> : <><Link to="/summaries" className="back-link">← Back to summaries</Link><SummaryCard summary={summary} /><Link className="text-link detail-source" to={`/links/${summary.linkId}`}>View original link <ArrowRight size={15} /></Link></>}</> }

function NotificationsPage({ onUnread }: { onUnread: () => void }) { const [data, setData] = useState<Notification[]>([]), [busy, setBusy] = useState(true), [error, setError] = useState(''); const load = () => { setBusy(true); api.notifications(0, 100).then(result => setData(result.content)).catch(e => setError(errorMessage(e))).finally(() => setBusy(false)) }; useEffect(() => { load() }, [])
  async function markOne(item: Notification) { if (item.read) return; try { await api.markRead(item.id); setData(items => items.map(n => n.id === item.id ? { ...n, read: true } : n)); onUnread() } catch (e) { setError(errorMessage(e)) } }
  async function markAll() { try { await api.markAllRead(); setData(items => items.map(n => ({ ...n, read: true }))); onUnread() } catch (e) { setError(errorMessage(e)) } }
  return <><PageTitle eyebrow="WHAT’S NEW" title="Notifications" subtitle="A quiet nudge when a link is ready or needs attention." action={data.some(n => !n.read) ? <button className="button button-outline button-small" onClick={markAll}><Check size={14} /> Mark all read</button> : undefined} />{error && <ErrorBanner message={error} onRetry={load} />}{busy ? <LoadingCard /> : data.length ? <div className="panel notification-list">{data.map(item => <NotificationRow key={item.id} item={item} onRead={() => markOne(item)} />)}</div> : <EmptyState className="empty-card" icon={<Bell />} title="All caught up" body="When one of your links is ready, you’ll see a note here." />}</> }
function NotificationRow({ item, onRead }: { item: Notification; onRead: () => void }) { const content = <><span className={`notification-mark ${item.type === 'LINK_SUMMARY_READY' ? 'mark-ready' : 'mark-warning'}`}>{item.type === 'LINK_SUMMARY_READY' ? <Check size={17} /> : <CircleHelp size={17} />}</span><span className="notification-copy"><b>{item.title}</b><span>{item.message}</span><small>{formatDate(item.createdAt)}</small></span>{!item.read && <i className="unread-dot" />}</>; return item.linkId ? <Link className={`notification-row ${item.read ? '' : 'notification-unread'}`} to={item.type === 'LINK_SUMMARY_READY' ? `/links/${item.linkId}` : `/links/${item.linkId}`} onClick={onRead}>{content}</Link> : <div className="notification-row" onMouseEnter={onRead}>{content}</div> }

function ProfilePage() { const [profile, setProfile] = useState<Profile | null>(null), [prefs, setPrefs] = useState<NotificationPreferences | null>(null), [busy, setBusy] = useState(true), [saving, setSaving] = useState(false), [error, setError] = useState(''), [message, setMessage] = useState('')
  const load = () => { setBusy(true); Promise.all([api.profile(), api.preferences()]).then(([p, n]) => { setProfile(p); setPrefs(n) }).catch(e => setError(errorMessage(e))).finally(() => setBusy(false)) }; useEffect(() => { load() }, [])
  async function saveProfile(event: React.FormEvent) { event.preventDefault(); if (!profile) return; setSaving(true); setMessage(''); setError(''); try { const saved = await api.updateProfile({ displayName: profile.displayName, bio: profile.bio, avatarUrl: profile.avatarUrl }); setProfile(saved); setMessage('Your profile is up to date.') } catch (e) { setError(errorMessage(e)) } finally { setSaving(false) } }
  async function togglePref(key: 'inAppEnabled' | 'emailEnabled') { if (!prefs) return; const next = { ...prefs, [key]: !prefs[key] }; setPrefs(next); try { setPrefs(await api.updatePreferences({ inAppEnabled: next.inAppEnabled, emailEnabled: next.emailEnabled })) } catch (e) { setPrefs(prefs); setError(errorMessage(e)) } }
  if (busy) return <LoadingCard />
  return <><PageTitle eyebrow="YOUR ACCOUNT" title="Profile & preferences" subtitle="A few details to make this space feel like yours." />{error && <ErrorBanner message={error} onRetry={load} />}{profile && <div className="profile-layout"><form className="panel profile-form" onSubmit={saveProfile}><div className="panel-heading"><div><h2>Your profile</h2><p>Your name and a little about you.</p></div><span className="profile-avatar">{(profile.displayName || profile.email || 'Y').slice(0, 1).toUpperCase()}</span></div><label className="form-label">Display name<input value={profile.displayName ?? ''} maxLength={80} onChange={e => setProfile({ ...profile, displayName: e.target.value || null })} placeholder="What should we call you?" /></label><label className="form-label">Email address<input value={profile.email ?? ''} disabled /><small>Email is managed by your sign-in account.</small></label><label className="form-label">About you<textarea value={profile.bio ?? ''} maxLength={500} rows={4} onChange={e => setProfile({ ...profile, bio: e.target.value || null })} placeholder="A sentence about what you like to read…" /></label><label className="form-label">Avatar image URL <span className="optional">OPTIONAL</span><input type="url" value={profile.avatarUrl ?? ''} onChange={e => setProfile({ ...profile, avatarUrl: e.target.value || null })} placeholder="https://…" /></label>{message && <p className="success-message"><Check size={15} />{message}</p>}<button className="button button-dark" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button></form><section className="panel preference-panel"><div className="panel-heading"><div><h2>Notifications</h2><p>Choose how you’d like to hear from us.</p></div><Settings2 size={18} /></div>{prefs && <><ToggleRow title="In-app notifications" subtitle="See updates in your workspace." checked={prefs.inAppEnabled} onChange={() => togglePref('inAppEnabled')} /><ToggleRow title="Email notifications" subtitle="A short note when a link is ready." checked={prefs.emailEnabled} onChange={() => togglePref('emailEnabled')} /></>}</section></div>}</> }
function ToggleRow({ title, subtitle, checked, onChange }: { title: string; subtitle: string; checked: boolean; onChange: () => void }) { return <div className="toggle-row"><span><b>{title}</b><small>{subtitle}</small></span><button role="switch" aria-checked={checked} aria-label={title} className={`switch ${checked ? 'switch-on' : ''}`} onClick={onChange}><i /></button></div> }

function PageTitle({ eyebrow, title, subtitle, action }: { eyebrow: string; title: string; subtitle: string; action?: ReactNode }) { return <div className="page-title-row"><div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{subtitle}</p></div>{action && <div className="page-title-action">{action}</div>}</div> }
function EmptyState({ icon, title, body, action, className = '' }: { icon: ReactNode; title: string; body: string; action?: ReactNode; className?: string }) { return <div className={`empty-state ${className}`}><span className="empty-icon">{icon}</span><h3>{title}</h3><p>{body}</p>{action}</div> }
function LoadingInline() { return <div className="loading-inline"><LoaderCircle className="spin" size={18} /> Loading your library…</div> }
function LoadingCard() { return <div className="panel loading-card"><LoaderCircle className="spin" size={20} /> Loading your library…</div> }
function ErrorBanner({ message, onRetry }: { message: string; onRetry: () => void }) { return <div role="alert" className="error-banner"><span>{message}</span><button onClick={onRetry}>Try again</button></div> }
function NotFound() { return <EmptyState className="empty-card" icon={<CircleHelp />} title="We couldn’t find that page" body="It may have moved, or the link may no longer be available." action={<Link to="/" className="button button-dark button-small">Back to overview</Link>} /> }
function errorMessage(error: unknown) { return error instanceof Error ? error.message : 'Something went wrong. Please try again.' }
function safeHostname(value: string) { try { return new URL(value).hostname.replace(/^www\./, '') } catch { return value } }
function formatDate(value: string) { const date = new Date(value); return Number.isNaN(date.valueOf()) ? '' : new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', year: 'numeric' }).format(date) }
