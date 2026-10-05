# Giao tiếp giữa các service

> Audit source: 2026-10-02. Tài liệu phân biệt HTTP đồng bộ, Kafka bất đồng bộ và đường đi xác thực; health check không được xem là bằng chứng cho business flow.

## 1. Ma trận HTTP/Feign

| Caller | Target | Endpoint | Mục đích | Khi lỗi |
|---|---|---|---|---|
| Feed | User | `GET /api/friendships/user/{userId}/friend-ids` | Fan-out bài viết tới bạn bè. | `UserServiceFallback` fail closed; không giả lập danh sách thành công. |
| Feed | User | `GET /api/follows/user/{userId}/follower-ids` | Fan-out tới followers. | Như trên. |
| Feed | Post | `GET /api/internal/feed-posts?authorId=...&limit=...` | Warm feed khi có quan hệ bạn bè mới. | `PostServiceFallback`; endpoint đích yêu cầu `ROLE_INTERNAL`. |
| Post | User | `GET /api/friendships/check?...` | Kiểm tra quyền xem post `FRIENDS`. | `UserServiceClientFallback`. |
| Post | User | `GET /api/friendships/user/{userId}/friend-ids` | Lấy social graph phục vụ truy vấn. | `UserServiceClientFallback`. |
| Post | Media | `GET /api/media/{id}` | Xác minh owner/status/purpose trước khi gắn media. | `MediaServiceClientFallback`; tạo post phải dừng khi không xác minh được. |
| User | Media | `GET /api/media/{id}` | Xác minh avatar/cover. | `MediaServiceClientFallback`. |
| Comment | User | `GET /api/friendships/check?...` | Kiểm tra visibility của comment theo post. | `UserServiceFallback`. |
| Comment | Post | `GET /api/posts/{id}` | Bù `post_cache` khi read model thiếu. | `PostFeignClientFallback`; đường chính vẫn là Kafka projection. |

Feign dùng service name trong Consul, không hard-code host. Key cấu hình hiệu lực là:

```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
```

Các service có Feign (`userService`, `postService`, `commentService`, `feedService`) khai báo key này trong `application.yml`. Resilience4j và Actuator expose `circuitbreakers`/`circuitbreakerevents`, nhưng trạng thái runtime `OPEN -> HALF_OPEN -> CLOSED` vẫn phải được chứng minh bằng outage thật qua Gateway.

## 2. Xác thực cho HTTP nội bộ

- Trong request đồng bộ của người dùng, interceptor chuyển tiếp Bearer token hiện tại.
- Trong Kafka/background flow không có user context, Feed có thể dùng OAuth2 Client Credentials của client `internal` để gọi endpoint nội bộ.
- `GET /api/internal/feed-posts` yêu cầu `ROLE_INTERNAL`; token service-account phải thực sự chứa authority này.
- Không dùng token client-credentials để chứng minh `/api/account` hoặc `/userinfo`; các endpoint đó cần ngữ cảnh normal user.

## 3. Ma trận Kafka và outbox

| Producer | Topic/binding | Consumer | Mục đích |
|---|---|---|---|
| Post outbox | `post-events` | Feed | Tạo/cập nhật/xóa projection MariaDB. |
| Post outbox | `post-events` | Comment | Đồng bộ `post_cache`. |
| User outbox | `friendship-events` | Feed | Cập nhật feed projection khi quan hệ bạn bè đổi. |
| User outbox | `friend-request-events` | Gateway | Phát realtime trạng thái lời mời kết bạn. |
| Post/User outbox | `media-cleanup-topic` | Media | Dọn media không còn được tham chiếu. |
| Post outbox | `post-reaction-events` | Gateway | Phát realtime reaction của post. |
| Comment outbox | `comment-events` | Gateway | Phát realtime comment. |

Post và User lưu event cùng transaction nghiệp vụ trong transactional outbox. Publisher chỉ chuyển các propagation header nằm trong allowlist của `Propagator.fields()` sang Kafka record. Vì vậy cần kiểm tra header thật khi nghiệm thu trace; không mặc định duy nhất `b3` hoặc `traceparent`.

## 4. Ranh giới độ tin cậy

- Feign fallback phải bảo toàn nguyên nhân và fail closed ở quyết định quyền/ownership; không trả empty/null/false như một kết quả nghiệp vụ hợp lệ nếu điều đó che giấu outage.
- Kafka single-node + volume/AOF là durability cho staging, không phải HA.
- Gateway realtime sink và SSE emitter trong service là process-local; nhiều replica có thể không cùng nhìn thấy event. Phải có bài test client kết nối qua các pod khác nhau trước khi scale ngang.
- Health 200, Consul passing hoặc consumer đã đăng ký không chứng minh redelivery, DLT, trace xuyên Kafka hay recovery hoàn chỉnh.

## 5. Gate nghiệm thu tối thiểu

1. Dùng token normal-user gọi một business route qua Gateway và giữ `traceId`.
2. Dừng downstream thật, gọi lại route, kiểm tra response fallback và `/management/circuitbreakerevents`.
3. Khởi động downstream, chờ `HALF_OPEN -> CLOSED`, rồi xác nhận request thành công.
4. Với Kafka: dừng broker/consumer, tạo event qua business API, khởi động lại và xác minh outbox retry, consumer idempotency và DLT khi vượt retry.
5. Với realtime: kết nối SSE trước khi phát event, kiểm tra event ID/type/payload và reconnect/reconciliation.
