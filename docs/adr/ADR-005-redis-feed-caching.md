# ADR-005: Redis Sorted Sets for News Feed Timeline Caching

## Trạng thái
**Accepted**

## Bối cảnh (Context)
Bảng tin mạng xã hội có tần suất đọc vượt trội so với tần suất ghi (Read-to-Write ratio có thể lên đến 100:1).
Mặc dù Feed Service đã lưu các bản ghi `feed_items` vào MariaDB theo mô hình Fan-out, việc hàng ngàn người dùng liên tục query phân trang trên bảng `feed_items` (kèm `ORDER BY created_at DESC LIMIT 20 OFFSET X`) sẽ tạo gánh nặng I/O rất lớn lên ổ đĩa của MariaDB.

Ban đầu dự án từng thử nghiệm MongoDB cho read-model, nhưng việc vận hành thêm một cơ sở dữ liệu NoSQL phân tán làm tăng đáng kể chi phí RAM và độ phức tạp backup.

## Quyết định (Decision)
1. Loại bỏ MongoDB feed read model, chuẩn hóa dữ liệu lưu trữ quan hệ dài hạn trong **MariaDB `feed_service`**.
2. Sử dụng **Redis In-Memory** làm tầng tăng tốc truy vấn trực tiếp cho Bảng tin:
   - Áp dụng cấu trúc **Redis Sorted Set (ZSet)** với key `feed:user:{userId}`.
   - Member: `postId` (UUID).
   - Score: Thời điểm tạo bài viết theo Epoch Millisecond (`createdAt.toEpochMilli()`).
3. Áp dụng kỹ thuật **Sliding Window Capping**: Mỗi khi thêm bài mới, chỉ giữ lại tối đa **500 bài viết mới nhất** (`redisTemplate.opsForZSet().removeRange(key, 0, -501)`), các bài cũ hơn sẽ được truy vấn trực tiếp từ MariaDB nếu người dùng cuộn sâu.
4. Ghi Redis an toàn thông qua Spring Transaction Synchronization `afterCommit`.

## Các giải pháp thay thế (Alternatives Considered)
- **Chỉ dùng MariaDB với Composite Index `(user_id, created_at DESC)`**: Đơn giản nhưng độ trễ đọc vẫn ở mức vài chục đến hàng trăm mili-giây, không đáp ứng được SLA lướt feed mượt mà khi lượng dữ liệu lớn.
- **MongoDB Read Model**: Phức tạp trong việc bảo trì replica set và tiêu tốn nhiều RAM của hệ thống.

## Hệ quả (Consequences)
- **Tích cực**:
  - Độ trễ đọc Bảng tin giảm xuống mức dưới 5 mili-giây (In-Memory $O(\log(N) + M)$).
  - Tiết kiệm RAM cho máy chủ nhờ cơ chế giới hạn 500 bài viết mới nhất per user.
- **Thách thức**:
  - Cần cơ chế fallback sang MariaDB khi cache miss hoặc khi Redis khởi động lại.
