# Tổng quan và hướng dẫn chạy dự án Fakebook

## 1. Tổng quan kiến trúc

Fakebook là hệ thống mạng xã hội được xây dựng bằng JHipster 9.3.0 theo kiến trúc microservice. Hệ thống sử dụng:

- Java 21 và Spring Boot cho backend.
- React, TypeScript và Vite cho giao diện web.
- OAuth 2.0/OpenID Connect với Keycloak để đăng nhập và phân quyền.
- Consul để đăng ký, khám phá service và nạp cấu hình tập trung.
- Kafka để truyền sự kiện bất đồng bộ giữa các service.
- MariaDB theo mô hình database-per-service.
- Redis làm cache cho Feed Service.
- Zipkin cho distributed tracing.
- Maven Wrapper và npm Wrapper để build/chạy đúng phiên bản công cụ của dự án.

Luồng truy cập tổng quát:

```text
Trình duyệt
    |
    v
React/Vite (9000) ----> Gateway (8080) ----> Consul tìm service
                              |                    |
                              |                    +--> Auth Service (8081)
                              |                    +--> User Service (8082)
                              |                    +--> Post Service (8083)
                              |                    +--> Media Service (8084)
                              |                    +--> Comment Service (8085)
                              |                    +--> Feed Service (8086)
                              |
                              +--> Keycloak (9080)

Các service trao đổi sự kiện qua Kafka và lưu dữ liệu trong MariaDB riêng.
Feed Service sử dụng thêm Redis.
```

## 2. Ý nghĩa từng thư mục và thành phần

### `gateway`

Là điểm vào chính của toàn hệ thống, bao gồm cả backend Gateway và frontend React.

- Nhận request từ trình duyệt.
- Xử lý đăng nhập OAuth2/OIDC thông qua Keycloak.
- Tra cứu địa chỉ microservice qua Consul.
- Chuyển tiếp request dạng `/services/{service-name}/...` đến service phù hợp.
- Chứa giao diện web trong `gateway/src/main/webapp`.
- Có database riêng cho dữ liệu quản trị nội bộ của JHipster.

### `authService`

Là lớp tích hợp/nghiệp vụ liên quan đến xác thực. Keycloak vẫn là hệ thống quản lý danh tính thực sự.

Service này không được lưu mật khẩu hoặc sao chép thông tin đăng nhập của Keycloak. Nó có thể được dùng cho các nghiệp vụ như đăng xuất, đổi mật khẩu, quên/đặt lại mật khẩu, social login hoặc gọi Keycloak Admin API.

### `userService`

Quản lý hồ sơ và quan hệ giữa người dùng:

- `UserProfile`: hồ sơ người dùng; ID trùng UUID người dùng trong Keycloak.
- `FriendRequest`: lời mời kết bạn.
- `Friendship`: quan hệ bạn bè đã được xác lập.
- `Follow`: quan hệ theo dõi.

### `postService`

Quản lý nội dung bài đăng:

- `Post`: bài đăng.
- `PostMedia`: liên kết giữa bài đăng và media.
- `PostReaction`: cảm xúc/tương tác với bài đăng.

### `mediaService`

Quản lý metadata của ảnh, video hoặc tệp được tải lên qua entity `Media`. Phần lưu file thực tế cần được triển khai hoặc kết nối với hệ thống lưu trữ phù hợp.

### `commentService`

Quản lý bình luận thông qua entity `Comment`. Tham chiếu đến bài đăng hoặc người dùng thuộc service khác được lưu bằng UUID, không dùng quan hệ JPA xuyên database.

### `feedService`

Quản lý các mục xuất hiện trên bảng tin qua entity `FeedItem`.

- MariaDB lưu dữ liệu feed lâu dài.
- Redis hỗ trợ cache để đọc bảng tin nhanh hơn.
- Kafka có thể nhận sự kiện từ post, user, comment và các service khác để cập nhật feed bất đồng bộ.

### `infrastructure`

