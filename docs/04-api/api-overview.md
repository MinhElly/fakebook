# API contract và quy ước

> Audit source: 2026-10-02 (Asia/Bangkok). Baseline working tree là `575c738`; `origin/develop` đang ở `2978a9f`. Hai API Saved Posts ở mục Post Service đã có trên `origin/develop` nhưng chưa có trong checkout local cho tới khi fast-forward/merge.

Tài liệu này là danh mục contract HTTP dành cho frontend và tích hợp. Source controller, DTO và security configuration vẫn là nguồn sự thật cuối cùng.

## 1. Base URL và định tuyến

Client gọi service qua Gateway:

```text
http://localhost:8080/services/{serviceId}/api/{resource}
```

Gateway dùng Consul discovery, `StripPrefix=2` và `TokenRelay`. Ví dụ:

```text
GET /services/postservice/api/posts
 -> postservice nhận GET /api/posts
```

Các `serviceId` hiện có: `authservice`, `userservice`, `postservice`, `mediaservice`, `commentservice`, `feedservice`. API cục bộ của Gateway dùng trực tiếp tiền tố `/api`.

## 2. Xác thực, phân quyền và định dạng

- API cần đăng nhập nhận `Authorization: Bearer <access-token>` hoặc session OAuth2 của Gateway.
- `ROLE_ADMIN` dành cho endpoint quản trị; `ROLE_INTERNAL` dành cho HTTP nội bộ giữa service.
- Gateway chỉ public: health/info/prometheus, `/api/authenticate`, `/api/auth-info`, tài nguyên SPA, readiness của downstream, `/services/*/api/*/public/**` và `GET /services/*/api/media/**`.
- Một endpoint được `permitAll` tại service chưa chắc public qua Gateway. Hiện `GET /api/posts/**` và `GET /api/post-reactions/**` public tại Post Service, nhưng `/services/**` tương ứng vẫn yêu cầu xác thực ở Gateway.
- JSON dùng `application/json`; PATCH chấp nhận thêm `application/merge-patch+json`; upload dùng `multipart/form-data`.
- Phân trang dùng `page` (từ 0), `size`, `sort=field,asc|desc`. Response danh sách phân trang trả body là array cùng `X-Total-Count` và `Link`.
- Criteria JHipster dùng cú pháp như `postId.equals=<uuid>`, `id.in=<uuid1>,<uuid2>`, `visibility.equals=PUBLIC`.

## 3. API Gateway

| Method | Path | Quyền | Contract |
|---|---|---|---|
| `GET` | `/api/authenticate` | Public | `204` nếu đã xác thực, `401` nếu chưa xác thực. |
| `GET` | `/api/auth-info` | Public | Trả `issuer` và OIDC `clientId`. |
| `GET` | `/api/account` | Authenticated | Thông tin tài khoản/authorities của principal hiện tại. |
| `POST` | `/api/logout` | Authenticated | Kết thúc session và trả thông tin logout OIDC. |
| `GET` | `/api/realtime/events` | Authenticated | SSE `text/event-stream`; frontend tự reconnect. |
| `GET` | `/api/users` | Authenticated theo security hiện tại | Danh sách user public của Gateway; comment trong controller nói public nhưng matcher `/api/**` đang yêu cầu login. |
| `GET` | `/api/gateway/routes` | `ROLE_ADMIN` | Danh sách route Gateway. |
| CRUD | `/api/authorities/**` | `ROLE_ADMIN` | Quản lý authority của Gateway. |

Các endpoint `*-kafka/publish|register|unregister` là probe do JHipster sinh, không phải business API cho frontend.

## 4. User Service (`userservice`)

### 4.1 Hồ sơ và tìm kiếm

| Method | Path sau Gateway | Quyền | Contract chính |
|---|---|---|---|
| `GET` | `/services/userservice/api/user-profiles/public` | Public | Danh sách profile phân trang; hỗ trợ criteria như `id.in`. |
| `GET` | `/services/userservice/api/user-profiles/public/{id}` | Public | Profile public theo UUID. |
| `GET` | `/services/userservice/api/user-profiles/me` | Authenticated | Profile của JWT subject. |
| `PATCH` | `/services/userservice/api/user-profiles/me` | Owner | Cập nhật các field cho phép: `displayName`, `bio`, `birthday`, `gender`, `location`, `education`, `work`, `relationship`, `avatarMediaId`, `coverMediaId`. |
| `GET` | `/services/userservice/api/user-profiles/search?query=...` | Authenticated | `Page<UserSearchDTO>`. |
| `GET` | `/services/userservice/api/user-profiles/{id}/details` | Authenticated | Profile kèm trạng thái quan hệ với user hiện tại. |
| `GET` | `/services/userservice/api/user-profiles/suggestions` | Authenticated | Gợi ý kết bạn. |
| CRUD | `/services/userservice/api/user-profiles/**` (không gồm các route trên) | `ROLE_ADMIN` cho create/update/patch/delete | API quản trị sinh bởi JHipster. |

