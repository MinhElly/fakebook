# Testing Strategy (Chiến lược Kiểm thử)

Tài liệu này đặc tả các tầng kiểm thử chất lượng mã nguồn trong Fakebook từ cấp độ Unit Test, Integration Test đến End-to-End Smoke Test.

---

## 1. Kim tự tháp kiểm thử (Testing Pyramid)

```text
               / \
              /   \      E2E Smoke Test (scripts/e2e-smoke-test.sh)
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
- Sử dụng bash script tự động hóa `scripts/e2e-smoke-test.sh`.
- Kịch bản kiểm thử trực tiếp trên môi trường đang chạy:
  1. Lấy JWT Access Token từ Keycloak qua luồng Client Credentials (`internal`).
  2. Gọi qua Gateway lấy danh sách User Profiles (`/services/userservice/api/user-profiles`).
  3. Gọi qua Gateway tạo một bài viết mới (`/services/postservice/api/posts`).
  4. Gọi qua Gateway kiểm tra việc xuất hiện bài viết trên Feed (`/services/feedservice/api/feeds`).
- Lệnh chạy:
  ```bash
  ./scripts/e2e-smoke-test.sh http://localhost:8080
  ```
