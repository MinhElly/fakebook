# Microservices Specification

Tài liệu này đặc tả chi tiết từng microservice trong hệ thống Fakebook: trách nhiệm, cổng kết nối, cơ sở dữ liệu, Kafka events, cache, dependency và endpoint kiểm tra sức khỏe.

---

## 1. Gateway (`gateway`)

- **Trách nhiệm**: Cổng API tập trung (API Gateway) cho toàn bộ hệ thống; reverse proxy; định tuyến động theo danh mục Consul; tự động forward token OAuth2 JWT (`TokenRelay`); quản lý CORS tập trung; quản lý session reactive.
- **Framework / Runtime**: Spring Boot 3.4.x, Spring Cloud Gateway (Reactive WebFlux), R2DBC MariaDB driver.
- **Port mặc định**: `8080` (Consul discovery ID: `gateway`)
- **Database**: `gateway` (sử dụng R2DBC reactive driver cho runtime và Liquibase JDBC cho database migration).
- **Cache**: Không sử dụng.
- **Kafka**: Tích hợp Kafka binder cơ bản (`application-kafka.yml`).
- **Main APIs**:
  - `/services/{serviceId}/**`: Định tuyến động tới bất kỳ service nào đăng ký trên Consul (bỏ 2 prefix đầu `/services/{serviceId}`).
  - `/api/**`: Các endpoint API cục bộ của Gateway (ví dụ lấy thông tin user session).
  - `/management/health`: Endpoint kiểm tra sức khỏe Gateway.
- **Dependencies**: Consul, Keycloak, Downstream microservices.
- **Authentication**: OAuth2 Client (`web_app`), OAuth2 Resource Server.
- **Health Check**: `GET http://localhost:8080/management/health`

---

## 2. User Service (`userService`)

- **Trách nhiệm**: Quản lý hồ sơ người dùng (User Profiles), yêu cầu kết bạn (Friend Requests), quan hệ bạn bè 2 chiều (Friendships), theo dõi (Follows) và tính toán danh sách gợi ý bạn bè (Friend Suggestions dựa trên bạn chung).
- **Framework / Runtime**: Spring Boot, Spring Data JPA / JDBC, OpenFeign, Spring Cache.
- **Port mặc định**: `8082` (Consul discovery ID: `userservice`)
- **Database**: MariaDB schema `user_service` (Local port 3307 / Staging AWS RDS TLS)
- **Entities**: `UserProfile`, `FriendRequest`, `Follow`, `Friendship`.
- **Cache**: Redis Cache (`@Cacheable(value = "userProfiles")` trong `UserProfileService`, `FriendshipService`, `FollowService`).
- **Kafka**:
  - **Produces**:
    - `friendship-events-topic` (binding: `friendshipEventsOut-out-0`): Phát sinh khi trạng thái kết bạn thay đổi.
    - `media-cleanup-topic` (binding: `mediaCleanupOut-out-0`): Phát sinh khi avatar/cover cũ được thay thế để MediaService dọn dẹp trên Cloudinary.
  - **Consumes**: Không có consumer trực tiếp (chỉ có probe topic mặc định).
- **Main APIs**:
  - `GET/POST/PUT /api/user-profiles`: Quản lý hồ sơ cá nhân.
  - `GET /api/user-profiles/friend-suggestions`: Lấy gợi ý kết bạn kèm số lượng bạn chung.
  - `GET/POST /api/friendships`: Quản lý danh sách bạn bè.
  - `GET /api/friendships/friends-list/{userId}`: Lấy danh sách ID bạn bè (được FeedService gọi qua Feign).
  - `GET/POST /api/user-follows`: Quản lý danh sách theo dõi.
  - `GET /api/user-follows/followers-list/{userId}`: Lấy danh sách ID người theo dõi (được FeedService gọi qua Feign).
  - `POST /api/friend-requests`: Gửi, chấp nhận hoặc từ chối lời mời kết bạn.
- **Dependencies**: Media Service (qua `MediaServiceClient`), Consul, Redis, MariaDB.
- **Authentication**: OAuth2 Resource Server (xác thực token Bearer JWT phát hành bởi Keycloak).
- **Health Check**: `GET http://localhost:8082/management/health`

---

## 3. Post Service (`postService`)

