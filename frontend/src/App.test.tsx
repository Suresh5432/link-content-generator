import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { api, ApiError } from './api'
import keycloak, { initializeKeycloak } from './auth'
import { isFailed, statusLabels } from './types'

vi.mock('./auth', () => ({ default: { authenticated: true, token: 'test-token', tokenParsed: { name: 'Avery Reader', email: 'avery@example.test' }, init: vi.fn().mockResolvedValue(true), updateToken: vi.fn().mockResolvedValue(true), createLoginUrl: vi.fn().mockResolvedValue('/auth/realms/link-context/protocol/openid-connect/auth?client_id=link-context-web'), createLogoutUrl: vi.fn().mockReturnValue('/auth/realms/link-context/protocol/openid-connect/logout?client_id=link-context-web'), createRegisterUrl: vi.fn().mockResolvedValue('/auth/realms/link-context/protocol/openid-connect/registrations?client_id=link-context-web') }, initializeKeycloak: vi.fn().mockResolvedValue(true) }))
vi.mock('./api', () => ({ api: { links: vi.fn(), link: vi.fn(), summaryForLink: vi.fn(), summaries: vi.fn(), unreadCount: vi.fn(), createLink: vi.fn(), deleteLink: vi.fn(), profile: vi.fn(), preferences: vi.fn(), notifications: vi.fn() }, ApiError: class ApiError extends Error { constructor(message: string, public status: number) { super(message) } } }))

const page = <T,>(content: T[]) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, first: true, last: true })

afterEach(() => { cleanup(); vi.clearAllMocks(); window.history.replaceState({}, '', '/') })

