# Trạng thái hoạt động bạn bè qua WebSocket

## Hành vi và ranh giới

- Presence dùng WebSocket JSON riêng. Comment, reaction và lời mời kết bạn tiếp tục dùng SSE hiện có.
- Một tài khoản online khi có ít nhất một kết nối đã xác thực, chưa hết lease, ở bất kỳ tab/thiết bị/Gateway nào. Tab ở nền vẫn được tính online.
- Đóng kết nối cuối cùng: offline ngay trong Redis; thông báo tới người xem qua Redis Pub/Sub. Mất mạng hoặc Gateway chết: lease 65 giây và sweeper chạy mỗi 5 giây. Đây là thời gian mục tiêu khi hệ thống hoạt động bình thường, không phải cam kết 70 giây ở mọi tải: mỗi lượt sweep xử lý tối đa 100 người, 8 người song song.
- `unknown` khác `offline`: mất socket, lỗi Redis hoặc không xác minh được quyền đều bỏ trạng thái đã biết. Không giữ chấm xanh từ dữ liệu cũ.
- Chỉ bản thân và bạn bè đã chấp nhận được xem presence/lastSeen. Mỗi snapshot/delta đều kiểm tra lại DB quan hệ bạn bè tại UserService. Admin không tự có quyền đọc presence người lạ.
- Không đưa `lastSeenAt` vào DTO hồ sơ công khai hay cache quyền truy cập.

## Luồng xác thực và giao thức

1. Frontend refresh access token qua Keycloak, gọi `POST /api/presence/tickets` bằng Bearer JWT. JWT `sub` là ID tài khoản; server không nhận ID chủ kết nối từ client.
2. API trả `{ticket, expiresAt, authExpiresAt}`, `Cache-Control: no-store`. Ticket ngẫu nhiên, lưu Redis, TTL tối đa 30 giây và không vượt hạn JWT.
3. Mở `/api/presence/ws` bằng `ws:` hoặc `wss:` theo API base URL. Không có token hoặc ticket trong URL. Origin phải khớp danh sách cấu hình chính xác; URL có query bị từ chối.
4. Gửi `{"type":"AUTH","ticket":"..."}` trong 5 giây. Ticket được lấy/xóa nguyên tử. Gateway kiểm tra hồ sơ tồn tại trước khi tạo lease và trả `READY` với `epoch`, `authExpiresAt`.
5. `SUBSCRIBE` / `UNSUBSCRIBE` nhận `userIds` là danh sách UUID; tối đa 100 ID đang theo dõi mỗi socket. Frontend gộp các nơi sử dụng cùng ID và giải phóng khi unmount; thẻ trong danh sách dài chỉ đăng ký khi gần viewport, sidebar ẩn không đăng ký.
6. Server gửi native WebSocket PING mỗi 20 giây với nonce riêng. Trình duyệt tự trả PONG; chỉ PONG khớp nonce còn hạn mới gia hạn lease. JSON `PING` / `PONG` không được dùng thay thế.
7. `PRESENCE_SNAPSHOT` / `PRESENCE_CHANGED` trả `{epoch, sequence, entries, removed}`. Mỗi entry là `{userId, status, lastSeenAt}`. `removed` yêu cầu bỏ dữ liệu riêng tư của những ID không còn đọc được. Sequence tăng trong một socket; frontend bỏ response cũ hoặc thuộc epoch cũ.
8. Redis Pub/Sub chuyển hint thay đổi tới mọi Gateway. Hint không chứa nội dung presence riêng tư; Gateway đọc lại quyền và trạng thái trước khi gửi. Consumer `presenceFriendshipConsumer` dùng group riêng `gateway-presence-permissions` trên `friendship-events`, gửi hint khi kết bạn/hủy kết bạn.
9. Snapshot được kiểm tra lại mỗi 30 giây để khôi phục sự kiện Pub/Sub bị bỏ lỡ, kể cả thay đổi quyền. Đây không phải replay log bền vững.
10. Frontend refresh JWT và xin ticket mới trước khi hết hạn 45 giây; `REAUTH` chỉ được giữ cùng ID. Server yêu cầu refresh bằng `REAUTH_REQUIRED` và trả `REAUTHENTICATED`. JWT hết hạn thì đóng socket.
11. Reconnect có exponential backoff 1–30 giây và jitter; luôn xác thực/đăng ký lại. Watchdog bỏ trạng thái và reconnect nếu kết nối ban đầu treo 10 giây hoặc không có thông điệp ứng dụng trong 75 giây. Logout/đổi tài khoản đóng socket, hủy timer và bỏ callback thế hệ cũ.

