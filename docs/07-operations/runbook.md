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
docker exec -it fakebook-mariadb mariadb -uroot -e "SHOW DATABASES; SELECT COUNT(*) FROM postservice.posts;"
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

## 6. Kiểm tra & Thao tác trên Redis Cache

```bash
# Mở redis-cli tương tác
docker exec -it fakebook-redis redis-cli

# Kiểm tra kết nối
127.0.0.1:6379> PING
PONG

# Xem danh sách các key Timeline
127.0.0.1:6379> KEYS feed:user:*

# Đếm số lượng bài viết trong ZSet của một người dùng
127.0.0.1:6379> ZCARD feed:user:c7a8b9e0-1234-5678-9abc-def012345678

# Lấy 10 bài viết mới nhất kèm score timestamp
127.0.0.1:6379> ZREVRANGEBYSCORE feed:user:c7a8b9e0-1234-5678-9abc-def012345678 +inf -inf WITHSCORES LIMIT 0 10

# Xóa toàn bộ cache (Khẩn cấp)
127.0.0.1:6379> FLUSHDB
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
4. Chạy lại smoke test:
   ```bash
   ./scripts/e2e-smoke-test.sh https://20-189-114-210.nip.io
   ```

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
