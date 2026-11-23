#!/usr/bin/env python3
"""Set the local Keycloak realm's browser login theme to Link Context."""

import json
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
STARTUP_ATTEMPTS = 45
RETRY_SECONDS = 2


def open_when_ready(request: urllib.request.Request):
    """Wait for Keycloak to accept HTTP requests after a container restart."""
    for attempt in range(1, STARTUP_ATTEMPTS + 1):
        try:
            return urllib.request.urlopen(request, timeout=8)
        except urllib.error.HTTPError as error:
            if error.code not in {502, 503, 504}:
                raise
            last_error = error
        except (urllib.error.URLError, TimeoutError, ConnectionError, OSError) as error:
            last_error = error
        if attempt % 5 == 0:
            print(f"Waiting for Keycloak Admin API ({attempt}/{STARTUP_ATTEMPTS})…")
        time.sleep(RETRY_SECONDS)
    raise RuntimeError(f"Keycloak did not become ready: {last_error}")


def env_file() -> dict[str, str]:
    values: dict[str, str] = {}
    for raw in (ROOT / ".env").read_text().splitlines():
        line = raw.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip().strip('"').strip("'")
    return values


def ensure_api_audiences(base: str, realm: str, headers: dict[str, str], audiences: list[tuple[str, str]]) -> None:
    realm_path = f"/admin/realms/{urllib.parse.quote(realm, safe='')}"

    def get_json(path: str):
        with open_when_ready(urllib.request.Request(f"{base}{path}", headers=headers)) as response:
            return json.load(response)

    clients = get_json(f"{realm_path}/clients?clientId=link-context-web")
    if not clients:
        raise RuntimeError("Keycloak browser client link-context-web was not found.")
    client_id = urllib.parse.quote(clients[0]["id"], safe="")

    scopes = get_json(f"{realm_path}/client-scopes")
    scope = next((item for item in scopes if item.get("name") == "link-context-api-audience"), None)
    if scope is None:
        raise RuntimeError("Keycloak client scope link-context-api-audience was not found.")
    roles_scope = next((item for item in scopes if item.get("name") == "roles"), None)
    if roles_scope is None:
        raise RuntimeError("Keycloak built-in roles client scope was not found.")
    scope_id = urllib.parse.quote(scope["id"], safe="")
    scope_path = f"{realm_path}/client-scopes/{scope_id}"

    default_scopes = get_json(f"{realm_path}/clients/{client_id}/default-client-scopes")
    default_scope_ids = {item.get("id") for item in default_scopes}
    for required_scope in (scope, roles_scope):
        if required_scope.get("id") in default_scope_ids:
            continue
        required_scope_id = urllib.parse.quote(required_scope["id"], safe="")
        attach_scope = urllib.request.Request(
            f"{realm_path}/clients/{client_id}/default-client-scopes/{required_scope_id}",
            headers=headers,
            method="PUT",
        )
        with open_when_ready(attach_scope):
            pass
    print("Browser client has the API audience and realm roles scopes enabled by default.")

    mapper_url = f"{base}{scope_path}/protocol-mappers/models"
    mappers = get_json(f"{scope_path}/protocol-mappers/models")
    for service, audience in audiences:
        mapper_name = f"link-context-{service}-audience"
        mapper = {
            "name": mapper_name,
            "protocol": "openid-connect",
            "protocolMapper": "oidc-audience-mapper",
            "config": {
                "included.custom.audience": audience,
                "id.token.claim": "false",
                "access.token.claim": "true",
            },
        }
        existing = next((item for item in mappers if item.get("name") == mapper_name), None)
        if existing is None:
            update = urllib.request.Request(
                mapper_url,
                data=json.dumps(mapper).encode(),
                headers=headers,
                method="POST",
            )
        else:
            mapper["id"] = existing["id"]
            update = urllib.request.Request(
                f"{mapper_url}/{urllib.parse.quote(existing['id'], safe='')}",
                data=json.dumps(mapper).encode(),
                headers=headers,
                method="PUT",
            )
        with open_when_ready(update):
            pass


