# Source package conventions

Business events in active producers/listeners belong to `service/event/`.
User Service retains deprecated `service/dto/events` records solely to deserialize
existing Namastack outbox rows: the library persists a Java class name and resolves
it with the context classloader. `OutboxCompatibilityTest` checks old/new JSON shapes.
Do not remove these compatibility records until pending/failed historical rows have
been accounted for. This change does not modify or delete runtime outbox data.

Downstream response types belong to `client/dto/`; HTTP response field names stay
unchanged. Classes implementing OpenFeign `FallbackFactory` use the `Factory`
suffix. Gateway Kafka consumer configurations belong to `broker/`; its SSE hub,
connection handling and realtime response models remain in `realtime/`.

The obsolete `PostCreateEvent` has no source references and was removed. The
producer's active contracts are `PostCreatedEvent`, `PostUpdatedEvent`, and
`PostDeletedEvent` in `service/event/`.
