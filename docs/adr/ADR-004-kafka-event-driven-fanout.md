# ADR-004: Apache Kafka for Event-Driven Communication and Feed Fan-out

## Trạng thái
**Accepted**

## Bối cảnh (Context)
Khi người dùng đăng bài viết, hệ thống cần thực hiện nhiều tác vụ liên quan:
- Phân phối bài viết vào bảng tin (Timeline) của hàng trăm, hàng ngàn bạn bè và followers.
- Cập nhật thông tin bài viết vào bộ nhớ đệm kiểm tra bình luận của Comment Service (`post_cache`).
- Khi bài viết bị xóa, cần dọn dẹp các tài nguyên hình ảnh trên Cloudinary.

Nếu thực hiện tất cả các thao tác trên một cách **đồng bộ (Synchronous HTTP)** trong cùng một request `POST /api/posts`:
- Thời gian phản hồi cho tác giả sẽ cực kỳ chậm (vài giây đến vài chục giây).
- Nếu Feed Service hoặc Cloudinary gặp lỗi mạng, toàn bộ transaction tạo bài viết sẽ bị rollback vô lý.

## Quyết định (Decision)
1. Sử dụng **Apache Kafka Native (KRaft mode)** làm Message Broker bất đồng bộ.
2. Thiết kế theo mô hình **Fan-out on Write**:
   - `postService` chỉ ghi bài vào MariaDB của mình rồi bắn sự kiện `PostCreatedEvent` lên topic `post-events` và phản hồi HTTP 201 cho tác giả ngay lập tức (< 100ms).
   - `feedService` và `commentService` đóng vai trò là các Consumer độc lập lắng nghe topic `post-events`.
3. Kích hoạt **Dead Letter Queue (DLQ)** (`post-events-feed-dlt`, `post-events-dlt`) và cơ chế thử lại tự động (Retry Back-off) để xử lý an toàn khi consumer gặp sự cố.

## Các giải pháp thay thế (Alternatives Considered)
- **Fan-out on Read (Pull Model)**: Không nhân bản feed bản ghi trước; khi người dùng mở app, hệ thống mới query toàn bộ bài viết của tất cả bạn bè rồi sắp xếp. Cách này tiết kiệm dung lượng ghi nhưng làm nghẽn database nghiêm trọng khi người dùng lướt feed (độ trễ đọc cao).
- **RabbitMQ**: Thích hợp cho message queuing đơn giản nhưng không hỗ trợ cơ chế streaming log replay và partition scaling tốt bằng Kafka khi dữ liệu mạng xã hội tăng trưởng lớn.

## Hệ quả (Consequences)
- **Tích cực**:
  - Tối ưu hóa tối đa thời gian phản hồi cho người dùng tạo bài viết.
  - Phân tách hoàn toàn sự phụ thuộc (Decoupling) giữa Post Service và các dịch vụ downstream.
- **Thách thức**:
  - Dữ liệu hiển thị mang tính chất **Nhất quán cuối cùng (Eventual Consistency)**: Có thể mất vài trăm mili-giây để bài viết xuất hiện trên feed của bạn bè.
