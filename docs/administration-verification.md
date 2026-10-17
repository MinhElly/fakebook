# Kiểm chứng triển khai quản trị — 17/10/2026

## Source

Commit code: `6ab606bc656d605277ac64c7d04e70bee7822432`, nhánh `codex/admin-dashboard-authorization`, [PR #107 → develop](https://github.com/MinhElly/fakebook/pull/107). Nhánh đã cập nhật từ base `8e8e89ae6d5aa325df702b8072906ac5a272c58e` trước commit; baseline triển khai ban đầu là `6f2e0fe6740ac633a1296837756f7c66a7b5f374`. Source đã commit/push; không merge, cập nhật image/GitOps hoặc deploy. Cấu hình vận hành, API và lệnh backfill nằm tại [administration.md](administration.md).

- Danh mục tài khoản độc lập hồ sơ, chốt backfill trước khi mở cộng đồng; Admin thắng khi có cả `ROLE_USER`.
- Gateway và từng dịch vụ kiểm tra trạng thái hiện hành; M2M tách riêng, lỗi trạng thái đóng quyền, SSE/presence kiểm tra lại trước khi gửi.
- Lưu trữ dữ liệu Admin bằng marker, không dùng luồng xóa; lọc SQL trước count/phân trang và lookup ID. Cache nội dung không quyết định quyền đọc.
- Kiểm duyệt độc lập với xóa/lưu trữ; nhánh bình luận bị ẩn được kiểm tra từ DB ngay cả khi cờ tổng hợp chưa cập nhật.
- Mutation có reason/version/idempotency; audit và outbox cùng giao dịch. Đọc chi tiết và media quản trị có audit; media proxy dùng `no-store`.
- Sáu trang quản trị tiếng Việt; provider cộng đồng chỉ mount trong nhánh User. Bộ lọc trên URL, xác nhận, lỗi/thử lại, đồng bộ đang chờ và bố cục điện thoại.

## Kiểm thử backend nền trước phần metadata/chart

| Dịch vụ | Unit | Integration | Kết quả |
|---|---:|---:|---|
| User | 108 | 150 | PASS |
| Post | 108 | 110 | PASS |
| Comment | 90 | 76 | PASS |
| Media | 47 | 96 | PASS |
| Feed | 68 | 71 | PASS |
| Gateway | 56 | 91 | PASS |

Gate nền ghi nhận **1,071 kiểm thử backend PASS**, không failure/error, trước các bổ sung metadata/chart/GMT+7. Các dịch vụ đã chạy `clean verify`; đây không phải khẳng định toàn bộ sáu suite đã được chạy lại trên commit bàn giao cuối. Gateway bỏ các bước npm/webpack của webapp JHipster cũ không thay đổi; frontend React/Vite được kiểm chứng riêng. Test SSE mới của Gateway chạy bổ sung bằng Surefire. Các test DB/broker dùng H2 hoặc Testcontainers riêng, không dùng DB hay deployment hiện tại.

Các case mới gồm Admin+User, User/Internal không có quyền quản trị, token cũ/đổi role, lỗi danh mục, chốt backfill, retry/mở khóa, SSE đang mở, cache nóng, lọc trước phân trang/ID và liên kết media, giữ hồ sơ lưu trữ, nhánh bình luận và race với moderation, audit/outbox lỗi rollback mutation, truy cập riêng tư và media có audit, chống gửi lặp/version conflict, legacy feed loại candidate không còn được đọc trước khi phân trang.

Fixture của các test dịch vụ cũ stub ranh giới account directory mới; chúng không thay thế kiểm thử trạng thái/role và DB mới. Kiểm thử Keycloak dùng mock, chưa chứng minh cấu hình quyền hay đồng bộ với Keycloak thật.

Log cuối tại `.artifacts/`:

- `admin-user-final-acceptance.log`
- `admin-post-acceptance.log`
- `admin-comment-final2.log`
- `admin-media-complete.log`
- `admin-feed-final.log`
- `admin-gateway-verify2.log`, `admin-gateway-sse-final.log`

Trên Windows, TEMP/TMP được đặt trong `.artifacts/admin-tmp`; Mockito chạy bằng Java agent và tracing export tắt trong test. Một số fork test ghi cảnh báo timeout khi shutdown; kết quả gate được xác nhận từ exit code Maven và báo cáo JUnit, không suy ra từ một dòng log đơn lẻ.

## Bổ sung metadata, chart và GMT+7

- **PASS** User Service: 19 unit + 4 `AdminOverviewResourceIT`, Maven `verify` exit 0. Bao gồm SQL phân trang/tìm tên-email, UTC last seen, giao dịch metadata với Hikari `autoCommit=false`, trạng thái tài khoản, chart 7/30 ngày/ngày trống, ranh giới nửa đêm GMT+7, tham số sai và API chỉ cho Admin. Không cộng 23 test này vào 1,071 vì có các ca chạy lặp.
- **PASS** lượt metadata trước đó: 15 unit + 42 `UserProfileResourceIT`, Maven `verify` exit 0. Các lần test sau thay đổi được ghi riêng, không coi là một lượt full-suite duy nhất.
- **PASS** cú pháp `configure-administration-client.sh` bằng Git Bash `-n`; staged diff không lỗi whitespace, không chứa credential local. Script cấu hình đã bổ sung loại nhóm Users khỏi service account mới; chưa chạy toàn bộ script này trên một realm thử nghiệm mới.
- Log bổ sung: `.artifacts/admin-ui-user-verify.log`, `.artifacts/admin-charts-verify.log`; artifact là bằng chứng local và không được commit.

## Frontend

- **PASS** TypeScript `tsc --noEmit` và production build.
- **PASS** `test:admin`: sáu trang, User bị từ chối mà không gọi API quản trị, mở chi tiết/tệp qua API có audit, mutation reason/version/idempotency, desktop và 390px không tràn trang.
- **PASS** fixture cuối ở hai múi giờ browser `UTC` và `America/Los_Angeles`: chart 7/30 ngày, bảng số liệu, tooltip bàn phím, dữ liệu toàn 0/lỗi/thử lại; `03:12Z` hiển thị `10:12 GMT+7`, `18:30Z` sang `01:30` ngày sau, bộ lọc gửi UTC và khôi phục đúng giờ Việt Nam.
- **PASS** modal chi tiết User/Post/Comment: skeleton, Esc/focus restore, hủy request khi đóng, lỗi/thử lại; skeleton khởi tạo Admin, bảng và Tổng quan. Thời gian UI cố định `Asia/Ho_Chi_Minh`, có nhãn GMT+7.
- **PASS** `test:browser`: loading/history, ownership, lỗi/rỗng/thử lại, profile, search, media, friend list và realtime fixture.
- **PASS** `test:presence`: kiểm thử browser presence hiện có.
- Đã xem ảnh desktop/mobile tại `.artifacts/admin-browser/`. Vite vẫn cảnh báo bundle lớn và dynamic import hiện có; build hoàn tất.

## Runtime

**PARTIAL — local đã kiểm tra theo phạm vi được cấp phép:**

- Xác nhận Vite trỏ tới ingress cổng 80 không hoạt động trong khi Gateway chạy trực tiếp ở 8080; sửa bằng `.env.local` bị Git ignore. CORS và Keycloak login page phản hồi hợp lệ.
- Cấu hình client quản trị backend trong Keycloak, cấp bốn quyền realm-management cần thiết; loại nhóm Users tự gán, xác nhận không có ROLE_USER/ROLE_ADMIN trên service account. Credential chỉ ở local bị Git ignore, không có giá trị trong Git hoặc tài liệu.
- Đã áp dụng migration và backfill bằng implementation `AdminAccountService.backfill`: 1 Admin, 8 User; `ready=true`, tất cả `SYNCED`. API danh mục nội bộ qua Gateway trả 200 cho cả 9 identity; client nội bộ gọi API Admin nhận 403. Không có hồ sơ Admin chưa được đánh dấu ẩn khỏi cộng đồng.
- Sáu dịch vụ đã từng trả `UP` sau sửa. Migration metadata đã có trong database, 9 tài khoản lưu được email/tên. Đọc `last_seen_at` theo UTC khớp cấu hình Hibernate; không chạy migration cộng/trừ giờ vào dữ liệu cũ.
- Truy vấn tổng hợp chart chạy trực tiếp, chỉ đọc trên MariaDB local: 8 User, 1 Admin, 12 lượt xem, 0 thay đổi quản trị trong snapshot; đủ 7/30 bucket, múi giờ `Asia/Ho_Chi_Minh`. Đây là kiểm chứng SQL/runtime, không phải request browser Admin đã xác thực.
- Cuối phiên 17/10/2026 đã dừng sáu Java server và Vite theo yêu cầu; xác nhận cổng `5173`, `5198`, `8080`, `8082–8086` không còn listener. Hạ tầng Docker được giữ nguyên; trạng thái `UP` ở trên là snapshot trước khi đóng cổng.

**NOT VERIFIED:** đăng nhập bằng hai phiên Admin/User thật sau các thay đổi cuối; khóa/mở khóa/đổi role thật qua luồng API; thử lỗi/khôi phục Keycloak-Kafka và thu hồi quyền trên nhiều Gateway. Chưa có nghiệm thu staging/production, image hoặc Argo revision chứa source mới. CI GitHub trên head cuối của PR được theo dõi riêng, không suy ra từ các test local.

Khi mở lại runtime hoặc triển khai môi trường khác: dùng runbook cấu hình M2M/quản trị, migration/backfill có kiểm soát, kiểm tra worker/consumer/outbox và nghiệm thu bằng hai tài khoản riêng. `Synced/Healthy`, build, fixture và API nội bộ không thay thế kiểm chứng đăng nhập/phân quyền E2E.
