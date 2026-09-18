#!/usr/bin/env bash
# =============================================================================
# AUTOMATED LET'S ENCRYPT CERTIFICATE ISSUANCE SCRIPT FOR VPS
# File: infrastructure/nginx/init-letsencrypt.sh
# Usage: ./infrastructure/nginx/init-letsencrypt.sh [DOMAIN] [EMAIL]
# Example: ./infrastructure/nginx/init-letsencrypt.sh staging.fakebook.com admin@fakebook.com
# =============================================================================

set -e

DOMAIN="${1:-staging.fakebook.com}"
EMAIL="${2:-admin@fakebook.com}"
RSA_KEY_SIZE=4096
DATA_PATH="./nginx/certs"

echo "================================================================="
echo "🔑 INITIALIZING LET'S ENCRYPT SSL CERTIFICATE FOR ${DOMAIN}"
echo "================================================================="

if [ -d "$DATA_PATH/live/$DOMAIN" ] && [ -f "$DATA_PATH/live/$DOMAIN/fullchain.pem" ]; then
  echo "✅ Existing certificates found for $DOMAIN. Skipping issuance."
  exit 0
fi

echo "1. Creating dummy certificates for Nginx initial boot..."
mkdir -p "$DATA_PATH/live/$DOMAIN"

docker run --rm -v "$(pwd)/$DATA_PATH/live/$DOMAIN:/certs" alpine/openssl \
  req -x509 -nodes -newkey rsa:2048 \
  -keyout /certs/privkey.pem \
  -out /certs/fullchain.pem \
  -subj "/CN=$DOMAIN"

echo "2. Starting Nginx service..."
docker compose -f infrastructure/docker-compose-staging.yml --env-file infrastructure/.env up -d nginx

echo "3. Requesting Let's Encrypt SSL certificate via Certbot..."
docker run --rm \
  -v "$(pwd)/$DATA_PATH:/etc/letsencrypt" \
  -v "$(pwd)/nginx/certbot-www:/var/www/certbot" \
  certbot/certbot certonly --webroot -w /var/www/certbot \
  -d "$DOMAIN" \
  --email "$EMAIL" \
  --rsa-key-size $RSA_KEY_SIZE \
  --agree-tos \
  --force-renewal \
  --non-interactive

echo "4. Reloading Nginx with production SSL certificate..."
docker compose -f infrastructure/docker-compose-staging.yml --env-file infrastructure/.env exec nginx nginx -s reload

echo "================================================================="
echo "🎉 SUCCESS: SSL CERTIFICATE ISSUED AND APPLIED FOR ${DOMAIN}!"
echo "================================================================="
