# Local Development Setup Guide (Hướng dẫn chạy môi trường cục bộ)

Tài liệu này cung cấp hướng dẫn từng bước từ lúc clone repository đến khi toàn bộ hệ thống Fakebook chạy ổn định trên máy tính cá nhân.

---

## 1. Yêu cầu môi trường tiên quyết (Prerequisites)

| Công cụ | Phiên bản yêu cầu | Kiểm tra phiên bản |
| :--- | :--- | :--- |
| **Java Development Kit (JDK)** | **Java 21** (khuyên dùng Eclipse Temurin 21) | `java -version` |
| **Node.js** | **v22.x** LTS | `node -v` |
| **npm** | Đi kèm Node 22 | `npm -v` |
| **Docker Desktop / Docker Engine**| Docker 24+ & Docker Compose v2.20+ | `docker compose version` |
| **Hệ điều hành / Shell** | Windows (PowerShell 7+ hoặc Windows PowerShell 5.1) / Linux / macOS | `pwsh` hoặc `bash` |

---

## 2. Mô hình phân tách tiến trình (Host JVM vs Docker Infrastructure)

Để tối ưu hóa thời gian khởi động, hỗ trợ debug trực tiếp trên IDE (IntelliJ / VS Code) và tiết kiệm tài nguyên máy tính:
- **Hạ tầng nền tảng (Database, Broker, IAM, Registry, Cache)**: Chạy trong Docker container (`infrastructure/docker-compose.yml`).
- **Mã nguồn ứng dụng (Spring Boot Microservices & React Frontend)**: Chạy trực tiếp trên máy Host (JVM & Node.js).

---

## 3. Quy trình khởi động 4 bước

### Bước 1: Chuẩn bị biến môi trường (Environment Setup)

Tại thư mục gốc dự án (`C:\Code\fakebook`):

```powershell
# Tạo file .env từ template mẫu
Copy-Item infrastructure/.env.example infrastructure/.env

# Nạp các biến môi trường vào session PowerShell hiện tại
. .\infrastructure\load-dev-env.ps1
```

*(Trên Linux / macOS):*
```bash
cp infrastructure/.env.example infrastructure/.env
set -a; source infrastructure/.env; set +a
```

---

### Bước 2: Khởi động Hạ tầng Docker (Infrastructure)

Chạy script khởi động hạ tầng đã được tối ưu hóa:

```powershell
.\infrastructure\start.ps1
```

*Hoặc chạy lệnh Docker Compose trực tiếp:*
```bash
docker compose --project-directory infrastructure -f infrastructure/docker-compose.yml up -d --wait
```

Lệnh này sẽ khởi động và đợi cho đến khi các container sau đạt trạng thái **Healthy**:
- `fakebook-mariadb` (port `3307`) - Tự động tạo 8 database schemas qua init script.
- `fakebook-keycloak` (port `9080`) - Import sẵn Realm `jhipster`.
- `fakebook-keycloak-config` - Cấu hình Google IDP (nếu có).
- `fakebook-consul` (port `8500`) - Service Registry.
- `fakebook-consul-config-loader` - Nạp file YAML cấu hình tập trung vào Consul KV.
- `fakebook-kafka` (port `9092`) & `fakebook-kafka-ui` (port `8088`).
- `fakebook-redis` (port `6379`).
- `fakebook-zipkin` (port `9411`).

Kiểm tra trạng thái các container:
```powershell
docker compose --project-directory infrastructure -f infrastructure/docker-compose.yml ps
```

---

### Bước 3: Khởi động Backend Microservices

> [!IMPORTANT]
> Luôn truyền tham số `'-Dspring-boot.run.arguments=--spring.docker.compose.enabled=false'` khi chạy `./mvnw` để ngăn Spring Boot tự động bật thêm container trùng lặp.
> **Khởi động Gateway trước**, sau đó khởi động các service cần thiết.

Mở các tab terminal riêng biệt cho từng service:

#### 1. Khởi động API Gateway (Port 8080)
```powershell
cd gateway
.\mvnw.cmd spring-boot:run
```

#### 2. Khởi động User Service (Port 8082)
```powershell
cd userService
.\mvnw.cmd spring-boot:run
```

#### 3. Khởi động Post Service (Port 8083)
```powershell
cd postService
.\mvnw.cmd spring-boot:run
```

#### 4. Khởi động Feed Service (Port 8086)
```powershell
cd feedService
.\mvnw.cmd spring-boot:run
```

#### 5. Khởi động Comment Service (Port 8085)
```powershell
cd commentService
.\mvnw.cmd spring-boot:run
```

#### 6. Khởi động Media Service (Port 8084)
```powershell
cd mediaService
.\mvnw.cmd spring-boot:run
```

*(Trên Linux / macOS, thay `.\mvnw.cmd` bằng `./mvnw`)*

---

### Bước 4: Khởi động Frontend Web

Tại một terminal mới:

```bash
cd frontend
npm install
npm run dev
```

Frontend sẽ chạy tại địa chỉ: `http://localhost:5173` (hoặc cổng hiển thị trên terminal).

---

## 4. Danh mục Port & Tài khoản đăng nhập mặc định

| Giao diện / Trang quản trị | Địa chỉ truy cập | Tài khoản / Mật khẩu mặc định |
| :--- | :--- | :--- |
| **Fakebook Web App** | `http://localhost:5173` | Người dùng mẫu: `admin` / `admin` hoặc `user` / `user` |
| **Keycloak Admin Console** | `http://localhost:9080` | `admin` / `admin` |
| **Consul UI** | `http://localhost:8500` | Xem danh sách các microservice đã đăng ký |
| **Kafka UI** | `http://localhost:8088` | Xem Topics (`post-events`), Messages & Consumer Groups |
| **Zipkin Tracing UI** | `http://localhost:9411` | Theo dõi Distributed Tracing và độ trễ request |
| **Gateway Health Check** | `http://localhost:8080/management/health` | Kiểm tra trạng thái Gateway |

---

## 5. Dừng hệ thống

- **Dừng hạ tầng Docker (vẫn giữ dữ liệu Database)**:
  ```powershell
  .\infrastructure\stop.ps1
  ```
- **Dừng và xóa sạch dữ liệu Database & Keycloak (Fresh Reset)**:
  ```bash
  docker compose --project-directory infrastructure -f infrastructure/docker-compose.yml down -v
  ```
