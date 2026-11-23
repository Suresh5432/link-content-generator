// Link Context browser extension service worker.
// SECURITY: this file never reads, stores or transmits cookie.value, session
// identifiers or tokens. Only metadata is returned to the popup, and only a
// metadata subset is synced to Link Context.

const UPLOAD_FIELDS = ['name', 'domain', 'hostOnly', 'secure', 'httpOnly', 'sameSite', 'expirationDate']

function categoryFor(name) {
  const n = String(name).toLowerCase()
  if (/(^_ga|_gid|_gat|amplitude|mixpanel|_hjid|hotjar|segment|plausible|matomo)/.test(n)) return 'ANALYTICS'
  if (/(_fbp|fbp|doubleclick|ads|advert|gads|_gcl|criteo|taboola|outbrain)/.test(n)) return 'ADVERTISING'
  if (/(li_|_li|tw_|guest_id|personalization_id|_pinterest)/.test(n)) return 'SOCIAL_MEDIA'
  if (/(session|auth|token|sid|logged|remember)/.test(n)) return 'ESSENTIAL'
  if (/(lang|locale|pref|theme|consent|currency)/.test(n)) return 'FUNCTIONAL'
  return 'UNKNOWN'
}

function toMetadata(cookie) {
  const meta = {}
  for (const key of UPLOAD_FIELDS) meta[key] = cookie[key]
  meta.firstParty = Boolean(cookie.hostOnly)
  meta.trackingRelated = ['ANALYTICS', 'ADVERTISING', 'SOCIAL_MEDIA'].includes(categoryFor(cookie.name))
  meta.category = categoryFor(cookie.name)
  meta.purpose = meta.trackingRelated ? 'Possible analytics/advertising tracking' : null
  // Deliberately no value, path or storeId leave the browser.
  return {
    upload: meta,
    local: { name: cookie.name, domain: cookie.domain, path: cookie.path, secure: cookie.secure, storeId: cookie.storeId, hostOnly: cookie.hostOnly }
  }
}

async function scanDomain(domain) {
  const cookies = await chrome.cookies.getAll({ domain })
  return cookies.map(toMetadata)
}

async function removeCookie(target) {
  const scheme = target.secure ? 'https' : 'http'
  const host = target.domain.replace(/^\./, '')
  const url = `${scheme}://${host}${target.path || '/'}`
  return chrome.cookies.remove({ url, name: target.name, storeId: target.storeId })
}

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (message?.type === 'scan') {
    scanDomain(message.domain).then(list => sendResponse({ ok: true, cookies: list })).catch(e => sendResponse({ ok: false, error: String(e) }))
    return true
  }
  if (message?.type === 'remove') {
    removeCookie(message.cookie).then(result => sendResponse({ ok: Boolean(result), result })).catch(e => sendResponse({ ok: false, error: String(e) }))
    return true
  }
  if (message?.type === 'currentTab') {
    chrome.tabs.query({ active: true, currentWindow: true }).then(([tab]) => {
      let domain = ''
      try { domain = tab?.url ? new URL(tab.url).hostname : '' } catch {}
      sendResponse({ ok: true, domain })
    }).catch(e => sendResponse({ ok: false, error: String(e) }))
    return true
  }
  return false
})
