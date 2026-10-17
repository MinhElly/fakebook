# Quản trị và tài khoản Admin

## Phạm vi và vận hành

Admin được phân loại theo danh mục tài khoản tại `userService`; `ROLE_ADMIN` thắng `ROLE_USER`. Các tài khoản quản trị chỉ dùng `/admin`, không dùng API cộng đồng. Danh mục không phụ thuộc hồ sơ xã hội. Các credential chỉ cấu hình phía backend.

Source được bàn giao trong [PR #107](https://github.com/MinhElly/fakebook/pull/107), commit code `6ab606bc656d605277ac64c7d04e70bee7822432`. Local đã chạy migration/backfill và kiểm tra API danh mục; chưa nghiệm thu đầy đủ đăng nhập Admin/User, khóa/đổi role và môi trường nhiều Gateway bằng tài khoản thật. Xem [báo cáo kiểm chứng](administration-verification.md) để phân biệt bằng chứng source, fixture và runtime. Không có thay đổi image/GitOps hoặc triển khai staging/production.

Khi được phép đưa vào môi trường riêng: cấu hình client và service URL, chạy migration của User/Post/Comment/Media, backfill toàn bộ identity từ Keycloak, chờ mọi tác vụ lưu trữ hoàn tất rồi mới cho người dùng truy cập. Không bật từng dịch vụ rời rạc trên runtime cũ: các dịch vụ mới cần API danh mục tài khoản mới. Identity đăng ký mới được provision từ Keycloak khi kiểm tra lần đầu; lỗi Keycloak khi chưa provision trả 503.

Migration khởi tạo `account_directory_meta.ready=false`. Trước khi backfill hoàn tất, tài khoản User và các truy vấn cộng đồng bị chặn bằng 503. Admin được bootstrap từ Keycloak để chạy công cụ backfill. Chỉ lượt backfill hoàn tất toàn bộ mới mở cờ ready; lượt khởi tạo lỗi giữa chừng giữ nguyên trạng thái đóng.

## Cấu hình

Xem `infrastructure/keycloak/administration.env.example`. Client `FAKEBOOK_ADMIN_CLIENT_ID` là confidential service account trong realm `jhipster`, chỉ bật client credentials, tắt standard/direct access flow; cấp `realm-management` gồm `query-users`, `view-users`, `manage-users`, `view-realm`. Không cấp `ROLE_ADMIN` hay `ROLE_USER` cho client này. Client nội bộ hiện có giữ `ROLE_INTERNAL`; không dùng client quản trị thay cho client nội bộ. Không đưa secret vào frontend hoặc Git.

Có thể cấu hình client bằng `configure-administration-client.sh` trong môi trường có `kcadm.sh`, sau khi đặt biến môi trường như công cụ cấu hình Keycloak hiện có. Đây là công cụ operator chủ động chạy; ứng dụng không tự cấp quyền cho client khi khởi động.

Realm có thể tự thêm service account mới vào nhóm `Users`. Script gỡ nhóm này khỏi riêng service account quản trị và kiểm tra role hiệu lực: còn `ROLE_USER` hoặc `ROLE_ADMIN` thì dừng, yêu cầu operator xử lý mapping kế thừa. Không thay đổi nhóm hoặc role của tài khoản con người.

Khai báo `USER_SERVICE_URL` cho Gateway và từng dịch vụ; `POST_SERVICE_URL`, `COMMENT_SERVICE_URL`, `MEDIA_SERVICE_URL` cho worker. Lưu ý Media chạy cổng 8084, Comment cổng 8085. Cấp ACL Kafka cho `admin-audit-events` và `content-moderation-events`; consumer group audit dùng chung giữa các pod `userService`.

### Chạy Java/Vite trực tiếp trên local

- Nạp `FAKEBOOK_OIDC_ISSUER_URI`, client ID và secret nội bộ vào môi trường chạy của từng backend. Riêng User Service cần thêm client ID/secret quản trị. `infrastructure/.env` không tự được IntelliJ nạp: cần khai báo trong Run Configuration hoặc cơ chế nạp env hiện có. Chỉ lưu credential trong cấu hình local bị Git ignore.
- Khi ingress `api.fakebook.test:80` không chạy nhưng Gateway Java ở cổng 8080, đặt `VITE_API_BASE_URL=http://localhost:8080` trong `frontend/.env.local`. `VITE_KEYCLOAK_URL` phải khớp hostname công khai đã cấu hình cho Keycloak; local đã dùng `http://auth.fakebook.test:9080`. Không dùng client quản trị trong frontend.
- `Failed to fetch` cần kiểm tra hostname/cổng và CORS; preflight 200 không chứng minh request có Bearer thành công. Sau khi kết nối được, `account-state` 503 có thể do thiếu credential M2M/quản trị hoặc danh mục `ready=false`; không bỏ kiểm tra trạng thái tài khoản để chữa lỗi.
- Sau migration các dịch vụ sở hữu, hoàn tất backfill bằng công cụ bên dưới và chờ `sync_state=SYNCED`. Local được kiểm tra đã có 1 Admin và 8 User; đây là snapshot nghiệm thu, không phải cấu hình seed bắt buộc.

## Công cụ tài khoản và API

Đặt token Admin trong biến môi trường `FAKEBOOK_ADMIN_TOKEN`, rồi dùng `scripts/admin-accounts.ps1 -Action backfill -BaseUrl <gateway>`. Cấp/thu hồi quyền dùng `-Action grant-admin` hoặc `revoke-admin`, kèm `-Subject`, `-Version` hiện hành, `-Reason`. Giữ `-IdempotencyKey` khi retry cùng yêu cầu. Không có UI cấp quyền. Không sửa role trực tiếp trong Keycloak: thay đổi ngoài công cụ không có hiệu lực danh mục và không thuộc hợp đồng đồng bộ.

- `userservice/api/admin/overview`, `users`, `admins`, `audit`: số liệu thật, phân trang `page/size`; lọc tài khoản qua `q/status`. Audit lọc `actor/target/action/from/to` với thời gian ISO-8601.
- `userservice/api/admin/users/{id}`: chi tiết có audit; POST `/{id}/suspend` hoặc `/{id}/unlock` nhận `{reason,version}` và header `Idempotency-Key`.
- `postservice/api/admin/posts` và `commentservice/api/admin/comments`: metadata; lọc `q/status/visibility/archived`. GET `/{id}` ghi audit trước khi trả chi tiết. POST `/{id}/hide` hoặc `restore` nhận hợp đồng mutation như trên.
- Các URL trên đều có tiền tố `/services/`. `visibility` trong bộ lọc quản trị là `VISIBLE/HIDDEN`, khác với quyền riêng tư `PUBLIC/FRIENDS/PRIVATE` của bài viết.
- Tệp đính kèm dùng `mediaservice/api/admin/media/{id}/file`: audit tại Media và outbox commit trước khi proxy byte; luôn `Cache-Control: no-store`. Media dùng cùng thư viện Namastack Outbox với các dịch vụ sở hữu nội dung. Giao diện chỉ tải tệp sau thao tác xem riêng.
- API nội bộ `/api/internal/accounts/{subject}`, `/excluded`, `/{subject}/archive` chỉ nhận M2M. `/api/account-state` trả phân loại/trạng thái của chính caller để frontend chọn nhánh router, không tạo hồ sơ.

Danh sách và chi tiết tài khoản quản trị trả thêm `display_name`, `email`, `last_seen_at`. Tên hiển thị ưu tiên hồ sơ cộng đồng, sau đó tên trong Keycloak và cuối cùng tên tài khoản; tài khoản chưa có hồ sơ vẫn xuất hiện trong danh mục. Email và tên Keycloak là metadata được cập nhật theo lô khi khởi động và mỗi 5 phút (`administration.identity-sync-interval`), không dùng để quyết định quyền truy cập. Tìm kiếm tài khoản bao gồm tên hiển thị và email trước khi đếm/phân trang. `last_seen_at` lấy từ dữ liệu presence đã lưu, có thể null khi chưa ghi nhận; không suy ra từ thời gian đăng ký hay trạng thái đồng bộ. API đọc trường này với lịch UTC để khớp cấu hình ghi của Hibernate. Giao diện Admin luôn hiển thị `dd/MM/yyyy HH:mm:ss` theo `Asia/Ho_Chi_Minh` (GMT+7), có nhãn múi giờ rõ ràng, độc lập múi giờ máy. Bộ lọc ngày giờ hiểu đầu vào là GMT+7 và chuyển sang UTC khi gửi API; không sửa timestamp trong database.

Các nút Chi tiết của Người dùng/Bài viết/Bình luận mở modal, tải dữ liệu qua API có audit và có skeleton, lỗi/thử lại riêng. Có thể đóng bằng nút Đóng, Esc hoặc nhấp nền; đóng trong lúc tải hủy request. Bảng, tổng quan và màn hình khởi tạo `/admin` dùng skeleton theo bố cục quản trị.

### Biểu đồ Tổng quan

`GET /services/userservice/api/admin/overview?days=7` giữ các trường `users`, `admins`, `suspended`, `pending`, `directory_ready` và bổ sung:

- `user_states: {active,suspended,processing}`: chỉ User; processing là trạng thái ngoài ACTIVE/SUSPENDED. Trường tổng `suspended` cũ vẫn đếm mọi User bị hạn chế để tương thích.
- `sync_states: {synced,pending,retry}`: toàn bộ danh mục Admin và User. Trường tổng `pending` cũ gồm cả pending/retry.
- `days`, `timezone: "Asia/Ho_Chi_Minh"`, `activity: [{date,views,changes}]`: chỉ tổng hợp số lượng, không chứa danh tính hoặc nội dung audit.

`days` chỉ nhận 7 hoặc 30, mặc định 7; giá trị khác trả 400. Chuỗi ngày kết thúc ở ngày Việt Nam hiện tại, có đầy đủ ngày giá trị 0. `views` gồm VIEW_ACCOUNT/VIEW_POST/VIEW_COMMENT/VIEW_MEDIA; `changes` gồm các sự kiện còn lại. Ranh giới ngày là nửa mở `[00:00 GMT+7, 00:00 ngày tiếp theo)`. Audit từ các dịch vụ khác có thể trễ Kafka. UI dùng SVG/CSS, chọn 7/30 ngày qua URL, có chú giải, tooltip bằng chuột/bàn phím và bảng số liệu mở rộng; không polling. Quyền API vẫn chỉ Admin, loại client nội bộ.

Actor luôn lấy từ JWT, không nhận từ body. Replay cùng actor/key/body trả version ban đầu; đổi body hoặc version trả 409. Bản đầu không khóa chính mình hoặc Admin khác. Danh sách User và tổng số User không bao gồm Admin.

## Khóa, lưu trữ và kiểm duyệt

Khóa ghi DB và audit cùng giao dịch, chặn request mới ngay bằng `403 ACCOUNT_SUSPENDED`; không kiểm tra được trạng thái trả 503. Keycloak disable/logout được retry độc lập, tối đa lùi 1 giờ giữa các lần thử. Mở khóa giữ `UNLOCK_PENDING` cho đến khi đồng bộ xong; mốc token mới buộc đăng nhập lại. Thay đổi role cũng thu hồi token. SSE và presence kiểm tra lại trước khi gửi dữ liệu.

Chuyển role giữ nguyên dữ liệu, đánh dấu `community_archived` tại dịch vụ sở hữu. Trong khi đồng bộ lưu trữ, danh mục chặn truy cập cộng đồng cho subject đó. Không dùng API xóa, không cleanup media. Thu hồi Admin không xóa marker lưu trữ cũ; hồ sơ và quan hệ cũ tiếp tục ẩn. Công cụ không cung cấp thao tác phục hồi dữ liệu xã hội đã lưu trữ.

Hồ sơ đã lưu trữ không được luồng đăng nhập tự tạo lại sau khi đổi role; `/me` trả `403 COMMUNITY_PROFILE_ARCHIVED` thay vì ghi đè bản ghi cũ. Người quản trị dùng tài khoản User riêng cho cộng đồng như quy tắc tài khoản đã chốt.

Kiểm duyệt dùng `moderation_visibility`, độc lập với trạng thái xóa và lưu trữ. Bình luận có `effective_hidden` cho cả nhánh; khôi phục cha vẫn giữ quyết định ẩn riêng của con. Quyền đọc được kiểm tra từ DB hiện hành; cache không cung cấp quyết định cho phép. Các đường trả danh sách chứa quyền hoặc bộ đếm nhạy với Admin đọc lại DB, chấp nhận tăng truy vấn để bảo đảm đúng quyền.

Audit mutation nằm cùng giao dịch tại dịch vụ sở hữu; outbox gửi về `userService`, chống trùng theo event ID. Audit tập trung có thể trễ Kafka. Thông tin riêng tư chỉ trả sau khi audit tại dịch vụ sở hữu commit thành công. Theo dõi `sync_state`, `sync_attempts`, tuổi bản ghi pending, outbox lỗi và consumer lag trước khi nghiệm thu runtime.

## Kiểm chứng

Chạy TypeScript/build tại `frontend`, `node tests/admin.browser.cjs` với Playwright và Chrome, rồi `clean verify` từng dịch vụ bị ảnh hưởng. Browser fixture dùng mock API; không chứng minh runtime backend. Các test cũ dùng fixture danh mục tại ranh giới remote; test bảo mật mới dùng ma trận trạng thái riêng, kiểm thử H2 cho phân trang/ID/ẩn nhánh và audit/idempotency. Cần kiểm chứng riêng bằng tài khoản Admin/User thật khi được phép chạy mã mới.

Log và ảnh thử nghiệm được lưu ở `.artifacts/admin-*`, không thuộc source bàn giao. Báo cáo kiểm chứng cuối cùng ghi riêng gate PASS/FAIL và phần runtime chưa thực hiện.
