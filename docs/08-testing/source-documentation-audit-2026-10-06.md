# Source and Documentation Audit — 2026-10-06

## Baseline

- Source baseline: `develop@a847439`; working tree contains the Phase 1–4 corrections described below.
- Deployable backend applications: `gateway`, `userService`, `postService`, `mediaService`, `commentService`, `feedService`.
- Identity is provided by Keycloak infrastructure; there is no `authService` deployable.
- JHipster 9.3.0 generated structure, Spring Boot 4.1.1, MariaDB, Kafka, Redis, Consul and Zipkin are present in source/configuration.

## Phase status

| Phase | Result | Evidence |
|---|---|---|
| 1. CI baseline | GO (local) | All six backend `mvnw.cmd -ntp clean verify` gates passed, including unit/integration tests and zero Checkstyle violations. Frontend `npm ci` and `npm run build` passed. |
| 2. Documentation sync | GO | Topology corrected to six applications; Feed documented as MariaDB-authoritative; Redis documented as User Service read cache; ADR-005 marked Superseded; CI docs aligned to `clean verify`. |
| 3. Local runtime | GO for the controlled run | Infrastructure was healthy and Gateway 8180 served authenticated health/account/User/Feed requests. The IDE-owned Gateway 8080 process was not restarted. |
| 4. Pattern proof | PARTIAL-GO | Discovery/config, synchronous routing, Kafka fan-out, tracing, and Circuit Breaker failure behavior were observed. Recovery, live Redis cache inspection, isolated backup/restore, and poison-message DLT injection were not completed safely in this run. |

## CI evidence collected on 2026-10-06

| Module | Unit tests | Integration tests | Result |
|---|---:|---:|---|
| Gateway | 37 | 42 | PASS |
| User Service | 84 | 134 | PASS |
| Post Service | 69 | 52 | PASS |
| Media Service | 41 | 90 | PASS |
| Comment Service | 59 | 60 | PASS |
| Feed Service | 55 | 56 | PASS |
| Frontend | — | production bundle | PASS |

Warnings that do not currently fail the gate remain technical debt: dependency-convergence warnings from transitive libraries, deprecated Redis serializers/APIs, and oversized frontend chunks. Vulnerability reports must be triaged separately; do not apply forced dependency upgrades without regression testing.

## Runtime evidence contract

The Phase 3–4 run must capture, without secrets:

1. health for all six applications plus Keycloak, Consul, Kafka, Redis, MariaDB and Zipkin;
2. exactly one passing Consul registration per application and the expected Consul KV keys;
3. an authenticated request through Gateway and at least one synchronous Feign call;
4. Kafka producer → topic → consumer effects, including retry/DLT behavior where safely reproducible;
5. Circuit Breaker outage and recovery evidence, ideally `OPEN → HALF_OPEN → CLOSED`;
6. a Zipkin trace that spans more than one application;
7. Redis cache hit/miss or graceful fallback for User Service;
8. MariaDB persistence and an isolated backup/restore drill.

Compose rendering, source wiring, or Testcontainers alone do not prove the full local distributed flow. Staging and Argo CD remain outside Phase 4 and require their own evidence.

## Controlled runtime observations

- Infrastructure Compose rendered successfully and the local stack exposed healthy Consul, Keycloak, Kafka, Redis, MariaDB and Zipkin containers.
- Gateway 8180 with a service-account JWT returned `200` for `/management/health`, `/api/account`, `/services/userservice/api/user-profiles/me`, and `/services/feedservice/api/feed/me`.
- A post created through Gateway returned `201`; the same post appeared in the MariaDB-backed Feed projection after five polling attempts. This is the observed Post outbox → Kafka → Feed consumer path.
- Zipkin contained multi-service traces, including Gateway→Post Service and an asynchronous trace containing `commentservice`, `feedservice`, and `postservice` with `post-events` spans.
- With User Service deliberately stopped, the Gateway route changed from `500` to `503` after repeated calls, demonstrating the failure path and open-breaker behavior. Recovery could not be rerun after the sandbox denied the restart process; therefore `OPEN → HALF_OPEN → CLOSED` is not claimed.
- User Service Redis outage fallback remains covered by `UserCacheRedisIT`; a live cache-key inspection and live Redis stop/restart were not performed because Docker process control was unavailable in the final sandbox pass.
- MariaDB persistence and Liquibase/Testcontainers coverage are present in the service gates. No destructive backup/restore drill was run against the shared local database.

## Phase 5 boundary

Phase 5 is intentionally not started. Kubernetes/Kustomize or Helm deployment manifests, Argo CD `Application`/`ApplicationSet` and sync policy, External Secrets integration, immutable image promotion/rollback, and staging deployment evidence remain outstanding.