Chứa hạ tầng dùng chung cho môi trường development. Được thiết kế mô-đun hóa thành các thư mục con riêng biệt:

- `infrastructure/docker-compose.yml`: Compose tổng khởi động tất cả dịch vụ hạ tầng.
- `infrastructure/keycloak/`: Khởi động riêng rẽ Keycloak OAuth2 Identity Provider (`docker-compose.yml`, `.env`, `.env.example`).
- `infrastructure/consul/`: Khởi động riêng rẽ Consul Agent & Consul Config Loader (`docker-compose.yml`, `.env`, `.env.example`).
- `infrastructure/kafka/`: Khởi động riêng rẽ Kafka Native broker & Kafka UI (`docker-compose.yml`, `.env`, `.env.example`).
- `infrastructure/tracing/`: Khởi động riêng rẽ OpenZipkin server (`docker-compose.yml`, `.env`, `.env.example`).
- `infrastructure/config/`: Chứa cấu hình tập trung (`central-server-config/application.yml`) nạp tự động vào Consul KV store.
- `infrastructure/mariadb/`: Chứa script khởi tạo database ban đầu.
- `infrastructure/start.ps1` & `stop.ps1`: Script PowerShell để bật/tắt toàn bộ hạ tầng nhanh chóng.

### `fakebook-master.jdl`

Là mô tả nguồn của kiến trúc, application, entity, enum và relationship dùng để sinh dự án bằng JHipster. Không cần chạy file này khi khởi động ứng dụng.

## 3. Ý nghĩa các công nghệ hạ tầng

### Keycloak

Keycloak là Identity Provider, chịu trách nhiệm:

- Đăng nhập và đăng xuất.
- Phát hành access token/ID token.
- Quản lý người dùng, mật khẩu và role.
- Xác thực request gửi đến Gateway và microservice.

Realm phát triển là `jhipster`, client là `web_app`.

### Consul

Mỗi service đăng ký tên và địa chỉ của mình vào Consul. Gateway nhờ đó có thể tìm microservice mà không phải hard-code URL của từng service. Ứng dụng dev sử dụng Consul tại `localhost:8500`.

### Kafka & Kafka UI

Kafka truyền sự kiện bất đồng bộ. Ví dụ, sau khi tạo bài đăng, Post Service có thể phát một sự kiện để Feed Service xử lý mà không cần chờ xử lý feed hoàn tất trong cùng request. Giao diện Kafka UI (`http://localhost:8085`) giúp quản lý topic/message trực quan.

### MariaDB

Là database quan hệ. Thiết kế mục tiêu là mỗi service sở hữu database/schema của riêng mình, không tạo khóa ngoại hoặc quan hệ JPA xuyên service. Cổng đính kèm host là `3307` (`127.0.0.1:3307:3306`).

### Redis

Là kho dữ liệu trong bộ nhớ, hiện dành cho Feed Service để cache dữ liệu được truy cập thường xuyên.

### Zipkin

Thu thập và hiển thị distributed tracing (`http://localhost:9411`) để theo dõi luồng đi của request giữa các microservices.

## 4. Bảng port

### Port ứng dụng

| Port | Thành phần | Ý nghĩa |
| ---: | --- | --- |
| `8080` | Gateway backend | API Gateway và backend phục vụ web |
| `8081` | Auth Service | API nghiệp vụ xác thực/tích hợp Keycloak |
| `8082` | User Service | API hồ sơ, bạn bè và theo dõi |
| `8083` | Post Service | API bài đăng và reaction |
| `8084` | Media Service | API metadata media |
| `8085` | Comment Service | API bình luận |
| `8086` | Feed Service | API bảng tin |
| `9000` | Vite dev server | Frontend React khi chạy chế độ phát triển |

### Port hạ tầng

