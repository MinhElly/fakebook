# Phân quyền, Post/Profile và Home Feed

## Hợp đồng hiện tại

- Bắt buộc đăng nhập để dùng ứng dụng. Gateway và từng service từ chối đọc/ghi tài nguyên nghiệp vụ khi thiếu hoặc sai JWT. Health, preflight và các endpoint bootstrap xác thực tiếp tục phục vụ theo chức năng vận hành.
- PUBLIC nghĩa là người dùng đã đăng nhập có thể đọc, không còn là API dành cho guest. FRIENDS/PRIVATE tiếp tục kiểm quyền theo quan hệ/sở hữu. INTERNAL chỉ phục vụ API nội bộ.
- Bỏ quota 20 bài, cookie guest, mã tab và toàn bộ thao tác ghi bảng guest. Những bảng guest từ migration cũ được giữ tương thích với migration đã chạy, không còn được sử dụng; chỉ xóa vật lý sau khi không còn phiên bản ứng dụng cũ chạy.
- ADMIN sửa bài người khác có thể thêm upload của mình hoặc media của tác giả. Media đang liên kết hợp lệ có thể được giữ lại; mọi media vẫn phải ACTIVE và purpose POST.
- PUT/PATCH Post bắt buộc version: thiếu trả 400, version cũ trả 409. Giữ JPA optimistic locking để chống race giữa transaction. Version đúng và no-op không tạo revision.
- Edit giữ createdAt, chỉ thay updatedAt, trả Post/media/tag đầy đủ. Media/tag bị bỏ qua thì giữ nguyên; [] thì xóa. History theo quyền Post hiện tại và không lưu file media cũ.
- Profile có keyset riêng theo (createdAt, id), lấy bài tác giả hoặc được tag và lọc quyền. Tag mới chỉ nhận bạn bè của tác giả.
- Home trộn 5 FRIENDS (gồm bài của mình), 2 FOLLOWING-only PUBLIC, 4 DISCOVERY PUBLIC; thiếu nguồn thì bù. Friends API chỉ gồm mình/bạn bè. Frontend hiển thị một Home chung.
- Feed session TTL 30 phút, signed cursor gắn viewer/mode; dedup, replay, revalidate quyền từng trang. Projection được hydrate một lần/request và chỉ sửa những dòng thay đổi.
- Frontend nhận 410 thì cho mở snapshot mới; timeout/503 retry cùng cursor. Response cũ không được ghi đè generation mới. Ảnh bài, avatar và cover đều dùng Bearer/blob, có revoke blob.

## Cấu hình và migration

- Feed bắt buộc FEED_CURSOR_SECRET ngẫu nhiên, ít nhất 32 ký tự, cùng giá trị giữa replica và ổn định qua restart. Thiếu, ngắn hoặc dùng placeholder REPLACE_ME thì từ chối khởi động.
- sync-local-secrets.ps1 truyền key từ infrastructure/.env vào feedservice-secrets. Compose staging khai báo biến bắt buộc. File .env.example chỉ có placeholder, không dùng để chạy ứng dụng thật.
- Post version/revision và các bảng Feed session/page/seen là migration additive của PR. Không sửa checksum migration đã có.
- Không deploy staging. Chưa có cluster/domain staging được xác nhận.

## Kiểm chứng

Các log kiểm chứng hiện tại nằm trong .artifacts/login-required-*.log và .artifacts/review-fix-*.log; đây là artifact local, không commit vào Git.

- Comment Modernizer: thay đủ sáu String.format bằng formatted; giữ nguyên guard và assertion.
- Bổ sung negative tests thiếu JWT tại Gateway và từng service, thiếu version, ADMIN upload/author giữ ảnh, signing key không hợp lệ và projection reconcile.
- Browser dùng API/OIDC fixture để kiểm login redirect, editor/media/history/profile, 410 mở phiên mới và 503 giữ cursor. Fixture không chứng minh E2E xuyên Gateway/Keycloak/Cloudinary thật.
- CI của commit cũ 5f134b9 không đại diện cho các sửa đổi working tree sau đó. Chỉ báo PASS khi lệnh kiểm tương ứng đã hoàn tất.

## Giới hạn

- Các URL Cloudinary công khai đã tồn tại chưa được thu hồi. Yêu cầu JWT ở file API không đóng truy cập trực tiếp tại provider; cần migration storage riêng.
- Không triển khai block/hide, sở thích, điểm tương tác, notification inbox, Messenger hoặc Stories.
