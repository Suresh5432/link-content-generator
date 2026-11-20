# RabbitMQ event operations

RabbitMQ is a shared transport, not a shared database. Durable topology is declared by service configuration when consumers start. Producers/consumers use versioned routing keys; inspect the shared `backend/common/event-contracts` module and service config when changing a contract. Keep event IDs, correlation IDs, timestamps, and schema versions compatible across producers and consumers.

## Exchanges and main routes

| Exchange | Routing key | Queue(s) | Flow |
|---|---|---|---|
| `link.events` | `link.submitted.v1` | `extraction.link-submitted` | Link Service to Extraction Service |
| `extraction.events` | `content.extracted.v1` | `summarization.content-extracted` | Extraction Service to Summarization Service |
| `link.events` | `summary.generated.v1` | `link.summary-generated`, `notification.summary-generated` | Summary Service to Link and Notification Services |
| `link.events` | `extraction.failed.v1` | `link.extraction-failed`, `notification.extraction-failed` | Extraction failure handling |
| `link.events` | `summarization.failed.v1` | `link.summarization-failed`, `notification.summarization-failed` | Summary failure handling |

Additional retry and dead-letter topology is declared in service `RabbitMQConfig` classes. Current names include `extraction.link-submitted.retry`, `extraction.link-submitted.dlq`, `summarization.content-extracted.retry`, `summarization.content-extracted.dlq`, `link.summary-generated.retry`, `link.summary-generated.dlq`, plus `link.extraction-failed.*` and `link.summarization-failed.*` retry/DLQ queues. Retry queues currently use a 10-second TTL and code-defined bounded attempts. Verify actual bindings and queue arguments in the RabbitMQ management UI before operating a live deployment; code-level declarations do not prove broker topology or recovery behavior.

## Operations and recovery

- Inspect queue depth, unacked messages, consumer count, and publish/return rates at `http://localhost:15672` on the local Compose host.
- Do not purge queues or requeue a DLQ in bulk without identifying event IDs and the consumer failure cause. Preserve a copy/export of failed messages for diagnosis.
- Before replay, determine whether the event was already applied. Consumers use event identity/idempotency state, but replay behavior must be validated against the deployed database and event version.
- Recover a DLQ by correcting the cause, selecting bounded messages, and republishing to the original exchange/routing key with the original event identity and correlation metadata. Confirm the destination queue is bound and observe successful processing.
- Publisher confirm, mandatory return, ack, retry, and DLQ paths are runtime behavior; validate them with broker interruption, unroutable messages, duplicate delivery, and consumer restart scenarios.

Credentials should use a dedicated RabbitMQ application account. The current Compose setup provides one configured app account; production deployments should assess per-service vhosts/users and permissions before exposure beyond the local network.
