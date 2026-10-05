# Production Environment Specification

> **Status: Planned / Not deployed**  
> Môi trường Production hiện tại chưa được triển khai. Tài liệu này đặc tả kiến trúc mục tiêu đã được lập kế hoạch và các tiêu chuẩn cần đạt được trước khi đưa hệ thống Fakebook vào vận hành thương mại chính thức.

---

## 1. Kiến trúc Mục tiêu (Planned Target Architecture)

Khi chuyển giao sang môi trường Production, hệ thống sẽ được nâng cấp từ mô hình Single VM Docker Compose sang hạ tầng Cloud Native có khả năng tự động mở rộng (Autoscaling) và sẵn sàng cao (High Availability):

```text
                                Internet (Users)
                                       │
                                       ▼
                       Cloudflare DDoS & Global WAF
                                       │
                      AWS Application Load Balancer (ALB)
                                       │
                        ┌──────────────┴──────────────┐
                        ▼                             ▼
              Frontend (Vercel / S3)       AWS EKS Cluster (Kubernetes)
                                           ├── Ingress Nginx Controller
                                           ├── API Gateway Pods (HPA)
                                           ├── Microservices Pods (HPA)
                                           └── Keycloak Clustered Pods
                                                      │
             ┌─────────────────┬──────────────────────┴─────────────────┐
             ▼                 ▼                                        ▼
   AWS RDS MariaDB Aurora    AWS ElastiCache for Redis       AWS Managed Kafka (MSK)
      (Multi-AZ Cluster)        (Cluster Mode Enabled)          (Multi-AZ 3 Brokers)
```

---

## 2. Khoảng cách kỹ thuật cần hoàn thiện (Production Readiness Gaps)

Trước khi kích hoạt môi trường Production, nhóm phát triển cần thực hiện các hạng mục sau:

1. **Kubernetes Helm Charts & Manifests**: Chuyển đổi toàn bộ cấu hình từ `docker-compose-staging.yml` sang Kubernetes Deployment, Service, Ingress, ConfigMap và Secret.
2. **Quản trị Bí mật tập trung (Secrets Management)**: Tích hợp AWS Secrets Manager hoặc HashiCorp Vault để quản lý mật khẩu cơ sở dữ liệu và client secrets thay vì lưu trữ trong file `.env`.
3. **Database High Availability (HA)**: Kích hoạt Multi-AZ Replication trên AWS RDS MariaDB với tính năng tự động chuyển đổi dự phòng (Automatic Failover).
4. **Hạ tầng Tự động hóa (GitOps)**: Triển khai ArgoCD hoặc FluxCD để đồng bộ tự động trạng thái cluster với Git repository khi có image mới được push lên GHCR.
5. **Observability & Cảnh báo thời gian thực**: Thiết lập cụm Prometheus + Grafana với Alertmanager gửi thông báo qua Slack/Telegram khi tỷ lệ lỗi HTTP 5xx vượt quá 1% hoặc CPU/RAM của container vượt ngưỡng 80%.
