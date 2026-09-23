# Inter-Service Communication (Giao tiếp giữa các dịch vụ)

Tài liệu này đặc tả các hình thức giao tiếp giữa các microservices trong Fakebook, phân định rõ khi nào dùng gọi đồng bộ (Synchronous via OpenFeign) và khi nào dùng gọi bất đồng bộ (Asynchronous via Kafka).

---

## 1. Ma trận giao tiếp giữa các Microservice

| Caller Service | Target Service | Phương thức giao tiếp | Cơ chế thực thi | Mục đích |
| :--- | :--- | :--- | :--- | :--- |
| **Feed Service** | `User Service` | **Đồng bộ (HTTP Feign)** | `UserServiceClient` | Lấy danh sách bạn bè & followers của tác giả để phân phối Feed |
| **Post Service** | `User Service` | **Đồng bộ (HTTP Feign)** | `UserServiceClient` | Kiểm tra quyền tác giả, thông tin người dùng |
| **Post Service** | `Media Service` | **Đồng bộ (HTTP Feign)** | `MediaServiceClient`| Xác thực danh sách media IDs trước khi liên kết bài viết |
| **User Service** | `Media Service` | **Đồng bộ (HTTP Feign)** | `MediaServiceClient`| Xác thực ảnh avatar / cover profile |
| **Comment Service**| `Post Service` | **Đồng bộ (HTTP Feign)** | `PostFeignClient` | Kiểm tra bài viết tồn tại |
| **Comment Service**| `User Service` | **Đồng bộ (HTTP Feign)** | `UserServiceClient` | Kiểm tra thông tin người bình luận |
| **Post Service** | `Feed Service` | **Bất đồng bộ (Kafka)** | Topic `post-events` | Thông báo bài viết mới, cập nhật, xóa bài viết |
| **Post Service** | `Comment Service` | **Bất đồng bộ (Kafka)** | Topic `post-events` | Cập nhật bảng bản sao `post_cache` |
| **Post / User Service**| `Media Service`| **Bất đồng bộ (Kafka)** | Topic `media-cleanup-topic` | Yêu cầu dọn dẹp ảnh/video rác trên Cloudinary |

---

## 2. Cấu hình OpenFeign & Circuit Breaker

### 2.1 Khai báo Feign Client
Các Feign Client sử dụng tên dịch vụ đăng ký trên Consul để định tuyến động mà không cần hardcode địa chỉ IP:

```java
@FeignClient(name = "userservice", configuration = FeignConfiguration.class)
public interface UserServiceClient {

    @GetMapping("/api/friendships/friends-list/{userId}")
    List<UUID> getUserFriendsList(@PathVariable("userId") UUID userId);

    @GetMapping("/api/user-follows/followers-list/{userId}")
    List<UUID> getUserFollowersList(@PathVariable("userId") UUID userId);
}
```

### 2.2 Circuit Breaker & Resilience4j (Staging)
Trong môi trường Staging (`application-staging.yml`), tính năng Circuit Breaker được kích hoạt để ngăn chặn lỗi xếp tầng (cascading failure):

```yaml
feign:
  circuitbreaker:
    enabled: true
```

Khi service đích gặp sự cố hoặc quá tải:
- Circuit Breaker sẽ chuyển sang trạng thái **OPEN** sau một tỷ lệ lỗi nhất định (Error Rate Threshold).
- Các cuộc gọi tiếp theo sẽ trả về fallback mặc định ngay lập tức (Fail Fast) thay vì chờ đợi timeout gây tắc nghẽn connection pool và thread pool.

---

## 3. Xác thực trong Feign Client (Hybrid Security Interceptor)

Như đã phân tích tại tài liệu [authentication-flow.md](../02-architecture/authentication-flow.md), các Feign client được gắn `TokenRelayRequestInterceptor`:
1. Nếu request xuất phát từ một request HTTP của người dùng, token Bearer JWT của người dùng được sao chép và chuyển tiếp nguyên vẹn.
2. Nếu request xuất phát từ tiến trình chạy ngầm (như Kafka Consumer của Feed Service), interceptor tự động kích hoạt luồng **OAuth2 Client Credentials (M2M)** lấy token với client ID `internal` và client secret `FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET` để xác thực hợp lệ với Resource Server của service đích.
