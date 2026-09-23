# Project Structure & Centralized Configuration

Tài liệu này giải thích cấu trúc cây thư mục monorepo của Fakebook và cơ chế nạp cấu hình tập trung thông qua HashiCorp Consul.

---

## 1. Cấu trúc thư mục Monorepo

```text
fakebook/
├── .github/                      # CI/CD Workflows (GitHub Actions)
│   └── workflows/ci.yml         # Pipeline build frontend & Jib push backend image lên GHCR
│
├── authService/                  # JHipster skeleton microservice (Port 8081)
├── commentService/               # Comment & PostCache microservice (Port 8085)
├── feedService/                  # Personalized Feed & Redis Fanout microservice (Port 8086)
├── gateway/                      # Spring Cloud Gateway WebFlux (Port 8080)
├── mediaService/                 # Cloudinary Media upload & metadata microservice (Port 8084)
├── postService/                  # Post & Reactions microservice (Port 8083)
├── userService/                  # UserProfile, Friends & Follows microservice (Port 8082)
│
├── frontend/                     # React 19 / Vite / Tailwind CSS web application
│   ├── public/config/            # Runtime config (app-config.json) nạp lúc khởi chạy
│   ├── src/                      # Source code React components, pages, stores, services
│   ├── Dockerfile                # Multi-stage Dockerfile cho frontend (Node builder -> Nginx)
│   └── package.json              # Quản lý dependencies (sử dụng npm)
│
├── infrastructure/               # Toàn bộ cấu hình hạ tầng Dev và Staging
│   ├── .env.example              # Mẫu biến môi trường cho môi trường Local Dev
│   ├── .env.staging.example      # Mẫu biến môi trường cho môi trường Staging VPS
│   ├── docker-compose.yml        # Hạ tầng chạy Local (MariaDB, Keycloak, Consul, Kafka, Redis, Zipkin)
│   ├── docker-compose-staging.yml# Triển khai toàn bộ hệ thống trên VM Staging
│   ├── start.ps1 / stop.ps1      # Script PowerShell bật/tắt nhanh hạ tầng Dev
│   ├── load-dev-env.ps1          # Script nạp biến môi trường từ .env vào PowerShell session
│   ├── config/
│   │   └── central-server-config/# Cấu hình tập trung nạp vào Consul KV Store
│   ├── keycloak/                 # Scripts tự động cấu hình Internal Client & Google IDP
│   ├── mariadb/init/             # Script SQL tự động khởi tạo 8 schema database
│   ├── nginx/                    # Nginx reverse proxy config & certbot SSL challenge
│   └── certs/                    # Chứng chỉ RDS CA (global-bundle.pem)
│
├── performance/                  # Kịch bản kiểm thử hiệu năng
│   └── k6/http-baseline.js       # Load test baseline cho API Gateway & Microservices
│
├── scripts/                      # Utility scripts
│   └── e2e-smoke-test.sh         # Script kiểm thử khói (Smoke Test) tự động toàn bộ luồng E2E
│
├── docs/                         # Toàn bộ tài liệu kỹ thuật của dự án
├── fakebook-master.jdl           # JHipster Domain Language file mô tả toàn bộ entity
└── README.md                     # Tài liệu tổng quan dự án tại root
```

---

## 2. Kiến trúc cấu hình tập trung (Consul Centralized Configuration)

Fakebook sử dụng **HashiCorp Consul** làm kho lưu trữ cấu hình tập trung thay vì Spring Cloud Config Server:

```text
infrastructure/config/central-server-config/*.yml
          │
          ▼
   consul-config-loader (Docker Container)
          │
          ▼
   Consul KV Store (:8500)
          │
          ▼
   Gateway & Microservices (Spring Cloud Consul Config bootstrap)
```

### 2.1 Quy tắc đặt tên Key trong Consul KV
Với định dạng YAML và ký tự phân cách profile là dấu gạch ngang (`-`), các file trong thư mục `infrastructure/config/central-server-config/` được ánh xạ tương ứng vào Consul KV:

```text
application.yml           ──► config/application/data
application-dev.yml       ──► config/application-dev/data
application-staging.yml   ──► config/application-staging/data
gateway-dev.yml           ──► config/gateway-dev/data
gateway-staging.yml       ──► config/gateway-staging/data
userService-dev.yml       ──► config/userService-dev/data
...
```

> [!NOTE]
> Tên file cấu hình service-specific phải khớp chính xác với thuộc tính `spring.application.name` (có phân biệt hoa/thường, ví dụ `userService-dev.yml`), trong khi tên đăng ký trên Consul Service Discovery luôn là chữ thường (`userservice`).

### 2.2 Thứ tự ưu tiên nạp cấu hình (Configuration Precedence)
Khi một service khởi động, Spring Boot nạp cấu hình theo thứ tự ưu tiên từ thấp đến cao:
1. `bootstrap.yml` trong source code (chỉ chứa thông tin kết nối tới Consul).
2. `config/application/data` trong Consul KV (cấu hình chung cho mọi profile).
3. `config/application-<profile>/data` trong Consul KV (cấu hình chung theo môi trường dev/staging).
4. `config/<serviceName>-<profile>/data` trong Consul KV (cấu hình riêng của từng service).
5. Biến môi trường hệ thống (Environment Variables) hoặc cờ dòng lệnh CLI (ghi đè tất cả cấu hình trên).

Cơ chế Config Watch được cố ý tắt (`watch.enabled: false`) để đảm bảo tính ổn định; khi thay đổi file cấu hình trong `central-server-config`, chỉ cần restart lại service bị ảnh hưởng.
