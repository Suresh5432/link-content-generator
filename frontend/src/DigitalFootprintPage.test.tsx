import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { DigitalFootprintPage } from './DigitalFootprintPage'
import { api } from './api'
import type { PrivacyMetrics, PrivacyPreference, PrivacyRequest, TrackedWebsite } from './types'

vi.mock('./api', () => ({
  api: {
    digitalFootprintWebsites: vi.fn(), digitalFootprintMetrics: vi.fn(), allDataRecords: vi.fn(),
    cookies: vi.fn(), privacyRequests: vi.fn(), privacyPreferences: vi.fn(), browserIntegrations: vi.fn(),
    websiteAnalyses: vi.fn(), privacyAssistant: vi.fn(), privacyRequestDraft: vi.fn(), updatePrivacyPreferences: vi.fn(),
    createDigitalFootprintWebsite: vi.fn(), updateDigitalFootprintWebsite: vi.fn(), deleteDigitalFootprintWebsite: vi.fn(),
    importDigitalFootprintWebsite: vi.fn(), websiteAnalysisFindings: vi.fn(), updatePrivacyContact: vi.fn(),
    dataRecords: vi.fn(), createDataRecord: vi.fn(), deleteDataRecord: vi.fn(),
    createPrivacyRequest: vi.fn(), updatePrivacyRequest: vi.fn(), approvePrivacyRequest: vi.fn(),
    submitPrivacyRequest: vi.fn(), cancelPrivacyRequest: vi.fn(), recordPrivacyResponse: vi.fn(),
    verifyPrivacyRequest: vi.fn(), privacyRequestEvidence: vi.fn(), privacyRequestHistory: vi.fn(),
    privacyRequestReminders: vi.fn(), exportDigitalFootprint: vi.fn(), deleteDigitalFootprintAccountData: vi.fn(),
    websiteAccounts: vi.fn(), createWebsiteAccount: vi.fn(), deleteWebsiteAccount: vi.fn(),
    searchDigitalFootprintWebsites: vi.fn(), requestPrivacyReview: vi.fn(), addPrivacyRequestEvidence: vi.fn(),
    updatePrivacyRequestStatus: vi.fn(), updateDataRecord: vi.fn(), registerBrowserIntegration: vi.fn(),
    deleteBrowserIntegration: vi.fn(),
    connectedEmailStatus: vi.fn(), connectedEmailAccounts: vi.fn(), connectedEmailDiscoveries: vi.fn(),
    startConnectedEmailOAuth: vi.fn(), scanConnectedEmail: vi.fn(), confirmEmailDiscovery: vi.fn(),
    dismissEmailDiscovery: vi.fn(), deleteEmailDiscovery: vi.fn(), disconnectConnectedEmail: vi.fn(),
  },
  ApiError: class ApiError extends Error { constructor(message: string, public status: number) { super(message) } },
}))

const page = <T,>(content: T[]) => ({ content, page: 0, size: 100, totalElements: content.length, totalPages: 1, first: true, last: true })

const metrics: PrivacyMetrics = { totalWebsites: 1, sensitiveWebsites: 1, websitesWithTrackers: 1, trackerCookies: 2, trackedCookies: 5, pendingDeletions: 1, completedDeletions: 0, awaitingApproval: 0, outstandingActions: 1 }
const website: TrackedWebsite = { id: 'w1', name: 'Example Bank', domain: 'example.com', url: 'https://example.com', category: 'Finance', accountStatus: 'active', privacyPolicyUrl: null, deletionPolicyUrl: null, riskLevel: 'HIGH', lastInspectionAt: null, dataCategories: 'email, address', trackingTechnologies: '_ga', deletionRequestStatus: null, fromAnalysisId: 'a1', firstRecordedAt: new Date().toISOString(), createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() }
const prefs: PrivacyPreference = { id: 'p1', analyticsCookies: false, advertisingCookies: false, marketingCommunications: true, thirdPartyTracking: false, personalizedAdvertising: false, dataSharing: false, updatedAt: new Date().toISOString() }