def ensure_application_user_role(base: str, realm: str, headers: dict[str, str]) -> None:
    """Grant the gateway's required USER role to existing and future human accounts."""
    realm_path = f"/admin/realms/{urllib.parse.quote(realm, safe='')}"

    def get_json(path: str):
        with open_when_ready(urllib.request.Request(f"{base}{path}", headers=headers)) as response:
            return json.load(response)

    role_path = f"{realm_path}/roles/USER"
    try:
        role = get_json(role_path)
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
        create_role = urllib.request.Request(
            f"{realm_path}/roles",
            data=json.dumps({"name": "USER", "description": "Standard Link Context account"}).encode(),
            headers=headers,
            method="POST",
        )
        with open_when_ready(create_role):
            pass
        role = get_json(role_path)

    default_role_path = f"{realm_path}/roles/default-roles-{urllib.parse.quote(realm, safe='')}"
    get_json(default_role_path)
    composites_path = f"{default_role_path}/composites"
    composites = get_json(composites_path)
    if not any(item.get("id") == role.get("id") for item in composites):
        assign_default_role = urllib.request.Request(
            f"{base}{composites_path}",
            data=json.dumps([role]).encode(),
            headers=headers,
            method="POST",
        )
        with open_when_ready(assign_default_role):
            pass

    first = 0
    page_size = 100
    assigned = 0
    while True:
        users = get_json(f"{realm_path}/users?first={first}&max={page_size}")
        if not users:
            break
        for user in users:
            # Keep service accounts limited to their client-specific permissions.
            if user.get("serviceAccountClientId") or not user.get("id"):
                continue
            user_id = urllib.parse.quote(user["id"], safe="")
            mappings_path = f"{realm_path}/users/{user_id}/role-mappings/realm"
            mappings = get_json(mappings_path)
            if any(mapping.get("id") == role.get("id") for mapping in mappings):
                continue
            assign = urllib.request.Request(
                mappings_path,
                data=json.dumps([role]).encode(),
                headers=headers,
                method="POST",
            )
            with open_when_ready(assign):
                pass
            assigned += 1
        if len(users) < page_size:
            break
        first += page_size
    print(f"The USER application role is enabled for new accounts and assigned to {assigned} existing account(s).")


def main() -> int:
    config = env_file()
    base = config.get("KEYCLOAK_ADMIN_URL", "http://localhost:8080/auth").rstrip("/")
    realm = config.get("FRONTEND_KEYCLOAK_REALM", "link-context")
    smtp_username = config.get("SMTP_USERNAME", "").strip()
    smtp_password = config.get("SMTP_PASSWORD", "")
    smtp_host = config.get("SMTP_HOST", "smtp.gmail.com").strip()
    smtp_port = config.get("SMTP_PORT", "587").strip()
    smtp_configured = bool(smtp_username and smtp_password)
    if smtp_configured and (not smtp_host or not smtp_port.isdigit()):
        print("Could not configure Keycloak SMTP: SMTP_HOST must be set and SMTP_PORT must be numeric.")
        return 1

    form = urllib.parse.urlencode({
        "client_id": "admin-cli",
        "username": config.get("KEYCLOAK_ADMIN", ""),
        "password": config.get("KEYCLOAK_ADMIN_PASSWORD", ""),
        "grant_type": "password",
    }).encode()
    try:
        token_request = urllib.request.Request(
            f"{base}/realms/master/protocol/openid-connect/token",
            data=form,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        )
        with open_when_ready(token_request) as response:
            token = json.load(response)["access_token"]
        headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
        realm_url = f"{base}/admin/realms/{urllib.parse.quote(realm, safe='')}"
        with open_when_ready(urllib.request.Request(realm_url, headers=headers)) as response:
            representation = json.load(response)
        audiences = [
            ("gateway", config.get("KEYCLOAK_EXPECTED_AUDIENCE", "link-context-gateway").strip()),
            ("link-service", config.get("LINK_SERVICE_EXPECTED_AUDIENCE", "link-service").strip()),
            ("summarization-service", config.get("SUMMARIZATION_SERVICE_EXPECTED_AUDIENCE", "summarization-service").strip()),
            ("user-service", config.get("USER_SERVICE_EXPECTED_AUDIENCE", "user-service").strip()),
            ("notification-service", config.get("NOTIFICATION_SERVICE_EXPECTED_AUDIENCE", "notification-service").strip()),
        ]
        if any(not audience for _, audience in audiences):
            raise RuntimeError("API audience values in .env must not be empty.")
        ensure_api_audiences(base, realm, headers, audiences)
        ensure_application_user_role(base, realm, headers)
        representation["loginTheme"] = "link-context"
        representation["resetPasswordAllowed"] = True
        if smtp_configured:
            representation["smtpServer"] = {
                "host": smtp_host,
                "port": smtp_port,
                "from": smtp_username,
                "auth": "true",
                "user": smtp_username,
                "password": smtp_password,
                "ssl": "false",
                "starttls": "true",
            }
        update = urllib.request.Request(realm_url, data=json.dumps(representation).encode(), headers=headers, method="PUT")
        with open_when_ready(update) as response:
            print(f"Link Context theme, password recovery, and SMTP settings applied to realm {realm} (HTTP {response.status}).")
        print("Browser client link-context-web now receives the configured Gateway and API service audiences.")
        if smtp_configured:
            print(f"SMTP configured for {smtp_host}:{smtp_port} with authentication and STARTTLS; credentials were not printed.")
        else:
            print("SMTP settings were left unchanged because SMTP_USERNAME or SMTP_PASSWORD is not configured.")
        print("Restart Keycloak after changing theme files so it reloads the mounted theme.")
        return 0
    except (OSError, KeyError, RuntimeError, urllib.error.URLError, urllib.error.HTTPError, json.JSONDecodeError) as error:
        print(f"Could not select the Link Context login theme: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
