-- ============================================================================
-- 1. KHIỂN TẠO 7 DATABASES CHO 7 MICROSERVICES
-- ============================================================================
CREATE DATABASE IF NOT EXISTS gateway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS authservice CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS userservice CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS postservice CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mediaservice CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS commentservice CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS feedservice CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================================
-- 2. TẠO 7 DATABASE USERS RIÊNG BIỆT & PHÂN QUYỀN TRUY CẬP
-- ============================================================================

-- Gateway User
CREATE USER IF NOT EXISTS 'gateway_user'@'%' IDENTIFIED BY 'GatewayPass2026!';
GRANT ALL PRIVILEGES ON gateway.* TO 'gateway_user'@'%';

-- Auth Service User
CREATE USER IF NOT EXISTS 'auth_user'@'%' IDENTIFIED BY 'AuthPass2026!';
GRANT ALL PRIVILEGES ON authservice.* TO 'auth_user'@'%';

-- User Service User
CREATE USER IF NOT EXISTS 'user_user'@'%' IDENTIFIED BY 'UserPass2026!';
GRANT ALL PRIVILEGES ON userservice.* TO 'user_user'@'%';

-- Post Service User
CREATE USER IF NOT EXISTS 'post_user'@'%' IDENTIFIED BY 'PostPass2026!';
GRANT ALL PRIVILEGES ON postservice.* TO 'post_user'@'%';

-- Media Service User
CREATE USER IF NOT EXISTS 'media_user'@'%' IDENTIFIED BY 'MediaPass2026!';
GRANT ALL PRIVILEGES ON mediaservice.* TO 'media_user'@'%';

-- Comment Service User
CREATE USER IF NOT EXISTS 'comment_user'@'%' IDENTIFIED BY 'CommentPass2026!';
GRANT ALL PRIVILEGES ON commentservice.* TO 'comment_user'@'%';

-- Feed Service User
CREATE USER IF NOT EXISTS 'feed_user'@'%' IDENTIFIED BY 'FeedPass2026!';
GRANT ALL PRIVILEGES ON feedservice.* TO 'feed_user'@'%';

-- Áp dụng ngay thay đổi quyền
FLUSH PRIVILEGES;
