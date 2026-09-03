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

Chứa cấu hình hạ tầng dùng chung. Hiện tại thư mục này mới có Docker Compose riêng cho Keycloak tại `infrastructure/keycloak/docker-compose.yml`, chưa có Compose tổng để chạy toàn bộ hệ thống.

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

### Kafka

Kafka truyền sự kiện bất đồng bộ. Ví dụ, sau khi tạo bài đăng, Post Service có thể phát một sự kiện để Feed Service xử lý mà không cần chờ xử lý feed hoàn tất trong cùng request.

### MariaDB

Là database quan hệ. Thiết kế mục tiêu là mỗi service sở hữu database/schema của riêng mình, không tạo khóa ngoại hoặc quan hệ JPA xuyên service.

### Redis

Là kho dữ liệu trong bộ nhớ, hiện dành cho Feed Service để cache dữ liệu được truy cập thường xuyên.

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
| `3306` | MariaDB | Database của các microservice hiện tại |
| `3307` | MariaDB Gateway | Host port hiện tại của database Gateway; ánh xạ vào `3306` trong container |
| `6379` | Redis | Cache của Feed Service |
| `8500` | Consul HTTP/UI | API, UI và service discovery |
| `8300` | Consul server RPC | Giao tiếp nội bộ Consul |
| `8600` | Consul DNS | Truy vấn service bằng DNS |
| `9080` | Keycloak HTTP | Đăng nhập và trang quản trị Keycloak |
| `9443` | Keycloak HTTPS | HTTPS của Keycloak trong cấu hình module |
| `9092` | Kafka | Broker dùng bởi các ứng dụng |

### Port công cụ tùy chọn

| Port | Thành phần | Ý nghĩa |
| ---: | --- | --- |
| `3000` | Grafana | Dashboard monitoring |
| `7419` | JHipster Control Center | Quan sát/quản trị ứng dụng JHipster |
| `9001` | SonarQube | Phân tích chất lượng mã; tránh đụng frontend port `9000` |
| `9090` | Prometheus | Thu thập metrics |
| `9411` | Zipkin | Theo dõi distributed tracing |

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

## 6. Chạy Gateway và frontend để kiểm tra dự án

Đây là cách ngắn nhất để chạy giao diện và Gateway ở chế độ development.

### Bước 1: chạy hạ tầng của Gateway

Mở PowerShell thứ nhất:

```powershell
cd C:\Code\fakebook\gateway
docker compose -f src/main/docker/services.yml up --wait
```

Lệnh trên khởi động các dependency khai báo cho Gateway: MariaDB, Keycloak, Consul, Consul config loader và Kafka.

Kiểm tra container:

```powershell
docker compose -f src/main/docker/services.yml ps
```

### Bước 2: chạy backend Gateway

Mở PowerShell thứ hai:

```powershell
cd C:\Code\fakebook\gateway
.\mvnw.cmd
```

Gateway backend chạy tại `http://localhost:8080`.

Spring Boot cũng được cấu hình tự quản lý `src/main/docker/services.yml`. Vì vậy có thể thử chạy trực tiếp `.\mvnw.cmd`; tuy nhiên việc bật hạ tầng trước giúp nhìn lỗi container rõ ràng hơn.

### Bước 3: cài dependency và chạy frontend

Mở PowerShell thứ ba:

```powershell
cd C:\Code\fakebook\gateway
.\npmw.cmd install
.\npmw.cmd run start
```

Truy cập frontend tại `http://localhost:9000`. Vite sẽ proxy request backend sang `http://localhost:8080`.

Chỉ cần chạy `npmw.cmd install` lần đầu hoặc khi `package.json`/lock file thay đổi.

### Tài khoản phát triển

- Keycloak Admin Console: `http://localhost:9080`.
- Tài khoản quản trị Keycloak: `admin` / `admin`.
- Realm mẫu có user ứng dụng tên `admin` và `user`. Với realm JHipster mặc định, mật khẩu thường tương ứng là `admin` và `user`.

Không sử dụng các tài khoản/mật khẩu mẫu này ở production.

## 7. Chạy riêng một microservice

Mỗi module có cùng quy trình. Ví dụ chạy User Service:

```powershell
cd C:\Code\fakebook\userService
docker compose -f src/main/docker/services.yml up --wait
.\mvnw.cmd
```

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

Feed Service cần Redis; Redis đã có trong `feedService/src/main/docker/services.yml`.