| Port | Thành phần | Ý nghĩa |
| ---: | --- | --- |
| `3307` | MariaDB dùng chung | Chứa database riêng của Gateway và từng microservice (Host port: 3307) |
| `6379` | Redis | Cache của Feed Service |
| `8500` | Consul HTTP/UI | API, UI và service discovery |
| `8300` | Consul server RPC | Giao tiếp nội bộ Consul |
| `8600` | Consul DNS | Truy vấn service bằng DNS |
| `8085` | Kafka UI | Giao diện quản lý trực quan Kafka topic/message |
| `9080` | Keycloak HTTP | Đăng nhập và trang quản trị Keycloak |
| `9443` | Keycloak HTTPS | HTTPS của Keycloak trong cấu hình module |
| `9092` | Kafka | Broker dùng bởi các ứng dụng |
| `9411` | Zipkin | Giao diện theo dõi distributed tracing |

### Port công cụ tùy chọn

| Port | Thành phần | Ý nghĩa |
| ---: | --- | --- |
| `3000` | Grafana | Dashboard monitoring |
| `7419` | JHipster Control Center | Quan sát/quản trị ứng dụng JHipster |
| `9001` | SonarQube | Phân tích chất lượng mã; tránh đụng frontend port `9000` |
| `9090` | Prometheus | Thu thập metrics |

## 5. Yêu cầu môi trường

Cần cài đặt:

- JDK 21.
- Docker Desktop và Docker Compose.
- Node.js từ `24.20.0` trở lên nếu gọi `npm` trực tiếp.
- npm `12.0.2` theo cấu hình Maven của Gateway.
- Git.

Không bắt buộc cài Maven toàn cục vì mỗi module có `mvnw.cmd`. Gateway cũng có `npmw.cmd` để dùng phiên bản Node/npm do build quản lý.

Kiểm tra môi trường trên PowerShell:

```powershell
java -version
docker --version
docker compose version
node --version
npm --version
```

Docker Desktop phải được khởi động trước khi chạy các file Compose.

## 6. Khởi động hạ tầng dùng chung

### Cách 1: Khởi động toàn bộ hạ tầng chung (Khuyên dùng 👍)

Từ thư mục gốc, chạy:

```powershell
cd C:\Code\fakebook
.\infrastructure\start.ps1
```
*(Nếu gặp lỗi PowerShell ExecutionPolicy, chạy: `powershell -ExecutionPolicy Bypass -File .\infrastructure\start.ps1`)*

Hoặc gọi Docker Compose trực tiếp:

```powershell
docker compose -f infrastructure/docker-compose.yml up -d --wait
```

### Cách 2: Khởi động riêng rẽ từng mô-đun hạ tầng (Để tiết kiệm RAM)

Bạn cũng có thể chọn chỉ khởi động từng dịch vụ hạ tầng cần thiết:

- **Chỉ bật Keycloak**:
  ```powershell
  docker compose -f infrastructure/keycloak/docker-compose.yml up -d
  ```
- **Chỉ bật Consul**:
  ```powershell
  docker compose -f infrastructure/consul/docker-compose.yml up -d
  ```
- **Chỉ bật Kafka & Kafka UI**:
  ```powershell
  docker compose -f infrastructure/kafka/docker-compose.yml up -d
  ```
- **Chỉ bật Tracing Zipkin**:
  ```powershell
  docker compose -f infrastructure/tracing/docker-compose.yml up -d
  ```

Lần khởi động đầu tiên, script `infrastructure/mariadb/init/01-create-databases.sql` tạo bảy database: `gateway`, `authservice`, `userservice`, `postservice`, `mediaservice`, `commentservice` và `feedservice`.

Script SQL trong `/docker-entrypoint-initdb.d` chỉ tự chạy khi volume MariaDB còn trống. Nếu bổ sung database vào SQL sau khi volume đã tồn tại, cần tự tạo database hoặc xóa volume development để khởi tạo lại.

## 7. Chạy Gateway và frontend để kiểm tra dự án

Đây là cách ngắn nhất để chạy giao diện và Gateway ở chế độ development.