- **Trách nhiệm**: Quản lý bài đăng của người dùng, liên kết hình ảnh/video đính kèm (`PostMedia`), và tương tác cảm xúc (`PostReaction`: LIKE, LOVE, WOW, SAD, ANGRY).
- **Framework / Runtime**: Spring Boot, Spring Data JPA, OpenFeign, Spring Cloud Stream Kafka.
- **Port mặc định**: `8083` (Consul discovery ID: `postservice`)
- **Database**: MariaDB schema `post_service` (Local port 3307 / Staging AWS RDS TLS)
- **Entities**: `Post`, `PostMedia`, `PostReaction`.
- **Cache**: Không trực tiếp sử dụng Redis (ủy quyền cho FeedService).
- **Kafka**:
  - **Produces**:
    - `post-events` (binding: `binding-out-0`): Phát event khi bài viết được tạo (`POST_CREATED`), cập nhật (`POST_UPDATED`) hoặc xóa (`POST_DELETED`).
    - `media-cleanup-topic` (binding: `mediaCleanupOut-out-0`): Gửi danh sách media ID cần xóa khi bài viết bị xóa hoặc sửa bớt ảnh.
  - **Consumes**: Không có.
- **Main APIs**:
  - `GET/POST/PUT/DELETE /api/posts`: CRUD bài viết.
  - `GET /api/posts/{id}`: Chi tiết bài viết.
  - `POST/DELETE /api/post-reactions`: Thả/hủy cảm xúc trên bài viết.
  - `GET/POST /api/post-medias`: Quản lý media liên kết với bài viết.
- **Dependencies**: User Service (qua `UserServiceClient`), Media Service (qua `MediaServiceClient`), Consul, Kafka, MariaDB.
- **Authentication**: OAuth2 Resource Server (Bearer JWT).
- **Health Check**: `GET http://localhost:8083/management/health`

---

## 4. Comment Service (`commentService`)

- **Trách nhiệm**: Quản lý bình luận trên bài viết (bao gồm cả phân cấp bình luận cha-con `parentId`), tương tác cảm xúc trên bình luận (`CommentReaction`). Đồng bộ metadata bài viết qua Kafka để kiểm tra tính hợp lệ trước khi comment.
- **Framework / Runtime**: Spring Boot, Spring Data JPA, JdbcTemplate, OpenFeign, Spring Cloud Stream Kafka.
- **Port mặc định**: `8085` (Consul discovery ID: `commentservice`)
- **Database**: MariaDB schema `comment_service` (Local port 3307 / Staging AWS RDS TLS)
- **Entities**: `Comment`, `CommentReaction`, `PostCache` (read model).
- **Cache**: Local read model bảng `post_cache` trong MariaDB.
- **Kafka**:
  - **Produces**: Không có.
  - **Consumes**:
    - `post-events` (binding: `processPostEvent-in-0`, group: `comment-service-post-sync`, DLQ: `post-events-dlt`): Lắng nghe sự kiện để thêm/cập nhật/xóa bản ghi tương ứng trong bảng `post_cache`.
- **Main APIs**:
  - `GET/POST/PUT/DELETE /api/comments`: CRUD bình luận trên bài viết.
  - `GET /api/comments/post/{postId}`: Lấy danh sách bình luận theo bài viết.
  - `POST/DELETE /api/comment-reactions`: Thả cảm xúc trên bình luận.
- **Dependencies**: Post Service (qua `PostFeignClient`), User Service (qua `UserServiceClient`), Consul, Kafka, MariaDB.
- **Authentication**: OAuth2 Resource Server (Bearer JWT).
- **Health Check**: `GET http://localhost:8085/management/health`

---

## 5. Media Service (`mediaService`)

- **Trách nhiệm**: Xử lý tải lên tài nguyên đa phương tiện (ảnh, video) lên dịch vụ đám mây Cloudinary, lưu trữ siêu dữ liệu (URL, kích thước, định dạng, mimetype, user upload). Thực hiện dọn dẹp file rác trên Cloudinary khi nhận event từ Kafka.
- **Framework / Runtime**: Spring Boot, Spring Data JPA, Cloudinary SDK, Spring Cloud Stream Kafka.
- **Port mặc định**: `8084` (Consul discovery ID: `mediaservice`)
- **Database**: MariaDB schema `media_service` (Local port 3307 / Staging AWS RDS TLS)
- **Entities**: `Media`.
- **Cache**: Không sử dụng.
- **Kafka**:
  - **Produces**: Không có.
  - **Consumes**:
    - `media-cleanup-topic` (binding: `processMediaCleanup-in-0`, group: `media-service`): Nhận danh sách media ID cần xóa, xóa bản ghi trong database và gọi Cloudinary API xóa file tương ứng.
