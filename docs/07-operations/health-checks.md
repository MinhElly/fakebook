# Health Checks & Service Readiness

Tài liệu này đặc tả cơ chế kiểm tra sức khỏe (**Health Checks**) cho toàn bộ các thành phần hạ tầng Docker và microservices Spring Boot trong Fakebook.

---

## 1. Cơ chế Health Check của Spring Boot Actuator

Mỗi microservice đều được cấu hình Actuator Health Probes tương thích chuẩn Cloud Native:

```yaml
management:
  endpoint:
    health:
      show-details: when_authorized
      roles: 'ROLE_ADMIN'
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState
```

### Các Endpoint kiểm tra sức khỏe:
- **Tổng quan**: `GET http://<host>:<port>/management/health`
  - Kiểm tra kết nối tới cơ sở dữ liệu MariaDB (`db`), không gian đĩa trống (`diskSpace`), kết nối Redis (`redis`), và broker Kafka (`ping`).
- **Liveness Probe**: `GET http://<host>:<port>/management/health/liveness`
  - Trả về `{"status":"UP"}` nếu JVM tiến trình vẫn đang chạy bình thường (dùng để container orchestrator quyết định có cần restart container hay không).
- **Readiness Probe**: `GET http://<host>:<port>/management/health/readiness`
  - Trả về `{"status":"UP"}` khi service đã kết nối thành công tới Database, Liquibase migration đã hoàn tất, và sẵn sàng nhận traffic từ Gateway.

---

## 2. Đăng ký Health Check với Consul Service Discovery

Khi một microservice khởi động, nó tự động gửi thông tin đăng ký tới Consul:

```yaml
spring:
  cloud:
    consul:
      discovery:
        health-check-path: /management/health
        health-check-interval: 10s
        prefer-ip-address: true
```

- Consul Agent sẽ định kỳ mỗi 10 giây gửi HTTP GET request tới `/management/health`.
- Nếu service phản hồi mã HTTP 200, Consul đánh dấu service là **Passing (Màu xanh)** và Gateway sẽ điều phối request tới nó.
- Nếu service không phản hồi hoặc trả về mã lỗi 503, Consul đánh dấu là **Critical (Màu đỏ)** và Gateway lập tức ngừng chuyển tiếp traffic tới instance này để tránh lỗi người dùng.

---

## 3. Docker Container Health Checks

Hạ tầng Docker được cấu hình kiểm tra sức khỏe tự động trong `docker-compose.yml`:

| Container | Healthcheck Test Command | Interval | Timeout | Retries |
| :--- | :--- | :---: | :---: | :---: |
| **`fakebook-mariadb`** | `/usr/local/bin/healthcheck.sh --connect --innodb_initialized` | 5s | 5s | 20 |
| **`fakebook-keycloak`** | `bash /opt/keycloak/health-check.sh` | 5s | 5s | 50 |
| **`fakebook-consul`** | `consul members` | 5s | 5s | 20 |
| **`fakebook-redis`** | `redis-cli ping` | 5s | 5s | 20 |
