# Chuẩn bị Kubernetes staging

Overlay `infrastructure/k8s/overlays/staging` là cấu hình mẫu cho bảy workload,
namespace riêng, Service DNS nội bộ, MariaDB/Kafka/Redis/Keycloak external,
HTTPS ingress và External Secrets Operator (ESO). Chưa có hạ tầng staging nên
không apply overlay này vào cluster local. Domain `.example.invalid` cố ý không
hoạt động; render thành công không chứng minh staging deploy được.

## Thay các giá trị trước triển khai

1. Đổi domain app/API/auth, database/Kafka/Redis và endpoint Zipkin.
2. Đổi destination cluster trong `infrastructure/k8s/argocd/fakebook-staging.yaml`.
   Application dùng branch `staging`, auto-sync chưa bật. Đưa cấu hình lên branch
   đó trước khi đăng ký Application. Không bật auto-sync trước preflight.
3. Cài ESO hỗ trợ `external-secrets.io/v1`, chọn provider và cấu hình SecretStore
   tên `fakebook-staging`. `secret-store.yaml.example` minh họa Vault, không phải
   provider đã cấu hình. Dùng workload identity phù hợp provider.
4. Tạo sáu JSON secret objects `fakebook/staging/{service}`. ESO extract thành
   Secret `{service}-secrets`. Tham khảo `secret-payload.json.example`, bỏ các key
   không liên quan cho từng service; dùng DB user riêng, quyền riêng theo schema.
5. Secret gateway cần client ID/secret gateway; các backend cần internal client
   credentials. Media cần thêm Cloudinary. TLS Redis và SASL_SSL Kafka được minh
   họa trong `SPRING_APPLICATION_JSON`, cấu hình cả Spring Kafka và Stream binder.
   Chọn SASL mechanism đúng broker, CA truststore nếu dùng private CA.
6. Cấp chứng chỉ TLS thực vào Secrets `fakebook-app-tls`, `fakebook-api-tls`;
   có thể quản lý bằng cert-manager với Issuer đã được xác minh. Cấu hình HTTPS
   cho Keycloak riêng, issuer phải trùng chính xác JWT `iss`. Đăng ký redirect URI
   và web origin đúng frontend staging, không dùng wildcard cho public client.
7. Tạo schema/user MariaDB, restore vào môi trường thử nghiệm và rehearsal Liquibase
   trước release. URL mẫu yêu cầu xác minh certificate (`verify-full`/`VERIFY_FULL`).

Các dữ liệu thật chỉ nằm trong secret manager/cluster, không commit `.pem`, password,
client secret, dump SQL hoặc token. Nếu GHCR private, thêm pull secret chỉ đọc vào
ServiceAccount/Pod namespace staging, không tái sử dụng token publish.

## Monitoring

`infrastructure/k8s/monitoring/staging-monitoring.yaml.example` gồm ServiceMonitor
cho sáu backend và PrometheusRule availability/restarts. Cần Prometheus Operator,
kube-state-metrics, điều chỉnh release labels và namespace selection của Prometheus.

Actuator `/management/prometheus` hiện được bảo vệ: tạo client monitoring riêng
với service account và quyền mà security configuration hiện yêu cầu (ROLE_ADMIN),
audience hợp lệ. Lưu client ID/secret trong Secret `monitoring-oauth`; giới hạn quyền
tiếp cận credential và đường mạng. Không mở metrics công khai để tránh cần token.
Giám sát frontend bằng HTTPS blackbox probe; Nginx hiện không xuất Prometheus metrics.
Thử scrape thật HTTP 200 và firing/resolved alert trước công nhận monitoring đạt.

## Preflight và promotion

```powershell
kubectl kustomize infrastructure/k8s/overlays/staging
```

Sau khi đã thay mọi placeholder, cài controllers và chuẩn bị secrets/TLS trên staging:

```powershell
kubectl --context <STAGING_CONTEXT> apply --dry-run=server -k infrastructure/k8s/overlays/staging
```

Không chạy apply thực chỉ vì dry-run pass. Kiểm tra provider SecretStore Ready,
ExternalSecrets Ready, database TLS/login, Kafka metadata + auth, Redis TLS/auth,
OIDC issuer/JWKS và registry pull trước sync. Giữ Gateway một replica cho tới khi
realtime broadcast qua nhiều replica được chứng minh.

Promotion staging dùng lại artifacts/digests đã nghiệm thu ở local: tạo PR cập nhật
bảy image trong overlay staging, không rebuild tag cho cùng release. Workflow hiện
tự tạo PR local; staging promotion vẫn là bước PR có kiểm soát và cần pipeline riêng
sau khi có hạ tầng. Nhánh staging phải chứa cấu hình và đúng bản source đã chọn.

Nghiệm thu: Argo revision/digest đúng, bảy workload Ready, login/feed/upload/realtime,
rollback qua Git, Pod recovery và database restore; kết quả local không thay thế staging.
