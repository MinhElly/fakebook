# ADR-003: HashiCorp Consul for Service Discovery and Centralized Configuration

## Trạng thái
**Accepted**

## Bối cảnh (Context)
Trong kiến trúc microservices:
- Các instance của dịch vụ có thể thay đổi địa chỉ IP/Port linh hoạt. API Gateway cần biết vị trí của từng dịch vụ mà không thể cấu hình hardcode IP tĩnh.
- Cần một cơ chế tập trung để quản lý các giá trị cấu hình theo môi trường (dev, staging) thay vì phải sửa từng file cấu hình trong từng service.

## Quyết định (Decision)
1. Sử dụng **HashiCorp Consul 2.0.3** kết hợp hai tính năng trong cùng một giải pháp:
   - **Service Discovery**: Microservice tự đăng ký với Consul kèm health check path `/management/health`. Gateway sử dụng Consul Discovery Locator để tự động định tuyến `/services/<service-id>/**`.
   - **Centralized Configuration**: Sử dụng công cụ `consul-config-loader` để đọc các file YAML từ `infrastructure/config/central-server-config/` và nạp vào Consul KV Store.
2. Không triển khai Spring Cloud Config Server độc lập để giảm bớt một tiến trình JVM nặng.
3. Tắt tính năng Dynamic Config Watch (`watch.enabled: false`) để đảm bảo các giá trị cấu hình được kiểm soát chặt chẽ lúc khởi động service (Bootstrap Phase).

## Các giải pháp thay thế (Alternatives Considered)
- **Eureka + Spring Cloud Config Server**: Kiến trúc truyền thống của Netflix OSS/JHipster, nhưng Eureka hiện đã bước vào chế độ bảo trì, và cần chạy thêm một tiến trình Spring Cloud Config Server gây tốn RAM.
- **Kubernetes Native Service Discovery & ConfigMap**: Rất tốt cho production nhưng không thuận tiện khi chạy môi trường phát triển cục bộ trực tiếp trên máy host (JVM).

## Hệ quả (Consequences)
- **Tích cực**:
  - Consul chạy bằng binary Go siêu nhẹ, khởi động tức thì, tiêu thụ ít hơn 50MB RAM.
  - Cung cấp giao diện web Consul UI trực quan theo dõi trạng thái sống của toàn bộ dịch vụ.
- **Thách thức**:
  - Cần đảm bảo Consul và `consul-config-loader` khởi động khỏe mạnh trước khi các backend microservices khởi chạy.
