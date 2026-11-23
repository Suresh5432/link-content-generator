import { test } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const manifest = JSON.parse(fs.readFileSync(new URL('./manifest.json', import.meta.url), 'utf8'))
const read = file => fs.readFileSync(new URL(file, import.meta.url), 'utf8')

test('manifest keeps minimal permissions and no broad host access by default', () => {
  assert.deepEqual(manifest.permissions.sort(), ['activeTab', 'cookies', 'scripting', 'storage'])
  assert.equal(manifest.host_permissions, undefined)
  assert.deepEqual(manifest.optional_host_permissions, ['http://localhost/*', 'https://localhost/*', 'http://127.0.0.1/*', 'https://127.0.0.1/*'])
})

test('manifest does not request dangerous extension capabilities', () => {
  const forbidden = ['<all_urls>', 'history', 'webRequest', 'webRequestBlocking', 'identity']
  for (const permission of forbidden) {
    assert.ok(!manifest.permissions.includes(permission), permission)
    assert.ok(!(manifest.optional_host_permissions || []).includes(permission), permission)
  }
})

test('background and popup never reference cookie value fields', () => {
  for (const file of ['background.js', 'popup.js', 'content.js', 'metadata.js']) {
    const source = read(`./${file}`)
    const code = source.split('\n').filter(line => !line.trim().startsWith('//')).join('\n')
    assert.ok(!/cookie\.value\b/.test(code), `${file} must not access cookie.value`)
    if (file !== 'content.js') {
      assert.ok(!/localStorage\.|sessionStorage\.|indexedDB\.|caches\./.test(code), `${file} must not read storage values`)
    }
  }
})
