// Link Context browser extension service worker.
// SECURITY: never reads, stores or transmits cookie.value, session identifiers,
// tokens, or browser storage values. Only metadata/names/counts are handled.

import { toMetadata, removeUrlFor, summarizeStorage } from './metadata.js'

const API_ORIGIN_ALLOWLIST_KEY = 'apiOriginAllowlist'

async function scanDomain(domain) {
  const cookies = await chrome.cookies.getAll({ domain })
  return cookies.map(toMetadata)
}

async function removeCookie(target) {
  return chrome.cookies.remove({
    url: removeUrlFor(target),
    name: target.name,
    storeId: target.storeId
  })
}

async function clearDomain(domain) {
  const cookies = await chrome.cookies.getAll({ domain })
  const targets = cookies.map(toMetadata).map(m => m.local)
  let removed = 0
  for (const target of targets) {
    const result = await removeCookie(target)
    if (result) removed++
  }
  return { found: targets.length, removed }
}

async function getActiveHttpTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true })
  if (!tab?.id || !/^https?:/.test(tab.url || '')) {
    throw new Error('Open an http(s) page and try again.')
  }
  return tab
}

async function runStorageAction(type) {
  const tab = await getActiveHttpTab()
  await chrome.scripting.executeScript({ target: { tabId: tab.id }, files: ['content.js'] })
  const response = await chrome.tabs.sendMessage(tab.id, { type })
  if (!response?.ok) throw new Error(response?.error || 'Storage action failed.')
  return summarizeStorage(response.storage)
}

async function authorizeOrigin(origin) {
  const normalized = new URL(origin).origin
  const allowed = await chrome.permissions.request({ origins: [`${normalized}/*`] })
  if (!allowed) throw new Error('Permission to access the selected Link Context origin was not granted.')
  const stored = await chrome.storage.local.get(API_ORIGIN_ALLOWLIST_KEY)
  const allowlist = Array.isArray(stored[API_ORIGIN_ALLOWLIST_KEY]) ? stored[API_ORIGIN_ALLOWLIST_KEY] : []
  if (!allowlist.includes(normalized)) allowlist.push(normalized)
  await chrome.storage.local.set({ [API_ORIGIN_ALLOWLIST_KEY]: allowlist })
  return { origin: normalized }
}

async function syncMetadata(message) {
  const api = new URL(message.apiBaseUrl).origin
  const stored = await chrome.storage.local.get(API_ORIGIN_ALLOWLIST_KEY)
  const allowlist = Array.isArray(stored[API_ORIGIN_ALLOWLIST_KEY]) ? stored[API_ORIGIN_ALLOWLIST_KEY] : []
  if (!allowlist.includes(api)) {
    throw new Error('The API origin is not authorized. Authorize it before syncing.')
  }
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${message.token}`,
    Accept: 'application/json'
  }
  const reg = await fetch(`${api}/api/v1/browser-integrations/register`, {
    method: 'POST', headers,
    body: JSON.stringify({
      installationId: message.installationId,
      browserName: 'Chromium',
      extensionVersion: chrome.runtime.getManifest().version,
      deviceLabel: message.deviceLabel || 'browser-extension'
    })
  })
  if (!reg.ok) throw new Error(`register failed (${reg.status})`)
  const cookies = Array.isArray(message.cookies) ? message.cookies.slice(0, 5000) : []
  const sync = await fetch(`${api}/api/v1/browser-integrations/sync`, {
    method: 'POST', headers, body: JSON.stringify({ installationId: message.installationId, cookies })
  })
  if (!sync.ok) throw new Error(`sync failed (${sync.status})`)
  return sync.json()
}

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (message?.type === 'scan') {
    scanDomain(message.domain).then(list => sendResponse({ ok: true, cookies: list })).catch(e => sendResponse({ ok: false, error: String(e?.message || e) }))
    return true
  }
  if (message?.type === 'remove') {
    removeCookie(message.cookie).then(result => sendResponse({ ok: Boolean(result), result })).catch(e => sendResponse({ ok: false, error: String(e?.message || e) }))
    return true
  }
  if (message?.type === 'clearDomain') {
    clearDomain(message.domain).then(result => sendResponse({ ok: true, result })).catch(e => sendResponse({ ok: false, error: String(e?.message || e) }))
    return true
  }
  if (message?.type === 'storage') {
    runStorageAction('inspect-storage').then(storage => sendResponse({ ok: true, storage })).catch(e => sendResponse({ ok: false, error: String(e?.message || e) }))
    return true
  }
  if (message?.type === 'clearStorage') {
    runStorageAction('clear-storage').then(storage => sendResponse({ ok: true, storage })).catch(e => sendResponse({ ok: false, error: String(e?.message || e) }))
    return true
  }
  if (message?.type === 'authorizeOrigin') {
    authorizeOrigin(message.origin).then(result => sendResponse({ ok: true, ...result })).catch(e => sendResponse({ ok: false, error: String(e?.message || e) }))
    return true
  }
  if (message?.type === 'sync') {
    syncMetadata(message).then(result => sendResponse({ ok: true, result })).catch(e => sendResponse({ ok: false, error: String(e?.message || e) }))
    return true
  }
  if (message?.type === 'currentTab') {
    chrome.tabs.query({ active: true, currentWindow: true }).then(([tab]) => {
      let domain = ''
      try { domain = tab?.url ? new URL(tab.url).hostname : '' } catch {}
      sendResponse({ ok: true, domain })
    }).catch(e => sendResponse({ ok: false, error: String(e?.message || e) }))
    return true
  }
  return false
})
