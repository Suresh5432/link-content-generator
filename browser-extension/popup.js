// Popup UI. It never touches cookie values/storage values: background.js/content.js return metadata only.
let scanned = []

const $ = id => document.getElementById(id)
const status = text => { $('status').textContent = text }

function categoryTag(category, tracking) {
  const cls = tracking ? 'tag warn' : 'tag'
  return `<span class="${cls}">${category}</span>`
}

function render() {
  const list = $('list')
  if (!scanned.length) { list.innerHTML = '<li>No cookies scanned yet.</li>'; return }
  list.innerHTML = scanned.map((c, i) => {
    const u = c.upload
    const flags = [u.secure && 'Secure', u.httpOnly && 'HttpOnly', u.sameSite && `SameSite=${u.sameSite}`].filter(Boolean).join(' · ')
    return `<li><input type="checkbox" data-i="${i}" /> <span><b>${escapeHtml(u.name)}</b> ${categoryTag(u.category, u.trackingRelated)}<br><span class="meta">${escapeHtml(u.domain)}${flags ? ' · ' + escapeHtml(flags) : ''}</span></span></li>`
  }).join('')
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]))
}

async function scan(domain) {
  if (!domain) { status('Enter a domain first.'); return }
  status('Scanning metadata…')
  const response = await chrome.runtime.sendMessage({ type: 'scan', domain })
  if (!response?.ok) { status(response?.error || 'Scan failed.'); return }
  scanned = response.cookies
  render()
  status(`Found ${scanned.length} cookies (metadata only).`)
}

$('current').addEventListener('click', async () => {
  const response = await chrome.runtime.sendMessage({ type: 'currentTab' })
  if (response?.ok && response.domain) { $('domain').value = response.domain; await scan(response.domain) }
  else status('Could not read the current site.')
})
$('scan').addEventListener('click', () => void scan($('domain').value.trim()))

$('delete').addEventListener('click', async () => {
  const chosen = [...document.querySelectorAll('#list input[type=checkbox]:checked')].map(el => Number(el.dataset.i))
  if (!chosen.length) { status('Select at least one cookie.'); return }
  if (!confirm(`Delete ${chosen.length} selected cookie(s) from this browser profile? This affects only this browser profile and may sign you out. Server-side account data is not changed.`)) return
  let removed = 0
  for (const i of chosen) {
    const response = await chrome.runtime.sendMessage({ type: 'remove', cookie: scanned[i].local })
    if (response?.ok) removed++
  }
  status(`Removed ${removed} of ${chosen.length} cookies in this browser profile. Server-side account data was not changed.`)
  await scan($('domain').value.trim())
})

$('clear-domain').addEventListener('click', async () => {
  const domain = $('domain').value.trim()
  if (!domain) { status('Enter a domain first.'); return }
  if (!confirm(`Remove ALL cookies for ${domain} in this browser profile? This affects only this browser profile, may sign you out, and reset preferences. Server-side account data is not changed.`)) return
  status('Clearing cookies…')
  const response = await chrome.runtime.sendMessage({ type: 'clearDomain', domain })
  if (!response?.ok) { status(response?.error || 'Clearing failed.'); return }
  status(`Removed ${response.result.removed} of ${response.result.found} cookies for ${domain} in this browser profile. Server-side account data was not changed.`)
  await scan(domain)
})

function renderStorage(storage) {
  const sections = [
    ['localStorage', storage.localStorageKeys],
    ['sessionStorage', storage.sessionStorageKeys],
    ['IndexedDB', storage.indexedDbDatabases],
    ['service workers', storage.serviceWorkerScopes],
    ['cache storage', storage.cacheStorageKeys]
  ]
  $('storage').innerHTML = sections.map(([label, values]) =>
    `<div><b>${escapeHtml(label)}:</b> ${values.length ? escapeHtml(values.join(', ')) : 'none found / unavailable'}</div>`
  ).join('') + (storage.errors?.length ? `<div class="warn">${escapeHtml(storage.errors.join(' '))}</div>` : '')
}

$('inspect-storage').addEventListener('click', async () => {
  status('Inspecting browser storage on this tab…')
  const response = await chrome.runtime.sendMessage({ type: 'storage' })
  if (!response?.ok) { status(response?.error || 'Storage inspection failed.'); return }
  renderStorage(response.storage)
  status('Storage names inspected. Values were never read.')
})

$('clear-storage').addEventListener('click', async () => {
  if (!confirm('Clear localStorage, sessionStorage, IndexedDB, service workers, and Cache Storage for the current site in this browser profile? This affects only this browser profile and may sign you out. Server-side account data is not changed.')) return
  status('Clearing browser storage for this site…')
  const response = await chrome.runtime.sendMessage({ type: 'clearStorage' })
  if (!response?.ok) { status(response?.error || 'Storage clear failed.'); return }
  renderStorage(response.storage)
  status('Browser storage cleared where supported. Server-side account data was not changed.')
})

$('authorize-origin').addEventListener('click', async () => {
  const api = $('api').value.trim()
  if (!api) { status('Enter the Link Context API URL first.'); return }
  const response = await chrome.runtime.sendMessage({ type: 'authorizeOrigin', origin: api })
  if (!response?.ok) { status(response?.error || 'Origin authorization failed.'); return }
  status(`Authorized ${response.origin} for metadata sync.`)
})

$('sync').addEventListener('click', async () => {
  const api = $('api').value.trim().replace(/\/$/, '')
  const token = $('token').value.trim()
  if (!api || !token) { status('API URL and access token are required to sync.'); return }
  if (!scanned.length) { status('Scan a domain first.'); return }
  if (!confirm('Sync cookie metadata only to Link Context? Cookie values, session tokens, and storage values are never uploaded.')) return
  const installationId = (await chrome.storage.local.get('installationId')).installationId || crypto.randomUUID()
  await chrome.storage.local.set({ installationId })
  status('Syncing metadata…')
  const response = await chrome.runtime.sendMessage({
    type: 'sync', apiBaseUrl: api, token, installationId,
    deviceLabel: navigator.platform, cookies: scanned.slice(0, 5000).map(c => c.upload)
  })
  if (!response?.ok) { status(response?.error || 'Sync failed.'); return }
  const result = response.result
  status(`Synced ${result.cookiesStored} cookie metadata records across ${result.domains} domain(s). No values were uploaded.`)
})
