package com.example.poc.api;

/**
 * Processes one event type. Runs inside the workflow, so implementations must follow the workflow
 * code rules (see docs/workflow-code-rules.md): no I/O, threads, clocks, or randomness except
 * through the Temporal {@code Workflow} API. All side effects go through activities.
 *
 * <p>An instance is created per workflow execution, so it may hold per-execution state.
 *
 * <p>To receive signals or answer queries, implement an interface whose methods are annotated with
 * {@code @SignalMethod} / {@code @QueryMethod}. The workflow registers the handler as a listener
 * before calling {@link #handle}.
 *
 * @param <T> payload record type
 */
public interface EventHandler<T> {

  Class<T> payloadType();

  /**
   * Runs the business process for one event.
   *
   * @return a short human-readable outcome, recorded as the workflow result
   */
  String handle(T payload);
}
