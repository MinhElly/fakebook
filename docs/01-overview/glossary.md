# Glossary (Bảng thuật ngữ dự án)

Bảng thuật ngữ chuẩn hóa các khái niệm kỹ thuật và thuật ngữ nghiệp vụ được sử dụng trong codebase và tài liệu dự án Fakebook.

---

## 1. Thuật ngữ Kiến trúc & Hạ tầng

| Thuật ngữ | Ý nghĩa trong dự án Fakebook |
| :--- | :--- |
| **API Gateway** | Dịch vụ cổng vào tập trung sử dụng Spring Cloud Gateway (WebFlux). Chịu trách nhiệm reverse proxy, định tuyến động theo Consul Service Discovery và forward token JWT sang các service phía sau. |
| **Service Discovery (Consul)** | Cơ chế giúp các microservice tự động đăng ký địa chỉ IP/Port khi khởi động và giúp Gateway tìm kiếm microservice mà không cần cấu hình hardcode IP. |
| **Consul KV (Centralized Config)** | Kho lưu trữ Key-Value của HashiCorp Consul. Được `consul-config-loader` nạp các file cấu hình YAML từ thư mục `infrastructure/config/central-server-config/` khi hệ thống khởi động. |
| **Database-per-Service** | Pattern thiết kế kiến trúc: Mỗi microservice sở hữu một cơ sở dữ liệu/schema MariaDB riêng biệt (`gateway`, `userservice`, `postservice`, `mediaservice`, `feedservice`, `commentservice`, `authservice`). Tuyệt đối không query chéo database. |
| **Liquibase** | Công cụ quản lý và thực thi migration cấu trúc cơ sở dữ liệu bằng các changelog YAML/XML, tự động chạy khi service khởi động. |
| **Fan-out on Write (Push Model)** | Mô hình xây dựng bảng tin: Khi tác giả tạo một bài viết mới, hệ thống bắn event qua Kafka. Feed Service tiêu thụ event này, truy vấn danh sách bạn bè/followers rồi ghi bản ghi FeedItem vào database/cache của từng người nhận. |
| **ZSet (Redis Sorted Set)** | Cấu trúc dữ liệu trong Redis lưu danh sách `postId` với `score` là timestamp (epoch millisecond). Cho phép phân trang bảng tin theo thời gian cực nhanh với độ phức tạp $O(\log(N) + M)$. |
| **DLQ (Dead Letter Queue)** | Hàng đợi chứa các message lỗi sau khi consumer đã thử lại (retry) vượt quá số lần cấu hình (ví dụ: `post-events-feed-dlt`). |
| **Token Relay** | Kỹ thuật chuyển tiếp Authorization Header (Bearer JWT) từ Gateway xuống các microservice downstream thông qua bộ lọc `TokenRelayGatewayFilterFactory`. |
| **M2M (Machine-to-Machine) Auth** | Cơ chế xác thực giữa các service mà không có sự hiện diện của người dùng (ví dụ: Kafka consumer gọi Feign client sang service khác). Sử dụng luồng OAuth2 `client_credentials` với client ID `internal`. |

---

## 2. Thuật ngữ Nghiệp vụ (Domain Entities)

| Thuật ngữ | Định nghĩa nghiệp vụ |
| :--- | :--- |
| **UserProfile** | Hồ sơ người dùng (display name, bio, avatar, vị trí, học vấn...). ID của UserProfile là UUID khớp chính xác với ID của tài khoản trên Keycloak. |
| **Friendship** | Quan hệ bạn bè 2 chiều giữa User A và User B sau khi lời mời kết bạn được chấp nhận. |
| **FriendRequest** | Yêu cầu kết bạn một chiều với các trạng thái: `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`. |
| **Follow** | Quan hệ theo dõi một chiều. Người dùng có thể theo dõi một tài khoản để nhận các bài viết PUBLIC trên feed mà không cần kết bạn. |
| **Post** | Bài đăng của người dùng, gồm nội dung text, trạng thái (`ACTIVE`, `INACTIVE`) và quyền hiển thị (`PUBLIC`, `PRIVATE`, `FRIENDS`). |
| **PostMedia** | Liên kết giữa bài viết và danh sách ảnh/video được lưu trong Media Service. |
| **PostReaction** | Phản ứng cảm xúc của người dùng đối với bài viết: `LIKE`, `LOVE`, `WOW`, `SAD`, `ANGRY`. |
| **Comment** | Bình luận trên bài viết. Hỗ trợ trả lời (reply) theo cấu trúc cha-con (`parentId`). |
| **PostCache** | Bảng bản sao cục bộ trong Comment Service dùng để kiểm tra tính hợp lệ và quyền riêng tư của bài viết trước khi cho phép bình luận, đồng bộ qua Kafka event. |
| **FeedItem** | Bản ghi bảng tin chỉ định rõ người nhận (`userId`), bài viết (`postId`) và thời điểm tạo để phục vụ hiển thị Timeline. |