### Bước 1: bảo đảm hạ tầng chung đang chạy

Mở PowerShell thứ nhất:

```powershell
cd C:\Code\fakebook
.\infrastructure\start.ps1
```

Lệnh trên khởi động MariaDB, Keycloak, Consul, Consul config loader, Kafka, Kafka UI, Redis và Zipkin dùng chung.

Kiểm tra container:

```powershell
docker compose -f infrastructure/docker-compose.yml ps
```

### Bước 2: chạy backend Gateway

Mở PowerShell thứ hai:

```powershell
cd C:\Code\fakebook\gateway
.\mvnw.cmd
```
*(Hoặc dùng lệnh đầy đủ với nháy đơn trong PowerShell: `.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.docker.compose.enabled=false'`)*

### Bước 3: cài dependency và chạy frontend

Mở PowerShell thứ ba:

```powershell
cd C:\Code\fakebook\gateway
.\npmw.cmd install
.\npmw.cmd run start
```

Truy cập frontend tại `http://localhost:9000`. Vite sẽ proxy request backend sang `http://localhost:8080`.

Chỉ cần chạy `npmw.cmd install` lần đầu hoặc khi `package.json`/lock file thay đổi.

### Tài khoản & Giao diện phát triển

- Keycloak Admin Console: `http://localhost:9080` (`admin` / `admin`).
- Consul Dashboard: `http://localhost:8500`.
- Kafka UI: `http://localhost:8085`.
- Zipkin UI: `http://localhost:9411`.
- Realm mẫu có user ứng dụng tên `admin` và `user`. Với realm JHipster mặc định, mật khẩu tương ứng là `admin` và `user`.

Không sử dụng các tài khoản/mật khẩu mẫu này ở production.

## 8. Chạy riêng một microservice

Mỗi module có cùng quy trình. Ví dụ chạy User Service:

```powershell
cd C:\Code\fakebook\userService
.\mvnw.cmd
```
*(Hoặc trong PowerShell: `.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.docker.compose.enabled=false'`)*

User Service sẽ chạy tại `http://localhost:8082`.

Thay `userService` bằng một trong các thư mục sau để chạy module khác:

```text
authService
postService
mediaService
commentService
feedService
gateway
```

Feed Service cần Redis; Redis đã được bật trong Compose hạ tầng chung.

Mỗi service nên được chạy trong một cửa sổ PowerShell riêng. Các Compose trong từng module vẫn được giữ để test module độc lập, nhưng không sử dụng chúng đồng thời với Compose hạ tầng chung để tránh trùng cổng.

## 9. Chạy backend không để Spring tự bật Docker Compose

