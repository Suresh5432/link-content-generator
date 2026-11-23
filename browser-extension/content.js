// Injected on demand into the active tab. Returns names/counts only, never values.
const STORAGE_METHOD = {
  inspect() {
    return inspectStorage()
  },
  clear() {
    return clearStorage()
  }
}

if (!window.__lcgStorageProbeInstalled) {
  window.__lcgStorageProbeInstalled = true

  chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
    if (message?.type !== 'inspect-storage' && message?.type !== 'clear-storage') return false

    const action = message.type === 'clear-storage' ? 'clear' : 'inspect'
    ;(async () => {
      try {
        const storage = await STORAGE_METHOD[action]()
        sendResponse({ ok: true, storage })
      } catch (error) {
        sendResponse({ ok: false, error: String(error?.message || error) })
      }
    })()
    return true
  })
}

function storageKeys(storage) {
  try {
    return Array.from({ length: storage.length }, (_, i) => storage.key(i)).filter(Boolean)
  } catch {
    return []
  }
}

async function inspectStorage() {
  const result = {
    localStorageKeys: storageKeys(localStorage),
    sessionStorageKeys: storageKeys(sessionStorage),
    indexedDbDatabases: [],
    serviceWorkerScopes: [],
    cacheStorageKeys: [],
    errors: []
  }
  try {
    if (typeof indexedDB.databases === 'function') {
      const databases = await indexedDB.databases()
      result.indexedDbDatabases = databases.map(d => d.name).filter(Boolean)
    } else {
      result.errors.push('IndexedDB database names are unavailable in this browser; deletion can only target the current origin when supported.')
    }
  } catch { result.errors.push('IndexedDB names could not be inspected.') }
  try {
    if (navigator.serviceWorker) {
      const registrations = await navigator.serviceWorker.getRegistrations()
      result.serviceWorkerScopes = registrations.map(r => r.scope)
    }
  } catch { result.errors.push('Service worker registrations could not be inspected.') }
  try {
    if (window.caches) result.cacheStorageKeys = await caches.keys()
  } catch { result.errors.push('Cache storage keys could not be inspected.') }
  return result
}

async function clearStorage() {
  const removed = {
    localStorageKeys: [],
    sessionStorageKeys: [],
    indexedDbDatabases: [],
    serviceWorkerScopes: [],
    cacheStorageKeys: [],
    errors: []
  }
  try {
    removed.localStorageKeys = storageKeys(localStorage)
    localStorage.clear()
  } catch { removed.errors.push('localStorage could not be cleared.') }
  try {
    removed.sessionStorageKeys = storageKeys(sessionStorage)
    sessionStorage.clear()
  } catch { removed.errors.push('sessionStorage could not be cleared.') }

  try {
    if (typeof indexedDB.databases === 'function') {
      const databases = await indexedDB.databases()
      for (const db of databases) {
        if (!db?.name) continue
        const result = await deleteIndexedDb(db.name)
        if (result.ok) removed.indexedDbDatabases.push(db.name)
        else removed.errors.push(`IndexedDB ${db.name}: ${result.error}`)
      }
    } else {
      removed.errors.push('IndexedDB database names are unavailable; this browser cannot enumerate IndexedDB databases for confirmed removal.')
    }
  } catch { removed.errors.push('IndexedDB could not be cleared.') }

  try {
    if (navigator.serviceWorker) {
      const registrations = await navigator.serviceWorker.getRegistrations()
      for (const registration of registrations) {
        const ok = await registration.unregister()
        if (ok) removed.serviceWorkerScopes.push(registration.scope)
        else removed.errors.push(`Service worker ${registration.scope} could not be unregistered.`)
      }
    }
  } catch { removed.errors.push('Service workers could not be cleared.') }

  try {
    if (window.caches) {
      const keys = await caches.keys()
      for (const key of keys) {
        const ok = await caches.delete(key)
        if (ok) removed.cacheStorageKeys.push(key)
        else removed.errors.push(`Cache ${key} could not be deleted.`)
      }
    }
  } catch { removed.errors.push('Cache Storage could not be cleared.') }

  return removed
}

function deleteIndexedDb(name) {
  return new Promise(resolve => {
    try {
      const request = indexedDB.deleteDatabase(name)
      request.onsuccess = () => resolve({ ok: true })
      request.onerror = () => resolve({ ok: false, error: request.error?.message || 'delete failed' })
      request.onblocked = () => resolve({ ok: false, error: 'delete blocked by an open tab' })
    } catch (error) {
      resolve({ ok: false, error: String(error?.message || error) })
    }
  })
}
