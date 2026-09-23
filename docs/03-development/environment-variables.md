# Environment Variables Reference

Tài liệu này tổng hợp toàn bộ các biến môi trường được sử dụng trong hệ thống Fakebook cho cả môi trường phát triển cục bộ (**Dev**) và môi trường máy chủ thử nghiệm (**Staging**).

> [!WARNING]
> Tuyệt đối không commit file `.env` hoặc `.env.staging` chứa mật khẩu thật lên Git. Tất cả các giá trị nhạy cảm dưới đây đều được thay bằng giá trị mẫu hoặc placeholder `<REPLACE_ME>`.

---

## 1. Biến môi trường cho hạ tầng cục bộ (Dev `.env`)

Được định nghĩa tại `infrastructure/.env.example`:

| Tên biến | Dịch vụ sử dụng | Bắt buộc | Giá trị mặc định | Mô tả & Ví dụ |
| :--- | :--- | :---: | :--- | :--- |
| `MARIADB_PORT` | `mariadb` | Không | `3307` | Cổng host expose của MariaDB container |
| `KEYCLOAK_HTTP_PORT` | `keycloak` | Không | `9080` | Cổng HTTP của Keycloak |
| `KEYCLOAK_HTTPS_PORT` | `keycloak` | Không | `9443` | Cổng HTTPS của Keycloak |
| `CONSUL_HTTP_PORT` | `consul` | Không | `8500` | Cổng HTTP UI của Consul |
| `CONSUL_SERF_LAN_PORT`| `consul` | Không | `8300` | Cổng LAN gossip của Consul |
| `CONSUL_DNS_PORT` | `consul` | Không | `8600` | Cổng DNS của Consul |
| `KAFKA_PORT` | `kafka` | Không | `9092` | Cổng truy cập Kafka broker từ máy host |
| `KAFKA_UI_PORT` | `kafka-ui` | Không | `8088` | Cổng truy cập Kafka UI |
| `REDIS_PORT` | `redis` | Không | `6379` | Cổng truy cập Redis |
| `ZIPKIN_PORT` | `zipkin` | Không | `9411` | Cổng giao diện Zipkin Tracing |
| `SPRING_CLOUD_CONSUL_HOST` | Backend Services | Không | `localhost` | Địa chỉ host Consul cho app chạy trên máy thật |
| `SPRING_CLOUD_CONSUL_PORT` | Backend Services | Không | `8500` | Cổng Consul cho app chạy trên máy thật |
| `FAKEBOOK_DB_USERNAME` | Backend Services | Không | `root` | Tài khoản kết nối MariaDB local |
| `FAKEBOOK_DB_PASSWORD` | Backend Services | Không | *(để trống)* | Mật khẩu kết nối MariaDB local |
| `FAKEBOOK_OIDC_ISSUER_URI` | Backend Services | **Có** | `http://localhost:9080/realms/jhipster` | Đường dẫn Issuer URI của Keycloak Realm |
| `FAKEBOOK_OIDC_GATEWAY_CLIENT_ID`| `gateway` | Không | `web_app` | Client ID cho Gateway |
| `FAKEBOOK_OIDC_GATEWAY_CLIENT_SECRET`| `gateway` | Không | `web_app` | Client Secret cho Gateway |
| `FAKEBOOK_OIDC_INTERNAL_CLIENT_ID` | Tất cả microservices | Không | `internal` | Client ID dùng cho Feign M2M |
| `FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET`| Tất cả microservices | **Có** | `change_me` | Client Secret dùng cho Feign M2M |
| `KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME`| `keycloak` | Không | `admin` | Tài khoản admin ban đầu của Keycloak |
| `KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD`| `keycloak` | Không | `admin` | Mật khẩu admin ban đầu của Keycloak |
| `KEYCLOAK_GOOGLE_CLIENT_ID` | `keycloak-config` | Không | *(để trống)* | Google OAuth2 Client ID cho SSO |
| `KEYCLOAK_GOOGLE_CLIENT_SECRET` | `keycloak-config` | Không | *(để trống)* | Google OAuth2 Client Secret cho SSO |
| `CLOUDINARY_CLOUD_NAME`| `mediaService` | Tùy chọn | *(để trống)* | Tên Cloudinary cloud |
| `CLOUDINARY_API_KEY` | `mediaService` | Tùy chọn | *(để trống)* | Cloudinary API Key |
| `CLOUDINARY_API_SECRET`| `mediaService` | Tùy chọn | *(để trống)* | Cloudinary API Secret |

---

## 2. Biến môi trường cho máy chủ Staging (`.env.staging`)

Được định nghĩa tại `infrastructure/.env.staging.example`:

