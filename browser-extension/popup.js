// Popup UI. It never touches cookie values: background.js returns metadata only.
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
  if (!confirm(`Delete ${chosen.length} selected cookie(s) from this browser profile? You may be signed out.`)) return
  let removed = 0
  for (const i of chosen) {
    const response = await chrome.runtime.sendMessage({ type: 'remove', cookie: scanned[i].local })
    if (response?.ok) removed++
  }
  status(`Removed ${removed} of ${chosen.length} cookies in this browser. Server-side account data was not changed.`)
  await scan($('domain').value.trim())
})

$('sync').addEventListener('click', async () => {
  const api = $('api').value.trim().replace(/\/$/, '')
  const token = $('token').value.trim()
  if (!api || !token) { status('API URL and access token are required to sync.'); return }
  if (!scanned.length) { status('Scan a domain first.'); return }
  const installationId = (await chrome.storage.local.get('installationId')).installationId || crypto.randomUUID()
  await chrome.storage.local.set({ installationId })
  const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}`, Accept: 'application/json' }
  try {
    const reg = await fetch(`${api}/api/v1/browser-integrations/register`, { method: 'POST', headers, body: JSON.stringify({ installationId, browserName: 'Chromium', extensionVersion: chrome.runtime.getManifest().version, deviceLabel: navigator.platform }) })
    if (!reg.ok) throw new Error(`register failed (${reg.status})`)
    const cookies = scanned.map(c => c.upload)
    const sync = await fetch(`${api}/api/v1/browser-integrations/sync`, { method: 'POST', headers, body: JSON.stringify({ installationId, cookies }) })
    if (!sync.ok) throw new Error(`sync failed (${sync.status})`)
    const result = await sync.json()
    status(`Synced ${result.cookiesStored} cookie metadata records across ${result.domains} domain(s). No values were uploaded.`)
  } catch (e) { status(String(e.message || e)) }
})
