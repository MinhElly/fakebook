# Monitoring & Observability

## 3.6. Tracing

### 3.6.1. Khái niệm

Tracing trong kiến trúc microservice là kỹ thuật theo dõi một request khi request đó đi qua nhiều thành phần và dịch vụ khác nhau trong hệ thống. Mỗi request được gắn một `traceId`; các thao tác phát sinh trong quá trình xử lý được biểu diễn bằng các span có `spanId`, thời gian bắt đầu, thời gian kết thúc và quan hệ cha-con.

Mục tiêu của tracing là cung cấp cái nhìn xuyên suốt về luồng xử lý thay vì phải đối chiếu riêng lẻ log của từng dịch vụ. Tracing cần thiết trong dự án vì các lý do sau:

- **Hiểu rõ luồng yêu cầu:** xác định request đi từ client qua Gateway tới những service nào, bao gồm cả các lời gọi HTTP đồng bộ và xử lý sự kiện Kafka bất đồng bộ.
- **Phát hiện lỗi:** xác định service hoặc thao tác phát sinh lỗi và xem được các span liên quan trong cùng một trace.
- **Tối ưu hiệu suất:** so sánh thời gian xử lý của từng span để phát hiện service, truy vấn cơ sở dữ liệu hoặc lời gọi mạng gây độ trễ lớn.
- **Giám sát và báo cáo:** cung cấp dữ liệu về thời gian đáp ứng, quan hệ giữa các service và lịch sử xử lý request để phục vụ vận hành hệ thống.

### 3.6.2. Áp dụng pattern vào dự án

Fakebook sử dụng **Micrometer Tracing**, **Brave** và **OpenZipkin 3.6.1** để triển khai distributed tracing. Micrometer Tracing cung cấp API instrumentation thống nhất, Brave tạo và truyền trace context, còn Zipkin thu thập, lưu trữ tạm thời và trực quan hóa các span.

Tracing được cấu hình cho toàn bộ bảy ứng dụng backend:

- `gateway`;
- `authService`;
- `userService`;
- `postService`;
- `mediaService`;
- `commentService`;
- `feedService`.

Mỗi ứng dụng sử dụng các module `spring-boot-micrometer-tracing`, `spring-boot-micrometer-tracing-brave`, `micrometer-tracing-bridge-brave` và `spring-boot-zipkin`. Nhờ đó, các request HTTP đến, lời gọi HTTP/Feign đi và những thành phần hỗ trợ Micrometer Observation được tự động instrument và gửi span về Zipkin.

Luồng tracing tổng quát trong dự án:

```text
Client -> Gateway -> Service HTTP/Feign -> Database/Redis
                    |
                    +-> Transactional Outbox -> Kafka -> Consumer Service

Gateway và các service -------------------------------> Zipkin (:9411)
```

Đối với giao tiếp bất đồng bộ, cấu hình dùng chung trên Consul Config bật `spring.cloud.stream.kafka.binder.enable-observation: true`; Post Service và Feed Service cũng khai báo thuộc tính này trong cấu hình Kafka cục bộ. Riêng các sự kiện được ghi qua transactional outbox của Post Service và User Service, trace context hiện tại được lưu cùng outbox record. Khi publish record lên Kafka, hệ thống chỉ chuyển tiếp các propagation header nằm trong danh sách do `Propagator.fields()` cung cấp; các metadata không liên quan không được đưa thành Kafka header.

Tên propagation header thực tế phụ thuộc vào `Propagator` đang được cấu hình. Vì vậy không mặc định mọi môi trường đều sử dụng riêng `b3` hoặc `traceparent`; khi kiểm thử luồng Kafka cần kiểm tra header thật trên Kafka record.

Cấu hình lấy mẫu và Zipkin endpoint hiện tại:

| Môi trường | Zipkin endpoint | Sampling |
|---|---|---:|
| Dev | `http://localhost:9411/api/v2/spans` | `1.0` |
| Staging | `http://${ZIPKIN_HOST:zipkin}:${ZIPKIN_PORT:9411}/api/v2/spans` | `0.1` |
| Prod | `${MANAGEMENT_TRACING_EXPORT_ZIPKIN_ENDPOINT:http://zipkin:9411/api/v2/spans}` | `1.0` |

Trong môi trường local, Zipkin được chạy bằng Docker:

```powershell
docker compose -f infrastructure/docker-compose.yml up -d zipkin
```

Hoặc chỉ khởi chạy stack tracing độc lập:

```powershell
docker compose --env-file infrastructure/tracing/.env.example `
  -f infrastructure/tracing/docker-compose.yml up -d
```

Sau khi Zipkin khởi động, truy cập giao diện tại `http://localhost:9411`. Lập trình viên có thể tìm kiếm theo service name, operation name, khoảng thời gian hoặc `traceId`; xem waterfall timeline để phân tích độ trễ; và kiểm tra quan hệ giữa các span của Gateway, service xử lý và các dependency.

Kết quả audit hiện tại:

| Mức bằng chứng | Trạng thái | Nội dung |
|---|---|---|
| Source/config | **Đạt** | Cả bảy ứng dụng có dependency tracing, Zipkin endpoint và cấu hình sampling; Kafka observation được bật; Post/User outbox có propagation context. |
| Automated test | **Đạt** | Integration test kiểm tra `BraveTracer` và Zipkin sender; unit test kiểm tra capture và allowlist propagation header của outbox. |
| Local runtime | **Đạt một phần** | Zipkin đã nhận span riêng lẻ từ Gateway, User, Post, Media, Comment và Feed. Auth Service hiện chưa chạy. |
| Business trace xuyên service | **Chưa chứng minh** | Chưa lưu được một trace xác thực duy nhất đi qua Gateway, service nghiệp vụ, outbox/Kafka và consumer. |
| Staging | **Chưa chứng minh** | Chưa có trace runtime được thu thập từ môi trường staging. |

Việc service trả về health 200, Zipkin liệt kê tên service hoặc unit test thành công không đồng nghĩa một business request đã được trace xuyên suốt. Để nghiệm thu runtime cần lưu `traceId`, danh sách span/service trong Zipkin và, đối với luồng bất đồng bộ, propagation header thực tế trên Kafka record.

## Spring Boot Actuator

Các ứng dụng cung cấp những endpoint quản trị chính:

- `GET /management/health`: kiểm tra liveness, readiness và trạng thái dependency;
- `GET /management/prometheus`: xuất metrics cho Prometheus;
- `GET /management/jhimetrics`: cung cấp metrics ứng dụng của JHipster;
- `GET/POST /management/loggers`: xem hoặc thay đổi log level tại runtime;
- `GET /management/circuitbreakers` và `GET /management/circuitbreakerevents`: xem trạng thái và sự kiện Circuit Breaker tại các service đã expose endpoint tương ứng.

Tracing cho biết một request đã đi qua đâu và mất bao lâu; metrics cho biết trạng thái tổng thể của hệ thống theo thời gian; log cung cấp chi tiết sự kiện. Khi điều tra sự cố nên kết hợp cả ba nguồn dữ liệu bằng timestamp, service name và `traceId`.
