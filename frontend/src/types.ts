export type LinkStatus = 'SUBMITTED' | 'PENDING_EXTRACTION' | 'EXTRACTING' | 'EXTRACTED' | 'SUMMARIZING' | 'COMPLETED' | 'EXTRACTION_FAILED' | 'SUMMARIZATION_FAILED' | 'FAILED'

export interface Link { id: string; url: string; status: LinkStatus; createdAt: string; updatedAt: string }
export interface Summary { id: string; linkId: string; contentId: string; title: string | null; summary: string; keyTakeaways: string[]; readingTimeMinutes: number; wordCount: number; aiProvider: string; aiModel: string | null; createdAt: string; updatedAt: string }
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean }
export interface Profile { userId: string; displayName: string | null; email: string | null; bio: string | null; avatarUrl: string | null; createdAt: string; updatedAt: string }
export interface Notification { id: string; linkId: string | null; type: 'LINK_SUMMARY_READY' | 'LINK_PROCESSING_FAILED'; title: string; message: string; read: boolean; createdAt: string }
export interface NotificationPreferences { inAppEnabled: boolean; emailEnabled: boolean; updatedAt: string }

export const statusLabels: Record<LinkStatus, string> = {
  SUBMITTED: 'Queued', PENDING_EXTRACTION: 'Queued', EXTRACTING: 'Reading page', EXTRACTED: 'Creating summary', SUMMARIZING: 'Creating summary', COMPLETED: 'Ready', EXTRACTION_FAILED: 'Extraction failed', SUMMARIZATION_FAILED: 'Summary failed', FAILED: 'Processing failed',
}
export const isFailed = (status: LinkStatus) => ['EXTRACTION_FAILED', 'SUMMARIZATION_FAILED', 'FAILED'].includes(status)

export type WebsiteAnalysisStatus = 'QUEUED' | 'CRAWLING' | 'EXTRACTING' | 'ANALYZING' | 'COMPLETED' | 'PARTIALLY_COMPLETED' | 'FAILED'

export interface WebsiteAnalysis {
  id: string
  url: string
  domain: string | null
  title: string | null
  status: WebsiteAnalysisStatus
  riskLevel: string | null
  confidence: number | null
  category: string | null
  report: string | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
  completedAt: string | null
}

export interface TrackedWebsite {
  id: string
  name: string
  domain: string
  url: string
  category: string | null
  accountStatus: string | null
  privacyPolicyUrl: string | null
  deletionPolicyUrl: string | null
  riskLevel: string | null
  lastInspectionAt: string | null
  dataCategories: string | null
  trackingTechnologies: string | null
  deletionRequestStatus: string | null
  fromAnalysisId: string | null
  firstRecordedAt: string | null
  createdAt: string
  updatedAt: string
}

export type PrivacyDataSource = 'USER_CONFIRMED' | 'OBSERVED_ON_WEBSITE' | 'DECLARED_IN_POLICY' | 'INFERRED_BY_AI' | 'UNKNOWN'

