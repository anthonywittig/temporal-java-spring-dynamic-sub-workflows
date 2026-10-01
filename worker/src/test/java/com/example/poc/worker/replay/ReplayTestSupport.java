package com.example.poc.worker.replay;

import com.example.poc.api.EventHandler;
import com.example.poc.api.EventHandlerFactory;
import com.example.poc.api.PocConstants;
import com.example.poc.testkit.Fixtures;
import com.example.poc.workflow.HandlerRegistry;
import com.example.poc.workflow.ProcessEventWorkflow;
import com.example.poc.workflow.ProcessEventWorkflowImpl;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.WorkflowReplayer;
import io.temporal.worker.Worker;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

final class ReplayTestSupport {

  /** Every captured history. Add new captures here (see scripts/capture-history.sh). */
  static final List<String> HISTORIES =
      List.of(
          "customer.registered-v1",
          "customer.registered-v2",
          "order.placed-success",
          "order.placed-compensated",
          "refund.requested-auto-approved",
          "refund.requested-signal-approved",
          "subscription.cancelled-v1");

  private ReplayTestSupport() {}

  static WorkflowExecutionHistory history(String name) {
    return WorkflowExecutionHistory.fromJson(Fixtures.historyJson(name));
  }

  /** Replays with the production workflow and handler set. */
  static void replay(String name) throws Exception {
    WorkflowReplayer.replayWorkflowExecution(history(name), ProcessEventWorkflowImpl.class);
  }

  /**
   * Replays with the default handler set, except {@code type} is handled by {@code handler}. Used
   * to show what the replay tests would catch if a handler changed that way.
   */
  static void replayWithHandler(String name, String type, Supplier<EventHandler<?>> handler)
      throws Exception {
    List<EventHandlerFactory> factories = new ArrayList<>();
    HandlerRegistry defaults = HandlerRegistry.defaultRegistry();
    for (String t : defaults.eventTypes()) {
      if (!t.equals(type)) {
        factories.add(factory(t, () -> defaults.forType(t)));
      }
    }
    factories.add(factory(type, handler));
    HandlerRegistry registry = HandlerRegistry.of(factories);

    try (TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance()) {
      Worker worker = env.newWorker(PocConstants.TASK_QUEUE);
      worker.registerWorkflowImplementationFactory(
          ProcessEventWorkflow.class, () -> new ProcessEventWorkflowImpl(registry));
      WorkflowReplayer.replayWorkflowExecution(history(name), worker);
    }
  }

  private static EventHandlerFactory factory(String type, Supplier<EventHandler<?>> create) {
    return new EventHandlerFactory() {
      @Override
      public String eventType() {
        return type;
      }

      @Override
      public EventHandler<?> create() {
        return create.get();
      }
    };
  }
}