### 4.2 Bạn bè, lời mời và follow

| Method | Path sau Gateway | Quyền | Contract chính |
|---|---|---|---|
| `GET` | `/services/userservice/api/friendships/me` | Authenticated | Danh sách bạn của user hiện tại. |
| `GET` | `/services/userservice/api/friendships/user/{userId}` | Authenticated | Danh sách bạn của user. |
| `DELETE` | `/services/userservice/api/friendships/user/{friendId}` | Authenticated | Hủy kết bạn. |
| `GET` | `/services/userservice/api/friendships/check?userId1=...&userId2=...` | Authenticated/internal | Kiểm tra quan hệ bạn bè; Post/Comment gọi qua Feign. |
| `GET` | `/services/userservice/api/friendships/user/{userId}/friend-ids` | Authenticated/internal | Danh sách UUID phục vụ Feed/Post. |
| `POST` | `/services/userservice/api/friend-requests/user/{userId}` | Authenticated | Gửi lời mời tới user đích. |
| `POST` | `/services/userservice/api/friend-requests/{requestId}/accept` | Receiver | Chấp nhận lời mời. |
| `POST` | `/services/userservice/api/friend-requests/{requestId}/reject` | Receiver | Từ chối lời mời. |
| `POST` | `/services/userservice/api/friend-requests/{requestId}/cancel` | Sender | Hủy lời mời đã gửi. |
| `GET` | `/services/userservice/api/friend-requests/received` | Authenticated | Danh sách lời mời nhận được, có phân trang. |
| `GET` | `/services/userservice/api/friend-requests/sent` | Authenticated | Danh sách lời mời đã gửi, có phân trang. |
| `POST` | `/services/userservice/api/follows/user/{userId}` | Authenticated | Follow user. |
| `DELETE` | `/services/userservice/api/follows/user/{userId}` | Authenticated | Unfollow user. |
| `GET` | `/services/userservice/api/follows/me/following` | Authenticated | Danh sách đang follow. |
| `GET` | `/services/userservice/api/follows/me/follower` | Authenticated | Danh sách follower. |
| `GET` | `/services/userservice/api/follows/user/{userId}/following` | Authenticated | Danh sách user đích đang follow. |
| `GET` | `/services/userservice/api/follows/user/{userId}/follower` | Authenticated | Danh sách follower của user đích. |
| `GET` | `/services/userservice/api/follows/user/{userId}/follower-ids` | Authenticated/internal | UUID follower phục vụ fan-out. |

Các CRUD gốc tại `/friendships`, `/friend-requests` và `/follows` được khóa `ROLE_ADMIN`; frontend nên dùng các route nghiệp vụ ở trên.

## 5. Post Service (`postservice`)

| Method | Path sau Gateway | Quyền | Contract chính |
|---|---|---|---|
| `GET` | `/services/postservice/api/posts` | Authenticated qua Gateway | Danh sách phân trang/criteria. Service lọc theo visibility; không xem đây là API admin dump. |
| `GET` | `/services/postservice/api/posts/{id}` | Authenticated qua Gateway | Chi tiết; service kiểm tra `PUBLIC`, `FRIENDS`, `PRIVATE`, trạng thái và role. |
| `POST` | `/services/postservice/api/posts/create` | Authenticated | Body `{content, visibility, mediaIds, taggedUserIds}`; trả `201 PostDTO`. |
| `PUT` | `/services/postservice/api/posts/{id}` | Author | Body `PostDTO`; chỉ author được cập nhật. |
| `PATCH` | `/services/postservice/api/posts/{id}` | Author | Partial `PostDTO`; `id` trong body phải khớp path. |
| `DELETE` | `/services/postservice/api/posts/{id}` | Author hoặc admin | Soft-delete, phát outbox event và lên lịch dọn media. |
| `POST` | `/services/postservice/api/posts/{id}/save` | Authenticated | Toggle trạng thái lưu bài viết, trả về boolean (`true` nếu lưu, `false` nếu bỏ lưu). |
| `GET` | `/services/postservice/api/posts/saved` | Authenticated | Danh sách bài viết đã lưu của người dùng hiện tại (trả về `List<PostDTO>`). |
| `PUT` | `/services/postservice/api/post-reactions/posts/{postId}` | Authenticated | Body `{reactionType}`; set/thay reaction và trả summary. |
| `DELETE` | `/services/postservice/api/post-reactions/posts/{postId}` | Authenticated | Gỡ reaction hiện tại. |
| `GET` | `/services/postservice/api/post-reactions/summaries?postId.in=...` | Authenticated qua Gateway | Batch reaction summary. |
| `GET` | `/services/postservice/api/post-reactions/posts/{postId}` | Authenticated qua Gateway | Danh sách reactor có phân trang. |