export interface PersonalDataRecord {
  id: string
  trackedWebsiteId: string
  dataCategory: string
  dataType: string
  description: string | null
  source: PrivacyDataSource
  confirmedByUser: boolean
  observedOnWebsite: boolean
  declaredInPolicy: boolean
  inferredByAi: boolean
  dateShared: string | null
  evidenceSource: string | null
  lastVerifiedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface UserWebsiteAccount {
  id: string
  trackedWebsiteId: string
  accountIdentifier: string | null
  accountCreationDate: string | null
  accountCategory: string | null
  sharedInformation: string | null
  accountStatus: string | null
  discoverySource: 'MANUAL' | 'CONNECTED_EMAIL'
  confirmed: boolean
  createdAt: string
  updatedAt: string
}

export type CookieCategory = 'ESSENTIAL' | 'FUNCTIONAL' | 'ANALYTICS' | 'ADVERTISING' | 'SOCIAL_MEDIA' | 'UNKNOWN'

export interface CookieRecord {
  id: string
  trackedWebsiteId: string | null
  browserIntegrationId: string | null
  cookieName: string
  domain: string
  firstParty: boolean
  expiration: string | null
  purpose: string | null
  category: CookieCategory
  secure: boolean
  httpOnly: boolean
  sameSite: string | null
  trackingRelated: boolean
  source: string
  createdAt: string
  updatedAt: string
}

export interface CookieMetadata {
  name: string
  domain: string
  firstParty?: boolean
  expiration?: string
  purpose?: string
  category?: CookieCategory
  secure?: boolean
  httpOnly?: boolean
  sameSite?: string
  trackingRelated?: boolean
}

export interface BrowserIntegration {
  id: string
  installationId: string
  browserName: string | null
  extensionVersion: string | null
  deviceLabel: string | null
  lastSyncAt: string | null
  createdAt: string
  updatedAt: string
}

export interface WebsitePrivacyContact {
  id: string
  trackedWebsiteId: string | null
  privacyPolicyUrl: string | null
  deletionPolicyUrl: string | null
  accessRequestUrl: string | null
  accountDeletionUrl: string | null
  cookieManagementUrl: string | null
  consentManagementUrl: string | null
  privacyContactEmail: string | null
  dpoContact: string | null
  sourceUrls: string | null
  discoveredAt: string
  updatedAt: string
}

export interface PrivacyPreference {
  id: string
  analyticsCookies: boolean
  advertisingCookies: boolean
  marketingCommunications: boolean
  thirdPartyTracking: boolean
  personalizedAdvertising: boolean
  dataSharing: boolean
  updatedAt: string
}

export interface PrivacyMetrics {
  totalWebsites: number
  sensitiveWebsites: number
  websitesWithTrackers: number
  trackerCookies: number
  trackedCookies: number
  pendingDeletions: number
  completedDeletions: number
  awaitingApproval: number
  outstandingActions: number
}

export type PrivacyRequestStatus = 'DRAFT' | 'READY_FOR_REVIEW' | 'AWAITING_USER_APPROVAL' | 'SUBMITTED' | 'ACKNOWLEDGED' | 'IN_PROGRESS' | 'NEEDS_VERIFICATION' | 'COMPLETED' | 'REJECTED' | 'EXPIRED' | 'FAILED' | 'CANCELLED'

export type PrivacyRequestType = 'ACCESS' | 'DELETION' | 'ACCOUNT_DELETION' | 'CORRECTION' | 'MARKETING_OPT_OUT' | 'TRACKING_OPT_OUT' | 'COOKIE_REMOVAL' | 'CONSENT_WITHDRAWAL' | 'PORTABILITY'

export interface PrivacyRequest {
  id: string
  trackedWebsiteId: string | null
  requestType: PrivacyRequestType
  status: PrivacyRequestStatus
  requestMessage: string | null
  externalReference: string | null
  privacyContactUrl: string | null
  privacyContactEmail: string | null
  dataCategories: string | null
  preferredContact: string | null
  legalBasis: string | null
  submissionMethod: string | null
  submissionEvidence: string | null
  expectedResponseDate: string | null
  submittedAt: string | null
  acknowledgedAt: string | null
  completedAt: string | null
  websiteResponse: string | null
  userVerified: boolean
  websiteClaimedDeletion: boolean
  reminderAt: string | null
  lastReminderAt: string | null
  approvedAt: string | null
  cancelledAt: string | null
  createdAt: string
  updatedAt: string
}

export interface PrivacyRequestEvidence {
  id: string
  requestId: string
  type: 'SUBMISSION' | 'RESPONSE' | 'VERIFICATION' | 'OTHER'
  description: string | null
  referenceUrl: string | null
  createdAt: string
}

export interface PrivacyRequestHistory {
  id: string
  requestId: string
  fromStatus: string | null
  toStatus: string
  note: string | null
  createdAt: string
}

export const privacyRequestStatusLabels: Record<PrivacyRequestStatus, string> = {
  DRAFT: 'Draft', READY_FOR_REVIEW: 'Ready for review', AWAITING_USER_APPROVAL: 'Awaiting your approval', SUBMITTED: 'Submitted', ACKNOWLEDGED: 'Acknowledged', IN_PROGRESS: 'In progress', NEEDS_VERIFICATION: 'Needs verification', COMPLETED: 'Completed', REJECTED: 'Rejected', EXPIRED: 'Expired', FAILED: 'Failed', CANCELLED: 'Cancelled',
}

export const privacyRequestTypeLabels: Record<PrivacyRequestType, string> = {
  ACCESS: 'Access my data', DELETION: 'Delete my data', ACCOUNT_DELETION: 'Delete my account', CORRECTION: 'Correct my data', MARKETING_OPT_OUT: 'Marketing opt-out', TRACKING_OPT_OUT: 'Tracking opt-out', COOKIE_REMOVAL: 'Cookie removal', CONSENT_WITHDRAWAL: 'Withdraw consent', PORTABILITY: 'Data portability',
}
