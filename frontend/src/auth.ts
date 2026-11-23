import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? `${window.location.origin}/auth`,
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'link-context',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'link-context-web',
})

let initialization: Promise<boolean> | undefined

export function initializeKeycloak() {
  if (!initialization) {
    initialization = keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      responseMode: 'query',
    })
  }
  return initialization
}

export default keycloak
