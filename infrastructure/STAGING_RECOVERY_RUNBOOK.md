# Staging recovery runbook

This staging topology intentionally keeps one Kafka broker and one Redis node. Persistence and recovery are tested, but this is not high availability. Production must use multi-node or managed Kafka and Redis.

## Preconditions

- Work only against staging and load the intended `.env.staging` file explicitly.
- Record the deployed immutable `IMAGE_TAG` and take a database snapshot before a destructive drill.
- Keep secrets in the environment; never paste them into this file or command history.

## Kafka restart and redelivery drill

1. Record topic offsets and consumer-group lag with `kafka-consumer-groups.sh` from the Kafka container.
2. Create one test Post through the authenticated Gateway smoke test and record its event ID.
3. Stop only the Kafka container, create another test Post, then start Kafka again.
4. Verify the outbox record retries and becomes `COMPLETED`, the Feed receives the Post once, and DLT remains empty.
5. Recheck consumer lag and confirm the broker is using the persisted `kafka-data` volume.

Acceptance: no acknowledged event is lost, redelivery is idempotent, and the broker recovers from its persisted log.

## Redis backup and recovery drill

1. Run `redis-cli BGSAVE` and wait until `LASTSAVE` changes.
2. Copy the resulting RDB/AOF files from `/var/lib/fakebook-staging/redis_data` to protected backup storage.
3. On a disposable staging copy, stop Redis, replace the data directory with the backup, and start Redis.
4. Verify `redis-cli ping`, then load `/api/feed/me` through Gateway.
5. Flush only the disposable cache and verify Feed cache warming rebuilds data from MariaDB/Post Service.

Acceptance: Redis restores successfully, and loss of cache data does not cause permanent Feed data loss.

## MariaDB backup, restore, and migration rollback

1. Create an RDS snapshot or logical backup of all Fakebook schemas.
2. Restore it to a separate staging database; never rehearse rollback on the active database.
3. Start one service at a time against the restored database and run its readiness plus smoke checks.
4. Apply pending Liquibase changes, verify constraints and data counts, then run Liquibase rollback for the target tag/change count.
5. Reapply the changes and rerun the authenticated Gateway smoke test.

Acceptance: restore is usable, migrations apply twice without drift, rollback removes only the intended schema change, and business data counts remain stable.

## Evidence to retain

- Timestamp, immutable image SHA, Compose render hash, backup identifier, commands executed, before/after counts, Kafka offsets, Redis `LASTSAVE`, health results, and Zipkin trace ID.
- Mask tokens, passwords, client secrets, Cloudinary credentials, database endpoints, and private IP addresses.
