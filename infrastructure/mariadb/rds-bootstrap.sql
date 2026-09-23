-- =============================================================================
-- FAKEBOOK MONOREPO: AWS RDS MARIADB BOOTSTRAP SCRIPT
-- File: infrastructure/mariadb/rds-bootstrap.sql
-- Description: Creates 8 isolated database schemas and dedicated microservice
--              users with least-privilege access and SSL enforcement.
-- Usage: Execute using RDS Master Admin account:
--   mariadb -h <RDS_ENDPOINT> -P 13306 -u <ADMIN_USER> -p \
--           --ssl-ca=infrastructure/certs/global-bundle.pem < infrastructure/mariadb/rds-bootstrap.sql
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. CREATE SCHEMAS (DATABASE-PER-SERVICE)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `gateway`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `auth_service`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `user_service`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `post_service`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `media_service`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `comment_service`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `feed_service`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `keycloak`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. CREATE SERVICE USERS & ENFORCE SSL
-- Note: Replace '<PASSWORD_...>' with strong production/staging passwords.
--       Refer to infrastructure/.env.staging for staging credentials.
-- -----------------------------------------------------------------------------

-- Gateway User
CREATE USER IF NOT EXISTS 'gateway_user'@'%' IDENTIFIED BY 'fakebook@Gateway';
ALTER USER 'gateway_user'@'%' IDENTIFIED BY 'fakebook@Gateway' REQUIRE SSL;
GRANT ALL PRIVILEGES ON `gateway`.* TO 'gateway_user'@'%';

-- Auth Service User
CREATE USER IF NOT EXISTS 'auth_user'@'%' IDENTIFIED BY 'fakebook@Auth';
ALTER USER 'auth_user'@'%' IDENTIFIED BY 'fakebook@Auth' REQUIRE SSL;
GRANT ALL PRIVILEGES ON `auth_service`.* TO 'auth_user'@'%';

-- User Service User
CREATE USER IF NOT EXISTS 'user_user'@'%' IDENTIFIED BY 'fakebook@User';
ALTER USER 'user_user'@'%' IDENTIFIED BY 'fakebook@User' REQUIRE SSL;
GRANT ALL PRIVILEGES ON `user_service`.* TO 'user_user'@'%';

-- Post Service User
CREATE USER IF NOT EXISTS 'post_user'@'%' IDENTIFIED BY 'fakebook@Post';
ALTER USER 'post_user'@'%' IDENTIFIED BY 'fakebook@Post' REQUIRE SSL;
GRANT ALL PRIVILEGES ON `post_service`.* TO 'post_user'@'%';

-- Media Service User
CREATE USER IF NOT EXISTS 'media_user'@'%' IDENTIFIED BY 'fakebook@Media';
ALTER USER 'media_user'@'%' IDENTIFIED BY 'fakebook@Media' REQUIRE SSL;
GRANT ALL PRIVILEGES ON `media_service`.* TO 'media_user'@'%';

-- Comment Service User
CREATE USER IF NOT EXISTS 'comment_user'@'%' IDENTIFIED BY 'fakebook@Comment';
ALTER USER 'comment_user'@'%' IDENTIFIED BY 'fakebook@Comment' REQUIRE SSL;
GRANT ALL PRIVILEGES ON `comment_service`.* TO 'comment_user'@'%';

-- Feed Service User
CREATE USER IF NOT EXISTS 'feed_user'@'%' IDENTIFIED BY 'fakebook@Feed';
ALTER USER 'feed_user'@'%' IDENTIFIED BY 'fakebook@Feed' REQUIRE SSL;
GRANT ALL PRIVILEGES ON `feed_service`.* TO 'feed_user'@'%';

-- Keycloak User
CREATE USER IF NOT EXISTS 'keycloak'@'%' IDENTIFIED BY 'fakebook@Keycloak';
ALTER USER 'keycloak'@'%' IDENTIFIED BY 'fakebook@Keycloak' REQUIRE SSL;
GRANT ALL PRIVILEGES ON `keycloak`.* TO 'keycloak'@'%';

-- -----------------------------------------------------------------------------
-- 3. APPLY PRIVILEGES
-- -----------------------------------------------------------------------------
FLUSH PRIVILEGES;
