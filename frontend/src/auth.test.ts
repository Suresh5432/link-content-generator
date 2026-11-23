import { afterEach, describe, expect, it, vi } from 'vitest'
import keycloak, { initializeKeycloak } from './auth'

const keycloakConfig = vi.hoisted(() => ({ value: undefined as { url: string } | undefined }))

vi.mock('keycloak-js', () => ({
  default: class {
    constructor(config: { url: string }) {
      keycloakConfig.value = config
    }

    init = vi.fn().mockResolvedValue(true)
    createLoginUrl = vi.fn().mockResolvedValue('http://localhost:5173/auth/realms/link-context/protocol/openid-connect/auth')
  },
}))

afterEach(() => vi.clearAllMocks())

describe('Keycloak initialization', () => {
  it('uses the frontend origin as the Keycloak base URL', () => {
    expect(keycloakConfig.value?.url).toBe(`${window.location.origin}/auth`)
  })

  it('initializes the singleton only once when callers share the startup promise', async () => {
    const firstInitialization = initializeKeycloak()
    const secondInitialization = initializeKeycloak()

    expect(secondInitialization).toBe(firstInitialization)
    await expect(firstInitialization).resolves.toBe(true)
    expect(keycloak.init).toHaveBeenCalledOnce()
  })

})