describe('Link Context workspace', () => {
  it('shows the signed-in dashboard and handles an empty library', async () => {
    vi.mocked(api.links).mockResolvedValue(page([]))
    vi.mocked(api.summaries).mockResolvedValue(page([]))
    vi.mocked(api.unreadCount).mockResolvedValue({ count: 0 })
    render(<BrowserRouter><App /></BrowserRouter>)
    expect(await screen.findByRole('heading', { name: /avery/i })).toBeInTheDocument()
    expect(await screen.findByText('Your reading list starts here')).toBeInTheDocument()
  })

  it('submits a valid link and reports success', async () => {
    vi.mocked(api.links).mockResolvedValue(page([]))
    vi.mocked(api.summaries).mockResolvedValue(page([]))
    vi.mocked(api.unreadCount).mockResolvedValue({ count: 0 })
    vi.mocked(api.createLink).mockResolvedValue({ id: 'new-link', url: 'https://example.com/story', status: 'SUBMITTED', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() })
    render(<BrowserRouter><App /></BrowserRouter>)
    const input = await screen.findByRole('textbox', { name: 'Link URL' })
    fireEvent.change(input, { target: { value: 'https://example.com/story' } })
    fireEvent.click(screen.getByRole('button', { name: /save link/i }))
    await waitFor(() => expect(api.createLink).toHaveBeenCalledWith('https://example.com/story'))
    expect(await screen.findByRole('status')).toHaveTextContent('Your link is in the reading queue.')
  })

  it('confirms and removes a link from the library', async () => {
    window.history.replaceState({}, '', '/links')
    vi.mocked(api.links).mockResolvedValue(page([{ id: 'link-1', url: 'https://example.com/article', status: 'PENDING_EXTRACTION', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() }]))
    vi.mocked(api.unreadCount).mockResolvedValue({ count: 0 })
    vi.mocked(api.deleteLink).mockResolvedValue(undefined)
    render(<BrowserRouter><App /></BrowserRouter>)

    fireEvent.click(await screen.findByRole('button', { name: 'Delete example.com' }))
    expect(await screen.findByRole('alertdialog')).toHaveTextContent('Any generated summary remains in Summaries.')
    fireEvent.click(screen.getByRole('button', { name: 'Delete link' }))

    await waitFor(() => expect(api.deleteLink).toHaveBeenCalledWith('link-1'))
    expect(await screen.findByText('Nothing saved here yet')).toBeInTheDocument()
    expect(await screen.findByRole('status')).toHaveTextContent('Link deleted from your library.')
  })

  it('deletes a link from its detail page and returns to the library', async () => {
    window.history.replaceState({}, '', '/links/link-1')
    vi.mocked(api.link).mockResolvedValue({ id: 'link-1', url: 'https://example.com/article', status: 'PENDING_EXTRACTION', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() })
    vi.mocked(api.summaryForLink).mockRejectedValue(new ApiError('Summary not found', 404))
    vi.mocked(api.links).mockResolvedValue(page([]))
    vi.mocked(api.unreadCount).mockResolvedValue({ count: 0 })
    vi.mocked(api.deleteLink).mockResolvedValue(undefined)
    render(<BrowserRouter><App /></BrowserRouter>)

    fireEvent.click(await screen.findByRole('button', { name: /delete link/i }))
    fireEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Delete link' }))

    await waitFor(() => expect(api.deleteLink).toHaveBeenCalledWith('link-1'))
    expect(await screen.findByText('Nothing saved here yet')).toBeInTheDocument()
  })

  it('recognizes processing failures and labels statuses', () => {
    expect(isFailed('EXTRACTION_FAILED')).toBe(true)
    expect(isFailed('COMPLETED')).toBe(false)
    expect(statusLabels.COMPLETED).toBe('Ready')
  })

  it('starts Keycloak password recovery with the entered email hint', async () => {
    window.history.replaceState({}, '', '/forgot-password')
    render(<BrowserRouter><App /></BrowserRouter>)
    expect(await screen.findByRole('main')).toHaveClass('auth-recovery')
    fireEvent.change(await screen.findByRole('textbox', { name: /email address/i }), { target: { value: 'reader@example.test' } })
    fireEvent.click(screen.getByRole('button', { name: /continue to password reset/i }))
    await waitFor(() => expect(keycloak.createLoginUrl).toHaveBeenCalledOnce())
    expect(keycloak.createLoginUrl).toHaveBeenCalledWith({ loginHint: 'reader@example.test', redirectUri: `${window.location.origin}/login?authComplete=1` })
    expect(screen.getByTitle('Link Context secure sign-in')).toHaveAttribute('src', expect.stringContaining('/auth/realms/link-context/'))
  })

  it('keeps sign-in and sign-out on the same login page', async () => {
    window.history.replaceState({}, '', '/login')
    render(<BrowserRouter><App /></BrowserRouter>)
    expect(await screen.findByRole('heading', { name: 'You’re signed in.' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /^sign out$/i }))
    await waitFor(() => expect(keycloak.createLogoutUrl).toHaveBeenCalledOnce())
    expect(keycloak.createLogoutUrl).toHaveBeenCalledWith({ redirectUri: `${window.location.origin}/login?loggedOut=1` })
    expect(screen.getByRole('main')).toHaveClass('auth-welcome')
  })

  it('uses the fluid auth layout for welcome and sign-in screens', async () => {
    vi.mocked(initializeKeycloak).mockResolvedValueOnce(false)
    window.history.replaceState({}, '', '/login')
    render(<BrowserRouter><App /></BrowserRouter>)
    expect(await screen.findByRole('main')).toHaveClass('auth-welcome')
    fireEvent.click(screen.getByRole('button', { name: /continue to sign in/i }))
    await waitFor(() => expect(keycloak.createLoginUrl).toHaveBeenCalledOnce())

    cleanup()
    vi.clearAllMocks()
    vi.mocked(initializeKeycloak).mockResolvedValueOnce(false)
    window.history.replaceState({}, '', '/signin')
    render(<BrowserRouter><App /></BrowserRouter>)
    expect(await screen.findByRole('main')).toHaveClass('auth-welcome')
  })

  it('shows a clear sign-in action after session expiry', async () => {
    vi.mocked(initializeKeycloak).mockResolvedValueOnce(false)
    window.history.replaceState({}, '', '/login?sessionExpired=1')
    render(<BrowserRouter><App /></BrowserRouter>)
    expect(await screen.findByRole('heading', { name: 'Your session expired.' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign in again/i })).toBeInTheDocument()
  })

})