`/api/post-medias/**` và CRUD gốc `/api/post-reactions/**` vẫn tồn tại do code sinh; không nên dùng cho luồng frontend mới nếu route nghiệp vụ tương ứng đã có.

## 6. Comment Service (`commentservice`)

| Method | Path sau Gateway | Quyền | Contract chính |
|---|---|---|---|
| `GET` | `/services/commentservice/api/comments?postId.equals={postId}` | Authenticated qua Gateway | Danh sách comment ACTIVE có phân trang/criteria. Không có route `/comments/post/{postId}`. |
| `POST` | `/services/commentservice/api/comments/create` | Authenticated | Body `{postId, content}`; author lấy từ JWT; trả `201`. |
| `POST` | `/services/commentservice/api/comments/reply` | Authenticated | Body `{parentCommentId, content}`; trả `201`. |
| `PUT` | `/services/commentservice/api/comments/{commentId}` | Owner | Body `{content}`. |
| `DELETE` | `/services/commentservice/api/comments/{commentId}` | Owner hoặc admin | Soft-delete; trả `204`. |
| `POST` | `/services/commentservice/api/comments/summaries` | Authenticated | Body `{postIds:[...]}`; batch count và preview. |
| `GET` | `/services/commentservice/api/comments/count?postId.equals=...` | Authenticated | Đếm theo criteria. |
| `POST` | `/services/commentservice/api/comment-reactions/toggle?commentId=...&reactionType=...` | Authenticated | `200` khi set/thay, `204` khi toggle để gỡ. |

CRUD `/api/post-caches/**` và các GET quản trị của comment reaction yêu cầu `ROLE_ADMIN`.

## 7. Media Service (`mediaservice`)

| Method | Path sau Gateway | Quyền | Contract chính |
|---|---|---|---|
| `POST` | `/services/mediaservice/api/media/upload` | Authenticated | Multipart field `file`; optional `purpose=GENERAL|POST|AVATAR|COVER`; trả `201 MediaDTO`. |
| `GET` | `/services/mediaservice/api/media/{id}` | Public qua Gateway | Metadata; media không ACTIVE chỉ owner xem được. |
| `GET` | `/services/mediaservice/api/media/{id}/file` | Public qua Gateway | `302 Location` tới provider URL hoặc trả bytes nếu là data URL. |
| `GET` | `/services/mediaservice/api/media` | Public qua Gateway | Danh sách phân trang/criteria; cần cân nhắc mức lộ metadata trước production. |
| `DELETE` | `/services/mediaservice/api/media/{id}` | Owner hoặc admin | Soft-delete và xóa trên provider theo service logic. |

CRUD generic create/update/patch tồn tại nhưng là API kỹ thuật; upload là entrypoint chuẩn cho client.

## 8. Feed Service (`feedservice`)

| Method | Path sau Gateway | Quyền | Contract chính |
|---|---|---|---|
| `GET` | `/services/feedservice/api/feed/me?page=0&size=20&sort=createdAt,desc` | Authenticated | Feed cá nhân; body là `FeedItemDTO[]`. Hiện response chưa gắn `X-Total-Count`/`Link`. |

CRUD `/api/feed-items/**` chỉ dành cho `ROLE_ADMIN`.

## 9. Auth Service (`authservice`)

Auth Service hiện là JHipster skeleton, không sở hữu login/register business API. Keycloak là Identity Provider; các endpoint production có ý nghĩa của Auth Service hiện chủ yếu là `/management/health|info|prometheus`. Không gọi probe `/api/auth-service-kafka/**` từ frontend.

## 10. OpenAPI

- Các module dùng SpringDoc API starter, không dùng UI starter; không được giả định `swagger-ui/index.html` tồn tại.
- `/v3/api-docs` bị tắt trừ khi profile `api-docs` được bật.
- Qua Gateway, `/services/{serviceId}/v3/api-docs` yêu cầu `ROLE_ADMIN`.
- Gateway có `/v3/api-docs` riêng, cũng yêu cầu `ROLE_ADMIN`; đây không phải bộ tài liệu tổng hợp contract của tất cả service.

Ví dụ bật profile khi chạy một service:

```powershell
.\mvnw.cmd -ntp "-Dspring-boot.run.profiles=dev,api-docs" spring-boot:run
```

## 11. Mã lỗi cần xử lý

| Status | Ý nghĩa thường gặp |
|---:|---|
| `400` | Validation, UUID/body không khớp path, trạng thái nghiệp vụ không hợp lệ. |
| `401` | Thiếu/hết hạn token hoặc session. |
| `403` | Không phải owner/receiver/admin, visibility không cho phép, hoặc thiếu role nội bộ. |
| `404` | Resource không tồn tại hoặc được che giấu theo policy. |
| `409` | Xung đột unique relation như follow/friendship trùng. |
