# Signed Direct Upload cho ảnh bài viết — mục 1

Lượt nâng cấp tiếp theo cho 30 ảnh/video và playback đã được ghi riêng tại [post-multimedia.md](post-multimedia.md). Những giới hạn mục 1 bên dưới là lịch sử của giai đoạn trước.

## Luồng mới

1. React gọi `POST /services/mediaservice/api/media/uploads/sign` với `fileName`, `mimeType`, `size`.
2. Media Service tạo phiên trong `media_upload_session`, gắn owner từ JWT, public ID ngẫu nhiên và thời hạn một giờ.
3. React gửi file trực tiếp tới URL Cloudinary được cấp, bằng Axios riêng không mang JWT Fakebook. Các tham số có chữ ký gồm public ID, timestamp, overwrite=false, authenticated và danh sách định dạng.
4. React gọi `POST /services/mediaservice/api/media/uploads/complete` với `sessionId`.
5. Backend khóa phiên, kiểm tra owner/thời hạn, hỏi Cloudinary Admin API và lưu metadata từ kết quả thực. Retry complete trả cùng media, không tạo bản ghi thứ hai.
6. Post Service tiếp tục nhận `mediaIds` như trước. Ảnh authenticated được đọc qua API file hiện có; chỉ backend tạo URL đọc có chữ ký sau bước kiểm tra quyền.

Không cần unsigned preset. Dùng ba cấu hình Cloudinary hiện có: cloud-name, api-key, api-secret. API secret chỉ ở backend. Gói Cloudinary phải cho phép authenticated image upload và Admin API resource lookup; đây là điều kiện cần kiểm tra trên tài khoản thực.

## Phạm vi

- Mục này hỗ trợ JPEG, PNG, WebP, GIF, AVIF cho ảnh bài viết; chưa nâng cấp số lượng attachment hoặc video.
- Không đặt trần dung lượng nghiệp vụ ở endpoint ký; giới hạn kỹ thuật Cloudinary vẫn áp dụng. Chưa có chunking trong bước này.
- Avatar/cover và media cũ giữ luồng hiện tại. Không tự fallback về upload qua backend khi direct upload lỗi.
- Phiên chưa complete được dọn mỗi giờ, tối đa 50 phiên/lượt, sau ít nhất 24 giờ kể từ khi hết hạn; lỗi storage được giữ để retry. Phiên complete được giữ cho idempotency.
- Media đã complete nhưng chưa gắn bài chưa có cleanup mới trong bước này; cần bổ sung đối soát liên kết khi triển khai vòng đời attachment ở mục tiếp theo.
- Đường download vẫn dùng proxy byte[] hiện có; streaming/Range và tối ưu biến thể ảnh thuộc mục sau. Không tuyên bố đã tối ưu download hoặc video.

## Kiểm tra local trước khi phát hành

1. Chạy Liquibase qua khởi động Media Service mới; triển khai backend trước frontend.
2. Tạo bài có một ảnh, xem Network: bytes đi tới `api.cloudinary.com`; `/sign` và `/complete` chỉ trao đổi JSON. Request Cloudinary không có Authorization Bearer của Fakebook.
3. Xem lại bài, sửa thay ảnh, thử PRIVATE/FRIENDS bằng tài khoản khác; unsigned authenticated URL không được trả ảnh trực tiếp.
4. Retry complete cùng phiên phải trả cùng ID; owner khác bị từ chối. Cloudinary không khả dụng không được tạo metadata hoặc bài thiếu ảnh.
5. Chưa commit/push/deploy trong mục này. Unit tests dùng provider giả lập, không thay thế kiểm tra upload và quyền truy cập trên Cloudinary thật.

## Kết quả kiểm tra source ngày 21/10/2026

- Media Service: compile thành công; 55 unit test qua, gồm 8 test DirectUploadService. Architecture test cũng qua.
- Frontend: `npx tsc --noEmit` và `npm run build` qua; build còn cảnh báo bundle lớn và dynamic import của API đã có trong ứng dụng.
- Đã thử `clean verify`; dừng ở integration vì Testcontainers không tìm được Docker. `docker info` xác nhận context `desktop-linux` không có pipe `dockerDesktopLinuxEngine`. Log: `mediaService/direct-upload-verify.log` (artifact local, không commit).
- Unit test cần preload Byte Buddy agent trong môi trường kiểm tra này để tránh lỗi attach named pipe; không thay đổi cấu hình runtime ứng dụng.
- Migration trên database thực, upload Cloudinary thật và kiểm tra quyền xem end-to-end: **NOT VERIFIED**. Cần hoàn tất gate này trước khi phát hành frontend mới.

## Deploy local sau build ngày 21/10/2026

- Đã build và nạp `mediaservice:direct-upload-20261021` vào Kind `fakebook-local`, triển khai riêng Media Service. Pod Ready, health UP.
- Database `media_service`: DATABASECHANGELOG có changeset `20261021000000` và bảng `media_upload_session` tồn tại (đều count=1).
- POST qua Vite `/services/mediaservice/api/media/uploads/sign` không có token trả 401; chưa xác minh sign/complete và upload thật bằng phiên người dùng.
- Argo app `fakebook-local` đã tự khôi phục image cũ trong lần rollout đầu. Đã tạm đặt `spec.syncPolicy.automated.enabled=false` riêng app này để giữ bản deploy local; đây là trạng thái chủ ý sau deploy. Cần cập nhật desired image qua GitOps hoặc Application override trước khi bật lại auto-sync, nếu không image local sẽ bị thay về bản Git.
- Credential aliases của Post/Media/Comment vẫn có trong deployment. Chưa push image lên registry hoặc commit/push source/manifests.
