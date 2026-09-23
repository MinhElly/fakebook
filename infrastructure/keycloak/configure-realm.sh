#!/bin/sh
set -eu

server_url="${KEYCLOAK_INTERNAL_URL:-http://keycloak:9080}"
realm="${KEYCLOAK_REALM:-jhipster}"
admin_username="${KEYCLOAK_SYNC_ADMIN_USERNAME:?KEYCLOAK_SYNC_ADMIN_USERNAME is required}"
admin_password="${KEYCLOAK_SYNC_ADMIN_PASSWORD:?KEYCLOAK_SYNC_ADMIN_PASSWORD is required}"
staging_frontend_url="${STAGING_FRONTEND_URL:-}"
domain_name="${DOMAIN_NAME:-}"
realm_frontend_url="${KEYCLOAK_FRONTEND_URL:-}"

kcadm="/opt/keycloak/bin/kcadm.sh"
config_file="/tmp/kcadm.config"

echo "Waiting for Keycloak master login..."
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
    echo "Keycloak realm configurator failed: Keycloak did not become ready in time." >&2
    exit 1
  fi

  sleep 2
done

echo "Authenticated with Keycloak. Synchronizing realm '$realm'..."

# Update realm frontend URL if provided
if [ -n "$realm_frontend_url" ]; then
  "$kcadm" update "realms/$realm" \
    --config "$config_file" \
    -s "attributes.frontendUrl=$realm_frontend_url"
  echo "Realm frontend URL updated to: $realm_frontend_url"
fi

# Reconcile web_app client
web_app_uuid="$(
  "$kcadm" get clients \
    --config "$config_file" \
    -r "$realm" \
    -q "clientId=web_app" \
    --fields id \
    --format csv \
    --noquotes |
  head -n 1
)"

if [ -n "$web_app_uuid" ]; then
  echo "Reconciling web_app client ($web_app_uuid)..."

  # Base redirect URIs and web origins
  redirect_uris='["http://localhost:8080/*","http://localhost:8443/*","https://localhost:8443/*","http://localhost:5173/*","http://localhost:3000/*","http://localhost:9000/*","http://127.0.0.1:8443/*","http://127.0.0.1:5173/*","http://127.0.0.1:8080/*","http://localhost:*","https://localhost:*","http://127.0.0.1:*","https://127.0.0.1:*"'
  web_origins='["http://localhost:8080","http://localhost:8443","https://localhost:8443","http://localhost:5173","http://localhost:3000","http://localhost:9000","http://127.0.0.1:8443","http://127.0.0.1:5173","http://127.0.0.1:8080","+"'

  if [ -n "$staging_frontend_url" ]; then
    clean_fe="$(echo "$staging_frontend_url" | sed 's:/*$::')"
    redirect_uris="${redirect_uris},\"${clean_fe}/*\""
    web_origins="${web_origins},\"${clean_fe}\""
  fi

  if [ -n "$domain_name" ]; then
    redirect_uris="${redirect_uris},\"https://${domain_name}/*\",\"http://${domain_name}/*\""
    web_origins="${web_origins},\"https://${domain_name}\",\"http://${domain_name}\""
  fi

  redirect_uris="${redirect_uris}]"
  web_origins="${web_origins}]"

  "$kcadm" update "clients/$web_app_uuid" \
    --config "$config_file" \
    -r "$realm" \
    -s "standardFlowEnabled=true" \
    -s "publicClient=true" \
    -s "attributes.\"pkce.code.challenge.method\"=S256" \
    -s "attributes.\"post.logout.redirect.uris\"=+" \
    -s "redirectUris=$redirect_uris" \
    -s "webOrigins=$web_origins"

  echo "web_app client synchronized successfully."
else
  echo "Warning: web_app client was not found in realm '$realm'."
fi
