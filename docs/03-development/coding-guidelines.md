# Coding Guidelines & Git Workflow

Tài liệu này quy định các tiêu chuẩn kỹ thuật khi đóng góp mã nguồn vào repository **Fakebook**.

---

## 1. Tiêu chuẩn Backend (Java 21 & Spring Boot)

1. **Phân tách Layer rõ ràng**:
   - `web.rest`: Chỉ tiếp nhận HTTP request, validate dữ liệu đầu vào với `@Valid`, trả về DTO hoặc `ResponseEntity`. Không viết logic nghiệp vụ tại Controller.
   - `service`: Chứa toàn bộ nghiệp vụ (Business Logic). Sử dụng annotation `@Transactional(readOnly = true)` ở mức class và `@Transactional` ở các phương thức ghi dữ liệu.
   - `repository`: Giao tiếp cơ sở dữ liệu qua Spring Data JPA hoặc Spring Data JDBC.
   - `domain`: Các JPA Entity ánh xạ bảng cơ sở dữ liệu. Không trả trực tiếp Entity ra ngoài API; luôn chuyển đổi qua DTO.
2. **Quy tắc quan hệ dữ liệu liên Service**:
   - Tuyệt đối không tạo quan hệ JPA `@ManyToOne` hay `@OneToMany` xuyên qua microservice khác.
   - Chỉ lưu khóa ngoại sang service khác dưới dạng trường **`UUID`** độc lập (ví dụ `authorId`, `avatarMediaId`).
3. **Quản lý Transaction & Event Publishing**:
   - Khi phát sinh Kafka event sau khi ghi cơ sở dữ liệu, ưu tiên sử dụng Transaction Synchronization (`afterCommit`) để đảm bảo event chỉ được phát đi khi dữ liệu đã commit thành công vào MariaDB.
4. **Xử lý Exception chuẩn REST**:
   - Sử dụng các exception kế thừa từ `org.zalando.problem.AbstractThrowableProblem` hoặc `BadRequestAlertException` để Gateway và Frontend nhận diện được mã lỗi JSON Problem Details chuẩn RFC 7807.

---

## 2. Tiêu chuẩn Frontend (React 19 & TypeScript)

1. **Kiểu dữ liệu (Strict TypeScript)**:
   - Tuyệt đối tránh sử dụng kiểu `any`. Mọi response từ API phải có interface/type tương ứng trong thư mục `src/types/`.
2. **Styling với Tailwind CSS v4**:
   - Dự án sử dụng `@tailwindcss/vite` v4. CSS toàn cục được khai báo tại `src/index.css` thông qua `@import 'tailwindcss';`.
   - Sử dụng các utility class trực tiếp trên thẻ JSX, tránh style inline hoặc CSS ad-hoc.
3. **Giao tiếp API**:
   - Mọi request HTTP phải sử dụng Axios instance từ `src/services/apis.ts`. Không gọi trực tiếp `axios.get()` tự do nhằm đảm bảo token JWT và cơ chế refresh token của Keycloak luôn được tự động đính kèm qua interceptor.

---

## 3. Quy trình Git (Git Workflow)

### 3.1 Nhánh (Branches)
- `main` / `master`: Nhánh mã nguồn production ổn định nhất.
- `staging`: Nhánh triển khai tự động lên môi trường Staging (Azure VM + Vercel).
- `feature/<tên-tính-năng>`: Nhánh phát triển tính năng mới (ví dụ: `feature/post-reactions`).
- `fix/<tên-lỗi>`: Nhánh sửa lỗi nóng (ví dụ: `fix/staging-config`).

### 3.2 Quy chuẩn Commit Message (Conventional Commits)
Sử dụng format chuẩn: `<type>(<scope>): <mô tả ngắn>`
- `feat`: Tính năng mới (ví dụ: `feat(feed): implement redis zset timeline caching`)
- `fix`: Sửa lỗi (ví dụ: `fix(gateway): resolve cors origin for vercel frontend`)
- `docs`: Cập nhật tài liệu (ví dụ: `docs: restructure architecture documentation`)
- `infra`: Thay đổi hạ tầng Docker/Nginx/Scripts (ví dụ: `infra: tune mariadb connection pool for rds`)
- `chore`: Cập nhật dependencies, build tools, cấu hình CI/CD.