beforeEach(() => {
  vi.mocked(api.digitalFootprintWebsites).mockResolvedValue(page([website]))
  vi.mocked(api.digitalFootprintMetrics).mockResolvedValue(metrics)
  vi.mocked(api.allDataRecords).mockResolvedValue([])
  vi.mocked(api.cookies).mockResolvedValue([])
  vi.mocked(api.privacyRequests).mockResolvedValue([])
  vi.mocked(api.privacyPreferences).mockResolvedValue(prefs)
  vi.mocked(api.browserIntegrations).mockResolvedValue([])
  vi.mocked(api.websiteAnalyses).mockResolvedValue(page([]))
  vi.mocked(api.privacyRequestReminders).mockResolvedValue([])
  vi.mocked(api.websiteAccounts).mockResolvedValue([])
  vi.mocked(api.connectedEmailStatus).mockResolvedValue({ configured: false, provider: 'GOOGLE', requiredScope: 'gmail.metadata', limitation: 'Gmail discovery is not configured in this deployment.' })
  vi.mocked(api.connectedEmailAccounts).mockResolvedValue([])
  vi.mocked(api.connectedEmailDiscoveries).mockResolvedValue([])
  vi.mocked(api.searchDigitalFootprintWebsites).mockResolvedValue([website])
  vi.mocked(api.updatePrivacyPreferences).mockResolvedValue({ ...prefs, analyticsCookies: true })
})

afterEach(() => { cleanup(); vi.clearAllMocks() })

