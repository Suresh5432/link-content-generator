import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const dir = dirname(fileURLToPath(import.meta.url))
const manifest = JSON.parse(readFileSync(join(dir, 'manifest.json'), 'utf8'))
const background = readFileSync(join(dir, 'background.js'), 'utf8')

test('requests only the minimum documented permissions', () => {
  assert.deepEqual([...manifest.permissions].sort(), ['activeTab', 'cookies', 'storage'])
  assert.equal(manifest.manifest_version, 3)
})

test('never requests privileged browsing permissions', () => {
  for (const forbidden of ['webRequest', 'history', 'tabs', 'downloads', 'management', 'proxy', 'identity']) {
    assert.ok(!manifest.permissions.includes(forbidden), `must not request ${forbidden}`)
  }
})

test('uploads metadata only and never cookie values', () => {
  const uploadLine = background.split('\n').find((line) => line.includes('const UPLOAD_FIELDS'))
  assert.ok(uploadLine)
  for (const forbidden of ['value', 'path', 'storeId', 'session', 'token']) {
    assert.ok(!uploadLine.includes(`'${forbidden}'`), `must not upload ${forbidden}`)
  }
})

test('cookie removal uses an explicit user message and browser API', () => {
  assert.ok(background.includes("message?.type === 'remove'"))
  assert.ok(background.includes('chrome.cookies.remove'))
})
