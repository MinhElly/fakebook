# CI/CD Pipeline Documentation

Tài liệu này đặc tả toàn bộ quy trình Tích hợp liên tục (**CI**) và Phân phối liên tục (**CD**) được cấu hình trong repository Fakebook thông qua **GitHub Actions** (`.github/workflows/ci.yml`).

---

## 1. Sơ đồ Pipeline tổng thể

```mermaid
flowchart TD
    Push["Push / Pull Request tới<br/>main, staging, feature/*, fix/*"] --> Trigger["GitHub Actions Workflow: ci.yml"]
    
    Trigger --> Parallel["Khởi chạy đồng thời 2 Job"]
    
    subgraph Job1["Job: build-frontend"]
        NodeSetup["Set up Node.js 22 & Cache npm"] --> NpmCI["npm ci (trong thư mục frontend)"]
        NpmCI --> BuildFE["npm run build<br/>(Kiểm tra tính toàn vẹn bundle)"]
    end
    
    subgraph Job2["Job: build-backend-services (Matrix 6 Backend Components)"]
        JDK["Set up JDK 21 Temurin & Cache Maven"] --> MatrixInit["Matrix: gateway, userService,<br/>postService, mediaService, commentService, feedService"]
        
        MatrixInit --> Verify["./mvnw -ntp clean verify<br/>(unit + integration + Checkstyle)"]
        Verify --> CheckEvent{"Push vào nhánh được bảo vệ?"}
        CheckEvent -- "Có" --> JibBuild["Maven Jib: ./mvnw -ntp package jib:build -DskipTests<br/>- Đóng gói OCI container image sau verify<br/>- Gán tag sha-&lt;full-40-char-SHA&gt; và alias nhánh<br/>- Push lên GitHub Container Registry (ghcr.io)"]
        CheckEvent -- "Không / Pull Request" --> Verified["Chỉ giữ kết quả verify"]
    end
    
    Parallel --> Job1
    Parallel --> Job2
    
    Job1 --> Summary["Job: pipeline-summary<br/>Tổng hợp trạng thái thành công/thất bại"]
    Job2 --> Summary
```

---

## 2. Chi tiết các Jobs trong Pipeline

### 2.1 Job `build-frontend` (Kiểm thử giao diện)
- **Môi trường**: `ubuntu-latest`.
- **Node.js**: Phiên bản `22` (sử dụng cache npm dựa trên `frontend/package-lock.json`).
- **Nhiệm vụ**: Chạy `npm ci` và `npm run build` để đảm bảo code React/TypeScript không bị lỗi cú pháp, thiếu type hoặc lỗi import trước khi merge.

### 2.2 Job `build-backend-services` (Matrix Build OCI Image)
- **Cơ chế Matrix**: Chạy đồng thời 6 tác vụ độc lập cho Gateway và 5 microservices:
  - `gateway` -> `fakebook-gateway`
  - `userService` -> `fakebook-userservice`
  - `postService` -> `fakebook-postservice`
  - `mediaService` -> `fakebook-mediaservice`
  - `commentService` -> `fakebook-commentservice`
  - `feedService` -> `fakebook-feedservice`
- **Gate bắt buộc**: mọi Pull Request và push đều chạy `./mvnw -ntp clean verify` cho từng module.
- **Công nghệ đóng gói (Google Maven Jib)**:
  - Sau khi verify thành công trên push được bảo vệ, dự án dùng **Jib** (`./mvnw -ntp package jib:build -DskipTests`) thay vì `docker build`.
  - Ưu điểm: Đóng gói image OCI tiêu chuẩn trực tiếp từ bytecode Java mà không cần Docker daemon chạy trên runner, tận dụng cache tầng layer cực kỳ hiệu quả và tốc độ vượt trội.
- **Quy tắc gắn Tag Image**:
  - Mỗi image được push lên registry: `ghcr.io/<owner>/fakebook-<servicename>`.
  - Tag bất biến theo đủ 40 ký tự commit: `:sha-${GITHUB_SHA}`.
  - Tag theo tên nhánh đã chuẩn hóa: `:${CLEAN_BRANCH}` (ví dụ: `:staging`).
  - Nếu nhánh là `main` hoặc `master`, gắn thêm tag `:latest` và `:main`.

---

## 3. Trạng thái Triển khai (Continuous Deployment Status)

> [!NOTE]
> **CD hiện tại là bán tự động (Semi-Automated)**:
> - **Frontend**: Vercel tự động nhận diện commit mới trên nhánh `staging` và kích hoạt deploy tức thì.
> - **Backend Microservices**: Pipeline CI hiện tại chỉ dừng lại ở bước đóng gói và đẩy image lên **GHCR**. Quá trình cập nhật image lên Azure VM Staging được thực thi bằng lệnh `docker compose pull && docker compose up -d` (xem chi tiết tại [staging.md](staging.md)).
