// Pure metadata helpers. No Chrome APIs and never any cookie values.

export const UPLOAD_FIELDS = ['name', 'domain', 'hostOnly', 'secure', 'httpOnly', 'sameSite', 'expirationDate']

export function categoryFor(name) {
  const n = String(name).toLowerCase()
  if (/(^_ga|_gid|_gat|amplitude|mixpanel|_hjid|hotjar|segment|plausible|matomo)/.test(n)) return 'ANALYTICS'
  if (/(_fbp|fbp|doubleclick|ads|advert|gads|_gcl|criteo|taboola|outbrain)/.test(n)) return 'ADVERTISING'
  if (/(li_|_li|tw_|guest_id|personalization_id|_pinterest)/.test(n)) return 'SOCIAL_MEDIA'
  if (/(session|auth|token|sid|logged|remember)/.test(n)) return 'ESSENTIAL'
  if (/(lang|locale|pref|theme|consent|currency)/.test(n)) return 'FUNCTIONAL'
  return 'UNKNOWN'
}

export function toMetadata(cookie) {
  const meta = {}
  for (const key of UPLOAD_FIELDS) meta[key] = cookie[key]
  meta.firstParty = Boolean(cookie.hostOnly)
  meta.trackingRelated = ['ANALYTICS', 'ADVERTISING', 'SOCIAL_MEDIA'].includes(categoryFor(cookie.name))
  meta.category = categoryFor(cookie.name)
  meta.purpose = meta.trackingRelated ? 'Possible analytics/advertising tracking' : null
  return {
    upload: meta,
    local: {
      name: cookie.name,
      domain: cookie.domain,
      path: cookie.path,
      secure: cookie.secure,
      storeId: cookie.storeId,
      hostOnly: cookie.hostOnly
    }
  }
}

export function removeUrlFor(target) {
  const scheme = target.secure ? 'https' : 'http'
  const host = String(target.domain || '').replace(/^\./, '')
  const path = target.path && target.path.startsWith('/') ? target.path : '/'
  return `${scheme}://${host}${path}`
}

export function summarizeStorage(raw, maxKeys = 200) {
  const clean = raw && typeof raw === 'object' ? raw : {}
  const list = value => Array.isArray(value)
    ? value.filter(v => typeof v === 'string').slice(0, maxKeys).map(v => v.slice(0, 300))
    : []
  return {
    localStorageKeys: list(clean.localStorageKeys),
    sessionStorageKeys: list(clean.sessionStorageKeys),
    indexedDbDatabases: list(clean.indexedDbDatabases),
    serviceWorkerScopes: list(clean.serviceWorkerScopes),
    cacheStorageKeys: list(clean.cacheStorageKeys),
    errors: list(clean.errors)
  }
}