- **Main APIs**:
  - `POST /api/media/upload`: Tải file lên Cloudinary và lưu metadata.
  - `GET /api/media/{id}`: Lấy thông tin metadata file.
  - `DELETE /api/media/{id}`: Xóa file thủ công.
- **Dependencies**: Cloudinary API, Consul, Kafka, MariaDB.
- **Authentication**: OAuth2 Resource Server (Bearer JWT).
- **Health Check**: `GET http://localhost:8084/management/health`

---

## 6. Feed Service (`feedService`)

- **Trách nhiệm**: Xây dựng và phân phối bảng tin cá nhân hóa (Timeline) cho người dùng. Triển khai mô hình **Fan-out on Write**: khi nhận event bài viết mới, tự động phân phối bài viết vào danh sách Feed của bạn bè và followers.
- **Framework / Runtime**: Spring Boot, Spring Data JPA, Spring Data Redis, OpenFeign, Spring Cloud Stream Kafka.
- **Port mặc định**: `8086` (Consul discovery ID: `feedservice`)
- **Database**: MariaDB schema `feed_service` (Local port 3307 / Staging AWS RDS TLS)
- **Entities**: `FeedItem`.
- **Cache / Storage**:
  - **Redis Sorted Sets (ZSet)**: Khóa `feed:user:{userId}`, member là `postId`, score là `createdAt (epoch millisecond)`. Tự động cắt tỉa giữ lại tối đa 500 bài viết mới nhất (`removeRange(key, 0, -501)`).
- **Kafka**:
  - **Produces**: Không có.
  - **Consumes**:
    - `post-events` (binding: `processPostEvent-in-0`, group: `feed-service-post-sync`, DLQ: `post-events-feed-dlt`): Tiêu thụ event tạo bài, cập nhật quyền riêng tư hoặc xóa bài viết để cập nhật đồng thời MariaDB và Redis.
- **Main APIs**:
  - `GET /api/feeds`: Lấy bảng tin của người dùng hiện tại (truy vấn nhanh qua Redis ZSet, fallback sang MariaDB nếu cache miss).
  - `GET /api/feed-items`: Quản lý các bản ghi feed item.
- **Dependencies**: User Service (qua `UserServiceClient` để lấy bạn bè/followers), Consul, Kafka, Redis, MariaDB.
- **Authentication**: OAuth2 Resource Server (Bearer JWT). Khi tiêu thụ Kafka không có user context, sử dụng `TokenRelayRequestInterceptor` với luồng OAuth2 Client Credentials (client ID `internal`) để gọi sang User Service.
- **Health Check**: `GET http://localhost:8086/management/health`

---

## 7. Auth Service (`authService`)

- **Trách nhiệm**: JHipster Microservice boilerplate. Trong kiến trúc hiện tại, **Keycloak IAM** đảm nhiệm toàn bộ vai trò Identity Provider. Dịch vụ này hiện duy trì kết nối database riêng (`auth_service`), đăng ký Consul và sẵn sàng cho các nghiệp vụ mở rộng trong tương lai (Keycloak Admin API orchestration, custom credential flows).
- **Framework / Runtime**: Spring Boot, Spring Data JPA, MariaDB driver.
- **Port mặc định**: `8081` (Consul discovery ID: `authservice`)
- **Database**: MariaDB schema `auth_service` (Local port 3307 / Staging AWS RDS TLS)
- **Main APIs**:
  - `/management/health`: Health endpoint.
- **Authentication**: OAuth2 Resource Server.
- **Health Check**: `GET http://localhost:8081/management/health`

---

## 8. Frontend Application (`frontend`)

- **Trách nhiệm**: Giao diện người dùng Web SPA tương tác hoàn chỉnh. Hỗ trợ hiển thị News Feed thời gian thực, quản lý bài viết, bộ chọn emoji, gallery hình ảnh, quản lý bạn bè và kết nối với Keycloak để xác thực.
- **Tech Stack**: React 19, Vite 8, TypeScript 5.7, Tailwind CSS v4, `keycloak-js`, Axios.
- **Port Dev**: `5173` (hoặc cấu hình tùy ý)
- **Deployment**:
  - Dev: Chạy máy host qua `npm run dev`
  - Staging: Triển khai trên **Vercel** (`https://fakebook-zeta.vercel.app`), kết nối API Gateway qua domain Staging Nginx.
- **Cấu hình runtime**: `public/config/app-config.json` tải động lúc khởi chạy trình duyệt (`apiBaseUrl`, `keycloakBaseUrl`).
