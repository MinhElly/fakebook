# Hướng dẫn & Bảng lệnh vận hành dự án Fakebook

## 1. Tổng quan & Cách hệ thống hoạt động

**Fakebook** là hệ thống mạng xã hội được xây dựng theo kiến trúc Microservices (sử dụng JHipster 9.3.0, Java 21, Spring Boot, React Vite).

### Mô hình hoạt động tổng quát:
```text
Trình duyệt (Browser)
       |
       v
React/Vite (9000) ---> Gateway (8080) ----> Consul (8500) tìm địa chỉ
                             |
                             +---> Auth Service (8081)    <---> Keycloak (9080)
                             +---> User Service (8082)    <---> MariaDB (3307)
                             +---> Post Service (8083)    <---> Kafka (9092)
                             +---> Media Service (8084)
                             +---> Comment Service (8085)
                             +---> Feed Service (8086)    <---> Redis (6379)
```

### Cách các thành phần phối hợp:
- **Xác thực & Phân quyền (Keycloak)**: Keycloak quản lý tài khoản và phát hành OAuth2 Token. Gateway & các microservice xác thực request dựa trên Token này.
- **Khám phá dịch vụ (Consul)**: Các microservice tự đăng ký địa chỉ với Consul. Gateway tra cứu Consul để chuyển tiếp request `/services/{service-name}/...` đúng chỗ.
- **Giao tiếp sự kiện (Kafka)**: Xử lý các tác vụ bất đồng bộ (ví dụ: tạo bài viết -> bắn event qua Kafka -> Feed Service nhận event để cập nhật bảng tin).
- **Lưu trữ & Cache**: Mỗi microservice có 1 database MariaDB riêng chạy chung container port `3307`. Feed Service dùng thêm **Redis** để cache bảng tin.
- **Phân tách Tiến trình (Host JVM vs Docker)**:
  - **Hạ tầng (MariaDB, Keycloak, Consul, Kafka, Redis, Zipkin)**: Chạy trong Docker Containers.
  - **Mã nguồn App (Backend Java & Frontend React)**: Chạy **trực tiếp trên máy Host (JVM local)** qua `.\mvnw.cmd` và `.\npmw.cmd`, KHÔNG chạy trong Docker khi dev để phản hồi nhanh và tiết kiệm tài nguyên.

---

## 2. Bảng tổng hợp Port & Dịch vụ

### Dịch vụ Ứng dụng & Hạ tầng

| Thành phần | Port | Vai trò chính |
| :--- | :---: | :--- |
| **Frontend Web** | `9000` | Giao diện React Vite (chế độ Dev) |
| **Gateway** | `8080` | Định tuyến API Gateway & Backend phục vụ web |
| **Auth Service** | `8081` | Nghiệp vụ xác thực & Tích hợp Keycloak |
| **User Service** | `8082` | Quản lý profile, kết bạn, theo dõi |
| **Post Service** | `8083` | Quản lý bài đăng, reaction |
| **Media Service** | `8084` | Quản lý metadata hình ảnh/video |
| **Comment Service** | `8085` | Quản lý bình luận |
| **Feed Service** | `8086` | Bảng tin (kết hợp Redis cache) |
| **Keycloak** | `9080` | Identity Provider (OAuth2/OIDC login) |
| **Consul UI** | `8500` | Quản lý Service Discovery & Config tập trung |
| **Kafka UI / Broker** | `8085` / `9092` | Giao diện quản lý Kafka Topic & Event bus |
| **MariaDB** | `3307` | Database server dùng chung (tự tạo 7 DB riêng) |
| **Redis** | `6379` | In-memory cache cho Feed Service |
| **Zipkin UI** | `9411` | Distributed Tracing (theo dõi luồng request) |

---

## 3. Quy trình khởi động hệ thống (3 bước)

1. **Bước 1: Khởi động Hạ tầng Docker**: Đảm bảo Consul, Keycloak, MariaDB sẵn sàng trước.
2. **Bước 2: Khởi động Backend Services**: Chạy Gateway trước, sau đó khởi động các microservice cần thiết.
3. **Bước 3: Khởi động Frontend**: Chạy dev server Vite để mở giao diện `http://localhost:9000`.

