# Kind, Argo CD và monitoring local

Cluster: `fakebook-local`, context: `kind-fakebook-local`, chạy trên Docker Desktop.
Kubernetes được pin ở image `kindest/node:v1.34.11`; Argo CD ở `v3.5.5`.
Không dùng context `docker-desktop` cho cluster này.

## Các manifest

- `infrastructure/k8s/kind-local.yaml`: một control-plane; NodePort 30080 ánh xạ
  vào `127.0.0.1:8080` trên Windows. Cổng 80 của host được giữ nguyên.
- `infrastructure/argocd/traefik-kind.yaml`: Traefik NodePort, công bố địa chỉ
  Ingress `127.0.0.1` để Argo đánh giá health mà không cần cloud load balancer.
- `infrastructure/argocd/fakebook-kind.yaml`: Application theo nhánh `develop`,
  overlay local, thêm env `MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED=true` cho
  sáu backend. Profile `prod` tắt exporter mặc định; chỉ có dependency và exposure
  endpoint là chưa đủ. Env này làm rollout pod khi Argo sync.
  JVM local dùng heap 64–256MiB, code cache 64MiB và ActiveProcessorCount=2.
  Rollout không surge, cho phép thiếu một replica để tránh chạy hai JVM/service
  cùng lúc trên VM 8GiB; có gián đoạn ngắn khi thay pod một replica.
- `infrastructure/argocd/monitoring-local.yaml`: chart `kube-prometheus-stack`
  `92.10.1`, PodMonitor cho sáu backend, PVC 5Gi/1Gi/1Gi và retention 3 ngày.
  Grafana dùng Recreate để tránh hai pod ghi cùng SQLite/PVC, startup probe tối đa
  10 phút và giới hạn CPU 2 core. Grafana có RAM limit 768MiB, Go memory limit
  512MiB và SQLite WAL; mức limit 384MiB trước đó không đủ khi mở giao diện.

Các Application chart được bootstrap từ file local. Sửa file cần `kubectl apply`
lại; Argo không tự đọc file trên ổ C. Application Fakebook vẫn lấy workload từ Git.
Không apply `fakebook-local.yaml` thay cho `fakebook-kind.yaml` khi cần giữ patch
exporter của cluster này. Không apply `traefik-local.yaml` thay cho bản Kind.

## Hạ tầng ngoài Kubernetes

MariaDB, Keycloak, Kafka, Redis, Zipkin dùng volume Docker Compose hiện có:

```powershell
docker compose -f infrastructure/docker-compose.yml -f infrastructure/docker-compose.k8s-local.yml up -d mariadb keycloak kafka redis zipkin
```

Override bổ sung Kafka listener 29094 với advertised host `host.docker.internal`
và hostname OIDC `http://auth.fakebook.test:9080`. Pod dùng hostAliases từ overlay
local; kiểm tra lại IP host nếu Docker Desktop thay mạng.

Các Secret được tạo từ `.env`, không nằm trong Git:

```powershell
kubectl --context kind-fakebook-local apply -f infrastructure/k8s/overlays/local/namespace.yaml
.\infrastructure\k8s\sync-local-secrets.ps1 -Context kind-fakebook-local -Namespace fakebook-local
```

## Mở giao diện

```powershell
.\scripts\operations\open-observability-local.ps1
```

Script mở port-forward bằng tiến trình ẩn, chỉ bind `127.0.0.1`:

| Giao diện | URL |
|---|---|
| Argo CD | https://localhost:8443 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

Script không chiếm cổng đã có listener và không tự dừng tiến trình khác. Nếu
workload chưa Ready, chạy lại sau khi pod sẵn sàng. Nếu cluster dừng làm port-forward
kết thúc, chạy script lại. Logs nằm trong `.artifacts/observability-local/`.
Argo dùng chứng chỉ tự ký trong môi trường local.

Cả Argo CD và Grafana dùng tên đăng nhập `admin`. Chạy tại máy để xem mật khẩu,
không gửi vào chat hoặc commit output:

```powershell
$argoLogin = kubectl --context kind-fakebook-local -n argocd get secret argocd-initial-admin-secret -o json | ConvertFrom-Json
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($argoLogin.data.password))
Remove-Variable argoLogin

$grafanaLogin = kubectl --context kind-fakebook-local -n monitoring get secret monitoring-grafana -o json | ConvertFrom-Json
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($grafanaLogin.data.'admin-password'))
Remove-Variable grafanaLogin
```

Nếu đã đổi password Argo và xóa initial secret, dùng mật khẩu mới thay cho lệnh
đọc initial secret. Không reset mật khẩu chỉ vì initial secret đã bị xóa.

## Kiểm tra

```powershell
kubectl --context kind-fakebook-local get nodes
kubectl --context kind-fakebook-local -n argocd get applications
kubectl --context kind-fakebook-local -n fakebook-local get deployments
kubectl --context kind-fakebook-local -n monitoring get pods,pvc
```

Trong Prometheus:

```promql
up{namespace="fakebook-local",app=~"gateway|userservice|postservice|mediaservice|commentservice|feedservice"}
```

