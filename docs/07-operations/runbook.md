# Operations Runbook (Sổ tay Vận hành & Cứu hộ Sự cố)

Sổ tay này cung cấp các quy trình thao tác chuẩn (Standard Operating Procedures - SOP) và các câu lệnh copy-paste trực tiếp phục vụ quản trị viên và lập trình viên khi vận hành hoặc xử lý sự cố khẩn cấp trên hệ thống Fakebook.

---

## 1. Kiểm tra trạng thái toàn bộ Container

### Môi trường Local Dev:
```powershell
docker compose --project-directory infrastructure -f infrastructure/docker-compose.yml ps
```

### Môi trường Staging (Trên Azure VM):
```bash
cd /opt/fakebook/infrastructure
docker compose --env-file .env.staging -f docker-compose-staging.yml ps
```

---

## 2. Xem Log thời gian thực (Log Inspection)

Xem log của một dịch vụ cụ thể:
```bash
# Xem log của Gateway
docker compose -f docker-compose-staging.yml logs -f --tail=100 gateway

# Xem log của Feed Service
docker compose -f docker-compose-staging.yml logs -f --tail=100 feedservice

# Xem log của Keycloak
docker compose -f docker-compose-staging.yml logs -f --tail=100 keycloak

# Xem log lỗi của Nginx
docker compose -f docker-compose-staging.yml logs -f --tail=100 nginx
```

---

## 3. Khởi động lại dịch vụ (Service Restart)

### Khởi động lại một container đơn lẻ:
```bash
# Ví dụ khi sửa cấu hình trong central-server-config:
docker compose -f docker-compose-staging.yml restart userservice
```

### Khởi động lại toàn bộ hệ thống Staging:
```bash
docker compose --env-file .env.staging -f docker-compose-staging.yml down
docker compose --env-file .env.staging -f docker-compose-staging.yml up -d --wait
```

---

## 4. Kiểm tra Cơ sở dữ liệu MariaDB

### Kiểm tra từ dòng lệnh trong container:
```bash
# Truy cập MySQL CLI vào database post_service
docker exec -it fakebook-mariadb mariadb -uroot -e "SHOW DATABASES; SELECT COUNT(*) FROM post_service.posts;"
```

### Kiểm tra kết nối từ VM tới AWS RDS Staging:
```bash
mariadb -h <MARIADB_HOST> -P 3306 -u post_user -p --ssl-ca=/opt/fakebook/infrastructure/certs/global-bundle.pem post_service
```

---

## 5. Giám sát & Quản lý Apache Kafka

1. **Giao diện Kafka UI**:
   - Truy cập: `http://localhost:8088` (Dev) hoặc port được forward qua SSH tunnel.
   - Kiểm tra: Danh sách Topics (`post-events`, `media-cleanup-topic`), độ trễ Consumer Lag, và các Dead Letter Queues (`post-events-feed-dlt`).
2. **Kiểm tra Consumer Group Lag qua CLI**:
   ```bash
   docker exec -it fakebook-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
     --bootstrap-server localhost:9092 \
     --describe --group feed-service-post-sync
   ```

---

## 6. Kiểm tra Redis Cache và Feed projection

Feed đọc MariaDB `feed_items`, không còn sử dụng `feed:user:*`. Kiểm tra thứ tự
và access path bằng query tương ứng với user cần chẩn đoán:

```sql
EXPLAIN SELECT * FROM feed_items
WHERE user_id = '<user UUID>'
ORDER BY created_at DESC, post_id DESC LIMIT 20;
```

Redis vẫn phục vụ cache User. Dùng SCAN để xem các trang/sort của một user:

```bash
# Mở redis-cli tương tác
docker exec -it fakebook-redis redis-cli

# Kiểm tra kết nối
127.0.0.1:6379> PING
PONG

# Xem cache friends của đúng user; tiếp tục SCAN nếu cursor trả về khác 0
127.0.0.1:6379> SCAN 0 MATCH userFriends::c7a8b9e0-1234-5678-9abc-def012345678_* COUNT 100
```

---

## 7. Quy trình Rollback phiên bản Image (Emergency Rollback)

Khi một bản deploy mới gặp lỗi nghiêm trọng trên Staging:

1. Tìm mã băm commit ổn định gần nhất trên GitHub (ví dụ: `4e9bcae`).
2. Sửa biến `IMAGE_TAG` trong file `/opt/fakebook/infrastructure/.env.staging`:
   ```bash
   IMAGE_TAG=4e9bcae
   ```
3. Kéo image của commit cũ và tái khởi động stack:
   ```bash
   docker compose --env-file .env.staging -f docker-compose-staging.yml pull
   docker compose --env-file .env.staging -f docker-compose-staging.yml up -d
   ```
4. Kiểm tra lại login/callback/logout, /api/account và các API nghiệp vụ bằng normal-user qua Gateway; lưu evidence đã mask. Repository hiện không còn runner smoke test tự động.

---

## 8. Giám sát Tài nguyên Máy chủ (CPU, RAM, Disk)

```bash
# Xem mức sử dụng CPU & RAM theo thời gian thực của từng container
docker stats --no-stream

# Kiểm tra dung lượng ổ đĩa còn trống
df -h

# Kiểm tra dung lượng RAM và Swap của máy chủ
free -m
```
