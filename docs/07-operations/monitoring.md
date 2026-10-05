# Monitoring & Observability (Giám sát & Khả năng quan sát)

Tài liệu này hướng dẫn cách theo dõi hiệu năng, độ trễ và phân tích luồng request phân tán trong hệ thống Fakebook bằng **OpenZipkin** và **Spring Boot Actuator**.

---

## 1. Distributed Tracing với OpenZipkin

Khi một request đi qua nhiều microservices (ví dụ: Frontend -> Gateway -> FeedService -> UserServiceClient), việc tìm ra bottleneck hoặc lỗi trở nên phức tạp nếu chỉ đọc log rời rạc.

Fakebook tích hợp **Micrometer Tracing** tự động sinh `traceId` và `spanId` xuyên suốt qua các header HTTP B3 Propagation:

```text
Browser ──► Gateway ──► FeedService ──► UserService
   │           │             │               │
   └───────────┴─────────────┴───────────────┴──► Zipkin Server (:9411)
```

### 1.1 Tỷ lệ lấy mẫu Trace (Sampling Probability)
- **Dev (`application-dev.yml`)**: `sampling.probability: 1.0` (Lấy mẫu 100% tất cả các request phục vụ debug).
- **Staging (`application-staging.yml`)**: `sampling.probability: 0.1` (Lấy mẫu 10% các request để tránh quá tải mạng và lưu trữ Zipkin).

### 1.2 Truy cập giao diện Zipkin UI
- Địa chỉ: `http://localhost:9411`
- Tính năng:
  - Tìm kiếm Trace theo `serviceName`, `operationName`, khoảng thời gian hoặc theo `traceId` cụ thể.
  - Sơ đồ trực quan (Waterfall timeline) hiển thị thời gian xử lý chi tiết tại từng tầng (Gateway -> Network -> Microservice -> Database query).

---

## 2. Spring Boot Actuator Metrics

Tất cả các microservice đều mở các endpoint quản trị chuẩn:

- **Health Check Endpoint**:
  - `GET /management/health`: Trạng thái sống (Liveness) và sẵn sàng (Readiness) của database, disk, Consul, Redis.
- **Prometheus Metrics**:
  - `GET /management/prometheus`: Xuất định dạng metrics tương thích với Prometheus scraper (CPU usage, JVM Heap memory, Garbage Collection pauses, HTTP request latencies).
- **JHipster Metrics**:
  - `GET /management/jhimetrics`: Thống kê tần suất gọi các REST controller, repository queries và cache hit/miss ratio.
- **Loggers Endpoint**:
  - `GET/POST /management/loggers`: Cho phép xem và thay đổi cấp độ log (DEBUG, INFO, WARN, ERROR) ngay tại runtime mà không cần khởi động lại container.
