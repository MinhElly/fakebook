# Phát hành local qua CI → GHCR → Git → Argo CD

Phạm vi: sáu backend và frontend trong namespace `fakebook-local` trên Docker
Desktop Kubernetes. Quy trình này không chứng minh staging hoặc authenticated E2E.

## 1. Cấu hình GitHub một lần

Tạo GitHub App và cài vào riêng repository `MinhElly/fakebook`:

- Repository permissions: **Contents: Read and write**, **Pull requests: Read and write**.
- Repository variable `GITOPS_APP_ID`: App ID.
- Repository Actions secret `GITOPS_APP_PRIVATE_KEY`: private key PEM của App.

Không commit private key hoặc đưa vào log. Workflow lấy installation token ngắn hạn,
giới hạn vào repository hiện tại. GitHub App token cho phép PR do bot tạo kích hoạt
workflow `pull_request`; dùng `GITHUB_TOKEN` cho bước tạo PR không bảo đảm điều đó.
Xem [GitHub workflow triggers](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow).

Bật branch protection/ruleset cho `develop`: yêu cầu review, CI/CD Pipeline Summary
thành công, và cập nhật branch với base mới nhất trước merge. Điều này ngăn PR
phát hành cũ được merge sau khi source mới đã vào `develop`. Chỉ merge PR có SHA
nguồn đúng bản muốn phát hành; không bật auto-merge trong lần diễn tập đầu.

GHCR packages phải cho repository Actions quyền publish và cluster quyền pull.
Nếu package private, cấu hình `imagePullSecrets` cho Pod hoặc ServiceAccount trong
namespace bằng credential chỉ có quyền đọc package. Không commit credential.

## 2. Hành vi workflow

PR source chạy Compose/Kustomize validation, release-script tests, frontend typecheck
+ build và `clean verify` trên sáu backend. PR không publish hoặc tạo PR khác.

Push source vào `develop/staging/main/master` publish cả bảy image với tag
`sha-<40-character-source-SHA>`. Frontend được đóng gói bằng `Dockerfile.ci` từ chính
`dist` đã build; backend `jib:build` dùng classes sau `clean verify`, không chạy lại
`package -DskipTests`. Mỗi publisher lưu JSON digest thành artifact riêng.

Chỉ push source vào `develop` tạo/cập nhật PR `codex/gitops-local-release`:

1. Chờ frontend và toàn bộ matrix backend thành công.
2. Từ chối promotion nếu có source mới hơn trong `develop`.
3. Download đúng artifact từ cùng CI run; yêu cầu đủ bảy image cùng SHA/registry.
4. Đọc lại GHCR tags và so sánh digest với artifact publisher.
5. Ghi `newTag: sha-...` và `digest: sha256:...` cho bảy image trong overlay local.
6. Render Kustomize, rồi mở PR chỉ chứa `kustomization.yaml`.

Kustomize render reference có digest; tag giữ khả năng truy vết, digest quyết định
artifact thực tế được pull. Một lần rerun có thể publish lại tag SHA, nhưng manifest
đã pin digest vẫn chọn đúng artifact đã promotion.

Commit chỉ đổi `infrastructure/k8s/**`, tài liệu hoặc Markdown không build/publish;
vẫn chạy config validation và tests. Vì vậy merge/revert PR image không gây vòng lặp.
`workflow_dispatch` chạy kiểm thử đầy đủ nhưng không publish/promotion.

Nếu GitHub App chưa cấu hình, `promote-local` và summary fail rõ ràng sau publish;
overlay đang chạy không bị cập nhật. Cấu hình App rồi rerun workflow source.

## 3. Argo CD và nghiệm thu

Application `fakebook-local` cần theo repo `https://github.com/MinhElly/fakebook.git`,
`targetRevision: develop`, `path: infrastructure/k8s/overlays/local`, destination
namespace `fakebook-local`, server `https://kubernetes.default.svc`.

