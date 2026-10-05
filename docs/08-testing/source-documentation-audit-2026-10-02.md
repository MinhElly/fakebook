# Source và Documentation Audit — 2026-10-02

## 1. Phạm vi và baseline

- Working tree: `develop@575c738`, chậm hơn `origin/develop` 4 commit.
- Remote snapshot đã đọc: `origin/develop@2978a9f`; thay đổi đáng chú ý là Saved Posts và frontend liên quan.
- Thay đổi có sẵn của người dùng trong `docs/07-operations/monitoring.md` và `docs/08-testing/requirement-traceability-matrix.md` được giữ nguyên, không ghi đè.
- Audit dựa trên controller, DTO, Spring Security, Feign client, cấu hình, frontend service calls và deployment files. Không chạy authenticated E2E, không xác minh live staging và không coi lịch sử CI là kết quả test mới.

## 2. Kết luận

| Miền | Trạng thái | Kết luận |
|---|---|---|
| Source topology | GO để mô tả | Bảy backend deployable, frontend và hạ tầng được nhận diện rõ từ source. |
| API documentation | Đã cập nhật | Catalog cũ có nhiều path/security sai; `docs/04-api/api-overview.md` đã được viết lại theo controller hiện tại và snapshot remote. |
| Inter-service documentation | Đã cập nhật | Sửa key OpenFeign, thêm Feed -> Post, outbox, role nội bộ và ranh giới resilience. |
| Local runtime | Không đánh giá lại | Không chạy service/health/auth trong audit này; dùng matrix hiện có làm lịch sử, không nâng trạng thái. |
| Staging | NO-GO để tuyên bố hoàn tất | Source Compose có nhưng live deployment, external integrations và recovery chưa được chứng minh. |
| k3s + Argo CD | NO-GO | Chưa có manifests/Kustomize/Helm/Argo/External Secrets trong repo. |

## 3. Sai lệch API đã sửa

1. Feed thật là `GET /api/feed/me`, không phải `/api/feeds`.
2. Tạo post là `POST /api/posts/create`, không phải `POST /api/posts`.
3. Lấy comment theo post dùng criteria `GET /api/comments?postId.equals=...`; tạo/reply dùng `/create` và `/reply`.
4. Friend suggestion là `/api/user-profiles/suggestions`, không phải `/friend-suggestions`.
5. Luồng follow dùng `/api/follows`, không phải `/api/user-follows`.
6. Reaction chuẩn cho frontend là `PUT|DELETE /api/post-reactions/posts/{postId}` và `POST /api/comment-reactions/toggle`.
7. SpringDoc dùng API starter và profile `api-docs`; không có bằng chứng cho Swagger UI tổng hợp tại Gateway.
8. Saved Posts (`POST /api/posts/{id}/save`, `GET /api/posts/saved`) chỉ có trên remote snapshot `2978a9f`, chưa có trong checkout local.

## 4. Findings cần xử lý tiếp

### P1 — Security contract không đồng nhất giữa Gateway và service

- Post Service cho phép anonymous `GET /api/posts/**` và `GET /api/post-reactions/**`, nhưng Gateway yêu cầu authenticated cho các route `/services/**` này. Chọn một policy và đồng bộ hai tầng; hiện guest post viewing qua public Gateway không hoạt động như comment trong source.
- `PublicUserResource.GET /api/users` tự mô tả là public, nhưng Gateway matcher `/api/**` yêu cầu authenticated. Sửa matcher hoặc sửa contract/controller comment.
- Gateway cho public toàn bộ `GET /services/*/api/media/**`, bao gồm list metadata. Cần review data minimization trước production.

### P1 — Client/API mismatch

- `frontend/src/services/searchService.ts` gọi `DELETE /friend-requests/{id}/cancel`, trong khi backend chỉ khai báo `POST`. `friendsService.ts` đã dùng đúng `POST`; hai client path cần thống nhất.

### P1 — API surface kỹ thuật còn lộ

- Nhiều CRUD/probe sinh bởi JHipster vẫn tồn tại (`*-kafka`, generic entity CRUD). Một số đã khóa admin, một số chỉ dựa vào catch-all authenticated. Nên lập allowlist public/business API tại Gateway và loại probe khỏi production profile nếu không dùng.

### P1 — E2E script cũ không còn là release gate đáng tin cậy

- `scripts/e2e-smoke-test.sh` dùng token `internal`, gọi generic `/api/user-profiles` đang khóa admin và chỉ kiểm tra HTTP status.
- Dùng `scripts/e2e-normal-user-smoke-test.ps1` cho normal-user ownership, feed propagation và trace; vẫn cần chạy thật và giữ artifact.

### P2 — Performance artifact thiếu

- `docs/08-testing/load-testing.md` từng nói `performance/k6/http-baseline.js` tồn tại, nhưng thư mục hiện không có file này. Tài liệu đã hạ thành thiết kế dự kiến; cần thêm script và CI artifact trước khi công bố SLO.

### P2 — Feed pagination metadata

- `GET /api/feed/me` nhận `Pageable` nhưng trả array mà không phát `X-Total-Count`/`Link`, khác convention các endpoint JHipster khác. Frontend hiện có thể phân trang bằng số lượng item, nhưng contract không cho biết total/next page.

## 5. Tài liệu đã cập nhật

- `docs/04-api/api-overview.md`: endpoint catalog, auth, payload, OpenAPI và error semantics.
- `docs/04-api/service-communication.md`: Feign/Kafka/outbox/resilience gates.
- `docs/02-architecture/microservices.md`: version, endpoint và Saved Posts.
- `docs/02-architecture/data-flow.md`: path thật và transactional outbox.
- `docs/01-overview/project-overview.md`: stack version và baseline source/remote.
- `docs/06-deployment/deployment-overview.md`: tách source topology khỏi live proof và ghi target k3s + Argo.
- `docs/08-testing/testing-strategy.md`: normal-user E2E là gate ưu tiên.
- `docs/08-testing/load-testing.md`: ghi rõ k6 artifact chưa tồn tại.

## 6. Gate tiếp theo

1. Fast-forward/merge `origin/develop` sau khi xử lý an toàn hai file docs đang bẩn, rồi re-run endpoint inventory trên HEAD mới.
2. Sửa ba mismatch P1: guest post policy, `/api/users` policy, `DELETE` vs `POST` cancel request.
3. Chạy `e2e-normal-user-smoke-test.ps1` với Gateway/User/Post/Comment/Feed/Auth và Zipkin đang hoạt động.
4. Thực hiện downstream outage qua Gateway, giữ Actuator events cho đủ `OPEN -> HALF_OPEN -> CLOSED`.
5. Thực hiện Kafka retry/redelivery/DLT và recovery; sau đó mới cập nhật matrix runtime/staging.