Trong `application.yml`, Spring Boot Compose được bật với chế độ `start-only`. Khi hạ tầng đã được chạy riêng hoặc dùng hạ tầng chung, có thể tắt chức năng này khi dùng PowerShell:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.docker.compose.enabled=false'
```
*(Lưu ý bọc tham số `-D` trong dấu nháy đơn `' '` để PowerShell không làm sai cú pháp Maven)*

Cách này đặc biệt cần thiết khi chạy nhiều module cùng lúc để tránh mỗi tiến trình cố khởi động một bộ container riêng.

## 10. Quy tắc khi chạy toàn bộ hệ thống

Compose tổng đã được đặt tại `infrastructure/docker-compose.yml`. Khi chạy toàn bộ hệ thống:

- Chỉ khởi động `infrastructure/docker-compose.yml` (hoặc `.\infrastructure\start.ps1`).
- Không khởi động `src/main/docker/*.yml` riêng lẻ của từng module.
- Chạy từng ứng dụng bằng `.\mvnw.cmd` (hoặc truyền `'-Dspring-boot.run.arguments=--spring.docker.compose.enabled=false'`).
- Tất cả ứng dụng kết nối MariaDB qua `localhost:3307`, nhưng dùng database riêng với tên chữ thường.
- Các service dùng chung Keycloak, Consul và Kafka; Feed Service dùng thêm Redis.

Các Compose trong module được giữ để chạy/test riêng một module. Do chúng vẫn dùng các port hạ tầng giống nhau, phải dừng hạ tầng chung trước khi sử dụng một Compose riêng.

## 11. Dừng và dọn môi trường

```powershell
cd C:\Code\fakebook
.\infrastructure\stop.ps1
```

Lệnh trên dừng và xóa container/network nhưng giữ volume dữ liệu.

Muốn xóa cả volume và dữ liệu phát triển:

```powershell
docker compose -f infrastructure/docker-compose.yml down -v
```

Chỉ dùng `-v` khi chắc chắn có thể xóa database và dữ liệu Keycloak hiện tại.

## 12. Kiểm tra và xử lý lỗi thường gặp

### Port đã được sử dụng (Port is already allocated)

```powershell
Get-NetTCPConnection -State Listen | Where-Object LocalPort -in 3307,6379,8080,8081,8082,8083,8084,8085,8086,8500,9000,9080,9092,9411
docker ps
```

Nếu thấy lỗi `port is already allocated`, hãy tắt các container rác ở các module lẻ bằng cách dùng hạ tầng chung hoặc tắt bộ Compose cũ.

### Backend không kết nối được Consul

Kiểm tra `http://localhost:8500` và trạng thái container Consul:

```powershell
docker ps
docker logs fakebook-infrastructure-consul-1
```

### Đăng nhập không hoạt động

Kiểm tra Keycloak tại `http://localhost:9080`, realm `jhipster`, client `web_app`, sau đó kiểm tra log của Gateway.

### Không kết nối được database (Fail to establish connection to localhost:3307)

Đối chiếu port trong hai file của module:

```text
src/main/docker/mariadb.yml
src/main/resources/config/application-dev.yml
```

Host port của Docker phải trùng port trong JDBC và R2DBC URL (`localhost:3307`).

### Lỗi `Unknown lifecycle phase` trong PowerShell

Xảy ra khi truyền `-Dspring-boot.run.arguments="..."` không bọc trong dấu nháy đơn `' '`.
Khắc phục bằng cách chạy đơn giản `.\mvnw.cmd` hoặc bọc nháy đơn: `.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.docker.compose.enabled=false'`.

### Node/npm không đúng phiên bản

Ưu tiên wrapper trong Gateway:

```powershell
.\npmw.cmd install
```

Wrapper giúp Maven tải và dùng phiên bản Node/npm được khai báo trong `gateway/pom.xml`.

## 13. Các lệnh hữu ích

Build và chạy test backend của một module:

```powershell
.\mvnw.cmd verify
```

Chạy test frontend trong Gateway:

```powershell
.\npmw.cmd test
```

Build frontend production:

```powershell
.\npmw.cmd run build
```

Xem danh sách service đã đăng ký trên Consul:

```text
http://localhost:8500
```

Xem Kafka UI (Quản lý Kafka Topic / Message):

```text
http://localhost:8085
```

Xem Zipkin UI (Distributed Tracing):

```text
http://localhost:9411
```

Xem health của Gateway:

```text
http://localhost:8080/management/health
```

## 14. Thứ tự khởi động khuyến nghị khi đã có hạ tầng chung

1. Khởi động hạ tầng chung: `.\infrastructure\start.ps1` (MariaDB, Redis, Kafka, Kafka UI, Consul, Keycloak và Zipkin).
2. Auth Service và User Service.
3. Post Service, Media Service và Comment Service.
4. Feed Service.
5. Gateway backend (`cd gateway; .\mvnw.cmd`).
6. Frontend Vite (`cd gateway; .\npmw.cmd run start`).

Consul phải sẵn sàng trước khi các ứng dụng đăng ký service. Keycloak phải sẵn sàng trước khi thực hiện đăng nhập. Gateway nên chạy sau các microservice để định tuyến đầy đủ ngay khi giao diện được mở.
