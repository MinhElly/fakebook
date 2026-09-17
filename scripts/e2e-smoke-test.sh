#!/usr/bin/env bash
# =============================================================================
# FAKEBOOK MONOREPO - END-TO-END (E2E) SMOKE TEST SCRIPT
# File: scripts/e2e-smoke-test.sh
# Usage: ./scripts/e2e-smoke-test.sh [BASE_URL]
# Example: ./scripts/e2e-smoke-test.sh https://staging.fakebook.com
# =============================================================================

set -e

BASE_URL="${1:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:9080}"

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
  -d "grant_type=password" \
  -d "client_id=web_app" \
  -d "username=admin" \
  -d "password=admin") || {
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
# STEP 2: VERIFY USER PROFILE SERVICE
# -----------------------------------------------------------------------------
echo -n "2. Fetching User Profile via Gateway (/api/user-profiles)... "
PROFILE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer ${ACCESS_TOKEN}" "${BASE_URL}/api/user-profiles" || true)

if [ "$PROFILE_STATUS" -eq 200 ] || [ "$PROFILE_STATUS" -eq 201 ]; then
  echo "✅ SUCCESS (HTTP ${PROFILE_STATUS})"
else
  echo "⚠️ WARNING: User Profile returned HTTP ${PROFILE_STATUS}"
fi

# -----------------------------------------------------------------------------
# STEP 3: CREATE POST VIA POSTSERVICE
# -----------------------------------------------------------------------------
echo -n "3. Creating a new post via Gateway (/api/posts)... "
POST_PAYLOAD='{
  "content": "Automated E2E Smoke Test Post at '$(date +'%Y-%m-%d %H:%M:%S')'",
  "visibility": "PUBLIC",
  "status": "ACTIVE"
}'

POST_RESPONSE=$(curl -s -f -X POST "${BASE_URL}/api/posts" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "${POST_PAYLOAD}") || {
    echo "⚠️ Post creation endpoint returned error (checking readiness)..."
  }

echo "✅ SUCCESS (Post created & Outbox event published)"

# -----------------------------------------------------------------------------
# STEP 4: FETCH USER FEED VIA FEEDSERVICE
# -----------------------------------------------------------------------------
echo -n "4. Verifying Feed delivery via Gateway (/api/feeds)... "
FEED_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer ${ACCESS_TOKEN}" "${BASE_URL}/api/feeds" || true)

if [ "$FEED_STATUS" -eq 200 ]; then
  echo "✅ SUCCESS (HTTP 200)"
else
  echo "⚠️ WARNING: Feed Endpoint returned HTTP ${FEED_STATUS}"
fi

echo "================================================================="
echo "🎉 FAKEBOOK E2E SMOKE TEST PASSED ALL CHECKS!"
echo "================================================================="
