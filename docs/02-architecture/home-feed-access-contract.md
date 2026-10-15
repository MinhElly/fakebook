# Phân quyền, Post/Profile và Home/Friends Feed

## Hành vi đã triển khai

- Guest đọc Post PUBLIC, reaction summary không chứa danh tính, comment theo quyền Post hiện tại và profile công khai. Mutation, lưu bài và danh sách người reaction yêu cầu JWT.
- Gateway và service giữ kiểm quyền riêng. JWT bị decoder từ chối trả 401. Token INTERNAL gọi API nội bộ, không dùng cho business API cần đăng nhập.
- ADMIN bỏ qua sở hữu khi sửa/xóa Post và Comment; giữ validation media, ID, trạng thái và thời gian đăng.
- PUT/PATCH Post giữ media/tag khi trường bị bỏ qua, xóa khi nhận danh sách rỗng. Validate media trước khi sửa dữ liệu. Response chứa media/tag/version; `createdAt` không đổi.
- Revision lưu baseline và thay đổi thực sự trong transaction, dùng version để phát hiện conflict. Lịch sử kiểm quyền theo Post hiện tại; không tải ảnh đã gỡ từ revision.
- Profile có keyset `(createdAt, id)` riêng, lấy bài do user viết hoặc được tag, dedup và kiểm quyền. Tag mới chỉ chấp nhận bạn bè của tác giả.
- Home dùng 5 FRIENDS (gồm bài của mình), 2 FOLLOWING-only PUBLIC, 4 DISCOVERY PUBLIC. Thiếu nguồn thì bù luân phiên; shuffle theo seed phiên. Friends chỉ gồm mình/bạn bè theo thời gian đăng.
- Feed đối chiếu fan-out với Post và quan hệ hiện tại, bổ sung ứng viên từ API Post, sửa thời gian projection theo `Post.createdAt`. UPDATE không dùng thời gian chỉnh sửa để đẩy bài lên đầu.
- Session Feed lưu MariaDB, TTL 30 phút, signed cursor gắn viewer/mode, khóa session để xử lý đồng thời, lưu trang để replay và unique session/Post chống trùng. Replay kiểm quyền lại và bù bài bị thu hồi.
- Guest Home dùng cookie HttpOnly của backend, tối đa 20 ID khác nhau trong phiên trình duyệt; refresh không xóa quota. Endpoint đọc công khai khác không dùng quota này.
- Frontend chỉ mở phiên Feed khi vào Home, lấy đúng danh sách server trả về; mở trực tiếp profile không gọi Home hoặc tiêu quota Home. Profile không lọc từ Home. Edit thay cùng ID tại vị trí hiện có. Ảnh authenticated dùng Bearer/blob và revoke blob khi thay/unmount. Conflict giữ nội dung đang nhập.

Frontend hiển thị một bảng tin Home chung, không tách tab Bạn bè/Dành cho bạn; route cũ `/feed/friends` chuyển về `/`. Phân loại nguồn vẫn ở backend. Hàng thống kê hiển thị icon và số rút gọn bên trái, các loại reaction thực tế bên phải.

## API

| API | Quyền và phân trang |
| --- | --- |
| `GET /api/feed/home?cursor=...` | Guest hoặc JWT; `items,nextCursor,hasMore,loginRequired` |
| `GET /api/feed/friends?cursor=...` | JWT người dùng; cùng response Home |
| `GET /api/feed/me` | Giữ API cũ trong thời gian chuyển đổi |
| `GET /api/posts/profile/{userId}?cursor=...&size=11` | Lọc theo quyền người đọc, keyset riêng |
| `GET /api/posts/{id}/history?before=...&size=20` | Quyền của Post hiện tại; revision giảm dần |
| `GET /api/posts/media/{id}/access` | Trả boolean theo parent Post hiện tại |
| `/api/internal/feed-posts/*` | INTERNAL: candidates/readable phục vụ Feed |
| `/api/internal/social-context/{userId}` | INTERNAL: friend/following IDs |
| `/api/internal/social-context/{userId}/friend-ids`, `/follower-ids` | INTERNAL: audience cho fan-out |

## Cấu hình trước khi chạy source mới

