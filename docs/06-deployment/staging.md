# Staging Environment Deployment (Triển khai Môi trường Staging)

Tài liệu này đặc tả toàn diện kiến trúc triển khai, quy trình cài đặt và các bước cập nhật phiên bản trên môi trường máy chủ thử nghiệm (**Staging Server**).

---

## 1. Thông số Kỹ thuật & Hạ tầng Staging

- **Máy chủ ảo (VM / VPS)**:
  - Nền tảng: Azure Virtual Machine (Ubuntu 22.04 LTS).
  - Tên miền định danh: `20-189-114-210.nip.io` (ánh xạ trực tiếp tới địa chỉ IP tĩnh của máy chủ).
  - Cổng mở ngoài Internet: `80` (HTTP - tự động chuyển hướng sang HTTPS) và `443` (HTTPS - Nginx Reverse Proxy).
  - Chứng chỉ SSL: **Let's Encrypt** (cấp tự động qua Certbot).
- **Cơ sở dữ liệu**:
  - **AWS RDS MariaDB** (chạy tại region riêng).
  - Chế độ bảo mật: Bắt buộc kết nối qua TLS/SSL với chứng chỉ `infrastructure/certs/global-bundle.pem`.
- **Frontend SPA**:
  - Triển khai trên **Vercel**: `https://fakebook-zeta.vercel.app`.
  - Tích hợp CI/CD tự động build khi push code vào nhánh `staging`.

---

## 2. Chuẩn bị môi trường trên Staging Host (Setup Checklist)

Trước khi khởi chạy hệ thống lần đầu trên VM:

1. **Cài đặt Docker Engine & Docker Compose v2**:
   ```bash
   sudo apt-get update
   sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
   ```
2. **Clone Repository & Chuẩn bị chứng chỉ RDS**:
   ```bash
   git clone https://github.com/MinhElly/fakebook.git /opt/fakebook
   cd /opt/fakebook
   
   # Tải chứng chỉ RDS CA
   mkdir -p infrastructure/certs
   curl -o infrastructure/certs/global-bundle.pem https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
   ```
3. **Cấu hình file biến môi trường bí mật (`infrastructure/.env.staging`)**:
   ```bash
   cp infrastructure/.env.staging.example infrastructure/.env.staging
   nano infrastructure/.env.staging
   ```
   *Điền các giá trị thực tế: `MARIADB_HOST`, mật khẩu database của từng service, secret của client OIDC `internal`, và API keys Cloudinary.*

4. **Đăng nhập vào GitHub Container Registry (GHCR)**:
   ```bash
   echo "<GITHUB_PERSONAL_ACCESS_TOKEN>" | docker login ghcr.io -u <GITHUB_USERNAME> --password-stdin
   ```

---

## 3. Quy trình Triển khai / Cập nhật phiên bản mới (Release Procedure)

Khi có bản build mới được CI build và push lên GHCR (với tag `staging` hoặc Git SHA ngắn):

```bash
cd /opt/fakebook/infrastructure

# 1. Kéo image mới nhất từ GHCR
docker compose --env-file .env.staging -f docker-compose-staging.yml pull

# 2. Khởi động lại toàn bộ stack container
docker compose --env-file .env.staging -f docker-compose-staging.yml up -d --remove-orphans

# 3. Theo dõi log khởi động
docker compose --env-file .env.staging -f docker-compose-staging.yml logs -f --tail=50
```

---

## 4. Kiểm thử Khói tự động sau Triển khai (Post-Deployment Smoke Test)

Sau khi toàn bộ container đã khởi động thành công, chạy script E2E Smoke Test để xác nhận luồng nghiệp vụ end-to-end:

```bash
# Nạp biến môi trường nội bộ
export KEYCLOAK_URL="https://20-189-114-210.nip.io"
export FAKEBOOK_OIDC_INTERNAL_CLIENT_ID="internal"
export FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET="<YOUR_INTERNAL_SECRET>"

# Chạy Smoke Test
./scripts/e2e-smoke-test.sh https://20-189-114-210.nip.io
```

Nếu script trả về `🎉 FAKEBOOK E2E SMOKE TEST PASSED ALL CHECKS!`, hệ thống đã sẵn sàng phục vụ kiểm thử.
