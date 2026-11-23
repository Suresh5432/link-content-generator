import keycloak from './auth'
import type { BrowserIntegration, CookieMetadata, CookieRecord, Link, LinkStatus, Notification, NotificationPreferences, Page, PersonalDataRecord, PrivacyMetrics, PrivacyPreference, PrivacyRequest, PrivacyRequestEvidence, PrivacyRequestHistory, Profile, Summary, TrackedWebsite, UserWebsiteAccount, WebsiteAnalysis, WebsitePrivacyContact } from './types'

const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8084').replace(/\/$/, '')

export class ApiError extends Error {
  constructor(message: string, readonly status: number) { super(message); this.name = 'ApiError' }
}

let tokenRefresh: Promise<boolean> | undefined
let loginRedirect: Promise<void> | undefined

function refreshToken(minValidity: number) {
  if (!tokenRefresh) {
    tokenRefresh = keycloak.updateToken(minValidity).finally(() => { tokenRefresh = undefined })
  }
  return tokenRefresh
}

function signInAgain() {
  if (!loginRedirect) {
    loginRedirect = Promise.resolve().then(() => {
      location.assign('/login?sessionExpired=1')
    })
  }
  return loginRedirect
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  if (!keycloak.authenticated) {
    await signInAgain()
    throw new ApiError('Your session has ended. Signing you in again.', 401)
  }
  try {
    await refreshToken(30)
  } catch {
    await signInAgain()
    throw new ApiError('Your session has ended. Signing you in again.', 401)
  }

  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body) headers.set('Content-Type', 'application/json')
  if (keycloak.token) headers.set('Authorization', `Bearer ${keycloak.token}`)

  const url = `${baseUrl}${path}`
  let response = await fetch(url, { ...init, headers })
  if (response.status === 401) {
    try {
      await refreshToken(-1)
    } catch {
      await signInAgain()
      throw new ApiError('Your session has ended. Signing you in again.', 401)
    }
    if (!keycloak.authenticated || !keycloak.token) {
      await signInAgain()
      throw new ApiError('Your session has ended. Signing you in again.', 401)
    }
    headers.set('Authorization', `Bearer ${keycloak.token}`)
    response = await fetch(url, { ...init, headers })
    if (response.status === 401) {
      throw new ApiError('The API rejected this session. Sign out and sign in again, or contact your administrator.', 401)
    }
  }
  if (response.status === 403) {
    try {
      await refreshToken(-1)
    } catch {
      await signInAgain()
      throw new ApiError('Your session has ended. Sign in again to continue.', 401)
    }
    if (!keycloak.authenticated || !keycloak.token) {
      await signInAgain()
      throw new ApiError('Your session has ended. Sign in again to continue.', 401)
    }
    headers.set('Authorization', `Bearer ${keycloak.token}`)
    response = await fetch(url, { ...init, headers })
    if (response.status === 403) {
      throw new ApiError('Your account does not have access to this feature. Sign out and back in, then try again.', 403)
    }
  }
  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try { const body = await response.json() as { message?: string; detail?: string }; message = body.message ?? body.detail ?? message } catch { /* use status text */ }
    if (response.status === 403) message = 'Your account does not have access to this feature. Sign out and back in, then try again.'
    throw new ApiError(message, response.status)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

const query = (params: Record<string, string | number | undefined>) => {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => { if (value !== undefined && value !== '') search.set(key, String(value)) })
  return search.size ? `?${search}` : ''
}

