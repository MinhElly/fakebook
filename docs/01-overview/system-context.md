# System Context (Bối cảnh hệ thống)

Tài liệu này mô tả sơ đồ ngữ cảnh hệ thống (C4 System Context) và các tác nhân tương tác với hệ thống Fakebook.

---

## 1. Tác nhân (Actors)

| Tác nhân | Mô tả vai trò | Giao diện tương tác |
| :--- | :--- | :--- |
| **Người dùng cuối (End User)** | Người dùng duyệt bảng tin, đăng bài, bình luận, kết bạn, upload ảnh. | Trình duyệt Web (React SPA) |
| **Quản trị viên (Admin)** | Quản lý người dùng, cấu hình Realm, client Keycloak, xem metrics hệ thống. | Keycloak Admin Console (`:9080`), Consul UI (`:8500`), Kafka UI (`:8088`), Zipkin UI (`:9411`) |
| **Hệ thống thứ 3 (Google OAuth2)** | Cung cấp định danh mạng xã hội cho tính năng "Đăng nhập với Google". | Google Identity Services API |
| **Cloudinary CDN** | Dịch vụ đám mây lưu trữ và tối ưu hóa hình ảnh/video thực tế. | Cloudinary REST API |

---

## 2. Sơ đồ bối cảnh hệ thống (Mermaid)

```mermaid
flowchart TB
    user["Người dùng<br/><i>(Lướt feed, đăng bài, tương tác bạn bè)</i>"]
    admin["Quản trị viên<br/><i>(Quản lý hệ thống, giám sát, IAM)</i>"]

    subgraph FakebookSystem["Hệ thống Fakebook"]
        frontend["Frontend SPA<br/><i>React 19 / Vite / Tailwind CSS</i>"]
        gateway["API Gateway<br/><i>Spring Cloud Gateway WebFlux (TokenRelay)</i>"]
        backend["Backend Microservices<br/><i>Spring Boot / Java 21 (User, Post, Feed...)</i>"]
        keycloak["Keycloak IAM<br/><i>Keycloak 26 (OAuth2 / OIDC)</i>"]
    end

    google["Google OAuth2<br/><i>(Nhà cung cấp danh tính ngoài)</i>"]
    cloudinary["Cloudinary CDN<br/><i>(Lưu trữ & phân phối media)</i>"]

    user -->|"Truy cập & Tương tác (HTTPS)"| frontend
    frontend -->|"Đăng nhập, lấy Token JWT (PKCE)"| keycloak
    frontend -->|"Gửi API request kèm Bearer JWT"| gateway
    gateway -->|"Chuyển tiếp request (TokenRelay)"| backend
    backend -->|"Upload / Quản lý media"| cloudinary
    keycloak -->|"Ủy quyền Google SSO"| google
    admin -->|"Quản lý User & Realm"| keycloak
    admin -->|"Kiểm tra Metrics & Actuator"| gateway
```

---

## 3. Ranh giới mạng & Giao tiếp

1. **Vùng công khai (Public Internet)**:
   - **Frontend App**: Truy cập trực tiếp từ máy khách (Web Browser).
   - **Keycloak IAM**: Công khai các endpoint OIDC (`/realms/jhipster/protocol/openid-connect/*`) để client thực hiện xác thực và refresh token.
   - **API Gateway**: Công khai endpoint `/services/**` và `/api/**` để nhận các yêu cầu nghiệp vụ.
2. **Vùng nội bộ (Private / Docker Network)**:
   - Toàn bộ các microservices (`userService`, `postService`, `commentService`, `mediaService`, `feedService`, `authService`) không mở cổng trực tiếp ra ngoài Internet trong môi trường Staging/Production. Mọi luồng vào đều bắt buộc phải qua Gateway hoặc Nginx.
   - Các dịch vụ nền tảng: **Consul**, **Kafka Broker**, **MariaDB Server**, **Redis**, **Zipkin** nằm hoàn toàn trong mạng nội bộ.
