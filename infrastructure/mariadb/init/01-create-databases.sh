#!/bin/sh
set -e

MARIADB_ROOT_PASSWORD="${MARIADB_ROOT_PASSWORD:-}"
PASS_ARG=""
if [ -n "${MARIADB_ROOT_PASSWORD}" ]; then
  PASS_ARG="-p${MARIADB_ROOT_PASSWORD}"
fi

mariadb --protocol=socket -uroot ${PASS_ARG} <<SQL
CREATE DATABASE IF NOT EXISTS gateway
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS authservice
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS userservice
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mediaservice
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS postservice
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS feedservice
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS commentservice
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS keycloak
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SQL

create_user_if_set() {
  db_name="$1"
  db_user="$2"
  db_pass="$3"
  if [ -n "$db_user" ]; then
    mariadb --protocol=socket -uroot ${PASS_ARG} <<SQL
CREATE USER IF NOT EXISTS '${db_user}'@'%' IDENTIFIED BY '${db_pass}';
ALTER USER '${db_user}'@'%' IDENTIFIED BY '${db_pass}';
GRANT ALL PRIVILEGES ON \`${db_name}\`.* TO '${db_user}'@'%';
FLUSH PRIVILEGES;
SQL
  fi
}

create_user_if_set "gateway" "${GATEWAY_DB_USERNAME:-}" "${GATEWAY_DB_PASSWORD:-}"
create_user_if_set "authservice" "${AUTH_DB_USERNAME:-}" "${AUTH_DB_PASSWORD:-}"
create_user_if_set "userservice" "${USER_DB_USERNAME:-}" "${USER_DB_PASSWORD:-}"
create_user_if_set "postservice" "${POST_DB_USERNAME:-}" "${POST_DB_PASSWORD:-}"
create_user_if_set "mediaservice" "${MEDIA_DB_USERNAME:-}" "${MEDIA_DB_PASSWORD:-}"
create_user_if_set "feedservice" "${FEED_DB_USERNAME:-}" "${FEED_DB_PASSWORD:-}"
create_user_if_set "commentservice" "${COMMENT_DB_USERNAME:-}" "${COMMENT_DB_PASSWORD:-}"
create_user_if_set "keycloak" "${KEYCLOAK_DB_USERNAME:-}" "${KEYCLOAK_DB_PASSWORD:-}"

