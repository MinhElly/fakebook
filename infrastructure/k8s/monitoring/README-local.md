# Monitoring local

Với cluster Kind `kind-fakebook-local`, dùng
`docs/07-operations/kind-observability-local.md` cho context, bootstrap ứng dụng,
patch exporter/JVM và cách mở giao diện. Các lệnh `docker-desktop` dưới đây dành
cho Kubernetes tích hợp trong Docker Desktop.

Phạm vi: Docker Desktop Kubernetes, namespace ứng dụng `fakebook-local`.
Manifest: `infrastructure/argocd/monitoring-local.yaml`.

Argo CD lấy chart `kube-prometheus-stack` phiên bản pin `92.10.1` từ Helm
repository của Prometheus Community. Không cần Helm CLI trên máy. Application
được bootstrap bằng `kubectl apply`; thay đổi file local cần apply lại Application,
vì Application này lấy chart upstream, không đọc file local hay nhánh Git Fakebook.

## 1. Kiểm tra và triển khai

Chạy từ `C:\Code\fakebook`:

```powershell
kubectl --context docker-desktop get nodes
kubectl --context docker-desktop get storageclass
kubectl --context docker-desktop -n fakebook-local get deployments
```

Cần node Ready, bảy Deployment sẵn sàng và một StorageClass mặc định
(`(default)`). Chart yêu cầu Kubernetes >= 1.25. Nếu thiếu StorageClass mặc định,
cấu hình storage trước khi triển khai; không xóa PVC để xử lý Pending.

```powershell
kubectl --context docker-desktop apply -f infrastructure/argocd/monitoring-local.yaml
kubectl --context docker-desktop -n argocd get application monitoring-local
kubectl --context docker-desktop -n monitoring get pods -w
```

Nhấn Ctrl+C để thoát theo dõi. Kiểm tra:

```powershell
kubectl --context docker-desktop -n monitoring get pods,pvc,svc
kubectl --context docker-desktop -n monitoring get podmonitors
kubectl --context docker-desktop -n argocd get applications
```

Mục tiêu: các workload monitoring Ready, ba PVC Bound, Application Synced/Healthy.
Nếu bị kẹt, xem operation bằng PowerShell JSON, tránh lỗi escape JSONPath:

```powershell
$monitoringApp = kubectl --context docker-desktop -n argocd get application monitoring-local -o json | ConvertFrom-Json
$monitoringApp.status.conditions | Format-List
$monitoringApp.status.operationState.message
kubectl --context docker-desktop -n monitoring get events --sort-by=.metadata.creationTimestamp
```

## 2. Truy cập giao diện

Trong hai terminal riêng, giữ các lệnh này chạy:

```powershell
kubectl --context docker-desktop -n monitoring port-forward svc/monitoring-grafana 3000:80
```

```powershell
kubectl --context docker-desktop -n monitoring port-forward svc/monitoring-prometheus 9090:9090
```

Grafana: http://localhost:3000, tài khoản `admin`. Lấy mật khẩu tại máy,
không gửi vào chat hoặc commit:

```powershell
$grafanaSecret = kubectl --context docker-desktop -n monitoring get secret monitoring-grafana -o json | ConvertFrom-Json
[System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($grafanaSecret.data.'admin-password'))
Remove-Variable grafanaSecret
```

Prometheus: http://localhost:9090. Mở trang Targets, lọc `fakebook-backends`.
PodMonitor thu trực tiếp từ cổng `http` của sáu backend, đường dẫn
`/management/prometheus`. Không chọn frontend và không sửa Service/Deployment.

Query kiểm tra:

```promql
up{namespace="fakebook-local",app=~"gateway|userservice|postservice|mediaservice|commentservice|feedservice"}
```

Kỳ vọng sáu series, mỗi series bằng 1 khi mỗi backend có một replica. Nếu nâng
số replica thì mỗi pod có một series riêng. Kiểm tra lỗi cụ thể của target nếu
giá trị 0; nếu không có series, kiểm tra PodMonitor, selector và discovery.

## 3. Xem số liệu

Grafana đã được chart provision datasource Prometheus và các dashboard Kubernetes.
Trong Dashboards tìm `Kubernetes / Compute Resources / Namespace (Pods)`, chọn
namespace `fakebook-local`. Nếu tiêu đề dashboard thay đổi, tìm `Compute Resources`.

Trong Explore chọn datasource Prometheus và thử:

```promql
sum by (app) (rate(http_server_requests_seconds_count{namespace="fakebook-local",uri!~"/management.*"}[5m]))
```

```promql
sum by (app) (jvm_memory_used_bytes{namespace="fakebook-local",area="heap"})
```

```promql
histogram_quantile(0.95, sum by (app, le) (rate(http_server_requests_seconds_bucket{namespace="fakebook-local",uri!~"/management.*"}[5m])))
```

Tạo request bằng cách dùng ứng dụng vài phút. Rate cần ít nhất hai lần scrape;
p95 cần histogram bucket và request thực tế. Không có dữ liệu không phải bằng 0.
Các query HTTP trên đo từng backend; để xem lưu lượng từ người dùng, lọc thêm
`app="gateway"`, tránh cộng cả Gateway và các service thành số request đầu vào.

## Phạm vi và giới hạn

- Prometheus scrape/evaluate mỗi 30 giây, retention 3 ngày và giới hạn retention
  theo dung lượng 4GB; điều kiện nào tới trước sẽ giới hạn dữ liệu. PVC 5Gi cho
  Prometheus, 1Gi cho Grafana, 1Gi cho Alertmanager, dùng StorageClass mặc định.
  Retention theo dung lượng không phải hard quota cho WAL/head; theo dõi disk/PVC.
- Giữ dữ liệu qua thay pod bằng PVC; reset cluster hoặc mất volume có thể mất dữ liệu.
- Prometheus request 512Mi và limit 1536Mi RAM. Các thành phần khác cũng có requests
  và limits; sidecar/job có thể dùng thêm. Điều chỉnh theo tài nguyên thực tế.
- Bao gồm Operator, Grafana, Prometheus, Alertmanager, kube-state-metrics,
  node exporter và monitoring Kubernetes theo mặc định chart. Không thu metrics
  etcd/controller-manager/scheduler/kube-proxy trong baseline Docker Desktop này.
- Cảnh báo backend scrape down/missing và các rule mặc định có thể xem trong
  Prometheus/Alertmanager. Chưa cấu hình gửi email, Slack hay kênh bên ngoài.
- Chưa thu metrics Kafka/Redis/MariaDB/Keycloak nằm trong Docker Compose;
  cần exporter/instrumentation và target riêng ở bước tiếp theo.
- Dashboard chỉ truy cập qua port-forward localhost, chưa có Ingress.
- Không cần bật public quyền truy cập các endpoint management khác.
- Mức xác minh runtime được ghi trong evidence của từng lần chạy; không suy ra
  scrape thành công chỉ từ YAML hoặc Argo Healthy.