export const api = {
  links: (page = 0, size = 20, status?: LinkStatus) => request<Page<Link>>(`/api/v1/links${query({ page, size, status, sortBy: 'createdAt', direction: 'DESC' })}`),
  createLink: (url: string) => request<Link>('/api/v1/links', { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify({ url }) }),
  link: (id: string) => request<Link>(`/api/v1/links/${encodeURIComponent(id)}`),
  deleteLink: (id: string) => request<void>(`/api/v1/links/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  retryLink: (id: string) => request<Link>(`/api/v1/links/${encodeURIComponent(id)}/retry`, { method: 'POST' }),
  summaries: (page = 0, size = 20) => request<Page<Summary>>(`/api/v1/summaries${query({ page, size, sortBy: 'createdAt', direction: 'DESC' })}`),
  summary: (id: string) => request<Summary>(`/api/v1/summaries/${encodeURIComponent(id)}`),
  summaryForLink: (id: string) => request<Summary>(`/api/v1/summaries/link/${encodeURIComponent(id)}`),
  websiteAnalyses: (page = 0, size = 20) => request<Page<WebsiteAnalysis>>(`/api/v1/website-analyses${query({ page, size })}`),
  createWebsiteAnalysis: (url: string) => request<WebsiteAnalysis>('/api/v1/website-analyses', { method: 'POST', body: JSON.stringify({ url }) }),
  websiteAnalysis: (id: string) => request<WebsiteAnalysis>(`/api/v1/website-analyses/${encodeURIComponent(id)}`),
  websiteAnalysisReport: (id: string) => request<unknown>(`/api/v1/website-analyses/${encodeURIComponent(id)}/report`),
  websiteAnalysisFindings: (id: string) => request<unknown[]>(`/api/v1/website-analyses/${encodeURIComponent(id)}/findings`),
  digitalFootprintWebsites: (page = 0, size = 100) => request<Page<TrackedWebsite>>(`/api/v1/digital-footprint/websites${query({ page, size })}`),
  searchDigitalFootprintWebsites: (params: { query?: string; category?: string; riskLevel?: string; deletionStatus?: string; sort?: string }) => request<TrackedWebsite[]>(`/api/v1/digital-footprint/websites/search${query(params)}`),
  digitalFootprintWebsite: (id: string) => request<TrackedWebsite>(`/api/v1/digital-footprint/websites/${encodeURIComponent(id)}`),
  createDigitalFootprintWebsite: (data: { name: string; domain: string; url: string; category?: string; privacyPolicyUrl?: string; deletionPolicyUrl?: string }) => request<TrackedWebsite>('/api/v1/digital-footprint/websites', { method: 'POST', body: JSON.stringify(data) }),
  importDigitalFootprintWebsite: (data: { analysisId: string; name: string; domain: string; url: string; category?: string; riskLevel?: string; dataCategories?: string; trackingTechnologies?: string }) => request<TrackedWebsite>('/api/v1/digital-footprint/websites/import', { method: 'POST', body: JSON.stringify(data) }),
  updateDigitalFootprintWebsite: (id: string, data: Partial<Record<string, unknown>>) => request<TrackedWebsite>(`/api/v1/digital-footprint/websites/${encodeURIComponent(id)}`, { method: 'PATCH', body: JSON.stringify(data) }),
  deleteDigitalFootprintWebsite: (id: string) => request<void>(`/api/v1/digital-footprint/websites/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  digitalFootprintMetrics: () => request<PrivacyMetrics>('/api/v1/digital-footprint/websites/metrics'),
  dataRecords: (websiteId: string) => request<PersonalDataRecord[]>(`/api/v1/digital-footprint/websites/${encodeURIComponent(websiteId)}/data-records`),
  allDataRecords: () => request<PersonalDataRecord[]>('/api/v1/digital-footprint/data-records'),
  createDataRecord: (websiteId: string, data: { dataCategory: string; dataType: string; description?: string; source?: string; dateShared?: string; evidenceSource?: string }) => request<PersonalDataRecord>(`/api/v1/digital-footprint/websites/${encodeURIComponent(websiteId)}/data-records`, { method: 'POST', body: JSON.stringify(data) }),
  updateDataRecord: (recordId: string, data: Partial<Record<string, unknown>>) => request<PersonalDataRecord>(`/api/v1/digital-footprint/data-records/${encodeURIComponent(recordId)}`, { method: 'PATCH', body: JSON.stringify(data) }),
  deleteDataRecord: (recordId: string) => request<void>(`/api/v1/digital-footprint/data-records/${encodeURIComponent(recordId)}`, { method: 'DELETE' }),
  websiteAccounts: (websiteId?: string) => request<UserWebsiteAccount[]>(`/api/v1/digital-footprint/accounts${query({ websiteId })}`),
  createWebsiteAccount: (data: { trackedWebsiteId: string; accountIdentifier?: string; accountCreationDate?: string; accountCategory?: string; sharedInformation?: string; accountStatus?: string; discoverySource?: string; confirmed?: boolean }) => request<UserWebsiteAccount>('/api/v1/digital-footprint/accounts', { method: 'POST', body: JSON.stringify(data) }),
  updateWebsiteAccount: (id: string, data: Partial<Record<string, unknown>>) => request<UserWebsiteAccount>(`/api/v1/digital-footprint/accounts/${encodeURIComponent(id)}`, { method: 'PATCH', body: JSON.stringify(data) }),
  deleteWebsiteAccount: (id: string) => request<void>(`/api/v1/digital-footprint/accounts/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  privacyContact: (websiteId: string) => request<WebsitePrivacyContact>(`/api/v1/digital-footprint/websites/${encodeURIComponent(websiteId)}/privacy-contact`),
  updatePrivacyContact: (websiteId: string, data: Partial<Record<string, unknown>>) => request<WebsitePrivacyContact>(`/api/v1/digital-footprint/websites/${encodeURIComponent(websiteId)}/privacy-contact`, { method: 'PUT', body: JSON.stringify(data) }),
  cookies: (domain?: string) => request<CookieRecord[]>(`/api/v1/cookies${query({ domain })}`),
  cookieDomains: () => request<string[]>('/api/v1/cookies/domains'),
  trackingCookies: () => request<CookieRecord[]>('/api/v1/cookies/tracking'),
  deleteCookieMetadata: (id: string) => request<{ message: string }>(`/api/v1/cookies/inventory/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  browserIntegrations: () => request<BrowserIntegration[]>('/api/v1/browser-integrations'),
  registerBrowserIntegration: (data: { installationId: string; browserName?: string; extensionVersion?: string; deviceLabel?: string }) => request<BrowserIntegration>('/api/v1/browser-integrations/register', { method: 'POST', body: JSON.stringify(data) }),
  syncCookies: (data: { installationId: string; cookies: CookieMetadata[] }) => request<{ integrationId: string; cookiesStored: number; domains: number }>('/api/v1/browser-integrations/sync', { method: 'POST', body: JSON.stringify(data) }),
  deleteBrowserIntegration: (id: string) => request<void>(`/api/v1/browser-integrations/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  privacyPreferences: () => request<PrivacyPreference>('/api/v1/privacy-preferences'),
  updatePrivacyPreferences: (data: Partial<Record<string, boolean>>) => request<PrivacyPreference>('/api/v1/privacy-preferences', { method: 'PUT', body: JSON.stringify(data) }),
  privacyRequests: () => request<PrivacyRequest[]>('/api/v1/privacy-requests'),
  privacyRequest: (id: string) => request<PrivacyRequest>(`/api/v1/privacy-requests/${encodeURIComponent(id)}`),
  createPrivacyRequest: (data: { trackedWebsiteId?: string; requestType: string; requestMessage?: string; privacyContactUrl?: string; privacyContactEmail?: string; dataCategories?: string; preferredContact?: string; legalBasis?: string }) => request<PrivacyRequest>('/api/v1/privacy-requests', { method: 'POST', body: JSON.stringify(data) }),
  updatePrivacyRequest: (id: string, data: Partial<Record<string, unknown>>) => request<PrivacyRequest>(`/api/v1/privacy-requests/${encodeURIComponent(id)}`, { method: 'PATCH', body: JSON.stringify(data) }),
  requestPrivacyReview: (id: string) => request<PrivacyRequest>(`/api/v1/privacy-requests/${encodeURIComponent(id)}/review`, { method: 'POST' }),
  approvePrivacyRequest: (id: string) => request<PrivacyRequest>(`/api/v1/privacy-requests/${encodeURIComponent(id)}/approve`, { method: 'POST' }),
  submitPrivacyRequest: (id: string, data?: { submissionMethod?: string; externalReference?: string }) => request<PrivacyRequest>(`/api/v1/privacy-requests/${encodeURIComponent(id)}/submit`, { method: 'POST', body: JSON.stringify(data ?? {}) }),
  cancelPrivacyRequest: (id: string) => request<PrivacyRequest>(`/api/v1/privacy-requests/${encodeURIComponent(id)}/cancel`, { method: 'POST' }),
  updatePrivacyRequestStatus: (id: string, status: string, note?: string) => request<PrivacyRequest>(`/api/v1/privacy-requests/${encodeURIComponent(id)}/status`, { method: 'PATCH', body: JSON.stringify({ status, note }) }),
  recordPrivacyResponse: (id: string, response: string, websiteClaimedDeletion: boolean) => request<PrivacyRequest>(`/api/v1/privacy-requests/${encodeURIComponent(id)}/response`, { method: 'POST', body: JSON.stringify({ response, websiteClaimedDeletion }) }),
  verifyPrivacyRequest: (id: string, userVerified: boolean) => request<PrivacyRequest>(`/api/v1/privacy-requests/${encodeURIComponent(id)}/verify`, { method: 'POST', body: JSON.stringify({ userVerified }) }),
  privacyRequestEvidence: (id: string) => request<PrivacyRequestEvidence[]>(`/api/v1/privacy-requests/${encodeURIComponent(id)}/evidence`),
  addPrivacyRequestEvidence: (id: string, data: { type: string; description?: string; referenceUrl?: string }) => request<PrivacyRequestEvidence>(`/api/v1/privacy-requests/${encodeURIComponent(id)}/evidence`, { method: 'POST', body: JSON.stringify(data) }),
  privacyRequestHistory: (id: string) => request<PrivacyRequestHistory[]>(`/api/v1/privacy-requests/${encodeURIComponent(id)}/history`),
  privacyRequestReminders: () => request<PrivacyRequest[]>('/api/v1/privacy-requests/reminders'),
  privacyAssistant: (question: string) => request<{ answer: string }>('/api/v1/privacy-assistant', { method: 'POST', body: JSON.stringify({ question }) }),
  privacyRequestDraft: (data: { requestType: string; websiteName: string; dataCategories?: string }) => request<{ message: string }>('/api/v1/privacy-request-drafts', { method: 'POST', body: JSON.stringify(data) }),
  exportDigitalFootprint: () => request<Record<string, unknown>>('/api/v1/digital-footprint/export'),
  deleteDigitalFootprintAccountData: () => request<Record<string, unknown>>('/api/v1/digital-footprint/account-data', { method: 'DELETE' }),
  retryWebsiteAnalysis: (id: string) => request<WebsiteAnalysis>(`/api/v1/website-analyses/${encodeURIComponent(id)}/retry`, { method: 'POST' }),
  updateWebsiteAnalysisReport: (id: string, report: string) => request<void>(`/api/v1/website-analyses/${encodeURIComponent(id)}/report`, { method: 'PATCH', body: JSON.stringify(report) }),
  websiteAnalysisAiReport: (analysisId: string) => request<string>('/api/v1/website-analysis-reports', { method: 'POST', body: JSON.stringify({ analysisId }) }),
  profile: () => request<Profile>('/api/v1/users/me'),
  updateProfile: (data: Pick<Profile, 'displayName' | 'bio' | 'avatarUrl'>) => request<Profile>('/api/v1/users/me', { method: 'PATCH', body: JSON.stringify(data) }),
  notifications: (page = 0, size = 20) => request<Page<Notification>>(`/api/v1/notifications${query({ page, size, direction: 'DESC' })}`),
  unreadCount: () => request<{ count: number }>('/api/v1/notifications/unread-count'),
  markRead: (id: string) => request<void>(`/api/v1/notifications/${encodeURIComponent(id)}/read`, { method: 'PATCH' }),
  markAllRead: () => request<{ updated: number }>('/api/v1/notifications/read-all', { method: 'PATCH' }),
  preferences: () => request<NotificationPreferences>('/api/v1/notifications/preferences'),
  updatePreferences: (data: Pick<NotificationPreferences, 'inAppEnabled' | 'emailEnabled'>) => request<NotificationPreferences>('/api/v1/notifications/preferences', { method: 'PUT', body: JSON.stringify(data) }),
}
