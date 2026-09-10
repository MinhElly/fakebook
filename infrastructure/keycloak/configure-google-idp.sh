#!/usr/bin/env bash
set -euo pipefail

server_url="${KEYCLOAK_INTERNAL_URL:-http://keycloak:9080}"
realm="${KEYCLOAK_REALM:-jhipster}"
admin_user="${KEYCLOAK_SYNC_ADMIN_USERNAME:-admin}"
admin_password="${KEYCLOAK_SYNC_ADMIN_PASSWORD:-admin}"
google_client_id="${KEYCLOAK_GOOGLE_CLIENT_ID:-}"
google_client_secret="${KEYCLOAK_GOOGLE_CLIENT_SECRET:-}"
kcadm="/opt/keycloak/bin/kcadm.sh"

if [[ -z "${google_client_id}" || -z "${google_client_secret}" ]]; then
  echo "Google IdP sync failed: KEYCLOAK_GOOGLE_CLIENT_ID and KEYCLOAK_GOOGLE_CLIENT_SECRET are required." >&2
  exit 1
fi

for attempt in $(seq 1 60); do
  if "${kcadm}" config credentials \
    --server "${server_url}" \
    --realm master \
    --user "${admin_user}" \
    --password "${admin_password}" >/dev/null 2>&1; then
    break
  fi

  if [[ "${attempt}" -eq 60 ]]; then
    echo "Google IdP sync failed: Keycloak did not become ready in time." >&2
    exit 1
  fi

  sleep 2
done

provider_args=(
  -s alias=google
  -s displayName=Google
  -s providerId=google
  -s enabled=true
  -s trustEmail=true
  -s storeToken=false
  -s addReadTokenRoleOnCreate=false
  -s authenticateByDefault=false
  -s linkOnly=false
  -s updateProfileFirstLoginMode=off
  -s config.clientId="${google_client_id}"
  -s config.clientSecret="${google_client_secret}"
  -s config.defaultScope="openid profile email"
  -s config.prompt=select_account
  -s config.hideOnLoginPage=false
)

if "${kcadm}" get identity-provider/instances/google -r "${realm}" >/dev/null 2>&1; then
  "${kcadm}" update identity-provider/instances/google -r "${realm}" "${provider_args[@]}" >/dev/null
  echo "Google IdP synchronized in realm '${realm}'."
else
  "${kcadm}" create identity-provider/instances -r "${realm}" "${provider_args[@]}" >/dev/null
  echo "Google IdP created in realm '${realm}'."
fi
