import { test } from 'node:test'
import assert from 'node:assert/strict'
import { categoryFor, toMetadata, removeUrlFor, summarizeStorage, UPLOAD_FIELDS } from './metadata.js'

const cookie = {
  name: '_ga',
  domain: '.example.com',
  path: '/',
  secure: true,
  httpOnly: false,
  sameSite: 'Lax',
  hostOnly: false,
  storeId: '1',
  expirationDate: 1800000000,
  value: 'super-secret-session-value'
}

test('categorises tracking cookies without reading values', () => {
  assert.equal(categoryFor('_ga'), 'ANALYTICS')
  assert.equal(categoryFor('_fbp'), 'ADVERTISING')
  assert.equal(categoryFor('li_session'), 'SOCIAL_MEDIA')
  assert.equal(categoryFor('theme'), 'FUNCTIONAL')
  assert.equal(categoryFor('unknown-cookie'), 'UNKNOWN')
})

test('metadata upload never includes value, path or storeId', () => {
  const meta = toMetadata(cookie)
  assert.deepEqual(Object.keys(meta.upload).sort(), [...UPLOAD_FIELDS, 'firstParty', 'trackingRelated', 'category', 'purpose'].sort())
  assert.ok(!('value' in meta.upload))
  assert.ok(!('path' in meta.upload))
  assert.ok(!('storeId' in meta.upload))
  assert.equal(meta.upload.category, 'ANALYTICS')
  assert.equal(meta.upload.trackingRelated, true)
  assert.equal(meta.local.name, '_ga')
  assert.equal(meta.local.storeId, '1')
})

test('removal URL is built from metadata only', () => {
  assert.equal(removeUrlFor(toMetadata(cookie).local), 'https://example.com/')
  assert.equal(removeUrlFor({ domain: 'example.com', path: '/app', secure: false }), 'http://example.com/app')
})

test('storage summary keeps names only and caps lists', () => {
  const summary = summarizeStorage({
    localStorageKeys: ['theme', 'token', 'a'.repeat(500)],
    sessionStorageKeys: 'not-an-array',
    indexedDbDatabases: ['app-db'],
    serviceWorkerScopes: ['https://example.com/sw.js'],
    cacheStorageKeys: ['v1'],
    errors: []
  }, 2)
  assert.deepEqual(summary.localStorageKeys, ['theme', 'token'])
  assert.equal(summary.localStorageKeys[0].length, 5)
  assert.deepEqual(summary.sessionStorageKeys, [])
  assert.equal(summary.serviceWorkerScopes[0], 'https://example.com/sw.js')
})

test('storage summary never returns values', () => {
  const summary = summarizeStorage({ localStorageKeys: ['theme'], values: ['secret'] })
  assert.ok(!('values' in summary))
})
