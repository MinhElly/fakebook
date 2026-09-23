# Caching Strategy (Chiến lược lưu bộ đệm)

Tài liệu này giải thích chi tiết chiến lược bộ đệm **Redis** trong hệ thống Fakebook, bao gồm cơ chế lưu trữ Timeline bằng Sorted Sets trong Feed Service và cơ chế Spring Cache trong User Service.

---

## 1. Tổng quan phân bổ Cache

| Service | Công nghệ Cache | Đối tượng Cache | Cấu trúc dữ liệu Redis | Mục đích & TTL |
| :--- | :--- | :--- | :--- | :--- |
| **Feed Service** | Spring Data Redis (`StringRedisTemplate`) | Bảng tin người dùng (Timeline) | **Redis Sorted Set (ZSet)**<br/>Key: `feed:user:{userId}`<br/>Score: `createdAt` (epoch ms)<br/>Value: `postId` | Giảm tải database truy vấn bảng tin xuống mili-giây. Cắt tỉa giữ tối đa 500 post mới nhất. |
| **User Service** | Spring Cache (`@Cacheable`, `@CacheEvict`) | Hồ sơ người dùng, quan hệ bạn bè | **Redis String / Hash** (Value serialization) | Giảm thiểu truy vấn MariaDB cho các profile được xem thường xuyên. |

---

## 2. Feed Timeline Caching (Redis Sorted Set)

Bảng tin mạng xã hội có đặc tính đọc rất cao (Read-Heavy). Để phục vụ feed tức thì mà không cần `JOIN` phức tạp qua hàng triệu bản ghi quan hệ, Feed Service sử dụng cấu trúc **Sorted Set**:

### 2.1 Cấu trúc Key & Value
- **Key**: `feed:user:<user-uuid>`
- **Member**: `<post-uuid>`
- **Score**: Thời điểm tạo bài viết tính theo Epoch Millisecond (`createdAt.toEpochMilli()`).

### 2.2 Quy trình ghi an toàn (Transaction Synchronization Pattern)
Để đảm bảo dữ liệu trong Redis luôn nhất quán với cơ sở dữ liệu quan hệ, Feed Service không ghi Redis ngay trong transaction DB, mà sử dụng `TransactionSynchronizationManager.registerSynchronization` với callback `afterCommit`:

```java
private void updateRedisAfterCommit(Set<UUID> recipientIds, UUID postId, Instant createdAt) {
    Runnable update = () -> updateRedis(recipientIds, postId, createdAt);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    update.run();
                }
            }
        );
    } else {
        update.run();
    }
}
```

### 2.3 Cơ chế giới hạn bộ nhớ (Sliding Window 500 items)
Để ngăn chặn Redis phình to vô hạn theo thời gian, mỗi khi nạp thêm bài viết mới, hệ thống tự động cắt tỉa các phần tử cũ nhất ngoài top 500:

```java
private void updateRedis(Set<UUID> recipientIds, UUID postId, Instant createdAt) {
    double score = createdAt.toEpochMilli();
    for (UUID recipientId : recipientIds) {
        String redisKey = feedKey(recipientId);
        // Thêm bài viết với score là thời gian tạo
        redisTemplate.opsForZSet().add(redisKey, postId.toString(), score);
        // Cắt bỏ tất cả các bài viết đứng sau vị trí 500
        redisTemplate.opsForZSet().removeRange(redisKey, 0, -501);
    }
}
```

### 2.4 Quy trình đọc phân trang Feed
Khi người dùng cuộn xem bảng tin:
1. Service gọi lệnh `ZREVRANGEBYSCORE` hoặc `ZREVRANGE` để lấy danh sách `postId` mới nhất kèm phân trang.
2. Nếu Redis gặp sự cố hoặc key không tồn tại (Cache Miss), hệ thống tự động fallback truy vấn từ bảng `feed_items` trong MariaDB.

---

## 3. User Service Caching

Trong `userService`, các phương thức truy vấn hồ sơ thường xuyên được đánh dấu bằng `@Cacheable`:
- `getUserProfile(UUID id)`: Được cache theo key `id`.
- Khi người dùng cập nhật thông tin qua `updateUserProfile()`, annotation `@CacheEvict(key = "#id")` sẽ tự động xóa bản ghi cache tương ứng để làm mới dữ liệu.
