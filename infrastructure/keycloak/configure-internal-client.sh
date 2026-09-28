#!/bin/sh
  set -eu

  server_url="${KEYCLOAK_INTERNAL_URL:-http://keycloak:9080}"
  realm="${KEYCLOAK_REALM:-jhipster}"
  admin_username="${KEYCLOAK_SYNC_ADMIN_USERNAME:?KEYCLOAK_SYNC_ADMIN_USERNAME is required}"
  admin_password="${KEYCLOAK_SYNC_ADMIN_PASSWORD:?KEYCLOAK_SYNC_ADMIN_PASSWORD is required}"
  client_id="${FAKEBOOK_OIDC_INTERNAL_CLIENT_ID:?FAKEBOOK_OIDC_INTERNAL_CLIENT_ID is required}"
  client_secret="${FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET:?FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET is required}"
  internal_role="${FAKEBOOK_OIDC_INTERNAL_ROLE:-ROLE_INTERNAL}"

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
    "$kcadm" create clients \
      --config "$config_file" \
      -r "$realm" \
      -s "clientId=$client_id" \
      -s "enabled=true" \
      -s "protocol=openid-connect" \
      -s "clientAuthenticatorType=client-secret" \
      -s "publicClient=false" \
      -s "serviceAccountsEnabled=true" \
      -s "standardFlowEnabled=false" \
      -s "directAccessGrantsEnabled=false" \
      -s "secret=$client_secret" >/dev/null

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
      echo "Internal client '$client_id' could not be created in realm '$realm'." >&2
      exit 1
    fi
  fi

  "$kcadm" update "clients/$client_uuid" \
    --config "$config_file" \
    -r "$realm" \
    -s "clientAuthenticatorType=client-secret" \
    -s "publicClient=false" \
    -s "serviceAccountsEnabled=true" \
    -s "standardFlowEnabled=false" \
    -s "directAccessGrantsEnabled=false" \
    -s "fullScopeAllowed=true" \
    -s "secret=$client_secret"

  if ! "$kcadm" get "roles/$internal_role" \
    --config "$config_file" \
    -r "$realm" >/dev/null 2>&1; then
    "$kcadm" create roles \
      --config "$config_file" \
      -r "$realm" \
      -s "name=$internal_role" \
      -s "description=Internal service-to-service access" >/dev/null
  fi

  service_account_user_id="$(
    "$kcadm" get "clients/$client_uuid/service-account-user" \
      --config "$config_file" \
      -r "$realm" \
      --fields id \
      --format csv \
      --noquotes |
    head -n 1
  )"

  if [ -z "$service_account_user_id" ]; then
    echo "Service account for internal client '$client_id' could not be resolved." >&2
    exit 1
  fi

  assigned_role="$(
    "$kcadm" get "users/$service_account_user_id/role-mappings/realm" \
      --config "$config_file" \
      -r "$realm" \
      --fields name \
      --format csv \
      --noquotes |
    tr -d '\r' |
    grep -Fx "$internal_role" || true
  )"

  if [ -z "$assigned_role" ]; then
    "$kcadm" add-roles \
      --config "$config_file" \
      -r "$realm" \
      --uid "$service_account_user_id" \
      --rolename "$internal_role"
  fi

  echo "Internal OAuth client and role synchronized."