Cần sáu backend đều có target bằng 1. Health của pod/Argo không chứng minh scrape
thành công; kiểm tra `lastError` trong Targets nếu `up=0`. Query này trả sáu series
khi mỗi backend có một replica. Dashboard Kubernetes đã được provision trong Grafana;
tìm `Compute Resources` và chọn namespace `fakebook-local`.

Xem thêm `infrastructure/k8s/monitoring/README-local.md` cho các query JVM/HTTP/p95.
Chưa cấu hình gửi cảnh báo ra email/Slack và chưa cài exporter của dependency Compose.

Sau khi mở đủ ba port-forward, chạy kiểm tra tổng hợp và lưu evidence đã bỏ secret:

```powershell
.\scripts\validation\verify-observability-local.ps1
```

Evidence: `.artifacts/observability-local/verification.json`. Script kiểm tra ba
Application Synced/Healthy/Succeeded, bảy Deployment, ba PVC Bound, readiness,
sáu target up=1, Grafana database health và Argo HTTP health. Đây là snapshot,
không thay thế kiểm thử tải hoặc kiểm tra browser end-to-end.

Ingress ứng dụng trên Kind nằm ở `http://app.fakebook.test:8080` và
`http://api.fakebook.test:8080`. Địa chỉ hiển thị trong Ingress không biểu diễn port
8080 trên host. Login browser/redirect/CORS cần được kiểm tra riêng theo origin này;
dashboard và metrics hoạt động không chứng minh toàn bộ hành trình người dùng.

PVC local giữ dữ liệu qua thay pod. Xóa cluster Kind, reset Docker hoặc xóa volume
có thể làm mất dữ liệu; không dùng reset/delete cluster để xử lý lỗi readiness.

## Kết quả triển khai ngày 2026-10-20

- Node `fakebook-local-control-plane`: Ready, Kubernetes v1.34.11.
- Ba Application `fakebook-local`, `traefik-local`, `monitoring-local`:
  Synced/Healthy, operation Succeeded.
- Bảy Deployment Fakebook sẵn sàng; các pod monitoring Running và đủ container Ready.
- Ba PVC monitoring Bound; sáu backend có target Prometheus bằng 1.
- Argo HTTP health trả 200; Grafana 13.2.4 báo database `ok`.
- Kiểm tra đăng nhập admin Grafana qua API thành công, datasource Prometheus
  báo `OK`, provision 25 dashboard. Không lưu password/token trong evidence.
- Gate `verify-observability-local.ps1`: tất cả bảy nhóm kiểm tra đạt.

Evidence runtime ở `.artifacts/observability-local/verification.json` và
`.artifacts/observability-local/grafana-api-verification.json`. Đây là snapshot
sau triển khai, chưa phải bằng chứng ổn định dài hạn, kiểm thử tải hay browser E2E.

Khi chuyển Grafana từ RollingUpdate sang Recreate trên một bản cài cũ, Kubernetes
có thể giữ trường `spec.strategy.rollingUpdate` và từ chối apply. Khi đó cần bỏ
trường cũ trong đúng Deployment Grafana trước khi sync tiếp; không xóa PVC.
Bản cài mới dùng Recreate ngay từ đầu không cần bước chuyển đổi này.

## Khắc phục giao diện localhost:3000 timeout

Ngày 2026-10-20, port-forward vẫn lắng nghe nhưng Grafana mất Ready, HTTP health
timeout và API giao diện trả 401/500/context canceled sau hàng chục giây. Working
set gần giới hạn 384MiB. Đã tăng limit Grafana lên 768MiB, đặt GOMEMLIMIT=512MiB,
bật SQLite WAL và rollout qua Argo CD. Không reset database/PVC hoặc đổi password.

Sau thay pod, chạy lại script mở giao diện để khôi phục port-forward. Truy cập
`http://127.0.0.1:3000` hoặc `http://localhost:3000` bằng HTTP.
Đừng nhầm với HTTPS của Argo CD trên cổng 8443.

Kiểm tra sau sửa: health, login, user API, tìm dashboard và datasource health đều
HTTP 200; thời gian ban đầu 0,1–0,5 giây. Evidence tại
`.artifacts/observability-local/grafana-recovery-http.json` và
`.artifacts/observability-local/grafana-recovery-dashboard.json`.

## Ghi chú sau nâng cấp Post/Media ngày 2026-10-21

Patch JAVA_TOOL_OPTIONS trong Application Kind ghép theo tên container và tên env,
không dựa vào vị trí trong mảng env; các biến OAuth mới không làm lệch patch JVM.
Đã render overlay local cùng các patch Application và xác nhận đủ sáu JVM.

Cluster local đang chạy image Post/Media publication-20261021-v4. Application
fakebook-local đã tắt automated để giữ các image local. Manifest này là cấu hình
bootstrap (automated bật, theo develop), không phải snapshot image local hiện tại.
Chỉ apply lại sau khi đã đối chiếu revision/image mong muốn; việc commit file
không đồng nghĩa đã apply hoặc đã đồng bộ GitOps các image local.
