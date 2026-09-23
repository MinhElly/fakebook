# Troubleshooting Guide (Cẩm nang xử lý sự cố)

Tài liệu này tổng hợp các lỗi thực tế có thể phát sinh trong quá trình phát triển và vận hành hệ thống Fakebook, kèm phương pháp chẩn đoán và khắc phục chi tiết.

---

## 1. Vercel Build Error: `ERR_PNPM_OUTDATED_LOCKFILE`

### Hiện tượng (Symptoms)
Quá trình build frontend trên Vercel hoặc CI/CD thất bại với lỗi:
```text
ERR_PNPM_OUTDATED_LOCKFILE Cannot install with "frozen-lockfile" because pnpm-lock.yaml is not up to date with <ROOT>/package.json
Failure reason: specifiers in the lockfile don't match specifiers in package.json
Error: Command "pnpm install" exited with 1
```

### Nguyên nhân (Possible causes)
Trong thư mục `frontend/` vô tình tồn tại cả 2 file khóa: `package-lock.json` (của npm) và `pnpm-lock.yaml` (file cũ từ template). Khi có thư viện mới được thêm vào qua `npm install`, `pnpm-lock.yaml` không được cập nhật. Vercel tự động ưu tiên pnpm khi thấy file `.yaml` và chạy `pnpm install --frozen-lockfile` dẫn đến build thất bại.

### Cách xử lý (Fix)
Xóa file `pnpm-lock.yaml` thừa để Vercel và Dockerfile chuyển sang dùng chuẩn `npm`:
```bash
cd frontend
git rm pnpm-lock.yaml
git commit -m "fix(frontend): remove obsolete pnpm-lock.yaml so Vercel uses npm"
git push origin <branch>
```

---

## 2. MariaDB: `Too many connections` trên AWS RDS Staging

### Hiện tượng (Symptoms)
Các container microservice trên Staging liên tục crash hoặc báo lỗi:
```text
java.sql.SQLNonTransientConnectionException: Could not connect to address=(host=...): Too many connections
```

### Nguyên nhân (Possible causes)
Instance MariaDB trên AWS RDS gói nhỏ (db.t3.micro / db.t4g.micro) có giới hạn `max_connections` (thường khoảng 30 - 60 kết nối). Với 7 microservices + Keycloak, nếu mỗi service để `maximum-pool-size: 10` mặc định thì tổng số connection vượt quá giới hạn của RDS.

### Cách xử lý (Fix)
Siết chặt giới hạn connection pool trong file `.env.staging` trên máy chủ Staging:
```bash
# Đặt giới hạn kết nối nghiêm ngặt
APP_DB_POOL_MIN_IDLE=1
APP_DB_POOL_MAX_SIZE=3
KEYCLOAK_DB_POOL_MIN_SIZE=1
KEYCLOAK_DB_POOL_MAX_SIZE=5
GATEWAY_DB_POOL_MAX_SIZE=3
```
Khởi động lại toàn bộ stack:
```bash
docker compose -f infrastructure/docker-compose-staging.yml up -d
```

---

## 3. Lỗi SSL Handshake với MariaDB: `SSL connection is required`

### Hiện tượng (Symptoms)
Microservice không thể kết nối tới RDS với lỗi:
```text
CommunicationsException: Communications link failure: require_secure_transport=ON
hoặc SSLHandshakeException: PKIX path building failed: unable to find valid certification path
```

### Cách xử lý (Fix)
1. Đảm bảo file chứng chỉ AWS RDS Global Bundle `global-bundle.pem` có mặt tại thư mục `infrastructure/certs/`:
   ```bash
   curl -o infrastructure/certs/global-bundle.pem https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
   ```
2. Đảm bảo `docker-compose-staging.yml` đã mount chứng chỉ vào container tại `/run/secrets/rds-global-bundle.pem`:
   ```yaml
   volumes:
     - ${MARIADB_SSL_CERT_HOST_PATH}:/run/secrets/rds-global-bundle.pem:ro
   ```
3. Đảm bảo JDBC URL chứa tham số SSL hợp lệ:
   ```text
   &sslMode=verify-full&serverSslCert=/run/secrets/rds-global-bundle.pem
   ```

---

## 4. Xung đột Port khi chạy Local (Port Already Allocated)

### Hiện tượng (Symptoms)
Lỗi khi chạy `docker compose up` hoặc khởi động Spring Boot:
```text
Bind for 0.0.0.0:3307 failed: port is already allocated
hoặc WebServerException: Port 8080 was already in use
```

### Cách chẩn đoán & xử lý (Diagnosis & Fix)
Trên Windows PowerShell:
```powershell
# Tìm tiến trình đang chiếm dụng port (ví dụ port 3307 hoặc 8080)
Get-NetTCPConnection -State Listen | Where-Object LocalPort -in 3307,8080,8082,8500,9080

# Tìm ID tiến trình (OwningProcess) và tắt tiến trình
Stop-Process -Id <PID> -Force
```

---

## 5. Lỗi Spring Boot tự động bật Container Docker trùng lặp

### Hiện tượng (Symptoms)
Khi chạy `./mvnw spring-boot:run`, máy bị chậm đơ, RAM tăng vọt, xuất hiện các container Docker lạ khởi động ngầm.

### Nguyên nhân (Possible causes)
Tính năng `spring-boot-docker-compose` tự động phát hiện file docker-compose trong thư mục `src/main/docker` và tự bật thêm cụm container độc lập cho từng microservice.

### Cách xử lý (Fix)
Luôn thêm cờ tắt tính năng này vào lệnh chạy:
```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.docker.compose.enabled=false'
```

---

## 6. Lỗi HTTP 401 Unauthorized khi gọi API qua Gateway

### Hiện tượng (Symptoms)
Frontend nhận mã lỗi `401 Unauthorized` mặc dù người dùng đã đăng nhập trên Keycloak thành công.

### Các bước chẩn đoán (Diagnosis Steps)
1. **Kiểm tra Token Expiry**: Mở tab Network trên DevTools, copy giá trị Bearer token giải mã tại `jwt.io` xem `exp` đã quá hạn chưa.
2. **Kiểm tra Issuer URI**: Đảm bảo `FAKEBOOK_OIDC_ISSUER_URI` trong backend trùng khớp 100% với `iss` trong payload của Token.
   - Ví dụ: `http://localhost:9080/realms/jhipster` (chú ý cổng `9080` chứ không phải `8080`).
3. **Kiểm tra Audience**: JHipster cấu hình kiểm tra audience (`account` hoặc `api://default`). Đảm bảo client `web_app` trong Keycloak cấp audience hợp lệ.

---

## 7. Kafka Message tắc nghẽn hoặc chuyển vào Dead Letter Topic (DLT)

### Hiện tượng (Symptoms)
Bài viết tạo thành công nhưng không xuất hiện trên Bảng tin của bạn bè, hoặc bình luận bị từ chối.

### Cách chẩn đoán & xử lý (Diagnosis & Fix)
1. Truy cập **Kafka UI** tại `http://localhost:8088`.
2. Kiểm tra topic `post-events-feed-dlt` hoặc `post-events-dlt`. Nếu có message trong DLT, xem trường header `x-exception-message` để biết chính xác lý do consumer từ chối (ví dụ: mất kết nối sang Redis, không lấy được danh sách bạn bè).
3. Khởi động lại service tiêu thụ hoặc khắc phục nguyên nhân gốc rồi phát lại event.