Giới hạn inbound frame/message là 8 KiB; hàng đợi gửi tối đa 256 thông điệp, client chậm bị đóng kết nối. Không xác thực hoặc payload sai không được tạo lease.

## Lưu thời gian hoạt động cuối

- Liquibase thêm `user_profiles.last_seen_at` nullable. Dữ liệu cũ không có thời gian thì giữ `null`, UI hiển thị “Ngoại tuyến”.
- Redis TIME là nguồn thời gian quan sát. Lần đăng ký/PONG hợp lệ cập nhật `lastSeenAt` trong metadata, đồng thời ghi pending observation và due index bằng cùng Lua script với cập nhật lease.
- Thời điểm đóng socket/quét hết hạn không tự nâng `lastSeenAt`; chỉ lưu thời gian cuối cùng đã quan sát thấy tài khoản còn sống.
- Worker chạy mỗi giây, tối đa 100 người/lượt, 8 request song song. Khóa 20 giây theo người ngăn nhiều Gateway ghi cùng checkpoint; HTTP request timeout 5 giây.
- Khi online liên tục, các PONG được gộp thành checkpoint cách nhau tối thiểu 60 giây. Khi kết nối cuối đóng/hết hạn, due time được kéo về hiện tại.
- API `PUT /api/internal/presence/{userId}/last-seen` chỉ cho `ROLE_INTERNAL`; SQL chỉ tăng timestamp. Retry/response tới trễ không làm lùi dữ liệu.
- Chỉ ACK observation sau khi API thành công. Nếu có PONG mới trong lúc HTTP đang chạy, observation mới vẫn còn pending; ACK cũ không xóa nó. Lỗi được dời lại 5 giây và tiếp tục retry. Worker/Gateway restart không làm mất pending đã có trong Redis.
- Metadata offline chỉ được đặt TTL 24 giờ sau ACK; dữ liệu lâu dài đọc lại từ SQL. Đăng ký lại bỏ TTL metadata.
- Snapshot hợp nhất timestamp Redis và SQL bằng giá trị mới hơn, sau khi kiểm tra quyền từ DB.

Các key nằm dưới `presence.redis-key-prefix` (mặc định `fakebook:presence`): `user:<id>:connections`, `user:<id>:metadata`, `due`, `pending`, `persist-due`, `persist-lock:<id>`, `ticket:<ticket>`, channel `changes`.

Redis dùng chung giữa Gateway và cần persistence/volume (AOF hoặc cấu hình bền vững phù hợp). Không thể khôi phục observation chỉ có trong Redis nếu mất toàn bộ dữ liệu Redis; checkpoint SQL còn lại vẫn đọc được. Namespace Lua hiện dành cho Redis standalone, không dùng trực tiếp Redis Cluster nhiều hash slot.

Gateway khởi tạo shared connection Lettuce ngay khi startup để việc tạo kết nối ban đầu không chặn thread xử lý HTTP/WebSocket. Redis không kết nối được lúc startup sẽ làm Gateway khởi động thất bại thay vì nhận request rồi mới treo ở bước tạo connection.

## Cấu hình chạy local

| Biến | Ý nghĩa |
|---|---|
| `REDIS_URL` | Redis dùng chung; local mặc định `redis://localhost:6379` |
| `USER_SERVICE_URL` | URL UserService trực tiếp, mặc định `http://localhost:8082` |
| `FAKEBOOK_OIDC_INTERNAL_CLIENT_ID` | OAuth client có service account, mặc định `internal` |
| `FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET` | Secret client trên, không hardcode trong source |
| `FRONTEND_ORIGIN` | Danh sách Origin chính xác, ngăn cách dấu phẩy; mặc định localhost/127.0.0.1 cổng 5173 |

