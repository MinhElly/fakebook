# Architecture Decision Records (ADR)

Thư mục này lưu trữ các bản ghi quyết định kiến trúc (**Architecture Decision Records - ADR**) của hệ thống Fakebook. Mỗi bản ghi tài liệu hóa bối cảnh, các giải pháp thay thế được cân nhắc, quyết định được lựa chọn và hệ quả kỹ thuật tương ứng.

---

## Danh mục ADR

| Mã hiệu | Tiêu đề | Trạng thái | Ngày tạo |
| :--- | :--- | :---: | :---: |
| [ADR-001](ADR-001-microservices-architecture.md) | Sử dụng Kiến trúc Microservices với mô hình Database-per-Service | **Accepted** | 2026-09 |
| [ADR-002](ADR-002-keycloak-identity-provider.md) | Chọn Keycloak làm Identity Provider tập trung cho OAuth2/OIDC | **Accepted** | 2026-09 |
| [ADR-003](ADR-003-consul-discovery-and-central-config.md) | Dùng HashiCorp Consul cho Service Discovery & Cấu hình tập trung | **Accepted** | 2026-09 |
| [ADR-004](ADR-004-kafka-event-driven-fanout.md) | Áp dụng Apache Kafka cho truyền thông sự kiện và Feed Fan-out | **Accepted** | 2026-09 |
| [ADR-005](ADR-005-redis-feed-caching.md) | Sử dụng Redis Sorted Sets để lưu trữ dòng thời gian Bảng tin (Timeline) | **Accepted** | 2026-09 |
| [ADR-006](ADR-006-hybrid-staging-deployment.md) | Triển khai Staging lai: Vercel Frontend + Azure VM Backend + AWS RDS | **Accepted** | 2026-09 |
