# Phiên đăng bài và bù trừ media

## Hành vi

Frontend chỉ tạo preview cục bộ khi chọn tệp. Khi bấm Đăng/Lưu, frontend tạo publication có khóa UUID, lưu bản nháp bất biến tại Post Service, upload từng tệp bằng khóa UUID riêng rồi yêu cầu publish. Tạo và sửa bài đều dùng luồng này.

Post Service giữ trạng thái PENDING, PUBLISHED hoặc CANCELLED trong bảng post_publication. PENDING không phải bài viết và không xuất hiện trong Feed. Khi publish, khóa hàng publication bằng FOR UPDATE; kiểm chứng media, tạo/sửa bài, ghi outbox và lưu kết quả PUBLISHED trong cùng transaction MariaDB. Retry cùng khóa và cùng danh sách media trả lại snapshot kết quả đã commit, không tạo bài hoặc sửa version lần nữa. Khác owner bị 403, khác bản nháp hoặc danh sách sau commit bị 409.

Cancel và publish dùng cùng khóa hàng. Nếu publish đã commit, cancel trả PUBLISHED và không thu hồi bài. Nếu cancel thắng, publish bị 410. Nếu kiểm chứng/lưu bài lỗi, transaction rollback trước, controller yêu cầu cancel trong transaction riêng; nếu database không sẵn sàng, expiry sẽ xử lý khi phục hồi.

Media mới có publication_id; upload public_id được tạo từ owner, publication và fileKey. Retry sign dùng lại upload session/signature. Không được gắn media tạm qua API tạo/sửa bài cũ hoặc qua publication khác. Media đã có trong bài đang sửa được giữ nguyên.

## Đóng tab, hết hạn và cleanup

- Khi đóng form, frontend abort upload và gửi cancel theo best effort. Đóng tab/mất mạng không bảo đảm request cancel tới server.
- Publication PENDING hết hạn sau một giờ. Post worker quét mỗi phút và chuyển CANCELLED; backend không phụ thuộc frontend để quyết định thu hồi.
- Media worker quét mỗi 10 giây, mỗi lượt tối đa 50 upload đến hạn; mỗi upload được kiểm tra lần đầu sau khoảng một phút. Chỉ thu hồi sau khi Post trả CANCELLED hoặc PUBLISHED nhưng asset không được dùng.
- Trước khi xóa media đã complete, kiểm tra liên kết Post. Nếu Post không truy cập được, không xóa. Lỗi Cloudinary hoặc Post được lưu lại qua cleanup_after và retry sau khoảng một phút; restart không mất danh sách công việc.
- Xóa asset không phải một transaction chung với Post. Asset có thể tồn tại tạm thời khi provider lỗi.
- Signature Cloudinary đã cấp không thu hồi tức thời được. Sau lần xóa đầu, kiểm tra/xóa lại sau khi signature hết hạn 5 phút và sau thêm 24 giờ để bắt upload tới muộn; tránh gọi destroy mỗi phút suốt 25 giờ. Chỉ kết thúc công việc khi provider xác nhận ok/not found.
- Phiên cũ không có publication_id vẫn theo cleanup legacy. API tạo bài cũ còn tương thích cho media cũ, nhưng không nhận media tạm của publication mới.

## API

| API | Mục đích |
| --- | --- |
| PUT /api/posts/publications/{key} | Tạo hoặc lấy lại phiên cùng bản nháp: content, visibility, taggedUserIds, targetPostId, version |
| GET /api/posts/publications/{key} | Chủ phiên đọc quyết định và kết quả đã commit |
| POST /api/posts/publications/{key}/publish | mediaIds theo thứ tự; chỉ trả PUBLISHED khi Post transaction thành công |
| DELETE /api/posts/publications/{key} | Hủy PENDING; không hủy PUBLISHED |
| GET /api/internal/publications/{key} | ROLE_INTERNAL đọc quyết định không có nội dung bài cho Media worker |
| POST /api/media/uploads/sign | Bổ sung publicationId, fileKey; xác minh owner/PENDING trước khi cấp chữ ký |

Frontend xác minh status khi response publish bị mất. Chỉ báo thành công khi có PUBLISHED và PostDTO. Nếu UI hydrate lỗi sau commit, giữ khóa cũ để đồng bộ lại. Nếu cancel được xác nhận, reset media tạm và tạo phiên mới khi thử lại; nội dung và preview vẫn giữ. Khóa frontend hiện giữ trong form, không khôi phục draft/File sau reload; trạng thái server vẫn bền vững.

## Kiểm chứng

- Post: 10 integration publication qua: replay, publish đồng thời, publish/cancel cạnh tranh, rollback/cancel khi media lỗi, owner/key binding, expiry không frontend, chống bypass API cũ, tạo/sửa và replay version. PostTimelineIT 39 test qua trước lượt bổ sung test edit cuối; unit attachment rules 3 qua.
- Media: 18 unit + PlaybackResourceIT 1 qua, gồm signature replay, cancelled/foreign owner rejection, cleanup retry/late upload, bảo vệ media attached và lỗi dependency; migration chạy trên Testcontainers.
- Frontend: TypeScript/build, client mock HTTP và Chrome fixture qua: mất response sau commit, không báo thành công với PENDING/CANCELLED/unknown; mỗi tệp gắn cùng publication; lỗi lưu hủy phiên và giữ draft; đóng giữa upload abort và cancel.
- Chưa thực hiện E2E upload/xóa asset Cloudinary thật bằng JWT người dùng sau triển khai. Test bù trừ provider dùng mock; không đánh đồng rollout Ready với kết quả E2E.

## Runtime local

Kind context kind-fakebook-local, namespace fakebook-local. Image Post postservice:publication-20261021-v4, Media mediaservice:publication-20261021-v4. Frontend dùng mã nguồn Vite 5173. Argo fakebook-local vẫn tắt automated để giữ image local; chưa commit/push/GitOps hóa bản này.
