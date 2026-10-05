# Requirement Traceability Matrix

Updated: 2026-10-05 (Asia/Bangkok)

## 1. Baseline và quy tắc bằng chứng

- Checkout được audit: `develop@bf14b43` (fast-forward đồng bộ hoàn toàn với `origin/develop`, tích hợp PR #90 FE/optimize, PR #91 develop, PR #92 staging).
- Tính năng Saved Posts đã tích hợp hoàn chỉnh trong working tree: Entity `SavedPost`, migration Liquibase `20260930000000_added_entity_SavedPost.xml`, controller `PostResource`, và giao diện `SavedPage.tsx`, `Post.tsx`, `RightSidebar.tsx`.
- Mismatch HTTP method trong `searchService.ts` (`DELETE` thay vì `POST` cho `/friend-requests/{requestId}/cancel`) đã được sửa đồng bộ với backend và `friendsService.ts`.
- Công cụ kiểm thử tải đã được loại bỏ; chưa có runner thay thế.
- `docker compose ... config --quiet` đã pass cho local và staging topology.

Các mức bằng chứng:

- **Source**: code/config/test hoặc contract tồn tại tại commit được nêu.
- **Local runtime**: hành vi đã quan sát trên local stack; luôn ghi ngày snapshot.
- **Staging**: hành vi đã quan sát trên staging thật, không suy ra từ Compose.
- **Not proven**: chưa thu thập đủ bằng chứng; không đồng nghĩa với test đã fail.

## 2. Matrix

| Requirement | Trạng thái hiện tại | Source proof | Local runtime proof | Staging proof | Gate tiếp theo |
|---|---|---|---|---|---|
| Service topology | 🟢 Source verified | Bảy backend deployable (`gateway`, Auth, User, Post, Media, Comment, Feed), React frontend và shared infrastructure đã đối chiếu từ source/config tại `bf14b43`. | Snapshot 2026-09-30 (PARTIAL). | Not proven. | Verify full local stack runtime. |
| Service Discovery | 🟡 Source merged; recovery pending | Stable Consul instance ID từ PR #87 (`f57c86d`) dùng application/instance identity có fallback hostname/port. | Snapshot 2026-09-30: Consul healthy; cần kiểm tra lại clean registrations. | Not proven. | Restart Gateway ba lần, yêu cầu đúng một stable passing registration. |
| Central Config | 🟡 Source exists; staging pending | Shared Consul dev/staging config và config loader tồn tại. | Snapshot 2026-09-30: Consul leader/config loader hoạt động. | Effective staging config/hash not proven. | Capture effective config và hash đã mask secrets trên staging. |
| API documentation | 🟢 Updated from source | `docs/04-api/api-overview.md` đã đối chiếu controller, DTO, security, Saved Posts và frontend calls; các path cũ đã sửa. | Không áp dụng. | Không áp dụng. | Sinh/so sánh OpenAPI khi service chạy profile `api-docs`; giữ catalog đồng bộ trong CI. |
| OpenAPI | 🟡 API JSON supported; UI/aggregation unproven | SpringDoc API starter có trong bảy module; `/v3/api-docs` chỉ bật với profile `api-docs` và yêu cầu admin qua Gateway. Không có UI starter hoặc bằng chứng Gateway aggregate docs. | Not proven. | Not proven. | Start profile `api-docs`, lưu JSON của từng service và thêm contract-diff gate. |
| Gateway routing | 🟡 Source verified; authenticated E2E pending | Discovery routing `/services/{serviceId}/**`, `StripPrefix=2`, TokenRelay và readiness matcher tồn tại. | Snapshot 2026-09-30: Gateway health 200. | Not proven. | Kiểm tra thủ công qua Gateway với normal-user token; runner smoke test đã được loại bỏ. |
| Gateway/security policy | 🟠 Contract mismatch | Gateway yêu cầu auth cho Post GET dù Post Service cho anonymous GET; `/api/users` được controller mô tả public nhưng matcher `/api/**` yêu cầu auth; GET Media qua Gateway public cả list metadata. | Chưa có negative/guest test cho ba policy này. | Not proven. | Chọn policy, đồng bộ Gateway/service và thêm 401/403/public integration tests. |
| Authentication / Keycloak | 🟡 Source + historical local proof | OIDC Resource Server, OAuth2 client/login và TokenRelay tồn tại; normal-user flow cần xác minh `/api/account`. | Snapshot 2026-09-30: discovery 200. | Not proven. | Chạy browser/normal-user login, callback, `/api/account` và logout; lưu evidence đã mask. |
| Google login | 🟡 Implemented/configurable; E2E pending | Keycloak IdP setup/login entry tồn tại; secret không được commit. | Redirect/consent/callback not proven. | Not proven. | Test bằng tài khoản test và xác minh `ROLE_USER`, không tự cấp admin. |
| User/Profile | 🟡 Contract verified; E2E pending | `/public`, `/me`, search, detail và suggestions đã đối chiếu; own-profile PATCH chỉ copy allowlisted fields. | Snapshot 2026-09-30: User health 200. | Not proven. | Normal-user profile read/update + avatar/cover replacement. |
| Friend Request | 🟢 Source & client aligned | Backend action dùng `POST /{requestId}/accept|reject|cancel`; constraints/409 mapping từ PR #87. Cả `friendsService.ts` và `searchService.ts` đều đã dùng `POST` cho cancel request. | Duplicate preflight/test đã pass lịch sử. | Not proven. | Chạy E2E gửi/accept/reject/cancel và concurrent accept. |
| Friendship | 🟡 Source/tests proven; concurrency pending | Unique `(user_id, friend_id)`, duplicate HALT, unfriend và friend-ID APIs tồn tại. | Snapshot trước: duplicate preflight không có group trùng; concurrent runtime chưa chạy. | Not proven. | Hai accept đồng thời phải tạo đúng một relation và event idempotent. |
| Follow | 🟡 Source/tests proven; concurrency pending | `/api/follows/**`, unique `(follower_id, following_id)` và 409 mapping tồn tại. | Snapshot trước: duplicate test pass; concurrent follow chưa chạy. | Not proven. | Hai follow đồng thời phải tạo đúng một row; test follow/unfollow qua Gateway. |
| Post & Saved Posts | 🟢 Source verified in HEAD | CRUD/visibility/ownership, reactions, media validation, outbox và Saved Posts (`SavedPost` entity, migration, REST toggle/list) đã tích hợp đầy đủ tại HEAD `bf14b43`. | Snapshot 2026-09-30: health 200 và 56 IT pass trên postService. | Not proven. | Chạy authenticated CRUD và save/unsave flow qua Gateway. |
| Comment | 🟡 Source/tests proven; E2E pending | Create/reply/update/soft-delete, criteria list, batch summaries, reactions, post-cache và comment outbox đã đối chiếu. | Snapshot trước: health/Kafka/Zipkin IT pass; Gateway flow chưa rerun. | Not proven. | Chạy create/reply/update/delete/summary/reaction bằng owner, non-owner và admin. |
| Media | 🟠 Source exists; external/security proof pending | Upload purpose, owner/admin delete, inactive-media visibility và cleanup consumer tồn tại. Public Gateway matcher hiện cho phép toàn bộ GET media. | Snapshot trước: health/Kafka IT pass. | Not proven. | Review public metadata; test POST/AVATAR/COVER, ownership, redirect/file và cleanup. |
| Feed | 🟡 Source/tests proven; contract gap remains | `GET /api/feed/me`, Redis ZSet, MariaDB projection, miss warming và Post/User Feign clients tồn tại. Endpoint nhận `Pageable` nhưng chưa trả pagination headers. | Snapshot trước: health/Kafka/Zipkin IT pass. | Not proven. | Authenticated feed smoke, Redis loss/warm drill; quyết định contract `Link`/`X-Total-Count`. |
| Realtime SSE | 🟠 Single-instance source; scale risk | Gateway `/api/realtime/events` và Kafka consumers tồn tại; `RealtimeEventHub`/emitters là process-local. | Single-client cross-event/reconnect proof chưa được thu thập. | Multi-replica delivery not proven. | Test hai client qua hai replicas, reconnect và reconciliation; thiết kế broadcast cross-pod trước scale. |
| Inter-service HTTP | 🟡 Feign source verified; outage pending | Feed→User/Post, Post→User/Media, User→Media, Comment→User/Post đã được lập matrix; key đúng là `spring.cloud.openfeign.circuitbreaker.enabled`. | Chưa chạy controlled downstream outage. | Not proven. | Dừng downstream thật qua Gateway, xác minh fail-closed response và trace. |
| Circuit Breaker | 🟡 Source/config/test present; recovery unproven | Resilience4j, Gateway filters, Feign `FallbackFactory`, Actuator events tồn tại. | Không có fresh `OPEN -> HALF_OPEN -> CLOSED` evidence. | Not proven. | Lưu request/response, `/management/circuitbreakerevents` và recovery timeline đầy đủ. |
| Transactional Outbox | 🟡 Source/tests proven; live recovery pending | Post/User outbox lưu event cùng transaction, có routing và propagation allowlist. Comment realtime cũng schedule qua outbox. | Unit/IT lịch sử pass. | Not proven. | Kill publisher/broker giữa transaction và publish; xác minh retry, idempotency và không mất event. |
| Kafka messaging | 🟡 Hardened single-node; recovery pending | `post-events`, friendship, friend-request, reaction, comment, media-cleanup và DLT bindings tồn tại; PR #88 bật volume, `acks=all`, idempotence/retry cho staging. | Snapshot trước: broker/consumers healthy. | Not proven; single-node, no HA claim. | Broker/consumer restart, poison message, retry/DLT và replay drill. |
| Distributed Tracing | 🟡 Source and historical spans; chain pending | Sáu thành phần backend có Micrometer/Brave/Zipkin, sampling và Kafka observation; outbox propagation tests tồn tại. | Snapshot 2026-09-30: Zipkin liệt kê bảy application names. | Not proven. | Giữ một trace ID có Gateway/User/Post/Comment/Feed spans và Kafka headers thật. |
| MariaDB | 🟡 Local source/health history; staging recovery pending | Database-per-service, Liquibase (`saved_post` included), constraints và pool limits tồn tại. | Snapshot trước: container healthy và duplicate preflight pass. | Backup/restore/rollback not proven. | Rehearse migration + rollback trên restored staging copy. |
| Redis | 🟡 Durable single-node source; recovery pending | AOF `everysec`, snapshot, volume và Feed warming tồn tại. | Snapshot trước: container healthy. | Not proven; single-node, no HA claim. | Backup/restore, flush/cache-loss và Feed warming drill. |
| Docker Compose local | 🟡 Config GO; runtime snapshot stale | `infrastructure/docker-compose.yml` render pass. | Snapshot 2026-09-30 là PARTIAL vì Auth down. | Không áp dụng. | Start full stack, verify six health endpoints, Consul IDs, Keycloak, Kafka, Redis, Zipkin. |
| Docker Compose staging | 🟡 Config GO only | `infrastructure/docker-compose-staging.yml` với `.env.staging` render pass. | Không áp dụng. | No live staging proof. | Deploy exact SHA, verify secrets/TLS/migrations/health/authenticated smoke/rollback. |
| Load & performance | 🔴 Tooling removed | Công cụ kiểm thử tải đã được loại bỏ khỏi repository; chưa có runner thay thế. | Chưa có run artifact/SLO evidence. | Not proven. | Chuẩn bị công cụ kiểm thử tải trước khi đo; dùng normal-user token, lưu raw summary và commit SHA. |
| Backend CI | 🟡 Historical green | Run `36684555905` green cho `575c738`; PR #90, #91, #92 đã merge lên `origin/develop`. | Không chạy lại Maven trong audit docs này. | GitHub status cho HEAD `bf14b43`. | Chạy per-service `mvnw.cmd -ntp clean test` khi cần release gate. |
| Frontend CI | 🟢 Source fixed; client aligned | Build gate tồn tại; đã sửa `searchService.ts` cancel request thành `POST`. | Không chạy frontend build trong audit docs này. | Staging deploy qua Vercel. | Chạy `npm run build` xác nhận bundle sạch trên HEAD `bf14b43`. |
| Image publishing | 🟡 Proven for protected branches | CI publish immutable images trên protected branch qua GitHub Actions. | Không áp dụng. | Cần deploy exact SHA `bf14b43`. | Deploy theo digest hoặc exact immutable SHA của commit đã qua CI. |
| Automated CD | 🔴 Missing | Chưa có exact-SHA promotion, deployment health polling, authenticated smoke và rollback lifecycle tự động. | Không áp dụng. | Not proven. | Thiết kế promotion + rollback, không suy ra CD từ image publishing. |
| k3s / Kubernetes | 🔴 Missing implementation | Chưa có Deployment/Service/Ingress, Helm hay Kustomize base/overlay. | Không áp dụng. | Not proven. | Tạo Kustomize/Kubernetes-native config cho bảy app; dùng external managed stateful services. |
| Argo CD / External Secrets | 🔴 Missing implementation | Chưa có AppProject/Application/ApplicationSet, sync waves hoặc ExternalSecret resources. | Không áp dụng. | Not proven. | Thêm ESO + secret references, migration wave và immutable-SHA promotion; không commit secret thật. |

## 3. Quyết định hiện tại

- **P0 source tại `bf14b43`: GO.** Working tree đã fast-forward đồng bộ với remote `origin/develop`, code Saved Posts đã hiện diện đầy đủ trong checkout, Liquibase migration đã sẵn sàng.
- **Documentation/API contract: GO.** Catalog `docs/04-api/api-overview.md`, glossary, microservices spec, data-flow, database-architecture và README đã được đồng bộ với code thật.
- **Client/API alignment: GO.** Sửa method `searchService.ts` từ `DELETE` thành `POST`, khớp với controller `FriendRequestResource.java`.
- **Performance baseline: Tooling removed.** Chưa có runner thay thế hoặc run artifact.
- **Local runtime: UNVERIFIED ngày 2026-10-05.** Snapshot 2026-09-30 là PARTIAL GO; Compose render pass không thay thế runtime proof.
- **Staging: NO-GO để tuyên bố hoàn tất.** Chưa có authenticated E2E, external integration, exact-image, recovery hoặc rollback evidence.
- **k3s + Argo CD target: NO-GO để triển khai/scale.** Chưa có manifest/GitOps/External Secrets và realtime vẫn có rủi ro process-local.

## 4. Thứ tự gate đề xuất

1. Chạy frontend build (`npm run build` trong `frontend/`) và per-service Maven verify để bảo đảm zero regression sau khi sync và fix `searchService.ts`.
2. Khởi chạy full local infrastructure (`.\infrastructure\start.ps1`) và các microservices; chạy `e2e-normal-user-smoke-test.ps1`.
3. Kiểm thử luồng Saved Posts mới (POST save/unsave, GET saved) qua Gateway.
4. Chạy Circuit Breaker, Kafka/DLT, Redis/cache warming và database recovery drills.
5. Triển khai k3s + Argo/ESO theo phase riêng sau khi các test local và staging đạt chuẩn.
