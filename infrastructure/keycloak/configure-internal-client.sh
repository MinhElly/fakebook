#!/bin/sh
  set -eu

  server_url="${KEYCLOAK_INTERNAL_URL:-http://keycloak:9080}"
  realm="${KEYCLOAK_REALM:-jhipster}"
  admin_username="${KEYCLOAK_SYNC_ADMIN_USERNAME:?KEYCLOAK_SYNC_ADMIN_USERNAME is required}"
  admin_password="${KEYCLOAK_SYNC_ADMIN_PASSWORD:?KEYCLOAK_SYNC_ADMIN_PASSWORD is required}"
  client_id="${FAKEBOOK_OIDC_INTERNAL_CLIENT_ID:?FAKEBOOK_OIDC_INTERNAL_CLIENT_ID is required}"
  client_secret="${FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET:?FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET is required}"

  kcadm="/opt/keycloak/bin/kcadm.sh"
  config_file="/tmp/kcadm.config"

  for attempt in $(seq 1 60); do
    if "$kcadm" config credentials \
      --config "$config_file" \
      --server "$server_url" \
      --realm master \
      --user "$admin_username" \
      --password "$admin_password" >/dev/null 2>&1; then
      break
    fi

    if [ "$attempt" -eq 60 ]; then
      echo "Internal client sync failed: Keycloak did not become ready in time." >&2
      exit 1
    fi

    sleep 2
  done
    
  client_uuid="$(
    "$kcadm" get clients \
      --config "$config_file" \
      -r "$realm" \
      -q "clientId=$client_id" \
      --fields id \
      --format csv \
      --noquotes |
    head -n 1
  )"

  if [ -z "$client_uuid" ]; then
    echo "Internal client '$client_id' was not found in realm '$realm'."
    exit 1
  fi

  "$kcadm" update "clients/$client_uuid" \
    --config "$config_file" \
    -r "$realm" \
    -s "clientAuthenticatorType=client-secret" \
    -s "publicClient=false" \
    -s "serviceAccountsEnabled=true" \
    -s "standardFlowEnabled=false" \
    -s "directAccessGrantsEnabled=false" \
    -s "secret=$client_secret"

  echo "Internal OAuth client synchronized."