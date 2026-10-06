# Source và Documentation Audit — 2026-10-05

> Tài liệu lịch sử, đã được thay thế bởi `source-documentation-audit-2026-10-06.md`. Các topology, đường dẫn và kết luận runtime bên dưới không còn là hướng dẫn vận hành hiện tại.

## 1. Phạm vi và baseline

- **Working tree commit**: `develop@bf14b43` (fast-forward thành công, đồng bộ hoàn toàn với `origin/develop` sau PR #90, PR #91, PR #92).
- **Tính năng mới được xác minh**: Saved Posts (Entity JPA `SavedPost`, migration Liquibase `20260930000000_added_entity_SavedPost.xml`, REST endpoints `POST /api/posts/{id}/save`, `GET /api/posts/saved`, và React UI `SavedPage.tsx`, `Post.tsx`, `RightSidebar.tsx`).
- **Phạm vi kiểm tra**: Toàn bộ tài liệu trong `docs/`, `README.md`, cấu hình Spring Boot (`pom.xml`), Docker Compose (`docker-compose.yml`, `docker-compose-staging.yml`), các Controller, Service, DTO, Feign clients, Kafka bindings và frontend API services.

---

## 2. Kết luận và Bảng đối soát

| Miền | Trạng thái | Chi tiết kết luận |
|---|---|---|
| **Source topology** | 🟢 GO | Bảy backend microservices (`gateway`, `authService`, `userService`, `postService`, `commentService`, `mediaService`, `feedService`), React 19 SPA frontend và hạ tầng phân tán đều đồng nhất giữa source code và tài liệu. |
| **API Catalog & Contract** | 🟢 GO | Đã cập nhật `docs/04-api/api-overview.md` và `docs/02-architecture/microservices.md`: phản ánh đúng các route Saved Posts (`POST /api/posts/{id}/save`, `GET /api/posts/saved`), các endpoint nghiệp vụ Feed (`/api/feed/me`), Comment (`/api/comments/create`, `/summaries`), User (`/api/user-profiles/me`, `/suggestions`). |
| **Client/API alignment** | 🟢 ĐÃ SỬA | Sửa lỗi `frontend/src/services/searchService.ts` gọi sai HTTP `DELETE` thành `POST /services/userservice/api/friend-requests/{requestId}/cancel`, đồng bộ với backend `FriendRequestResource.java` và `friendsService.ts`. |
| **Database Architecture** | 🟢 GO | Cập nhật `docs/05-database/database-architecture.md` và `docs/01-overview/glossary.md` bổ sung bảng `saved_post` trong schema `post_service`. |
| **Performance Testing** | 🟡 ĐÃ BỔ SUNG SCRIPT | Khởi tạo file script `performance/k6/http-baseline.js` trong kho mã nguồn và cập nhật `docs/08-testing/load-testing.md`. |
| **Tech Stack Documentation** | 🟢 GO | Cập nhật `README.md` và `docs/01-overview/project-overview.md`: phản ánh đúng phiên bản Spring Boot `4.1.1` và Spring Cloud `2025.1.3` theo các file `pom.xml`. |
| **Local Runtime** | 🟡 UNVERIFIED | Giữ nguyên giới hạn bằng chứng: không tự nâng trạng thái khi chưa chạy lại full stack và test E2E. |
| **Staging Deployment** | 🟡 NO-GO | Topology Docker Compose đã cấu hình đầy đủ, nhưng chưa có bằng chứng live deployment authenticated E2E. |
| **Target k3s + Argo CD** | 🔴 NO-GO | Chưa có Kubernetes manifests/Kustomize/Helm/Argo CD trong repo. |

---

## 3. Các điểm đã xử lý trong đợt Audit này

1. **Đồng bộ nhánh `develop`**: Fast-forward từ `575c738` lên `bf14b43`, tích hợp mã nguồn Saved Posts, căn chỉnh collation MariaDB testcontainer và tối ưu hóa frontend.
2. **Sửa lỗi Client Mismatch**: Sửa hàm `cancelFriendRequest` trong [searchService.ts](file:///c:/Code/fakebook/frontend/src/services/searchService.ts) từ `api.delete` sang `api.post`.
3. **Bổ sung Script Performance k6**: Tạo file [http-baseline.js](file:///c:/Code/fakebook/performance/k6/http-baseline.js) sẵn sàng cho kịch bản đo độ trễ và tải qua Gateway.
4. **Cập nhật Database Architecture**: Bổ sung bảng `saved_post` vào danh mục schema `post_service` trong [database-architecture.md](file:///c:/Code/fakebook/docs/05-database/database-architecture.md).
5. **Cập nhật Bảng thuật ngữ**: Thêm thực thể `SavedPost` vào [glossary.md](file:///c:/Code/fakebook/docs/01-overview/glossary.md), chuẩn hóa danh mục schema sang snake_case.
6. **Cập nhật Luồng dữ liệu**: Bổ sung "Luồng 4: Lưu bài viết & Trang Đã lưu" với sơ đồ sequence diagram trong [data-flow.md](file:///c:/Code/fakebook/docs/02-architecture/data-flow.md).
7. **Cập nhật Đặc tả Microservices & API**: Cập nhật Post Service và catalog trong [microservices.md](file:///c:/Code/fakebook/docs/02-architecture/microservices.md) và [api-overview.md](file:///c:/Code/fakebook/docs/04-api/api-overview.md).
8. **Cập nhật Ma trận Yêu cầu**: Cập nhật [requirement-traceability-matrix.md](file:///c:/Code/fakebook/docs/08-testing/requirement-traceability-matrix.md) và [load-testing.md](file:///c:/Code/fakebook/docs/08-testing/load-testing.md).
9. **Cập nhật Root README**: Đồng bộ Spring Boot 4.1.1, Saved Posts, và k6 baseline script trong [README.md](file:///c:/Code/fakebook/README.md).

---

## 4. Kế hoạch và Gate tiếp theo

1. **Frontend Typecheck & Build**: Chạy `npm run build` trong `frontend/` để kiểm tra compile TypeScript không còn lỗi.
2. **Local Integration Smoke**: Bật hạ tầng qua `.\infrastructure\start.ps1`, chạy các microservice, và thực thi `scripts/e2e-normal-user-smoke-test.ps1` với normal user token.
3. **Saved Posts E2E Verification**: Thực hiện test luồng toggle `POST /api/posts/{id}/save` và `GET /api/posts/saved` trên giao diện web hoặc script.
4. **Gateway Security Policy Consolidation**: Đánh giá và thống nhất policy anonymous GET post giữa Gateway và Post Service.
