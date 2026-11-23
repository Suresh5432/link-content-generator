import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import keycloak from './auth'
import { api } from './api'

vi.mock('./auth', () => ({
  default: {
    authenticated: true,
    token: 'test-access-token',
    updateToken: vi.fn().mockResolvedValue(true),
    login: vi.fn(),
  },
}))

describe('API requests', () => {
  const fetchMock = vi.fn()

  afterEach(() => vi.unstubAllGlobals())

  beforeEach(() => {
    vi.clearAllMocks()
    keycloak.token = 'test-access-token'
    vi.mocked(keycloak.updateToken).mockResolvedValue(true)
    vi.mocked(keycloak.login).mockResolvedValue(undefined)
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockResolvedValue({
      ok: true,
      status: 201,
      json: async () => ({ id: 'link-id' }),
    })
  })

  it('refreshes the token and preserves custom headers with the bearer authorization', async () => {
    vi.spyOn(crypto, 'randomUUID').mockReturnValue('00000000-0000-4000-8000-000000000001')

    await api.createLink('https://example.com/article')

    expect(keycloak.updateToken).toHaveBeenCalledWith(30)
    expect(fetchMock).toHaveBeenCalledOnce()
    const [url, options] = fetchMock.mock.calls[0] as [string, RequestInit]
    const headers = new Headers(options.headers)
    expect(url).toContain('/api/v1/links')
    expect(headers.get('Authorization')).toBe(`Bearer ${keycloak.token}`)
    expect(headers.get('Accept')).toBe('application/json')
    expect(headers.get('Content-Type')).toBe('application/json')
    expect(headers.get('Idempotency-Key')).toBe('00000000-0000-4000-8000-000000000001')
  })

  it('deletes a link with the authenticated DELETE endpoint', async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 204 })

    await expect(api.deleteLink('link-id')).resolves.toBeUndefined()

    const [url, options] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/api/v1/links/link-id')
    expect(options.method).toBe('DELETE')
    expect(new Headers(options.headers).get("Authorization")).toMatch(/^Bearer /)
  })

  it('forces a token refresh and retries an unauthorized request once', async () => {
    fetchMock
      .mockResolvedValueOnce({ ok: false, status: 401 })
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ id: 'link-id' }) })
    vi.mocked(keycloak.updateToken).mockImplementation(async minValidity => {
      if (minValidity === -1) keycloak.token = 'refreshed-access-token'
      return true
    })

    await api.links()

    expect(keycloak.updateToken).toHaveBeenNthCalledWith(1, 30)
    expect(keycloak.updateToken).toHaveBeenNthCalledWith(2, -1)
    expect(fetchMock).toHaveBeenCalledTimes(2)
    const [, retryOptions] = fetchMock.mock.calls[1] as [string, RequestInit]
    expect(new Headers(retryOptions.headers).get('Authorization')).toBe('Bearer refreshed-access-token')
    expect(keycloak.login).not.toHaveBeenCalled()
  })

  it('forces one token refresh when a forbidden response may be using stale roles', async () => {
    fetchMock
      .mockResolvedValueOnce({ ok: false, status: 403 })
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ content: [] }) })
    vi.mocked(keycloak.updateToken).mockImplementation(async minValidity => {
      if (minValidity === -1) keycloak.token = 'refreshed-role-token'
      return true
    })

    await api.summaries()

    expect(keycloak.updateToken).toHaveBeenNthCalledWith(1, 30)
    expect(keycloak.updateToken).toHaveBeenNthCalledWith(2, -1)
    expect(fetchMock).toHaveBeenCalledTimes(2)
    const [, retryOptions] = fetchMock.mock.calls[1] as [string, RequestInit]
    expect(new Headers(retryOptions.headers).get('Authorization')).toBe('******')
  })

  it('starts sign-in when the token cannot be refreshed', async () => {
    fetchMock.mockResolvedValueOnce({ ok: false, status: 401 })
    const navigate = vi.fn()
    vi.stubGlobal('location', { assign: navigate })
    vi.mocked(keycloak.updateToken).mockImplementation(async minValidity => {
      if (minValidity === -1) throw new Error('Refresh token expired')
      return true
    })
    await expect(api.links()).rejects.toMatchObject({ status: 401 })

    expect(navigate).toHaveBeenCalledWith('/login?sessionExpired=1')
    expect(keycloak.login).not.toHaveBeenCalled()
    expect(fetchMock).toHaveBeenCalledOnce()
  })

  it('does not redirect to Keycloak repeatedly when the API still rejects a refreshed token', async () => {
    fetchMock
      .mockResolvedValueOnce({ ok: false, status: 401 })
      .mockResolvedValueOnce({ ok: false, status: 401 })

    await expect(api.links()).rejects.toThrow(/API rejected this session/)

    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(keycloak.login).not.toHaveBeenCalled()
  })
})
