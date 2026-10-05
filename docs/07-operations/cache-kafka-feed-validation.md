# Cache, Kafka và Feed — kiểm chứng ngày 2026-10-05

## Source và gate local

Đã tách các thay đổi hành vi User cache, Kafka cleanup, Feed projection, cấu hình
và refactor package thành các commit riêng. HTTP/event field contracts được giữ.

| Service | Unit tests | Integration tests | Kết quả |
| --- | ---: | ---: | --- |
| User | 83 | 134 | PASS |
| Post | 68 | 52 | PASS |
| Comment | 58 | 60 | PASS |
| Feed | 53 | 56 | PASS |
| Gateway | 36 | 42 | PASS |
| Media | 40 | 90 | PASS |

Các gate dùng `mvnw.cmd -ntp clean verify` tại từng service, Checkstyle không có
vi phạm, tests không failure/error/skip. User dùng thêm
`-Dmaven.clean.failOnError=false` vì một resource trong `target` đang bị ứng dụng
local giữ; source vẫn được compile lại và toàn bộ tests chạy. Logs local là
`final-<service>-verify.log` tại repository root, không đưa vào Git.

## Hành vi đã kiểm chứng

- `UserCacheRedisIT` dùng Redis thật: reject/cancel xóa pending của cả hai người;
  accept/unfriend cập nhật friends và trạng thái DB; sửa profile làm mới bản sao
  trong friends/follows. Cache của user không liên quan vẫn tồn tại. Cache hit giữ
  đúng sort. Dừng Redis trong test vẫn đọc được profile, follow và pending từ DB.
- `UserCacheInvalidationTest` kiểm tra chưa eviction trước commit. Invalidation
  chọn user cũ/mới qua các đường CRUD service; mọi trang/sort dùng cùng prefix user.
  Quyết định friendship/quyền truy cập đọc DB, không dùng cache danh sách làm quyền.
- `MediaCleanupKafkaIT` dùng Kafka và MariaDB thật: storage adapter được giả lập
  lỗi IOException, consumer retry ba lần rồi chuyển DLT; phục hồi adapter và replay
  cập nhật DELETED. Replay lần nữa không xóa sai. Đây chưa phải test outage Cloudinary.
- `FriendshipRuntimeProof` khởi động User và Feed contexts với Kafka, Redis,
  MariaDB riêng; User DB là H2. Accept tạo đúng một outbox event và hai dòng FRIENDS;
  unfriend tạo đúng một event, xóa hai dòng FRIENDS và giữ dòng PUBLIC. Post metadata
  được giả lập. Không đi qua HTTP/Gateway và không dùng dữ liệu runtime hiện có.
- `FeedPersistenceIT` kiểm tra projection create/update/delete, visibility và
  idempotency. `UserFeedDatabaseIT` kiểm tra thứ tự, phân trang, index và EXPLAIN:
  access path dùng `idx_feed_items_user_created_post`, type `ref`. Fixture nhỏ này
  xác nhận wiring/index, chưa chứng minh hiệu năng production.

## Tái chạy friendship proof

Sau gate User và Feed, cần JDK và Docker Desktop:

```powershell
./scripts/validation/friendship-runtime-proof.ps1
```

Runner dùng container mới và tự đóng chúng; chỉ ghi compiler artifacts vào thư mục
temp. Output thành công có `PROOF PASS`. Không chạy runner đồng thời với clean build
User/Feed vì runner dùng classes trong `target` của hai service.

## Giới hạn còn lại

Redisson Post/Comment staging đã có wiring endpoint trong source, chưa kiểm chứng
kết nối staging. Chưa deploy, push hoặc thay đổi dữ liệu/hạ tầng đang chạy.
Realtime vẫn giới hạn một Gateway instance; backfill PUBLIC khi follow/unfollow và
thông báo bài mới không thuộc thay đổi này.

Cache get/put có fallback DB. Eviction thất bại sau commit được báo lỗi: giao dịch
DB đã hoàn tất, không tự rollback và chưa có retry eviction bền vững. Khi Redis
phục hồi, cache cũ có thể tồn tại tới TTL nếu eviction thất bại; không gọi đây là
đảm bảo write consistency xuyên Redis outage. Các record event cũ của User được giữ
dưới dạng deprecated compatibility classes để đọc outbox lịch sử, không tạo event mới.