> [!TIP]
> **Mẹo PowerShell**: Luôn truyền tham số `'-Dspring-boot.run.arguments=--spring.docker.compose.enabled=false'` khi chạy `.\mvnw.cmd` để tắt tính năng Spring tự bật container trùng lặp, giúp tiết kiệm đáng kể RAM và CPU.

---

## 4. Bảng tổng hợp tất cả các lệnh vận hành (Quick Reference)

| Tác vụ | Lệnh thực thi (PowerShell) | Vị trí / Ghi chú |
| :--- | :--- | :--- |
| **1. Khởi động TOÀN BỘ Hạ tầng** | `.\infrastructure\start.ps1` | Thư mục gốc `C:\Code\fakebook` *(Khuyên dùng 👍)* |
| **Khởi động Hạ tầng (Docker)** | `docker compose -f infrastructure/docker-compose.yml up -d --wait` | Lệnh gốc Docker |
| **2. Khởi động HẠ TẦNG LẺ (Tiết kiệm RAM)** | | |
| 🔹 *Chỉ bật Keycloak* | `docker compose -f infrastructure/keycloak/docker-compose.yml up -d` | Dành cho test Đăng nhập |
| 🔹 *Chỉ bật Consul* | `docker compose -f infrastructure/consul/docker-compose.yml up -d` | Dành cho test Service Discovery |
| 🔹 *Chỉ bật Kafka & UI* | `docker compose -f infrastructure/kafka/docker-compose.yml up -d` | Dành cho test Event Bus |
| 🔹 *Chỉ bật Zipkin* | `docker compose -f infrastructure/tracing/docker-compose.yml up -d` | Dành cho test Distributed Tracing |
| **3. Chạy Backend Gateway** | `cd gateway; .\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.docker.compose.enabled=false'` | Chạy Gateway (Port 8080) |
| **4. Cài dependency frontend (lần đầu / khi package thay đổi)** | `cd gateway; .\npmw.cmd install` | Chỉ cần chạy khi chưa có `node_modules` hoặc `package.json` / `package-lock.json` thay đổi |
| **5. Chạy Frontend Web** | `cd gateway; .\npmw.cmd run start` | Chạy Vite Web (Port 9000); không cần `npm install` mỗi lần |
| **6. Chạy Microservice lẻ** | `cd <moduleName>; .\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.docker.compose.enabled=false'` | Thay `<moduleName>` bằng `userService`, `postService`, `authService`, v.v. |
| **7. Tắt hạ tầng (Giữ data)** | `.\infrastructure\stop.ps1` | Dừng container hạ tầng |
| **7. Xóa sạch hạ tầng & Volume** | `docker compose -f infrastructure/docker-compose.yml down -v` | ⚠️ Xóa sạch DB & dữ liệu Keycloak |
| **8. Build & Test Backend** | `.\mvnw.cmd verify` | Chạy tại thư mục từng module |
| **9. Test Frontend** | `cd gateway; .\npmw.cmd test` | Test Jest/Vitest của Gateway |
| **10. Kiểm tra các Port đang bận** | `Get-NetTCPConnection -State Listen \| Where-Object LocalPort -in 3307,6379,8080,8082,8500,9080` | Tránh lỗi Port already allocated |

---

## 5. Bảng tra cứu Trang quản trị & Mật khẩu mặc định

| Giao diện / Trang Web | Địa chỉ URL | Tài khoản / Mật khẩu mặc định |
| :--- | :--- | :--- |
| **Ứng dụng Fakebook Web** | `http://localhost:9000` | User mẫu: `admin` / `admin` hoặc `user` / `user` |
| **Keycloak Admin Console** | `http://localhost:9080` | Quản trị viên Keycloak: `admin` / `admin` |
| **Consul Dashboard** | `http://localhost:8500` | Xem danh sách service đăng ký |
| **Kafka UI** | `http://localhost:8085` | Xem Topic, Message & Consumer Groups |
| **Zipkin UI** | `http://localhost:9411` | Theo dõi Tracing & thời gian phản hồi API |
| **Gateway Health Check** | `http://localhost:8080/management/health` | Kiểm tra trạng thái hoạt động Gateway |
