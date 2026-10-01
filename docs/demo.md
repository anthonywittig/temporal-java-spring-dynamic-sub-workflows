# Demo script

Prerequisites: JDK 21, the `temporal` CLI, Kafka (`brew install kafka`) or Docker, and `jq`.

## 1. Start everything (four terminals)

```bash
scripts/temporal-dev.sh          # or: docker compose up
scripts/kafka-dev.sh
./gradlew :worker:bootRun
./gradlew :consumer:bootRun
```

The worker logs `Registered handlers for event types [...]`, and the consumer logs
`Routing event types [...]`. The Temporal UI is at http://localhost:8233.

## 2. Send one event of each type

```bash
for e in customer.registered order.placed order.placed-shipping-fails \
         refund.requested-small refund.requested-large subscription.cancelled malformed; do
  scripts/send-event.sh "$e"
done
```

Show in the UI or CLI:

- Every execution has the same workflow type, `ProcessEventWorkflow`, and IDs of the form
  `<type>-<eventId>`.
- `order.placed-o-2002` failed after `RefundPayment` and `ReleaseInventory` ran (compensation in
  reverse order).
- `refund.requested-r-3002` is still running, waiting for approval.
- The consumer log shows `Dead-lettering ... Missing 'id'` for the malformed message.

## 3. Filter by event type

In the UI, enter this list query:

```
EventType = "refund.requested" AND ExecutionStatus = "Running"
```

Or use the CLI:

```bash
temporal workflow list --query 'EventType = "refund.requested" AND ExecutionStatus = "Running"'
```

## 4. Signal and query the long-running handler

```bash
temporal workflow query -w refund.requested-r-3002 --name refund.status
```

```bash
temporal workflow signal -w refund.requested-r-3002 --name refund.decide \
  --input '{"approved":true,"approver":"mgr-ana","comment":"ok"}'
```

```bash
temporal workflow result -w refund.requested-r-3002
```

## 5. Redeliver to show dedupe

```bash
scripts/send-event.sh order.placed
```

```bash
temporal workflow count --query 'WorkflowId = "order.placed-o-2001"'
```

The consumer logs `Duplicate event, workflow order.placed-o-2001 already exists`, and the count
stays at 1.

## 6. Add a type live

Walk through `git log --stat --grep subscription.cancelled`, or follow
[adding-an-event-type.md](adding-an-event-type.md) with a new type. Then restart the worker
first and the consumer second, and send the new event.

## 7. Show replay protection

```bash
./gradlew :worker:test --tests '*replay*'
```

To show it live, swap the `reserveInventory` and `chargePayment` blocks in
`OrderPlacedHandler`, rerun the tests, and point out that only the two `order.placed` histories
fail. Then revert.