Service account phải có `ROLE_INTERNAL` trong claim `roles` mà resource server đọc. Script có sẵn `infrastructure/keycloak/configure-internal-client.sh` đồng bộ role và mapper; không suy ra client đã được cấu hình chỉ từ application.yml.

- Vite `/api` đã bật proxy WebSocket. Runtime `apiBaseUrl` được dùng cho cả ticket REST và socket; HTTPS tự chọn WSS.
- Nginx `/api/` hiện có Upgrade headers; SSE giữ route và buffering riêng.
- Kubernetes Gateway nhận env từ `gateway-secrets`. Script chuẩn bị Secret local đã bổ sung credential client nội bộ; file mẫu Secret staging có thêm key secret tương ứng. Thay đổi source không tự cập nhật Secret đang chạy.
- Với frontend qua ingress, đặt `FRONTEND_ORIGIN` đúng Origin truy cập thực tế. Traefik hỗ trợ upgrade WebSocket; không cần sticky session vì ticket/lease dùng Redis chung.

## Kiểm chứng tự động

Từ thư mục service tương ứng:

```powershell
# UserService: unit, integration, Liquibase, phân quyền và hồi quy
.\mvnw.cmd -ntp '-Dspotless.skip=true' clean verify

# Gateway backend: bỏ profile webapp quản trị JHipster; frontend ứng dụng ở thư mục riêng
$env:REDIS_URL = 'redis://localhost:6379'
.\mvnw.cmd -ntp '-P!webapp' '-Dspotless.skip=true' clean verify

# Frontend
npx tsc --noEmit
npm run build
npm run test:presence
npm run test:browser
```

`test:presence` dùng Playwright/Chrome; đặt `PLAYWRIGHT_MODULE` nếu Playwright nằm ngoài node_modules dự án, `BROWSER_CHANNEL` nếu cần chọn browser channel khác. Test khởi động Vite ở cổng 5186 và đóng khi xong, dùng route REST/WebSocket có kiểm soát, không dùng tài khoản thật. Ảnh và kết quả nằm trong `.artifacts/presence-browser`.

Các integration test Gateway tự khởi động một Redis Testcontainer riêng cho mỗi JVM, dùng cổng được cấp động. Cả test ứng dụng đầy đủ và các test Redis/WebSocket độc lập đều nhận `spring.data.redis.url` từ container; không cần Redis ở `localhost:6379` và không sử dụng `REDIS_URL` của môi trường phát triển. Khởi động/pull container chạy ở bước thiết lập suite, ngoài timeout 15 giây của từng test. Cần Docker Desktop hoặc Docker trên CI; container test được Testcontainers dọn khi JVM kết thúc.

Môi trường Windows hạn chế attach agent có thể cần ByteBuddy premain qua `JAVA_TOOL_OPTIONS` cho Mockito/BlockHound; đây là cấu hình chạy test, không phải cấu hình ứng dụng. BlockHound chỉ bỏ qua khóa dispatch nội bộ `SharedLock` của Lettuce để không phá writer counter khi khóa có tranh chấp; không bỏ qua toàn bộ I/O của Redis.

Các nhóm kiểm tra:

