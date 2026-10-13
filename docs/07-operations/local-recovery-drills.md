# Diễn tập recovery local

Phạm vi: `docker-desktop`, namespace `fakebook-local`, infrastructure Docker Compose
local. Đây là phép thử thay đổi runtime, không chạy trên staging/production.

## Pod replacement

```powershell
python scripts/validation/recover-pods-local.py
```

Script xóa lần lượt đúng một Pod của từng Deployment đang có một replica, chờ UID
mới Ready, xác nhận image reference giữ nguyên và HTTP readiness qua ingress.
Gateway một replica nên có gián đoạn kết nối lúc thay Pod; client phải reconnect.
Không xóa database, Kafka hoặc persistent volume. Evidence: `.artifacts/pod-recovery.json`.

## Logical backup/restore

```powershell
python scripts/validation/backup-restore-local.py
```

Script dùng root credential có sẵn bên trong container `fakebook-mariadb`; không
in credential. Dump bảy schema bằng `--single-transaction --routines --events
--triggers --hex-blob`, copy SQL vào `.artifacts/restore-<timestamp>/backup.sql`.
Không đổi dữ liệu nguồn; tránh DDL trong lúc backup.

Restore vào container MariaDB mới, cùng image/version, `--network none`, không
publish port, không mount volume nguồn. Chờ server TCP chính thức Ready; server
socket tạm khi initialize không đủ để công nhận readiness. So sánh exact row counts
mọi base table, gồm migration tracking; lưu SHA256 dump và drift dữ liệu nguồn.
Container thử nghiệm và anonymous volume của nó được xóa sau diễn tập.

Dump chứa dữ liệu/credential ứng dụng và Keycloak: giữ tại thư mục ignored, quyền
truy cập hạn chế, mã hóa nếu đưa ra storage backup. Đây mới là logical restore/count
proof, chưa chứng minh service boot hoặc migration rollback trên database restored.

## Hai tài khoản và SSE

```powershell
python scripts/validation/runtime-proof-local.py --duration 1800
```

Script tạo client direct-grant riêng và hai tài khoản `gitops-proof-*` trong Keycloak
local. Không sửa client `web_app`. Test client có mapper subject UUID, audience
`account`, realm roles, phục vụ HTTP/SSE proof; không thay thế login browser PKCE.
Password được tạo ngẫu nhiên và lưu trong `.artifacts/runtime-proof/accounts.json`;
không in hoặc commit file này. Có thể chạy lại với cùng hai tài khoản: script chỉ
reset friendship giữa chúng, rồi test request/accept, không đụng friendship khác.

Test đi qua ingress với Host app/API đúng: profile của cả hai tài khoản, upload
PNG `purpose=POST`, đọc bytes qua redirect Cloudinary (không chuyển bearer token
hoặc Host header ra provider), tạo post, comment/reaction, friendship realtime,
MariaDB personalized feed và hai SSE streams. Streams nhận heartbeat, reconnect
định kỳ với token mới, lưu số kết nối/event/error và duration.

Feed của người chưa kết bạn không mặc định chứa public post trong `/api/feed/me`:
frontend ghép personalized feed với Post public query. Để chứng minh fan-out, tạo
friendship trước rồi kiểm tra post của A hiện trong feed B.

Evidence nằm trong `.artifacts/runtime-proof/`; không commit credentials hoặc raw
dump. Chỉ đưa kết quả tổng hợp đã kiểm tra vào tài liệu. Sau các diễn tập có thể
xóa riêng test client/accounts và nội dung test theo ID trong evidence; không xóa
các tài khoản/dữ liệu người dùng khác.

## Rollback

Release marker `/release-info.json` phục vụ diễn tập: image A trước thay đổi không
có marker JSON; image B có `gitops-recovery-drill-20261013`. Phát hành B bằng CI/PR,
kiểm tra marker và digests; revert commit promotion B qua PR, xác nhận A trở lại.
Nghiệm thu lại profile/feed/upload/realtime bằng các tài khoản test, kiểm tra không
phát sinh PR image mới do commit revert manifest. Không rollback schema tự động.
