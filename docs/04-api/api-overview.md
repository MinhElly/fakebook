# API Overview & Conventions

Tài liệu này quy định chuẩn thiết kế API RESTful và danh mục các endpoint API cốt lõi trong hệ thống Fakebook.

---

## 1. Chuẩn URL Endpoint

Tất cả các lời gọi API từ bên ngoài (Frontend SPA hoặc bên thứ 3) đều phải đi qua **API Gateway** với tiền tố định tuyến:

```text
/services/{serviceId}/api/{resource}
```

Trong đó:
- `{serviceId}` là tên định danh lowercase của microservice đã đăng ký trên Consul:
  - `userservice`
  - `postservice`
  - `commentservice`
  - `mediaservice`
  - `feedservice`
  - `authservice`
- Tiền tố `/services/{serviceId}` sẽ được Gateway cắt bỏ trước khi chuyển tới microservice đích (Ví dụ: `GET /services/postservice/api/posts` -> `GET /api/posts`).

---

## 2. Chuẩn Header & Xác thực

- **Authorization**: Mọi request tới các API yêu cầu đăng nhập phải gửi kèm header:
  ```text
  Authorization: Bearer <Keycloak_Access_Token_JWT>
  ```
- **Content-Type**: `application/json` (trừ endpoint upload media sử dụng `multipart/form-data`).
- **Phân trang (Pagination Headers)**:
  - Request parameters: `page` (bắt đầu từ 0), `size`, `sort=propertyName,(asc|desc)`.
  - Response headers trả về từ microservice:
    - `X-Total-Count`: Tổng số lượng bản ghi thỏa mãn điều kiện.
    - `Link`: Đường dẫn tới trang tiếp theo (next), trang trước (prev), trang đầu (first), trang cuối (last) theo chuẩn RFC 5988.

---

## 3. Bảng tổng hợp các Endpoint API chính

### 3.1 User Service (`userservice`)

| Method | Endpoint | Mô tả | Quyền truy cập |
| :--- | :--- | :--- | :--- |
| `GET` | `/services/userservice/api/user-profiles` | Lấy danh sách hồ sơ người dùng | Authenticated |
| `GET` | `/services/userservice/api/user-profiles/{id}` | Lấy chi tiết hồ sơ theo ID | Authenticated |
| `PUT` | `/services/userservice/api/user-profiles/{id}` | Cập nhật thông tin cá nhân | Owner |
| `GET` | `/services/userservice/api/user-profiles/friend-suggestions` | Lấy danh sách gợi ý bạn bè (dựa trên bạn chung) | Authenticated |
| `GET` | `/services/userservice/api/friendships` | Lấy danh sách bạn bè của người dùng | Authenticated |
| `POST` | `/services/userservice/api/friend-requests` | Gửi lời mời kết bạn | Authenticated |
| `PUT` | `/services/userservice/api/friend-requests/{id}` | Chấp nhận / từ chối lời mời kết bạn | Receiver |
| `POST` | `/services/userservice/api/user-follows` | Theo dõi một người dùng khác | Authenticated |

### 3.2 Post Service (`postservice`)

| Method | Endpoint | Mô tả | Quyền truy cập |
| :--- | :--- | :--- | :--- |
| `GET` | `/services/postservice/api/posts` | Lấy danh sách bài viết (phân trang) | Authenticated |
| `GET` | `/services/postservice/api/posts/{id}` | Xem chi tiết bài viết | Authenticated |
| `POST` | `/services/postservice/api/posts` | Đăng bài viết mới (kích hoạt Kafka fanout) | Authenticated |
| `PUT` | `/services/postservice/api/posts/{id}` | Chỉnh sửa nội dung / quyền riêng tư bài viết | Owner |
| `DELETE` | `/services/postservice/api/posts/{id}` | Xóa bài viết (kích hoạt dọn dẹp feed & media) | Owner / Admin |
| `POST` | `/services/postservice/api/post-reactions` | Thả cảm xúc (LIKE, LOVE, WOW, SAD, ANGRY) | Authenticated |
| `DELETE` | `/services/postservice/api/post-reactions/{id}`| Hủy bỏ cảm xúc trên bài viết | Owner |

### 3.3 Comment Service (`commentservice`)

| Method | Endpoint | Mô tả | Quyền truy cập |
| :--- | :--- | :--- | :--- |
| `GET` | `/services/commentservice/api/comments/post/{postId}` | Lấy danh sách bình luận của bài viết | Authenticated |
| `POST` | `/services/commentservice/api/comments` | Tạo bình luận mới (validate qua PostCache) | Authenticated |
| `PUT` | `/services/commentservice/api/comments/{id}` | Chỉnh sửa bình luận | Owner |
| `DELETE` | `/services/commentservice/api/comments/{id}` | Xóa bình luận | Owner |
| `POST` | `/services/commentservice/api/comment-reactions` | Thả cảm xúc trên bình luận | Authenticated |

### 3.4 Media Service (`mediaservice`)

| Method | Endpoint | Mô tả | Quyền truy cập |
| :--- | :--- | :--- | :--- |
| `POST` | `/services/mediaservice/api/media/upload` | Tải file ảnh/video lên Cloudinary | Authenticated |
| `GET` | `/services/mediaservice/api/media/{id}` | Lấy thông tin URL & metadata của file | Authenticated |
| `DELETE` | `/services/mediaservice/api/media/{id}` | Xóa file | Owner / Admin |

### 3.5 Feed Service (`feedservice`)

| Method | Endpoint | Mô tả | Quyền truy cập |
| :--- | :--- | :--- | :--- |
| `GET` | `/services/feedservice/api/feeds` | Lấy bảng tin cá nhân hóa (từ Redis ZSet / MariaDB) | Authenticated |

---

## 4. Tài liệu OpenAPI / Swagger UI

Mỗi microservice đều tích hợp SpringDoc OpenAPI:
- Gateway tổng hợp OpenAPI: `http://localhost:8080/swagger-ui/index.html`
- Xem trực tiếp endpoint của từng service qua Gateway:
  - User Service Docs: `http://localhost:8080/services/userservice/v3/api-docs`
  - Post Service Docs: `http://localhost:8080/services/postservice/v3/api-docs`
  - Feed Service Docs: `http://localhost:8080/services/feedservice/v3/api-docs`