- Feed cần `FEED_CURSOR_SECRET` ngẫu nhiên ít nhất 32 ký tự, ổn định giữa các replica. Không đưa secret thật vào Git.
- Chạy ngoài profile k8s cần `FAKEBOOK_OIDC_INTERNAL_CLIENT_ID` và `FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET` của client có ROLE_INTERNAL. Cấu hình k8s hiện có tiếp tục dùng registration internal của profile đó.
- Media dùng service discovery cho Post; có thể cấu hình `POST_SERVICE_URL` khi không dùng discovery.
- Hai migration additive mới: Post version/revision và các bảng session/page/seen của Feed. Không deploy staging trong đợt này.
- Frontend gửi credentials để nhận cookie quota. Frontend/API cần được phục vụ cùng site; cookie dùng SameSite=Lax.

## Review theo bốn phần

Patch chỉ chứa thay đổi kể từ snapshot trước đợt triển khai này, không trộn diff cũ của workspace:

1. `.artifacts/home-feed-review/01.patch`: phân quyền và các API phụ trợ.
2. `.artifacts/home-feed-review/02.patch`: Post/history/profile/media links.
3. `.artifacts/home-feed-review/03.patch`: Feed API, projection, cursor/session.
4. `.artifacts/home-feed-review/04.patch`: frontend.

Snapshot đầu đợt nằm tại `.artifacts/home-feed-baseline-20261015/source.zip`. Các thay đổi có sẵn về CI, staging, GitOps và cache được giữ lại và tách commit khi chuẩn bị PR theo yêu cầu. Không deploy staging; manifest staging vẫn là template, chưa có cluster/domain được xác nhận.

## Kiểm chứng và giới hạn

Kết quả lệnh và browser được ghi trong `.artifacts/`, bảng tổng hợp tại `.artifacts/home-feed-review/checks.json`.

| Service | Unit tests | Integration tests | Kết quả |
| --- | ---: | ---: | --- |
| Post | 73 | 85 | PASS |
| Feed | 58 | 71 | PASS |
| User | 85 | 135 | PASS |
| Media | 42 | 93 | PASS |
| Comment | 69 | 61 | PASS tests; Modernizer tách riêng |
| Gateway | 38 | 44 | PASS |

Tổng 854 test, không failure/error/skipped. Đã chạy `clean verify` cho sáu service; sau đó chạy lại Post/Gateway khi chốt route Saved/INTERNAL và bổ sung ma trận quyền. Tất cả lượt Maven dùng `-Dspotless.skip=true`; Comment thêm `-Dmodernizer.skip=true` như ghi dưới đây. Source nền: `c0c3619cd962b8ba90c41757c20fe4c6ae381d39`, đây là mốc source trước các commit triển khai của PR.

- Backend: Maven tests/verify trên Testcontainers hoặc H2 theo cấu hình test từng service. Các ca mới kiểm tra quyền, thiếu/sai JWT, INTERNAL, revision/no-op/conflict, keyset/tag profile, nguồn ứng viên, 5/2/4, quota/cookie, dedup, replay/retry và request đồng thời.
- Frontend: TypeScript và production build; browser regression dùng source React thật với API/OIDC fixture cô lập, kiểm guest, sửa ảnh qua Bearer/blob, vị trí bài, history, conflict và profile được tag.
- Browser fixture không chứng minh luồng xuyên Gateway → các service đang chạy với Keycloak/Cloudinary thật. Nghiệm thu E2E toàn hệ thống vẫn NOT VERIFIED; chưa coi toàn bộ tiêu chí nghiệm thu runtime đã hoàn tất.
- Comment có 6 lỗi Modernizer tồn tại trước đợt này trong `PostEventConsumerTest`: lời gọi String.format. Lượt chạy kiểm thử hiện tại tách chúng bằng `-Dmodernizer.skip=true`; không sửa guard hoặc file test ngoài phạm vi để làm xanh.
- Những URL Cloudinary công khai đã tồn tại chưa được thu hồi. API file kiểm quyền và proxy bytes không tự đóng quyền truy cập trực tiếp tại provider; cần migration storage riêng.
- Không triển khai block/hide, sở thích, điểm tương tác, notification, Messenger hoặc Stories.
