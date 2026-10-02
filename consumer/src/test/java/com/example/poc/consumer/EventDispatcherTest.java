package com.example.poc.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.poc.api.EventEnvelope;
import com.example.poc.api.PocConstants;
import com.example.poc.testkit.Fixtures;
import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.common.SearchAttributeKey;
import io.temporal.common.converter.EncodedValues;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.workflow.DynamicWorkflow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventDispatcherTest {

  /** Stands in for the real workflow, which isn't on the consumer's classpath. */
  public static class CompletesImmediately implements DynamicWorkflow {
    @Override
    public Object execute(EncodedValues args) {
      return "done";
    }
  }

  private TestWorkflowEnvironment env;
  private EventDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    env = TestWorkflowEnvironment.newInstance();
    env.registerSearchAttribute(
        PocConstants.EVENT_TYPE_SEARCH_ATTRIBUTE, IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
    Worker worker = env.newWorker(PocConstants.TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(CompletesImmediately.class);
    env.start();
    dispatcher = new EventDispatcher(env.getWorkflowClient());
  }

  @AfterEach
  void tearDown() {
    env.close();
  }

  @Test
  void redeliveryWhileRunningOrAfterCompletionIsDuplicate() {
    EventEnvelope event = Fixtures.event("order.placed");

    assertThat(dispatcher.dispatch(event)).isEqualTo(EventDispatcher.Result.STARTED);
    assertThat(dispatcher.dispatch(event)).isEqualTo(EventDispatcher.Result.DUPLICATE);

    env.getWorkflowClient().newUntypedWorkflowStub(event.workflowId()).getResult(String.class);
    // REJECT_DUPLICATE: completion doesn't free the ID for a second run.
    assertThat(dispatcher.dispatch(event)).isEqualTo(EventDispatcher.Result.DUPLICATE);
  }

  @Test
  void setsWorkflowTypeAndEventTypeSearchAttributeAtStart() {
    EventEnvelope event = Fixtures.event("customer.registered");
    dispatcher.dispatch(event);

    var description = env.getWorkflowClient().newUntypedWorkflowStub(event.workflowId()).describe();
    assertThat(description.getWorkflowType()).isEqualTo(PocConstants.WORKFLOW_TYPE);
    assertThat(
            description
                .getTypedSearchAttributes()
                .get(SearchAttributeKey.forKeyword(PocConstants.EVENT_TYPE_SEARCH_ATTRIBUTE)))
        .isEqualTo("customer.registered");
  }
}
