# Media cleanup retry and replay

`processMediaCleanup` consumes `media-cleanup-topic` in group `media-service`.
It retries up to three attempts, with exponential backoff starting at one second;
exhausted messages go to `media-cleanup-dlt`. Missing/invalid `mediaId` also fails
processing instead of being acknowledged as success.

The database transaction records `DELETED` only after storage confirms deletion.
A missing media row is a no-op. Cloudinary `ok` and `not found` are successful
outcomes, so retry after a storage success/database failure is safe for the same
immutable storage key. Do not reuse a storage key for a different media object.

Replay is an operator action after the cause has been repaired: inspect the DLT
record and original `mediaId`, verify that it refers to the intended obsolete
object, then republish the original JSON value to `media-cleanup-topic` using the
original key. Preserve source partition/offset and DLT exception headers in the
incident record; confirm the database status and storage result before closing
it. Replay only selected repaired records; never reset production group offsets
or bulk replay malformed records. This change does not execute replay.

Generated Kafka publish/register/unregister/consume APIs and `sse-topic` bindings
have been removed. Business producers use transactional outbox routes. Gateway
business realtime consumers retain their SSE hub and use per-topic DLTs.
