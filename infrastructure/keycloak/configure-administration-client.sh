#!/bin/sh
set -eu

# Explicit operator tool; this file is never run automatically by application startup.
server_url="${KEYCLOAK_INTERNAL_URL:-http://keycloak:9080}"
realm="${KEYCLOAK_REALM:-jhipster}"
client_id="${FAKEBOOK_ADMIN_CLIENT_ID:-fakebook-administration}"
client_secret="${FAKEBOOK_ADMIN_CLIENT_SECRET:?FAKEBOOK_ADMIN_CLIENT_SECRET is required}"
admin_username="${KEYCLOAK_SYNC_ADMIN_USERNAME:?KEYCLOAK_SYNC_ADMIN_USERNAME is required}"
admin_password="${KEYCLOAK_SYNC_ADMIN_PASSWORD:?KEYCLOAK_SYNC_ADMIN_PASSWORD is required}"
kcadm=/opt/keycloak/bin/kcadm.sh
config_file="$(mktemp /tmp/fakebook-administration.XXXXXX)"
trap 'rm -f "$config_file"' EXIT HUP INT TERM
chmod 600 "$config_file"
"$kcadm" config credentials --config "$config_file" --server "$server_url" --realm master --user "$admin_username" --password "$admin_password" >/dev/null 2>&1
client_uuid="$("$kcadm" get clients --config "$config_file" -r "$realm" -q "clientId=$client_id" --fields id --format csv --noquotes | head -n 1 | tr -d '\r')"
if [ -z "$client_uuid" ]; then
    "$kcadm" create clients --config "$config_file" -r "$realm" -s "clientId=$client_id" -s protocol=openid-connect -s publicClient=false -s serviceAccountsEnabled=true -s standardFlowEnabled=false -s directAccessGrantsEnabled=false -s "secret=$client_secret" >/dev/null
    client_uuid="$("$kcadm" get clients --config "$config_file" -r "$realm" -q "clientId=$client_id" --fields id --format csv --noquotes | head -n 1 | tr -d '\r')"
fi
test -n "$client_uuid"
"$kcadm" update "clients/$client_uuid" --config "$config_file" -r "$realm" -s enabled=true -s publicClient=false -s serviceAccountsEnabled=true -s standardFlowEnabled=false -s directAccessGrantsEnabled=false -s fullScopeAllowed=true -s "secret=$client_secret" >/dev/null
service_user="$("$kcadm" get "clients/$client_uuid/service-account-user" --config "$config_file" -r "$realm" --fields id --format csv --noquotes | head -n 1 | tr -d '\r')"
test -n "$service_user"
# The realm's default Users group is also assigned to new service accounts.
# Remove it only from this administration account; human memberships are untouched.
service_groups="$("$kcadm" get "users/$service_user/groups" --config "$config_file" -r "$realm" --fields id,name --format csv --noquotes)"
printf '%s\n' "$service_groups" | tr -d '\r' | while IFS=, read -r group_uuid group_name; do
    if [ "$group_name" = Users ]; then
        "$kcadm" delete "users/$service_user/groups/$group_uuid" --config "$config_file" -r "$realm" >/dev/null
    fi
done
effective_roles="$("$kcadm" get "users/$service_user/role-mappings/realm/composite" --config "$config_file" -r "$realm" --fields name --format csv --noquotes)"
if printf '%s\n' "$effective_roles" | tr -d '\r' | grep -Eq '^(ROLE_ADMIN|ROLE_USER)$'; then
    echo 'Administration service account must not have community or administrator application roles.' >&2
    exit 1
fi
"$kcadm" add-roles --config "$config_file" -r "$realm" --uid "$service_user" --cclientid realm-management --rolename query-users --rolename view-users --rolename manage-users --rolename view-realm >/dev/null
echo 'Administration client configuration completed.'
