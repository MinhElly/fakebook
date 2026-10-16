# Lịch sử comment và trạng thái tải

## Hợp đồng

- `PUT /api/comments/{id}` vẫn nhận `{ "content": "..." }`. Phản hồi comment và preview thêm `editedAt` (nullable) và `editCount` (mặc định 0).
- `GET /api/comments/{id}/history?before=<revision>&size=20` trả mảng `{ revision, content, editorId, editedAt }`, mới nhất trước. `before` là mốc loại trừ; kích thước từ 1 đến 50. Qua Gateway dùng tiền tố `/services/commentservice`.
- Phiên bản 0 là nội dung trước lần sửa đầu được ghi nhận. Các bản gốc thiếu `createdAt` trả thời gian null. Không phục dựng lịch sử trước migration.
- Nội dung giống hệt không tạo phiên bản hoặc event mới. Cập nhật nội dung, phiên bản và outbox cùng transaction; sửa/xóa dùng khóa hàng để tuần tự hóa thao tác.
- Mỗi lần đọc lịch sử kiểm tra quyền đọc comment hiện tại, gồm quyền bài viết và trạng thái xóa. Không có API khôi phục phiên bản.
- Migration `20261016000000_comment_history.xml` thêm bảng và cột; cache summary dùng namespace v2 để tránh preview cũ thiếu metadata. Triển khai migration/backend trước frontend.

## Quy ước loading

Lần tải đầu dùng skeleton đúng hình dạng nội dung. Làm mới giữ dữ liệu hiện có; tải thêm chỉ thêm placeholder phía cuối. Trạng thái rỗng chỉ hiển thị sau request thành công. Lỗi có thông báo và thao tác thử lại. Các request theo hồ sơ/truy vấn/tài khoản bỏ qua kết quả đã lỗi thời; dữ liệu provider được tạo lại khi đổi phiên đăng nhập.

Skeleton tắt animation khi `prefers-reduced-motion: reduce`. Ảnh có kích thước phù hợp với avatar, cover và media; skeleton chờ cả tải blob và sự kiện load/error của ảnh. Thao tác ghi dùng pending/disabled và giữ bản nháp khi lỗi.

## Kiểm chứng

Backend chạy tại `commentService`:

```powershell
.\mvnw.cmd -ntp clean verify
```

Trên môi trường Windows sandbox, Mockito dynamic attach có thể bị chặn ở named pipe. Dùng Java agent của phiên bản Mockito đang được resolve và thư mục TEMP/TMP không chứa dấu ngoặc nhọn khi chạy gate. Không thay đổi cấu hình production để xử lý giới hạn sandbox.

Frontend chạy tại `frontend`:

```powershell
npx tsc --noEmit
npm run build
npm run test:browser
```

Browser runner cần Playwright và Chrome có sẵn trong môi trường kiểm thử. Mặc định resolve module `playwright`; nếu dùng runtime nằm ngoài repo, đặt `PLAYWRIGHT_MODULE` thành đường dẫn tuyệt đối đến package đó trước khi chạy. `BROWSER_CHANNEL` mặc định `chrome`, `TEST_PORT` mặc định `5193`. Runner chỉ dùng API fixture và Vite local; ảnh kiểm chứng ghi dưới `.artifacts/comment-loading/`. Không cần tài khoản hay hệ thống backend đang chạy.

Các tình huống gồm: tải comment, quyền sở hữu bằng ID, nhãn chỉnh sửa, giữ nội dung khi lưu/gửi lỗi, lịch sử lỗi/thử lại/phân trang, Escape/mobile, refresh realtime giữ nội dung cũ, phân biệt lỗi/rỗng, tìm kiếm trả kết quả sai thứ tự, kích thước avatar, reduced motion, bài đã lưu và gắn thẻ/tìm địa điểm. Backend integration tests kiểm tra quyền PUBLIC/FRIENDS/PRIVATE và deleted, admin, reply, no-op, native preview/cache, sửa đồng thời và rollback.

Browser fixture và service integration tests không thay thế E2E bằng token thật qua Gateway hoặc kiểm chứng bản release đang triển khai.