describe('My Digital Footprint', () => {
  it('shows the dashboard metrics and outstanding actions', async () => {
    render(<DigitalFootprintPage />)
    expect(await screen.findByRole('heading', { name: 'My Digital Footprint' })).toBeInTheDocument()
    expect(await screen.findByText('Tracked websites')).toBeInTheDocument()
    expect(await screen.findByText('Pending deletions')).toBeInTheDocument()
    expect(await screen.findByText('Outstanding actions')).toBeInTheDocument()
  })

  it('lists tracked websites with data and tracker indicators', async () => {
    render(<DigitalFootprintPage />)
    fireEvent.click(await screen.findByRole('tab', { name: 'My websites' }))
    expect(await screen.findByRole('heading', { name: 'Example Bank' })).toBeInTheDocument()
    expect(screen.getByText(/Data: email, address/)).toBeInTheDocument()
    expect(screen.getByText(/Trackers: _ga/)).toBeInTheDocument()
  })

  it('keeps footprint data visible when Website Intelligence analyses are unavailable', async () => {
    vi.mocked(api.websiteAnalyses).mockRejectedValue(new Error('Request failed (404)'))
    render(<DigitalFootprintPage />)
    fireEvent.click(await screen.findByRole('tab', { name: 'My websites' }))
    expect(await screen.findByRole('heading', { name: 'Example Bank' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(/Website Intelligence analyses are unavailable/)
  })

  it('answers a privacy question using the assistant endpoint', async () => {
    vi.mocked(api.privacyAssistant).mockResolvedValue({ answer: 'Example Bank has your email address (confirmed by you).' })
    render(<DigitalFootprintPage />)
    fireEvent.click(await screen.findByRole('tab', { name: 'Privacy assistant' }))
    const input = await screen.findByPlaceholderText('Which websites have my email address?')
    fireEvent.change(input, { target: { value: 'Which websites have my email address?' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ask' }))
    await waitFor(() => expect(api.privacyAssistant).toHaveBeenCalled())
    expect(await screen.findByText(/Example Bank has your email address/)).toBeInTheDocument()
  })

  it('drafts a privacy request with AI only after choosing a website', async () => {
    vi.mocked(api.privacyRequestDraft).mockResolvedValue({ message: 'Subject: Request for Deletion\n\nHello Privacy Team,' })
    vi.mocked(api.createPrivacyRequest).mockResolvedValue({ id: 'r1' } as PrivacyRequest)
    render(<DigitalFootprintPage />)
    fireEvent.click(await screen.findByRole('tab', { name: 'Remove my data' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Draft with AI' }))
    await waitFor(() => expect(api.privacyRequestDraft).not.toHaveBeenCalled())
    fireEvent.change(screen.getByLabelText('Website'), { target: { value: 'w1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Draft with AI' }))
    await waitFor(() => expect(api.privacyRequestDraft).toHaveBeenCalledWith({ requestType: 'DELETION', websiteName: 'Example Bank', dataCategories: undefined }))
    await waitFor(() => expect(api.createPrivacyRequest).toHaveBeenCalledWith(expect.objectContaining({ requestMessage: 'Subject: Request for Deletion\n\nHello Privacy Team,' })))
  })

  it('updates a privacy preference through the API', async () => {
    render(<DigitalFootprintPage />)
    fireEvent.click(await screen.findByRole('tab', { name: 'Preferences' }))
    fireEvent.click(await screen.findByRole('switch', { name: 'Analytics cookies' }))
    await waitFor(() => expect(api.updatePrivacyPreferences).toHaveBeenCalledWith({ analyticsCookies: true }))
  })

  it('shows follow-up reminders and lets the user open them', async () => {
    vi.mocked(api.privacyRequestReminders).mockResolvedValue([{ id: 'r9', trackedWebsiteId: 'w1', requestType: 'DELETION', status: 'SUBMITTED', createdAt: new Date().toISOString() } as PrivacyRequest])
    render(<DigitalFootprintPage />)
    fireEvent.click(await screen.findByRole('tab', { name: 'Remove my data' }))
    expect(await screen.findByText('Follow-ups due')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Open' }))
    await waitFor(() => expect(api.privacyRequestEvidence).toHaveBeenCalledWith('r9'))
  })

  it('lists manually recorded website accounts', async () => {
    vi.mocked(api.websiteAccounts).mockResolvedValue([{
      id: 'a1', trackedWebsiteId: 'w1', accountIdentifier: 'me@example.com', accountCreationDate: null,
      accountCategory: null, sharedInformation: 'email', accountStatus: 'active', discoverySource: 'MANUAL',
      confirmed: true, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(),
    }])
    render(<DigitalFootprintPage />)
    fireEvent.click(await screen.findByRole('tab', { name: 'My websites' }))
    expect(await screen.findByText('me@example.com', { exact: false })).toBeTruthy()
    expect(screen.getByText(/confirmed by you/)).toBeTruthy()
  })

  it('explains provider configuration and does not offer a fake connection', async () => {
    render(<DigitalFootprintPage />)
    fireEvent.click(await screen.findByRole('tab', { name: 'My websites' }))
    expect(await screen.findByText(/Gmail discovery is not configured/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Connect Gmail' })).not.toBeInTheDocument()
  })

  it('labels email matches as possible accounts and allows user confirmation', async () => {
    vi.mocked(api.connectedEmailStatus).mockResolvedValue({
      configured: true, provider: 'GOOGLE', requiredScope: 'gmail.metadata',
      limitation: 'Checks up to 100 message headers only.',
    })
    vi.mocked(api.connectedEmailAccounts).mockResolvedValue([{
      id: 'gmail-1', provider: 'GOOGLE', accountEmail: 'person@example.test', status: 'CONNECTED',
      consentedAt: new Date().toISOString(), lastScanAt: null, createdAt: new Date().toISOString(),
    }])
    vi.mocked(api.connectedEmailDiscoveries).mockResolvedValue([{
      id: 'discovery-1', connectedAccountId: 'gmail-1', provider: 'GOOGLE', domain: 'shop.example',
      evidenceType: 'REGISTRATION_EMAIL', evidenceSummary: 'Registration-related message header matched.',
      firstSeenAt: null, lastSeenAt: null, confidence: 'LOW', confirmedByUser: false, dismissedByUser: false,
    }])
    vi.mocked(api.confirmEmailDiscovery).mockResolvedValue({
      id: 'discovery-1', connectedAccountId: 'gmail-1', provider: 'GOOGLE', domain: 'shop.example',
      evidenceType: 'REGISTRATION_EMAIL', evidenceSummary: 'Registration-related message header matched.',
      firstSeenAt: null, lastSeenAt: null, confidence: 'LOW', confirmedByUser: true, dismissedByUser: false,
    })

    render(<DigitalFootprintPage />)
    fireEvent.click(await screen.findByRole('tab', { name: 'My websites' }))
    expect(await screen.findByText('Possible account — needs your confirmation · Low confidence')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Confirm' }))
    await waitFor(() => expect(api.confirmEmailDiscovery).toHaveBeenCalledWith('discovery-1'))
  })
})
