# Testing Strategy (Chiến lược Kiểm thử)

Tài liệu này đặc tả các tầng kiểm thử chất lượng mã nguồn trong Fakebook từ cấp độ Unit Test, Integration Test đến End-to-End Smoke Test.

---

## 1. Kim tự tháp kiểm thử (Testing Pyramid)

```text
               / \
              /   \      E2E Smoke Test (normal-user PowerShell + legacy Bash)
             /     \
            /───────\    Integration Test (Spring Boot IT & Testcontainers)
           /         \
          /───────────\  Unit Test (JUnit 5, Mockito, Vitest)
```

---

## 2. Các cấp độ Kiểm thử trong Dự án

### 2.1 Unit Tests (Kiểm thử Đơn vị)
- **Backend (Java)**:
  - Sử dụng **JUnit 5**, **AssertJ** và **Mockito**.
  - Kiểm thử logic nghiệp vụ tại các class `*Service` mà không cần nạp toàn bộ Spring Context (chạy cực nhanh).
  - Lệnh chạy:
    ```bash
    ./mvnw test
    ```
- **Frontend (React)**:
  - Sử dụng **Vitest** và **React Testing Library**.
  - Kiểm thử render component và các hàm tiện ích trong thư mục `src/utils/`.
  - Lệnh chạy:
    ```bash
    cd frontend && npm test
    ```

### 2.2 Integration Tests (Kiểm thử Tích hợp với Testcontainers)
- Sử dụng **Spring Boot Test** (`@SpringBootTest`).
- Kiểm thử các luồng tương tác với Kafka thông qua **Testcontainers** (`KafkaTestContainer.java`): Tự động bật container Kafka tạm thời trên Docker để kiểm tra tính toàn vẹn của việc gửi/nhận message.
- Lệnh chạy:
  ```bash
  ./mvnw verify -Pdev
  ```

### 2.3 End-to-End Smoke Tests (Kiểm thử Khói Toàn diện)

Gate ưu tiên là `scripts/e2e-normal-user-smoke-test.ps1`. Script yêu cầu token của user thật qua `FAKEBOOK_USER_ACCESS_TOKEN`, sau đó kiểm tra `/api/account`, profile `/me`, tạo/đọc/xóa post, comment summaries, chờ post xuất hiện tại `/api/feed/me` và xác minh một trace Zipkin gồm Gateway/User/Post/Comment/Feed.

```powershell
$env:FAKEBOOK_USER_ACCESS_TOKEN = '<normal-user-access-token>'
.\scripts\e2e-normal-user-smoke-test.ps1 `
  -BaseUrl 'http://localhost:8080' `
  -ZipkinUrl 'http://localhost:9411'
```

`scripts/e2e-smoke-test.sh` là smoke cũ dùng client credentials `internal`. Nó chỉ kiểm tra HTTP status, không chứng minh normal-user ownership, `/api/account`, nội dung feed hay trace xuyên service. Tại source hiện tại script còn gọi generic `GET /api/user-profiles`, trong khi route này được method-security khóa cho admin; vì vậy không dùng kết quả script cũ làm gate phát hành cho tới khi contract/token được sửa.

### 2.4 Giới hạn bằng chứng

- Unit/Integration test pass không chứng minh môi trường local/staging đang chạy.
- Health 200 không chứng minh authenticated business flow.
- Compose render không chứng minh external Keycloak/Cloudinary/RDS hoặc recovery.
- Circuit Breaker chỉ hoàn tất khi quan sát đủ `OPEN -> HALF_OPEN -> CLOSED` qua Gateway.
