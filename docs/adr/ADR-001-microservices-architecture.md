# ADR-001: Microservices Architecture with Database-per-Service

## Trạng thái
**Accepted**

## Bối cảnh (Context)
Dự án Fakebook là một hệ thống mạng xã hội có nhiều domain nghiệp vụ khác nhau (User graph, Post content, Comment threads, Media storage, Feed generation). Mỗi domain có tần suất truy cập, khối lượng dữ liệu và đặc thù xử lý khác nhau:
- Post và Comment có tần suất ghi và đọc cao.
- Feed generation đòi hỏi xử lý phân phối nhanh.
- Media upload phụ thuộc vào latency dịch vụ đám mây (Cloudinary).

Nếu xây dựng theo mô hình Monolith và dùng chung một cơ sở dữ liệu duy nhất, hệ thống sẽ gặp các vấn đề:
- Khó mở rộng độc lập các thành phần nút thắt cổ chai (bottlenecks).
- Phụ thuộc chặt chẽ giữa các bảng (tight coupling qua foreign keys), rủi ro lỗi domino khi schema thay đổi.

## Quyết định (Decision)
1. Chia tách hệ thống thành các **Microservices** độc lập sử dụng Spring Boot và JHipster 9.3:
   - `gateway` (API Gateway)
   - `userService` (Social Graph)
   - `postService` (Content)
   - `commentService` (Comments)
   - `mediaService` (Media Assets)
   - `feedService` (Timeline)
   - `authService` (Identity integration)
2. Áp dụng mô hình **Database-per-Service**: Mỗi service sở hữu một schema MariaDB riêng. Tuyệt đối không tạo foreign key sang database khác; mọi liên kết xuyên service đều sử dụng `UUID`.

## Các giải pháp thay thế (Alternatives Considered)
- **Monolith Architecture**: Dễ triển khai ban đầu nhưng khó cô lập lỗi, khó scale riêng Feed Service và khó thử nghiệm công nghệ mới per-service.
- **Shared Database Microservices**: Các service tách riêng code nhưng dùng chung 1 database schema. Giải pháp này vi phạm ranh giới Bounded Context và gây khóa chết (deadlocks) khi nhiều service cùng truy cập một bảng.

## Hệ quả (Consequences)
- **Tích cực**:
  - Từng microservice có thể build, test, tối ưu pool connection và scale độc lập.
  - Lỗi tại một service (ví dụ: MediaService chậm khi upload Cloudinary) không làm chết các service khác như User hay Post.
- **Thách thức**:
  - Cần cơ chế gọi liên dịch vụ (OpenFeign, Kafka) và quản lý tính nhất quán cuối cùng (Eventual Consistency).
  - Tăng độ phức tạp khi deploy và vận hành.
