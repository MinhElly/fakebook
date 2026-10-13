# CI/CD Fakebook

Workflow: [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml).
Hướng dẫn cấu hình, phát hành, nghiệm thu và rollback:
[Phát hành qua GitOps](gitops-release.md).

## Kiểm thử và publish

- PR source: validate Compose/Kustomize, kiểm thử script promotion, frontend
  `tsc --noEmit` + `npm run build`, sáu backend `./mvnw -ntp clean verify`.
- Push source vào `develop`, `staging`, `main`, `master`: publish sáu backend qua
  Jib và frontend qua Docker Buildx lên GHCR với tag `sha-<full-40-character-SHA>`.
- Frontend image chứa chính bundle đã kiểm tra; Jib dùng classes sau verify.
- Publisher lưu digest thành artifacts của cùng workflow run.
- Commit chỉ đổi Kubernetes manifests/tài liệu chạy validation và không publish.
- `workflow_dispatch` kiểm thử đầy đủ, không publish.

## Promotion qua PR

Sau push source vào `develop`, chỉ khi cả bảy publisher thành công, job
`promote-local` đối chiếu digests trên GHCR và mở/cập nhật PR
`codex/gitops-local-release`. PR chỉ sửa
`infrastructure/k8s/overlays/local/kustomization.yaml`, chứa tag SHA và digest của
cả bảy image. Cần GitHub App configuration như hướng dẫn GitOps; thiếu cấu hình
thì promotion fail rõ ràng.

Merge PR cập nhật image cho phép Argo CD Application `fakebook-local` theo nhánh
`develop` đồng bộ overlay local. Merge/revert PR image không tạo vòng lặp build.
CI/CD Pipeline Summary phải pass trước merge, nhưng không chứng minh ứng dụng
đã được nghiệm thu trên cluster.

## Ranh giới bằng chứng

YAML render, unit tests hoặc image publish không chứng minh login, feed, upload,
realtime, rollback hoặc backup/restore hoạt động. Xác nhận Argo `Synced/Healthy`,
đúng commit manifest, bảy Deployment Ready, đúng image digests và authenticated
smoke sau mỗi promotion/rollback. Staging chưa có overlay/promotion trong workflow này.
