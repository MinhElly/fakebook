# Cache và Feed projection

Feed đọc trực tiếp `feed_items` trong MariaDB theo `created_at DESC, post_id DESC`,
phân trang theo user. Kafka create/update/delete và friendship accept/unfriend cập
nhật projection trong transaction. Unique `(user_id, post_id)` làm delivery lặp
idempotent. Index `idx_feed_items_user_created_post` đã tồn tại; không thêm index
trùng. Feed không còn đọc/ghi `feed:user:*` hoặc dùng Hibernate L2/Redisson.

User Service cache profile và các trang friends/follows/pending requests trong
Redis qua Spring Cache. Key danh sách gồm user, page, size và sort; serializer giữ
order, direction, ignoreCase và null handling. Profile Optional có key riêng với
profile DTO. TTL mặc định là 30 phút.

`UserCacheInvalidation` bao phủ service nghiệp vụ và CRUD admin, lấy cả quan hệ
cũ/mới khi cập nhật, rồi xóa mọi page/sort của các user liên quan sau DB commit.
Reject/cancel xóa pending của cả sender và receiver. Khi đổi profile, các cache
chứa bản sao profile trong friends/follows/requests được invalidate theo quan hệ.
Không còn eviction `friendSuggestions` vì phương thức này đọc DB trực tiếp.

Redis get/put lỗi được ghi log và các API cache đọc tiếp tục bằng DB. Eviction lỗi
được báo lỗi để không che giấu cache cũ sau write; DB đã commit ở thời điểm đó.
Quyết định quyền truy cập (`areFriends`, media ownership, visibility) không dùng
cache danh sách dài hạn. Realtime Gateway vẫn giới hạn một instance.
