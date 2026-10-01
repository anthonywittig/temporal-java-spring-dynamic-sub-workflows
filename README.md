# ProcessEventWorkflow POC

This POC tests whether one Temporal workflow type, `ProcessEventWorkflow`, can replace the
Kafka-consumer-to-per-type-workflow hop for about 50 event types. Each event type is a pluggable
handler that `ServiceLoader` discovers.

```
Kafka "events" ─► consumer (Spring Kafka, manual ack)
                    │ parse + validate envelope; malformed ─► "events.DLT"
                    ▼
     untyped stub.start("ProcessEventWorkflow", envelope)
       workflowId = "<type>-<id>", REJECT_DUPLICATE, EventType search attribute
                    ▼
     ProcessEventWorkflowImpl ─► HandlerRegistry.forType(type) ─► EventHandler<T>
                                                                    ▼
                                                     activities (Spring beans in worker)
```

| Module | Contents |
|---|---|
| `api` | `EventEnvelope`, `EventHandler`, `EventHandlerFactory`, `EventTypes`, constants. Depends only on Jackson. |
| `workflow` | `ProcessEventWorkflow` (+Impl), `HandlerRegistry` |
| `handlers` | One package per event type: payload record, activities (+impl), handler, factory |
| `worker` | Spring Boot app. Auto-discovers the workflow and activity beans. Has only a runtime dependency on `handlers`. |
| `consumer` | Spring Boot app: Kafka listener, envelope parser, dispatcher. **Has no dependency on `workflow` or `handlers`.** |
| `testkit` | Sample Kafka messages and captured workflow histories |

Event types: `customer.registered` (simple), `order.placed` (multi-step saga), `refund.requested`
(long-running, with a signal and a timer), and `subscription.cancelled` (the "fourth type").

## Running it

You need JDK 21 (Gradle downloads itself), the `temporal` CLI, `jq`, and either Kafka
(`brew install kafka`) or Docker.

```bash
./gradlew test
```

To run the services and walk through the demo, follow [docs/demo.md](docs/demo.md).

More docs:

- [Adding an event type](docs/adding-an-event-type.md)
- [Workflow code rules for handler authors](docs/workflow-code-rules.md)

## Results against the success criteria

| # | Criterion | Result | Evidence |
|---|---|---|---|
| 1 | Three types run end to end from Kafka | ✅ | Run on a dev server with real Kafka. The captured histories in `testkit/.../histories` come from those runs. `WorkerWiringTest` also covers it. |
| 2 | Adding a fourth type needs no consumer, workflow, or worker config changes | ✅, with one caveat | See commit "Add subscription.cancelled…". It adds 5 new files plus 1 services line. The caveat: it also needs **1 line in `EventTypes`** (see Findings). |
| 3 | Redelivered messages don't create duplicate executions | ✅ | `EventDispatcherTest` and `ConsumerKafkaIntegrationTest`. A redelivery during the live run gave count = 1. |
| 4 | Replay tests pass, and an unsafe change is caught | ✅ | `ReplayTest` covers 7 histories, all types. `NonDeterminismDetectionTest` and `VersioningTest` cover unsafe changes. Swapping two activities in the real `OrderPlacedHandler` failed exactly the two `order.placed` histories. |
| 5 | Executions are filterable by event type | ✅ | Tested with `EventType = "refund.requested" AND ExecutionStatus = "Running"` |

## Findings

**Signals and queries fit the handler abstraction (Phase 3 design risk).** The POC doesn't use a
generic `signal(name, payload)` method on the workflow. Instead, a handler implements its own
interface with `@SignalMethod` / `@QueryMethod` (see `RefundApproval`), and the workflow calls
`Workflow.registerListener(handler)` before `handle`. This gives typed signals and real signal
names in the UI and CLI (`--name refund.decide`), and the workflow class never changes. The SDK
buffers signals that arrive before registration. `signalSentWithStartIsBufferedUntilHandlerRegisters`
covers this. The costs:
- Clients signal through untyped stubs, by name.
- Name uniqueness across handlers is a convention: prefix names with the event type.

**Idempotency depends on `REJECT_DUPLICATE`.** The default reuse policy lets a redelivered
message start a *second* run once the first has closed. Even with `REJECT_DUPLICATE`, dedupe
only covers the namespace retention period. A redelivery after the history is deleted starts a
new run. If producers can replay older topics, size retention for that, or add a dedupe store.

**Exceptions in workflow code are a trap.** A plain exception from workflow code, such as a bad
payload conversion or a bug in a handler, fails the *workflow task*, which retries forever. It
doesn't fail the workflow. The workflow wraps payload conversion and unknown types in a
non-retryable `ApplicationFailure`, and the code rules tell handler authors to do the same. For
production, consider setting `WorkflowImplementationOptions.setFailWorkflowExceptionTypes` as a
safety net.

**The fourth type costs one more line than planned.** The startup checks need a shared
expected-types list, so `EventTypes.ALL` in `api` must also change. The worker refuses to start
if its registry doesn't match, and the consumer refuses to route types outside the list. As a
result, the consumer must be **rebuilt and redeployed**, with no code changes, before it routes
the new type. This also enforces the right rollout order: workers first. If that's unwanted,
the consumer could accept all types and rely on `UnknownEventType` failures in the workflow.

**Handlers share namespaces.** Activity type names, signal and query names, and `getVersion`
change IDs all share one workflow type and task queue. Prefix each with the event type. The
handlers use `@ActivityInterface(namePrefix = "order.placed.")`, which also makes histories
easy to read.

**Blast radius is contained by replay tests, not by isolation.** Reordering activities in
`OrderPlacedHandler` broke only `order.placed` histories, but those histories are the only
protection. Every type needs a captured history, and `ReplayTest` enforces that. For
production, use Worker Versioning so a bad deploy can be pinned or rolled back per build.

**The consumer stalls the partition while Temporal is down.** This is deliberate. Start
failures aren't acked and retry with backoff, capped at 30s, with no limit on attempts, so
nothing is skipped. Messages that can never be processed go to the DLT and are acked only after
the DLT write succeeds.

**`EventType` is set at start.** The consumer sets the search attribute in `WorkflowOptions`, so
it's visible before the first workflow task. The workflow upserts it only when a different
client started the workflow without it.

## Open questions for the remaining 47 types

- **Task queue and worker pool needs:** The POC puts every type on one task queue. Any type
  that needs its own rate limits, scaling, or isolation should stay a separate workflow or a
  child workflow on its own queue.
- **History size:** These three types produce short histories (at most 35 events). The refund
  approval waits up to 3 days on one timer. Any type that loops or waits on many signals needs
  continue-as-new. `ProcessEventWorkflow` would need a generic way to carry handler state across
  it. That isn't built.
- **Per-type retention:** Retention is set per namespace. Types that need different retention
  need different namespaces.
