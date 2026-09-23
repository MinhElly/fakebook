# Development Environment Deployment (Triển khai Môi trường Cục bộ)

Tài liệu này hướng dẫn chi tiết cách vận hành, tối ưu tài nguyên và triển khai các thành phần trên môi trường phát triển cục bộ (**Local Development**).

---

## 1. Khởi động hạ tầng linh hoạt theo nhu cầu (Resource Saving)

Nếu máy tính phát triển có giới hạn về RAM/CPU, bạn không bắt buộc phải bật toàn bộ hạ tầng mà có thể bật từng phần độc lập:

| Nhu cầu kiểm thử | Lệnh khởi động Docker Compose | Cổng hoạt động |
| :--- | :--- | :--- |
| **1. Toàn bộ hạ tầng** | `.\infrastructure\start.ps1` | `3307`, `9080`, `8500`, `9092`, `8088`, `6379`, `9411` |
| **2. Chỉ cần Keycloak (Test Đăng nhập/OAuth2)** | `docker compose -f infrastructure/keycloak/docker-compose.yml up -d` | `9080`, `9443` |
| **3. Chỉ cần Consul (Test Service Discovery/Config)** | `docker compose -f infrastructure/consul/docker-compose.yml up -d` | `8500`, `8300` |
| **4. Chỉ cần Kafka & UI (Test Event Streaming)** | `docker compose -f infrastructure/kafka/docker-compose.yml up -d` | `9092`, `8088` |
| **5. Chỉ cần Zipkin (Test Distributed Tracing)** | `docker compose -f infrastructure/tracing/docker-compose.yml up -d` | `9411` |

---

## 2. Các Spring Profiles trong môi trường Dev

Khi chạy trên máy host, các microservice được kích hoạt với profile:
```text
spring.profiles.active = dev,api-docs
```

- **`dev`**:
  - Bật Hibernate SQL logging (`DEBUG`) để theo dõi câu lệnh SQL chạy thực tế.
  - Cấu hình Liquibase context: `dev` (và `faker` nếu cần nạp dữ liệu mẫu).
  - Tracing sampling rate: `1.0` (ghi nhận 100% trace request gửi về Zipkin `http://localhost:9411`).
  - CORS cho phép mọi origin cục bộ: `http://localhost:5173`, `http://localhost:9000`, `http://localhost:8080`.
- **`api-docs`**:
  - Bật Swagger UI và OpenAPI generator (`/v3/api-docs`).

---

## 3. Quy trình làm việc với cấu hình tập trung trên Dev

1. Mọi giá trị cấu hình chung hoặc cấu hình riêng của service được lưu trong:
   ```text
   infrastructure/config/central-server-config/
   ├── application-dev.yml
   ├── gateway-dev.yml
   ├── userService-dev.yml
   └── ...
   ```
2. Khi sửa file cấu hình trong thư mục này:
   - File sẽ được container `fakebook-consul-config-loader` tự động nạp vào Consul KV Store.
   - Do tính năng Config Watch bị tắt để tránh xung đột runtime, bạn chỉ cần **Restart service tương ứng trên IDE/Terminal** để service nạp lại giá trị mới lúc bootstrap.
