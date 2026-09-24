# Project Overview

## 1. Giới thiệu Fakebook

**Fakebook** là một nền tảng mạng xã hội (Social Network) phân tán được xây dựng theo kiến trúc **Microservices** hiện đại, kết hợp giữa hệ sinh thái **Java 21 / Spring Boot** (sử dụng nền tảng JHipster 9.3.0) và giao diện người dùng **React 19 / TypeScript / Vite**.

Dự án mô phỏng các tính năng cốt lõi của mạng xã hội quy mô lớn:
- **Xác thực & Danh tính (IAM)**: Đăng nhập/đăng ký người dùng, phân quyền OAuth2/OIDC, SSO qua Google, xác thực token JWT thông qua Keycloak 26.
- **Hồ sơ & Mạng lưới bạn bè (Social Graph)**: Quản lý thông tin cá nhân, gửi lời mời kết bạn, chấp nhận/từ chối kết bạn, theo dõi (follow) người dùng và gợi ý kết bạn dựa trên bạn chung (mutual friends).
- **Đăng tải bài viết (Post & Reaction)**: Soạn thảo bài đăng với các chế độ hiển thị (PUBLIC, PRIVATE, FRIENDS), hỗ trợ tương tác cảm xúc (LIKE, LOVE, WOW, SAD, ANGRY).
- **Bình luận (Comments)**: Thảo luận trên bài viết, phản hồi bình luận phân cấp, đồng bộ dữ liệu bài viết qua Kafka read-model cache.
- **Quản lý đa phương tiện (Media Storage)**: Tải lên hình ảnh/video qua Cloudinary CDN, lưu trữ metadata và cơ chế tự động dọn dẹp (cleanup) media mồ côi qua Kafka event.
- **Bảng tin cá nhân hóa (Personalized News Feed)**: Kiến trúc Fan-out-on-write bất đồng bộ qua Kafka, lưu trữ dài hạn trên MariaDB và tăng tốc truy vấn bằng Redis In-Memory Sorted Sets.

---

## 2. Tech Stack tổng thể

| Tầng kiến trúc | Công nghệ & Thư viện | Vai trò |
| :--- | :--- | :--- |
| **Frontend** | React 19, Vite 8, TypeScript 5.7, Tailwind CSS v4, `keycloak-js`, Axios | Giao diện Single Page Application (SPA), xác thực PKCE |
| **API Gateway** | Spring Cloud Gateway (Reactive WebFlux), Spring Security OAuth2 | Reverse proxy, định tuyến động theo Consul, TokenRelay, CORS |
| **Backend Framework** | Java 21 (Temurin), Spring Boot 3.4.x, Spring Cloud 2024.x | Nền tảng thực thi microservices |
| **Identity Provider** | Keycloak 26.7.2 (Quay.io) | Quản lý Realm `jhipster`, OIDC / OAuth2, Google Identity Provider |
| **Service Registry & Config**| HashiCorp Consul 2.0.3 + `consul-config-loader` | Đăng ký dịch vụ, load YAML tập trung vào Consul KV Store |
| **Message Broker** | Apache Kafka Native 4.3.1 (KRaft mode) | Xử lý streaming sự kiện bất đồng bộ, Fan-out bảng tin, dọn dẹp media |
| **Databases** | MariaDB 12.3.3 (Local) / AWS RDS MariaDB (Staging) | Lưu trữ dữ liệu quan hệ theo mô hình Database-per-service |
| **Database Migration** | Liquibase | Quản lý schema versioning per service |
| **Cache & In-Memory** | Redis 8.10.1 (Redis Alpine) | Cache Spring `@Cacheable` cho User Service, Sorted Sets cho Feed Service |
| **Distributed Tracing** | OpenZipkin 3.6.1 + Micrometer Tracing | Thu thập và hiển thị trace request phân tán xuyên suốt các service |
| **Reverse Proxy / Ingress** | Nginx 1.27 Alpine | TLS Termination (Let's Encrypt), proxy pass Keycloak & Gateway |
| **Container & CI/CD** | Docker, Docker Compose, Jib Maven Plugin, GHCR, GitHub Actions | Đóng gói OCI image không cần Docker daemon, CI kiểm thử và push image |
| **Cloud Platforms** | Vercel (Frontend Staging), Azure VM (Backend / Infra Staging), AWS RDS (Database) | Môi trường triển khai Staging |

---

## 3. Kiến trúc Microservices ở mức cao

Hệ thống được thiết kế theo nguyên tắc:
1. **Database-per-Service**: Không có hai microservice nào được phép chia sẻ chung bảng dữ liệu. Mỗi service hoàn toàn sở hữu schema MariaDB của mình.
2. **Loosely Coupled via Events**: Các tác vụ nặng hoặc liên quan đến dữ liệu chéo (như bảng tin, dọn dẹp media) được giao tiếp bất đồng bộ thông qua Apache Kafka.
3. **Synchronous via API Gateway**: Toàn bộ giao tiếp từ bên ngoài đều phải đi qua Spring Cloud Gateway tại port `8080` (hoặc Nginx SSL tại staging) với token Bearer JWT hợp lệ.
4. **Resilient Inter-Service Calls**: Khi cần dữ liệu đồng bộ (như kiểm tra bạn bè, thông tin tác giả), các service sử dụng OpenFeign kèm cơ chế fallback và hybrid token relay.

---

## 4. Mục tiêu tài liệu này

Tài liệu trong thư mục `docs/` được biên soạn nhằm đảm bảo:
- **100% phản ánh đúng source code hiện tại**: Mọi port, endpoint, biến môi trường và lệnh chạy đều lấy trực tiếp từ mã nguồn thực tế.
- **Dễ dàng tiếp cận cho thành viên mới**: Chỉ cần làm theo hướng dẫn tại [local-setup.md](../03-development/local-setup.md), một lập trình viên mới có thể khởi chạy toàn bộ hệ thống trên máy cá nhân mà không gặp trở ngại.
- **Sẵn sàng vận hành (Production-Ready Mindset)**: Cung cấp đầy đủ hướng dẫn giám sát, runbook cứu hộ sự cố, và phân tích chi tiết môi trường Staging.
