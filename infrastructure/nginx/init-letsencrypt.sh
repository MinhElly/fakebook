#!/usr/bin/env bash
# =============================================================================
# AUTOMATED LET'S ENCRYPT CERTIFICATE ISSUANCE SCRIPT FOR VPS
# File: infrastructure/nginx/init-letsencrypt.sh
# Usage: ./infrastructure/nginx/init-letsencrypt.sh [DOMAIN] [EMAIL]
# Example: ./infrastructure/nginx/init-letsencrypt.sh staging.fakebook.com admin@fakebook.com
# =============================================================================

set -e

# Tự động xác định đường dẫn file docker-compose và file .env
COMPOSE_FILE="infrastructure/docker-compose-staging.yml"
DATA_PATH="./nginx/certs"
WWW_PATH="./nginx/certbot-www"

if [ ! -f "$COMPOSE_FILE" ]; then
  COMPOSE_FILE="docker-compose-staging.yml"
  DATA_PATH="./certs"
  WWW_PATH="./certbot-www"
fi

ENV_FILE="infrastructure/.env.staging"
if [ ! -f "$ENV_FILE" ]; then
  if [ -f "infrastructure/.env" ]; then
    ENV_FILE="infrastructure/.env"
  elif [ -f ".env.staging" ]; then
    ENV_FILE=".env.staging"
  elif [ -f ".env" ]; then
    ENV_FILE=".env"
  fi
fi

# Ưu tiên lấy domain từ đối số $1, nếu không có thì đọc từ ENV_FILE
if [ -n "$1" ]; then
  DOMAIN="$1"
elif [ -f "$ENV_FILE" ]; then
  ENV_DOMAIN=$(grep -E '^DOMAIN_NAME=' "$ENV_FILE" | cut -d '=' -f2 | tr -d ' "\r')
  DOMAIN="${ENV_DOMAIN:-staging.fakebook.com}"
else
  DOMAIN="staging.fakebook.com"
fi

EMAIL="${2:-admin@fakebook.com}"
RSA_KEY_SIZE=4096

echo "================================================================="
echo "🔑 INITIALIZING LET'S ENCRYPT SSL CERTIFICATE FOR ${DOMAIN}"
echo "📁 Using Compose File: ${COMPOSE_FILE}"
echo "⚙️  Using Env File:     ${ENV_FILE}"
echo "================================================================="

if [ -d "$DATA_PATH/live/$DOMAIN" ] && [ -f "$DATA_PATH/live/$DOMAIN/fullchain.pem" ]; then
  echo "✅ Existing certificates found for $DOMAIN. Skipping issuance."
  exit 0
fi

echo "1. Creating dummy certificates for Nginx initial boot..."
mkdir -p "$DATA_PATH/live/$DOMAIN"
mkdir -p "$WWW_PATH"

docker run --rm -v "$(pwd)/$DATA_PATH/live/$DOMAIN:/certs" alpine/openssl \
  req -x509 -nodes -newkey rsa:2048 \
  -keyout /certs/privkey.pem \
  -out /certs/fullchain.pem \
  -subj "/CN=$DOMAIN"

echo "2. Starting Nginx service..."
DOMAIN_NAME="$DOMAIN" TLS_CERT_NAME="$DOMAIN" docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d nginx

echo "3. Requesting Let's Encrypt SSL certificate via Certbot..."
docker run --rm \
  -v "$(pwd)/$DATA_PATH:/etc/letsencrypt" \
  -v "$(pwd)/$WWW_PATH:/var/www/certbot" \
  certbot/certbot certonly --webroot -w /var/www/certbot \
  -d "$DOMAIN" \
  --email "$EMAIL" \
  --rsa-key-size $RSA_KEY_SIZE \
  --agree-tos \
  --force-renewal \
  --non-interactive

echo "4. Reloading Nginx with production SSL certificate..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" exec nginx nginx -s reload

echo "================================================================="
echo "🎉 SUCCESS: SSL CERTIFICATE ISSUED AND APPLIED FOR ${DOMAIN}!"
echo "================================================================="
