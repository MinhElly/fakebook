#!/usr/bin/env bash
# =============================================================================
# FAKEBOOK MONOREPO - STRICT END-TO-END (E2E) SMOKE TEST SCRIPT
# File: scripts/e2e-smoke-test.sh
# Usage: ./scripts/e2e-smoke-test.sh [BASE_URL]
# Example: ./scripts/e2e-smoke-test.sh https://staging.fakebook.com
# =============================================================================

set -eo pipefail

BASE_URL="${1:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:9080}"
: "${FAKEBOOK_OIDC_INTERNAL_CLIENT_ID:?FAKEBOOK_OIDC_INTERNAL_CLIENT_ID is required}"
: "${FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET:?FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET is required}"

echo "================================================================="
echo "🚀 STARTING FAKEBOOK E2E SMOKE TEST SUITE"
echo "Target Base URL: ${BASE_URL}"
echo "Keycloak URL:   ${KEYCLOAK_URL}"
echo "================================================================="

# -----------------------------------------------------------------------------
# STEP 1: OBTAIN OIDC JWT ACCESS TOKEN FROM KEYCLOAK
# -----------------------------------------------------------------------------
echo -n "1. Authenticating against Keycloak OIDC... "
TOKEN_RESPONSE=$(curl -s -f -X POST "${KEYCLOAK_URL}/realms/jhipster/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=${FAKEBOOK_OIDC_INTERNAL_CLIENT_ID}" \
  -d "client_secret=${FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET}") || {
        echo "❌ Failed to obtain OIDC token!"
        exit 1
      }

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | grep -o '"access_token":"[^"]*' | grep -o '[^"]*$')

if [ -z "$ACCESS_TOKEN" ]; then
  echo "❌ Access Token is empty!"
  exit 1
fi
echo "✅ SUCCESS (Token acquired)"

# -----------------------------------------------------------------------------
# STEP 2: VERIFY USER PROFILE SERVICE VIA GATEWAY
# -----------------------------------------------------------------------------
echo -n "2. Fetching User Profiles via Gateway (/services/userservice/api/user-profiles)... "
PROFILE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer ${ACCESS_TOKEN}" "${BASE_URL}/services/userservice/api/user-profiles" || echo "000")

if [ "$PROFILE_STATUS" -eq 200 ] || [ "$PROFILE_STATUS" -eq 201 ]; then
  echo "✅ SUCCESS (HTTP ${PROFILE_STATUS})"
else
  echo "❌ FAILED: User Profile endpoint returned HTTP ${PROFILE_STATUS}"
  exit 1
fi

# -----------------------------------------------------------------------------
# STEP 3: CREATE POST VIA POSTSERVICE VIA GATEWAY
# -----------------------------------------------------------------------------
echo -n "3. Creating a new post via Gateway (/services/postservice/api/posts/create)... "
POST_PAYLOAD='{
  "content": "Automated E2E Smoke Test Post at '$(date +'%Y-%m-%d %H:%M:%S')'",
  "visibility": "PUBLIC",
  "mediaIds": [],
  "taggedUserIds": []
}'

POST_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/services/postservice/api/posts/create" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "${POST_PAYLOAD}" || echo "000")

if [ "$POST_STATUS" -eq 200 ] || [ "$POST_STATUS" -eq 201 ]; then
  echo "✅ SUCCESS (HTTP ${POST_STATUS})"
else
  echo "❌ FAILED: Post creation endpoint returned HTTP ${POST_STATUS}"
  exit 1
fi

# -----------------------------------------------------------------------------
# STEP 4: FETCH USER FEED VIA FEEDSERVICE VIA GATEWAY
# -----------------------------------------------------------------------------
echo -n "4. Verifying Feed delivery via Gateway (/services/feedservice/api/feed/me)... "
FEED_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer ${ACCESS_TOKEN}" "${BASE_URL}/services/feedservice/api/feed/me" || echo "000")

if [ "$FEED_STATUS" -eq 200 ] || [ "$FEED_STATUS" -eq 201 ]; then
  echo "✅ SUCCESS (HTTP ${FEED_STATUS})"
else
  echo "❌ FAILED: Feed Endpoint returned HTTP ${FEED_STATUS}"
  exit 1
fi

echo "================================================================="
echo "🎉 FAKEBOOK E2E SMOKE TEST PASSED ALL CHECKS!"
echo "================================================================="