| Tên biến | Dịch vụ sử dụng | Bắt buộc | Ví dụ mẫu | Mô tả |
| :--- | :--- | :---: | :--- | :--- |
| `DOMAIN_NAME` | Nginx, Gateway, Keycloak | **Có** | `20-189-114-210.nip.io` | Tên miền chính của môi trường staging |
| `STAGING_FRONTEND_URL` | Nginx, Gateway | **Có** | `https://fakebook-zeta.vercel.app` | URL ứng dụng frontend được Vercel host |
| `KEYCLOAK_HOSTNAME` | `keycloak` | **Có** | `https://20-189-114-210.nip.io` | Public Hostname mà trình duyệt gọi tới Keycloak |
| `DOCKER_REGISTRY_PREFIX`| Docker Compose | **Có** | `ghcr.io/minhelly/` | Tiền tố GitHub Container Registry |
| `IMAGE_TAG` | Docker Compose | **Có** | `staging` hoặc git SHA | Phiên bản tag Docker image kéo từ GHCR |
| `MARIADB_HOST` | Toàn bộ services | **Có** | `rds-endpoint.amazonaws.com` | Địa chỉ DNS AWS RDS MariaDB |
| `MARIADB_PORT` | Toàn bộ services | Không | `3306` | Cổng dịch vụ MariaDB RDS |
| `MARIADB_SSL_CERT_HOST_PATH` | Toàn bộ services | **Có** | `./certs/global-bundle.pem` | Đường dẫn file chứng chỉ RDS CA trên host |
| `MARIADB_SSL_CERT_CONTAINER_PATH` | Container mount | Không | `/run/secrets/rds-global-bundle.pem` | Đường dẫn chứng chỉ trong container |
| `GATEWAY_DB_USERNAME` | `gateway` | **Có** | `gateway_user` | Tài khoản database cho Gateway |
| `GATEWAY_DB_PASSWORD` | `gateway` | **Có** | `<REPLACE_ME>` | Mật khẩu database cho Gateway |
| `AUTH_DB_USERNAME` | `authservice` | **Có** | `auth_user` | Tài khoản database cho AuthService |
| `AUTH_DB_PASSWORD` | `authservice` | **Có** | `<REPLACE_ME>` | Mật khẩu database cho AuthService |
| `USER_DB_USERNAME` | `userservice` | **Có** | `user_user` | Tài khoản database cho UserService |
| `USER_DB_PASSWORD` | `userservice` | **Có** | `<REPLACE_ME>` | Mật khẩu database cho UserService |
| `POST_DB_USERNAME` | `postservice` | **Có** | `post_user` | Tài khoản database cho PostService |
| `POST_DB_PASSWORD` | `postservice` | **Có** | `<REPLACE_ME>` | Mật khẩu database cho PostService |
| `MEDIA_DB_USERNAME` | `mediaservice` | **Có** | `media_user` | Tài khoản database cho MediaService |
| `MEDIA_DB_PASSWORD` | `mediaservice` | **Có** | `<REPLACE_ME>` | Mật khẩu database cho MediaService |
| `COMMENT_DB_USERNAME` | `commentservice` | **Có** | `comment_user` | Tài khoản database cho CommentService |
| `COMMENT_DB_PASSWORD` | `commentservice` | **Có** | `<REPLACE_ME>` | Mật khẩu database cho CommentService |
| `FEED_DB_USERNAME` | `feedservice` | **Có** | `feed_user` | Tài khoản database cho FeedService |
| `FEED_DB_PASSWORD` | `feedservice` | **Có** | `<REPLACE_ME>` | Mật khẩu database cho FeedService |
| `KEYCLOAK_DB_USERNAME`| `keycloak` | **Có** | `keycloak_user` | Tài khoản database cho Keycloak |
| `KEYCLOAK_DB_PASSWORD`| `keycloak` | **Có** | `<REPLACE_ME>` | Mật khẩu database cho Keycloak |
| `APP_DB_POOL_MIN_IDLE`| Các services | Không | `1` | Số connection tối thiểu trong HikariCP |
| `APP_DB_POOL_MAX_SIZE`| Các services | Không | `3` | Giới hạn connection tối đa để bảo vệ RDS |
| `FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET` | Services & Keycloak | **Có** | `<REPLACE_ME>` | Secret cho client M2M `internal` |

---

## 3. Biến môi trường Frontend (`frontend/.env` hoặc Vercel)

| Tên biến | Bắt buộc | Ví dụ mẫu | Mô tả |
| :--- | :---: | :--- | :--- |
| `VITE_API_BASE_URL` | Tùy chọn | `https://20-189-114-210.nip.io` | URL gốc của Gateway (nếu để trống, frontend gọi relative) |
| `VITE_KEYCLOAK_URL` | **Có** | `https://20-189-114-210.nip.io` | URL gốc của Keycloak server phục vụ xác thực |
