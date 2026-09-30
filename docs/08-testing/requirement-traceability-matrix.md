# Requirement Traceability Matrix

Updated: 2026-09-30 (Asia/Bangkok)

Evidence levels are intentionally separate:

- **Source**: code/config/test exists and has passed the stated local gate.
- **Local runtime**: behavior was observed against the local Docker/runtime stack.
- **Staging**: behavior was observed against the deployed staging environment.
- **Not proven** is not equivalent to failed; it means the required runtime evidence has not been collected.

| Requirement | Source proof | Local runtime proof | Staging proof | Priority / next gate |
|---|---|---|---|---|
| Service Discovery | P1 ready on `f57c86d`: stable IDs use `${service}:${SPRING_APPLICATION_INSTANCE_ID}` with hostname/port fallback for Gateway and seven services. | Consul health was passing for Gateway and five running business services before the change. Stable-ID restart gate is not proven. | Not proven. | P1: restart Gateway three times, keep exactly one passing registration on port 8080, then deregister only identified stale IDs. |
| Central Config | Shared dev/staging Consul config exists. Staging tracing endpoint is configured. No independent production central config is added by design. | Consul KV loading was observed locally. | Not proven. | P1: validate effective staging config and retain rendered/config hash evidence. |
| API Gateway | Discovery routing, TokenRelay and prefix stripping are present. Authenticated smoke script is ready on `f57c86d`. | Gateway health returned 200; unauthenticated protected routes returned the expected 401. Normal-user flow is not proven. | Not proven. | P1: run `scripts/e2e-normal-user-smoke-test.ps1` with `FAKEBOOK_USER_ACCESS_TOKEN`. |
| Interservice Communication | Feign clients and fallbacks exist. OpenFeign circuit-breaker namespace was corrected in P0. | Service health is proven; failure/fallback behavior is not yet exercised end to end. | Not proven. | P1: inject dependency failures and retain fallback/trace evidence. |
| Messaging | P0 `52de054` persists friendship, friend-request and media-cleanup events through transactional outbox with routing and trace propagation. | User/Post verification passed; local broker and consumers were healthy. Broker restart/redelivery drill is not proven. | Not proven. | P1: execute Kafka restart/redelivery/DLT drill. |
| Circuit Breaker | Configuration source is present for Feign services. | No fresh runtime open/half-open/recovery proof. | Not proven. | P1: run controlled dependency-failure drill. |
| Distributed Tracing | P0 `356ea84` adds Brave bridge/Zipkin dependencies, dev/prod/staging endpoints, Kafka observation and anti-`NoopTracer` tests for seven services. | Zipkin received spans from `gateway`, `auth`, `user`, `post`, `media`, `comment` and `feed`. Authenticated single-trace business flow is not proven. | Not proven. | P1: retain one trace ID spanning Gateway plus User/Post/Comment/Feed. |
| Keycloak | OIDC/TokenRelay configuration exists. | Local discovery and login page were available. | Not proven. | P1: verify staging discovery and login/callback. |
| Third-party Login | Google IdP configuration script and login entry exist. | Redirect/consent/callback not proven. | Not proven. | P1: run with a test account and retain masked callback evidence. |
| User/Profile | JWT-owned profile endpoints and source tests exist. | Service health passed; fresh authenticated profile flow is not proven. | Not proven. | P1 authenticated smoke. |
| Friend | P1 `f57c86d` adds a duplicate-data HALT precondition, unique `(user_id, friend_id)` constraint, flush-on-write and HTTP 409 mapping. | MariaDB preflight found no duplicates. User Service duplicate REST test passed. Concurrent accept runtime test is not proven. | Not proven. | P1: run two concurrent accepts; require one relation and no duplicate row. |
| Follow | P1 `f57c86d` adds a duplicate-data HALT precondition, unique `(follower_id, following_id)` constraint, flush-on-write and HTTP 409 mapping. | MariaDB preflight found no duplicates. User Service duplicate REST test passed. Concurrent follow runtime test is not proven. | Not proven. | P1: run two concurrent follows; require one relation and no duplicate row. |
| Post | CRUD, reaction and post outbox source exists; cleanup event now uses outbox in P0. | Post Service `clean verify` passed with 56 integration tests. Fresh Gateway CRUD is not proven. | Not proven. | P1 authenticated smoke and Cloudinary cleanup flow. |
| Comment | Permission projection, batch summary and comment outbox source exists. | Service health and Kafka/Zipkin IT passed. Gateway summary flow not proven. | Not proven. | P1 authenticated smoke. |
| Media | Ownership and Cloudinary flow source exists; cleanup uses transactional outbox. | Service health and Kafka/Zipkin IT passed. Real Cloudinary upload/delete not proven. | Not proven. | P1 external integration proof with masked IDs. |
| Feed | Friendship lifecycle, Redis miss warming and batch comment hydration source exists. | Service health and Kafka/Zipkin IT passed. Normal-user Gateway visibility/reconciliation flow not proven. | Not proven. | P1 authenticated smoke plus cache-loss/warming drill. |
| MariaDB | Liquibase migrations and pool limits exist. P1 relation constraints are ready on `f57c86d`. | Local database healthy; relation duplicate preflight returned zero duplicate groups. | Backup/restore and rollback rehearsal not proven. | P1: rehearse only on a restored staging copy. |
| Kafka | Outbox/DLT/source configuration exists. P1 `216ae15` adds persistent local volume and staging producer `acks=all` plus idempotence. | Local broker healthy. Recovery/redelivery drill not proven. | Not proven. Single-node by accepted staging design; no HA claim. | P1 infrastructure drill. |
| Redis | Feed warming/cache source exists. P1 `216ae15` enables AOF `everysec`, snapshots and persistent local volume. | Local Redis healthy. Backup/restore and cache-loss warming drill not proven. | Not proven. Single-node by accepted staging design; no HA claim. | P1 infrastructure drill. |
| Docker | Local and staging Compose render successfully after P1 hardening. Healthchecks and restart policies exist for Kafka/Redis. | Gateway and five business services returned health 200 before P1 restart. | No running staging proof collected. | P1: execute recovery runbook and retain evidence. |
| Kubernetes | No manifest, Helm chart or Kustomize source. | Not applicable. | Not proven. | P0 only if the delivery rubric requires Kubernetes. |
| CI | P0 commits `356ea84` and `52de054` are on `develop`. | Local gates: User 76 unit + 134 integration before P0; Post 56 integration; seven Kafka/Zipkin IT suites 28/28. P1 User gate: 79 unit + 137 integration. | GitHub Actions run `36682281879` for `52de054` completed successfully, including configuration, frontend and all seven backend jobs. | P1 branches require pull-request CI after PR creation. |
| CD | Immutable image build/push exists; no automated exact-SHA staging deployment/health/smoke/rollback job. | Not applicable. | Not proven. | P0 if automated deployment is required. |
| ArgoCD | No Application/ApplicationSet or GitOps source. | Not applicable. | Not proven. | P0 only if the delivery rubric requires ArgoCD. |

## Current decision

- **P0: GO.** Both P0 commits are pushed and the develop CI run is green across all seven backend jobs.
- **P1 source: PARTIAL GO.** Local-correctness and staging-hardening changes are isolated on separate branches and locally validated.
- **P1 runtime/staging: NO-GO for completion.** Stable Consul restart proof, authenticated Gateway trace, Google/Keycloak/Cloudinary proof, and recovery drills remain outstanding.
- Production-only Kafka/Redis HA is a documented risk acceptance; staging remains hardened single-node.
