# Workflow code rules for handler authors

`EventHandler.handle` and everything it calls runs as workflow code. Temporal rebuilds a
handler's state by replaying its history, so this code must make the same decisions in the same
order every time it runs.

## Don't

- Do I/O of any kind: HTTP, database, Kafka, files, or reading Spring beans. Put it in an activity.
- Use `System.currentTimeMillis()`, `Instant.now()`, `LocalDate.now()`, `UUID.randomUUID()`, or
  `Random`. Use `Workflow.currentTimeMillis()`, `Workflow.randomUUID()`, and `Workflow.newRandom()`.
- Use `Thread.sleep`, threads, executors, `CompletableFuture`, or `synchronized` waits. Use
  `Workflow.sleep`, `Workflow.await`, `Async`, and `Promise`.
- Read mutable static state, system properties, or environment variables. Static constants
  and immutable lookups are fine.
- Iterate a `HashMap` or `HashSet` when the order decides which activities run. Use a sorted or
  insertion-ordered collection.
- Throw plain exceptions to fail the event. A non-Temporal exception fails the *workflow task*,
  which retries forever. Throw `ApplicationFailure` to fail the workflow. Activity failures
  (`ActivityFailure`) already fail the workflow if you don't catch them.

## Do

- Wrap any change to the order or number of activities, timers, or signals in
  `Workflow.getVersion("<event.type>.<change>", DEFAULT_VERSION, n)`.
- Prefix activity interfaces (`@ActivityInterface(namePrefix = "<event.type>.")`), signal and
  query names, and version change IDs with your event type. All handlers share one workflow type
  and one task queue.
- Set timeouts and retry policies on each activity stub to match the system being called. List
  business errors that shouldn't be retried in `setDoNotRetry`.
- Keep handlers short-lived where possible. If a type can wait or loop indefinitely, raise it:
  it may need continue-as-new, and the workflow doesn't support that yet.
- Capture a history after the first real run and add it to the replay suite.
