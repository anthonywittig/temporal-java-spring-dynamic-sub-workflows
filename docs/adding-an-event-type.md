# Adding an event type

For a worked example, see the commit "Add subscription.cancelled as the fourth event type"
(`git log --stat --grep subscription.cancelled`). Nothing in `consumer/`, `workflow/`, or
`worker/src/main` changes.

## Checklist

1. **Payload record.** Create `handlers/src/main/java/com/example/poc/handlers/<type>/`, and
   add a record for the payload (for example, `SubscriptionCancelled`). Unknown JSON fields are
   ignored, so producers can add fields first.
2. **Activities.** Add an `@ActivityInterface(namePrefix = "<event.type>.")` interface. The
   prefix keeps activity names unique on the shared task queue. Implement it as a
   `@Component @ActivityImpl(taskQueues = PocConstants.TASK_QUEUE)` class in the same package.
   The worker finds it by component scan.
3. **Handler.** Implement `EventHandler<YourPayload>`. Create activity stubs in fields with
   options that fit each call. Follow [workflow-code-rules.md](workflow-code-rules.md).
   For signals or queries, also implement an interface with `@SignalMethod` / `@QueryMethod`
   names prefixed with the event type (see `RefundApproval`).
4. **Factory.** Implement `EventHandlerFactory` that returns the type string and a new handler.
5. **Register.** Add the factory's class name to
   `handlers/src/main/resources/META-INF/services/com.example.poc.api.EventHandlerFactory`.
6. **Expected types.** Add the type to `EventTypes` and `EventTypes.ALL`. The worker refuses to
   start, and `HandlerRegistryConsistencyTest` fails, until steps 5 and 6 agree.
7. **Tests.** Add a handler test with mocked activities (copy `SubscriptionCancelledHandlerTest`)
   and a sample message in `testkit/src/main/resources/events/`.
8. **Replay history.** After the first real run, capture a history and add it to
   `ReplayTestSupport.HISTORIES`. `ReplayTest.everyEventTypeHasAtLeastOneCapturedHistory` fails
   until you do.

   ```bash
   scripts/capture-history.sh <event.type>-<id> <event.type>-v1
   ```

## Rollout

Deploy **workers first**, then the consumer. Rebuild the consumer so it gets the new `EventTypes`,
but its code doesn't change. Until the consumer is redeployed, it dead-letters the new type as
unsupported. To hold a type back, list the allowed types in `poc.consumer.event-types`.

All workers on the task queue must run the same handler set. A workflow task can land on any
worker, so a worker without the handler fails the execution with `UnknownEventType`.

## Changing an existing handler

Any change to the sequence of commands a handler issues (activities, timers, child workflows,
signals sent, search attribute upserts) must be wrapped in `Workflow.getVersion`, with a change
ID prefixed by the event type. See `CustomerRegisteredHandler` and `VersioningTest`. Run the
replay suite before merging. Because every type shares one workflow class, the suite covers
in-flight executions of every type.