## 8. Chạy backend không để Spring tự bật Docker Compose

Trong `application.yml`, Spring Boot Compose được bật với chế độ `start-only`. Khi hạ tầng đã được chạy riêng hoặc dùng hạ tầng chung, có thể tắt chức năng này:

```powershell
.\mvnw.cmd -Dspring-boot.run.arguments="--spring.docker.compose.enabled=false"
```

Cách này đặc biệt cần thiết khi chạy nhiều module cùng lúc để tránh mỗi tiến trình cố khởi động một bộ container riêng.

## 9. Hạn chế hiện tại khi chạy toàn bộ hệ thống

Repo chưa có `docker-compose.yml` tổng ở thư mục gốc.

Gateway hiện dùng MariaDB host port `3307`, nhưng sáu microservice còn lại đều khai báo MariaDB tại host port `3306`. Mỗi module cũng khai báo Keycloak, Consul và Kafka trên cùng port. Do đó:

- Không chạy đồng thời `services.yml` của nhiều microservice.
- Bộ Compose khởi động sau sẽ lỗi `port is already allocated`.
- Chỉ nên dùng một Keycloak, một Consul và một Kafka cho toàn hệ thống.
- Các database cần được tách bằng schema/database chung hoặc bằng container/host port riêng.

Để chạy toàn bộ hệ thống đúng kiến trúc, cần bổ sung Compose tổng với:

1. Một Keycloak tại `9080`.
2. Một Consul tại `8500`.
3. Một Kafka broker tại `9092`.
4. Một Redis tại `6379`.
5. MariaDB có database riêng cho `gateway`, `authService`, `userService`, `postService`, `mediaService`, `commentService` và `feedService`; hoặc các MariaDB container dùng port riêng.
6. Khởi động các Java service với `spring.docker.compose.enabled=false`.

## 10. Dừng và dọn môi trường

Trong thư mục module đã dùng để bật Compose:

```powershell
docker compose -f src/main/docker/services.yml down
```

Lệnh trên dừng và xóa container/network nhưng giữ volume dữ liệu.

Muốn xóa cả volume và dữ liệu phát triển:

```powershell
docker compose -f src/main/docker/services.yml down -v
```

Chỉ dùng `-v` khi chắc chắn có thể xóa database và dữ liệu Keycloak hiện tại.

## 11. Kiểm tra và xử lý lỗi thường gặp

### Port đã được sử dụng

```powershell
Get-NetTCPConnection -State Listen | Where-Object LocalPort -in 3306,3307,6379,8080,8081,8082,8083,8084,8085,8086,8500,9000,9080,9092
docker ps
```

Nếu thấy lỗi `port is already allocated`, không bật thêm `services.yml` của module khác; hãy dừng bộ Compose cũ hoặc dùng hạ tầng chung.

### Backend không kết nối được Consul

Kiểm tra `http://localhost:8500` và trạng thái container Consul:

```powershell
docker ps
docker logs <ten-container-consul>
```

### Đăng nhập không hoạt động

Kiểm tra Keycloak tại `http://localhost:9080`, realm `jhipster`, client `web_app`, sau đó kiểm tra log của Gateway.

### Không kết nối được database

Đối chiếu port trong hai file của module:

```text
src/main/docker/mariadb.yml
src/main/resources/config/application-dev.yml
```

Host port của Docker phải trùng port trong JDBC và R2DBC URL. Riêng Gateway hiện đang dùng host port `3307`.

### Node/npm không đúng phiên bản

Ưu tiên wrapper trong Gateway:

```powershell
.\npmw.cmd install
```

Wrapper giúp Maven tải và dùng phiên bản Node/npm được khai báo trong `gateway/pom.xml`.

## 12. Các lệnh hữu ích

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

Xem health của Gateway:

```text
http://localhost:8080/management/health
```

## 13. Thứ tự khởi động khuyến nghị khi đã có hạ tầng chung

1. MariaDB, Redis, Kafka, Consul và Keycloak.
2. Auth Service và User Service.
3. Post Service, Media Service và Comment Service.
4. Feed Service.
5. Gateway backend.
6. Frontend Vite.

Consul phải sẵn sàng trước khi các ứng dụng đăng ký service. Keycloak phải sẵn sàng trước khi thực hiện đăng nhập. Gateway nên chạy sau các microservice để định tuyến đầy đủ ngay khi giao diện được mở.
