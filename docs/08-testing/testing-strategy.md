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

Các công cụ smoke test đã được loại bỏ khỏi repository; hiện chưa có runner E2E thay thế. Gate phát hành vẫn yêu cầu normal-user login/callback/logout, /api/account, profile /me, tạo/đọc/xóa post, comment summaries, feed propagation và một trace Zipkin gồm Gateway/User/Post/Comment/Feed. Kiểm tra thủ công qua Gateway và lưu evidence đã mask; health check hoặc client credentials không chứng minh luồng người dùng.

### 2.4 Giới hạn bằng chứng

- Unit/Integration test pass không chứng minh môi trường local/staging đang chạy.
- Health 200 không chứng minh authenticated business flow.
- Compose render không chứng minh external Keycloak/Cloudinary/RDS hoặc recovery.
- Circuit Breaker chỉ hoàn tất khi quan sát đủ `OPEN -> HALF_OPEN -> CLOSED` qua Gateway.
