# Database Architecture (Kiến trúc Cơ sở dữ liệu)

Tài liệu này đặc tả chi tiết kiến trúc lưu trữ dữ liệu quan hệ của Fakebook trên hệ quản trị cơ sở dữ liệu **MariaDB**.

---

## 1. Mô hình Database-per-Service

Hệ thống áp dụng nghiêm ngặt nguyên tắc **Database-per-Service**:
- Mỗi microservice sở hữu một schema/database riêng biệt.
- Không có bất kỳ foreign key hoặc câu lệnh SQL JOIN nào được phép thực hiện xuyên giữa hai schema khác nhau.
- Định danh người dùng xuyên suốt các bảng là UUID được phát hành từ Keycloak.

### Bảng danh mục Schemas & Quyền sở hữu (Thống nhất chuẩn `snake_case`):

| Schema (MariaDB) | Microservice sở hữu | Mục đích lưu trữ |
| :--- | :--- | :--- |
| `gateway` | `gateway` | Session reactive (Spring Session R2DBC) & Liquibase tracking |
| `user_service` | `userService` | Bảng `user_profiles`, `friendships`, `friend_requests`, `follows` |
| `post_service` | `postService` | Bảng `posts`, `post_media`, `post_reactions` |
| `comment_service` | `commentService` | Bảng `comments`, `comment_reactions`, `post_cache` |
| `media_service` | `mediaService` | Bảng `media` (Cloudinary URLs, metadata) |
| `feed_service` | `feedService` | Bảng `feed_items` (Bản ghi timeline lưu trữ lâu dài) |
| `auth_service` | `authService` | Bảng của JHipster skeleton |
| `keycloak` | `keycloak` | Dữ liệu nội bộ của Keycloak (Users, Credentials, Realms, Clients) |

---

## 2. Kiến trúc Môi trường Local vs Staging

### 2.1 Môi trường Cục bộ (Local Dev)
- **Container**: `fakebook-mariadb` (hình ảnh `mariadb:12.3.3`) chạy trên port host `3307`.
- **Khởi tạo tự động**: File `infrastructure/mariadb/init/01-create-databases.sh` tự động chạy khi container khởi tạo lần đầu để tạo đủ 8 database schemas và cấp quyền cho user `root`.

### 2.2 Môi trường Staging (AWS RDS MariaDB Managed Database)
- Không chạy MariaDB container trên máy chủ ảo Staging, mà sử dụng **AWS RDS MariaDB**.
- **Yêu cầu bảo mật SSL/TLS**: Toàn bộ kết nối từ VM Staging tới RDS bắt buộc phải qua kênh mã hóa SSL (`require_secure_transport=ON`).
  - Chứng chỉ CA: `infrastructure/certs/global-bundle.pem` (RDS Global Bundle).
  - Chuỗi kết nối JDBC:
    ```text
    jdbc:mariadb://${MARIADB_HOST}:${MARIADB_PORT}/post_service?useLegacyDatetimeCode=false&sslMode=verify-full&serverSslCert=/run/secrets/rds-global-bundle.pem
    ```

---

## 3. Quản lý Connection Pool (HikariCP Sizing Budget)

AWS RDS có giới hạn tổng số lượng kết nối tối đa (`max_connections`). Để ngăn chặn tình trạng cạn kiệt connection dẫn đến lỗi `Too many connections`:

### Bảng ngân sách kết nối (Connection Budget):

| Service | Driver | Min Idle | Max Pool Size | Ghi chú |
| :--- | :--- | :---: | :---: | :--- |
| `gateway` | R2DBC Connection Pool | 1 | 3 | Reactive non-blocking, tiêu tốn rất ít connection |
| `authservice` | HikariCP | 1 | 3 | |
| `userservice` | HikariCP | 1 | 3 | |
| `postservice` | HikariCP | 1 | 3 | |
| `mediaservice` | HikariCP | 1 | 3 | |
| `commentservice`| HikariCP | 1 | 3 | |
| `feedservice` | HikariCP | 1 | 3 | |
| `keycloak` | Agroal Pool | 1 | 5 | Keycloak phục vụ luồng đăng nhập và kiểm tra token |
| **TỔNG CỘNG** | | | **~26 connections** | Hoàn toàn nằm trong ngưỡng an toàn của AWS RDS Micro |
