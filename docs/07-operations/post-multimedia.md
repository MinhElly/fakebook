# Bài viết tối đa 30 ảnh/video — triển khai local 21/10/2026

## Hành vi đã triển khai

- Tạo và sửa bài nhận tối đa 30 media ID khác nhau, giữ đúng thứ tự. Từ chối ID trùng, null, sai owner/purpose/status và tệp chưa xác minh.
- Form chọn nhiều lần hoặc kéo thả, xóa/đổi thứ tự bằng kéo thả hoặc nút bàn phím, đếm n/30. Upload tối đa ba tệp đồng thời, tiến độ từng tệp, hủy và thử lại. Chọn tệp chỉ tạo preview cục bộ, không gọi sign/Cloudinary. Chỉ bấm Đăng/Lưu mới upload rồi lưu bài; lỗi lưu giữ nguyên nội dung và media đã upload. Đóng trước khi bấm Đăng không tạo asset. Nếu đóng giữa lúc đang đăng, cleanup vẫn cần thiết vì upload và tạo bài không phải một transaction.
- JPG/JPEG, PNG, WebP, GIF, AVIF; video MP4, WebM và MOV. Không đặt trần dung lượng nghiệp vụ mỗi tệp; giới hạn Cloudinary và thời hạn phiên một giờ vẫn áp dụng.
- Signed direct upload tới Cloudinary, chunk 6 MiB, retry với backoff. Resume chỉ trong cùng trang và cùng File; reload trang không khôi phục upload. JWT Fakebook không gửi tới Cloudinary.
- Video yêu cầu eager async MP4 H.264/AAC và poster JPEG. Backend đọc metadata/derived từ Cloudinary Admin API; complete trả 409 MEDIA_PROCESSING khi chưa có bản phát. Frontend poll có giới hạn rồi cho thử lại bằng phiên cũ, không upload lại tệp đã xong.
- Feed/profile hiển thị tối đa năm ô và +N; viewer duyệt đủ danh sách. Video có controls và byte-range seek. Video đơn trên Feed tự phát tắt tiếng khi ít nhất 60% khung hiển thị, dừng khi ra khỏi màn hình hoặc tab bị ẩn. Trình xem nền đen hỗ trợ mở/ẩn cột bình luận; video giữ đúng tỷ lệ trong khung.
- Metadata và kiểm tra quyền theo lô, tối đa 50 ID/lần. Chỉ trả metadata có quyền đọc; không dùng provider URL trực tiếp ở frontend.
- File proxy stream InputStreamResource, không gom toàn bộ file vào byte[]. Giữ 206/Content-Range/Accept-Ranges và 416; chặn storage host khác Cloudinary và redirect upstream.
- Cleanup kiểm tra lại Post reference trước khi xóa asset. Phiên bỏ dở và media đã complete nhưng không gắn bài được dọn theo batch sau ít nhất 24 giờ từ khi phiên hết hạn. Asset còn liên kết được giữ.

## Hợp đồng bổ sung

- `POST /api/media/uploads/sign`: fileName, mimeType, size; trả sessionId, uploadUrl, apiKey, parameters, signature, expiresAt. Giữ contract của bước Direct Upload trước.
- `POST /api/media/uploads/complete`: sessionId; trả MediaDTO khi đã xác minh và sẵn sàng.
- `POST /api/media/{id}/playback`: JWT, body ticket tùy chọn để gia hạn; trả ticket và path file trong ứng dụng. Ticket ngẫu nhiên chỉ dành cho một media, tối đa năm phút và không quá hạn JWT; frontend gia hạn mỗi 30 giây.
- `GET /api/media/{id}/file?ticket=...`: native video dùng lease; filter chuyển lease thành bearer cho bộ kiểm tra JWT và account-state hiện có. Mỗi Range vẫn kiểm tra quyền Post và trạng thái media. Không đưa JWT vào URL. Ticket hiện lưu trong memory của một replica; restart cần lấy ticket mới. Chưa dùng Redis cho nhiều replica.
- `GET /api/media/{id}/file?poster=true`: ảnh poster video, cùng kiểm tra quyền.
- `POST /api/posts/media/access-batch`: danh sách tối đa 50 UUID; trả map attached/readable theo quyền người gọi.
- `GET /api/internal/media/{id}/attached`: kiểm tra liên kết dành riêng cho ROLE_INTERNAL để cleanup.
- Migration `20261021000001`: thêm resource_type vào media_upload_session, mặc định image cho phiên cũ.