Auto-sync đang dùng `enabled: true`, `selfHeal: true`, `prune: false` ở thời điểm
kiểm tra 2026-10-13. CI không gọi Kubernetes/Argo API để triển khai. Sau merge PR,
Argo CD đồng bộ desired state trong Git.

Chạy PowerShell sau merge:

```powershell
kubectl -n argocd get application fakebook-local
kubectl -n argocd get application fakebook-local -o jsonpath='{.status.sync.revision}'
kubectl -n fakebook-local get deployments -o wide
kubectl -n fakebook-local get pods -o custom-columns='NAME:.metadata.name,IMAGE:.spec.containers[*].image,IMAGE_ID:.status.containerStatuses[*].imageID'

$workloads = 'gateway','userservice','postservice','mediaservice','commentservice','feedservice','frontend'
foreach ($workload in $workloads) {
    kubectl -n fakebook-local rollout status "deployment/$workload" --timeout=180s
    if ($LASTEXITCODE -ne 0) { throw "Rollout failed: $workload" }
}
```

Yêu cầu: `Synced/Healthy`, sync revision đúng commit merge PR manifest, bảy Deployment
Ready và image references khớp digests trong PR. ImageID frontend có thể là digest
platform manifest, khác digest OCI index; đối chiếu platform manifest nếu cần.
SHA nguồn build và SHA commit merge PR manifest là hai giá trị khác nhau.

Test qua frontend K8s, dùng hai tài khoản trong hai phiên trình duyệt:

- Login/logout, feed và phân trang.
- Upload ảnh, mở bài để xác nhận đọc được file.
- Comment, reaction, lời mời kết bạn cập nhật ở phiên bên kia không reload.
- Giữ kết nối 30–60 phút, ngắt/nối mạng rồi xác nhận SSE reconnect và đối soát dữ liệu.

Ghi thời gian, SHA nguồn, SHA manifest, digest, kết quả từng bước và lỗi. `Healthy`
không thay thế phép thử này. Một replica Gateway chưa chứng minh realtime đa replica.

## 4. Diễn tập rollback

Sau khi có release A đã nghiệm thu, đổi một nội dung frontend dễ thấy, không đổi
schema dữ liệu. Phát hành release B bằng quy trình trên và kiểm tra chức năng.

Trên nhánh riêng, revert commit promotion B rồi tạo PR vào `develop`. Ví dụ nếu PR
promotion dùng squash merge, thay SHA bên dưới bằng SHA squash của PR image:

```powershell
git switch -c codex/rollback-local-release origin/develop
git revert <PROMOTION_SQUASH_COMMIT_SHA>
```

Chỉ stage/commit file trong phạm vi rollback; giữ nguyên các thay đổi cá nhân khác.
Nếu dùng merge commit thay vì squash, kiểm tra parent trước khi dùng `git revert -m 1`.
Merge PR rollback, đợi Argo sync rồi xác nhận lại toàn bộ bảy image và bốn nhóm chức
năng login/feed/upload/realtime. Workflow chỉ validate manifest và không tạo release C.

Không dùng `kubectl rollout undo` để thay desired state trong quy trình này:
self-heal sẽ đưa cluster về Git. Argo history rollback không thực hiện được khi
automated sync bật. Xem [Argo automated sync](https://argo-cd.readthedocs.io/en/stable/user-guide/auto_sync/).
Rollback image không rollback database; giữ các image/digest cũ trong GHCR và chỉ
diễn tập thay đổi tương thích schema trước khi có kế hoạch migration/restore.

## 5. Phần tiếp theo

Sau release và rollback: restart từng workload, kiểm tra readiness và SSE reconnect;
backup/restore MariaDB vào database thử nghiệm tách biệt; rồi tạo overlay staging,
Secrets, TLS, địa chỉ MariaDB/Kafka/Redis/Keycloak external và monitoring.

Chưa được ghi PASS cho publish/PR/Argo rollout/rollback/browser E2E chỉ từ việc YAML
render và tests local thành công. Các bước đó cần evidence từ CI run và runtime.