- Redis: nhiều kết nối, PONG trễ/giả, đóng lặp, sweep đồng thời, lỗi type/metadata, ACK khi có observation mới, retry, khóa giữa Gateway, TTL metadata.
- Socket thật + Redis thật: hai Gateway, tab cuối offline qua Pub/Sub, PING/PONG thật, thu hồi quyền qua hint và reconciliation, lỗi quyền fail closed, một lần dùng ticket, Origin, AUTH timeout, đổi danh tính khi REAUTH, hết hạn JWT, giới hạn 100 ID/8 KiB. Quyền UserService trong nhóm này là fixture được kiểm soát.
- UserService: query quyền chạy SQL thật trong H2 integration, cả hai chiều quan hệ, stranger/missing ID không lộ lastSeen, xóa quan hệ có hiệu lực lần gọi kế tiếp, internal/user/anonymous, timestamp chỉ tăng. Không đại diện cho E2E MariaDB/Keycloak đang chạy.
- Trình duyệt: nguồn trạng thái chung, dữ liệu ban đầu, delta/snapshot, bỏ sequence cũ, last seen, removal, REAUTH, reconnect, đổi tài khoản, unmount, logout, mobile và reduced motion.

## Nghiệm thu runtime sau khi triển khai thủ công

Kết quả local ngày 2026-10-16:

| Phần kiểm tra | Kết quả |
|---|---|
| UserService `clean verify` | PASS: 93 unit + 150 integration, 0 Checkstyle violations |
| Gateway backend `clean verify`, tắt profile `webapp` | PASS: 53 unit + 91 integration, 0 Checkstyle violations |
| Hợp đồng sự kiện friendship bổ sung sau lượt toàn bộ | PASS: 2 unit, deserialize envelope UserService và propagate lỗi để Kafka retry |
| Frontend TypeScript và build | PASS; còn cảnh báo bundle lớn/dynamic import vốn có |
| Browser presence | PASS: trạng thái dùng chung, sequence, quyền, error, REAUTH, reconnect, đổi tài khoản, unmount, mobile, watchdog, logout |
| Browser comment/history/loading hồi quy | PASS; fixture có kiểm soát, không phải backend E2E |

Gateway dùng Redis thật và MariaDB Testcontainers cho các integration test tương ứng. Kiểm thử WebSocket tạo hai server Netty thật, nhưng UserService của nhóm socket được mô phỏng để kiểm soát quyền/lỗi. API UserService được kiểm chứng riêng với DB H2.

Source và kiểm thử local không xác nhận image/Secret/Keycloak của hệ thống đang chạy đã cập nhật. Chưa tự push/deploy/restart dịch vụ.

1. Migration/UserService trước, Gateway sau, frontend cuối cùng. Xác nhận role client nội bộ và Origin; xem DevTools có một WS presence mỗi tab, SSE hiện có vẫn riêng.
2. Hai tài khoản đã kết bạn: mở nhiều tab và một thiết bị khác, kiểm tra online; đóng từng tab, chỉ tab cuối làm offline. Xem `last_seen_at` tiến lên bằng thời gian quan sát, không lùi khi retry.
3. Ngắt mạng/kill một Gateway của môi trường test được phép, chờ lease+sweep; reconnect và restart Redis có volume. Theo dõi pending/retry khi UserService lỗi rồi hồi phục.
4. Hủy kết bạn trong lúc xem: người bị thu hồi không còn thấy online/lastSeen, kể cả sau refresh/reconnect. Thử người lạ và tự giả ID subscription.
5. Chạy qua hơn một chu kỳ token hết hạn, kiểm tra REAUTH không tạo connection mới; logout/login tài khoản khác không thấy dữ liệu cũ.
6. Chạy hai Gateway cùng Redis và consumer group riêng; kiểm tra nhận thay đổi cross-Gateway và khôi phục qua snapshot khi bỏ lỡ Pub/Sub. Kiểm tra desktop/mobile và điều khiển bàn phím ở UI thật.

## Tài liệu framework

- [Spring WebFlux WebSocket](https://docs.spring.io/spring/reference/web/webflux-websocket.html): ghép luồng nhận/gửi và vòng đời session.
- [Spring Data Redis reactive Pub/Sub](https://docs.spring.io/spring-data/redis/reference/redis/pubsub-receiving.html): kênh thông báo và thời điểm hoàn tất subscription.
- [Spring Boot WebClient auto-configuration](https://docs.spring.io/spring-boot/4.0/api/java/org/springframework/boot/webclient/autoconfigure/WebClientAutoConfiguration.html): cấu hình builder HTTP client.