## Kiểm chứng

- Unit suite ban đầu: Media 60, Post 111, Gateway 57 test qua. Sau thay đổi batch/poster/cleanup cuối cùng: Media targeted verify qua 17 unit + 1 integration PlaybackResourceIT; Post targeted verify qua 7 unit + 39 integration PostTimelineIT. Gateway matcher mới nằm trong suite 57 test đã qua.
- Playback integration dùng database Testcontainers và provider giả lập: thiếu/sai ticket bị 401, ticket hợp lệ nhận 206, thu hồi quyền parent thì request Range tiếp bị 403.
- Post integration: 30 tệp đúng thứ tự, từ chối 31 và giữ dữ liệu cũ, batch permission, lịch sử/chỉnh sửa và optimistic version. Lượt thử Post ban đầu lỗi khởi tạo pool Redis; lượt đạt dùng pool test 4/minimum idle 1 và JVM ActiveProcessorCount=2, không thay runtime app.
- Frontend TypeScript/build qua. Uploader thực chạy với HTTP mock: range/chunk ID, retry chunk, không lộ bearer, resume sau abort và không complete trùng.
- Chrome fixture: giới hạn 30/31, concurrency 3, reorder, lỗi từng tệp/retry, hủy/retry, giữ/xóa/thêm media khi sửa, native drop, full form gửi đủ ba media ID và bố cục 390px. Có lượt fixture timeout dưới tải; chạy lại và full composer riêng đã qua. Screenshot local: work/post-composer-mobile.png.
- Đây không phải E2E upload video thật lên tài khoản Cloudinary. Chưa xác minh provider quota/transcode trên tệp thật hoặc phát video bằng phiên người dùng thật.

## Runtime local

- Kind/context: fakebook-local / kind-fakebook-local; namespace fakebook-local.
- Post: postservice:post-media-20261021-v2; Gateway: gateway:post-media-20261021-v2; Media: mediaservice:video-metadata-20261021-v3.
- Bản Media v3 bổ sung `media_metadata=true` khi đọc Cloudinary Admin API. Đối chiếu asset video thật: thiếu tham số không có duration; có tham số trả 22.766667 giây. DirectUploadServiceTest: 10 test qua; pod v3 Ready 1/1. Chưa xác minh lại thao tác complete bằng phiên người dùng sau rollout.
- Đã nạp image vào Kind, rollout từng deployment; cả ba ready=1 và health UP. DATABASECHANGELOG video migration và cột resource_type đều xác nhận count=1.
- Ticket giả qua http://localhost:5173 nhận 401 từ Media qua Gateway, không bỏ qua authentication ở service.
- Frontend dùng source Vite cổng 5173. Bản frontend container trong cluster không thay trong lượt này.
- Argo app fakebook-local vẫn automated.enabled=false theo đợt deploy local trước, tránh Git ghi đè image local. Cần đưa image/manifest tương ứng vào desired GitOps trước khi bật lại auto-sync. Chưa push registry, commit hoặc push Git.
- Build Gateway lần đầu lỗi credential Docker Hub. Build thành công bằng Jib offline với base image cache; không sửa Docker credential.

- Kiểm chứng frontend video: Chrome fixture dùng clip WebM cục bộ, playback API giả lập; autoplay muted, pause/resume theo cuộn, khung 16:9, toggle sidebar/Escape và không tràn ngang 390px đã qua. Không thay thế kiểm chứng Cloudinary bằng JWT người dùng thật. Chạy `PLAYWRIGHT_MODULE=<bundled playwright> node frontend/tests/video-player.browser.cjs`.

## Publication có bù trừ

Luồng frontend mới dùng phiên đăng bài; xem [post-publication.md](post-publication.md) để biết transaction, idempotency, expiry và retry cleanup. Image Post/Media mới là publication-20261021-v4; các image ở phần lịch sử bên trên đã được thay thế.
