package com.example.poc.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.poc.api.EventEnvelope;
import com.example.poc.api.EventHandler;
import com.example.poc.api.EventHandlerFactory;
import com.example.poc.api.PocConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessEventWorkflowImplTest {

  record Greeting(String name) {}

  static class GreetingHandler implements EventHandler<Greeting> {
    @Override
    public Class<Greeting> payloadType() {
      return Greeting.class;
    }

    @Override
    public String handle(Greeting payload) {
      return "hello " + payload.name();
    }
  }

  static class GreetingFactory implements EventHandlerFactory {
    @Override
    public String eventType() {
      return "test.greeting";
    }

    @Override
    public EventHandler<?> create() {
      return new GreetingHandler();
    }
  }

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private TestWorkflowEnvironment env;

  @BeforeEach
  void setUp() {
    env = TestWorkflowEnvironment.newInstance();
    env.registerSearchAttribute(
        PocConstants.EVENT_TYPE_SEARCH_ATTRIBUTE, IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
    Worker worker = env.newWorker(PocConstants.TASK_QUEUE);
    HandlerRegistry registry = HandlerRegistry.of(List.of(new GreetingFactory()));
    worker.registerWorkflowImplementationFactory(
        ProcessEventWorkflow.class, () -> new ProcessEventWorkflowImpl(registry));
    env.start();
  }

  @AfterEach
  void tearDown() {
    env.close();
  }

  private String run(String type, String payloadJson) throws Exception {
    EventEnvelope envelope = new EventEnvelope(type, "1", MAPPER.readTree(payloadJson));
    return env.getWorkflowClient()
        .newWorkflowStub(
            ProcessEventWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(envelope.workflowId())
                .setTaskQueue(PocConstants.TASK_QUEUE)
                .build())
        .process(envelope);
  }

  @Test
  void dispatchesToHandlerForType() throws Exception {
    assertThat(run("test.greeting", "{\"name\":\"ada\",\"ignored\":true}")).isEqualTo("hello ada");
  }

  @Test
  void unknownTypeFailsWorkflowInsteadOfRetryingTask() {
    assertThatThrownBy(() -> run("test.nope", "{}"))
        .isInstanceOf(WorkflowFailedException.class)
        .cause()
        .isInstanceOfSatisfying(
            ApplicationFailure.class,
            f -> assertThat(f.getType()).isEqualTo(HandlerRegistry.UNKNOWN_EVENT_TYPE));
  }

  @Test
  void unconvertiblePayloadFailsWorkflowInsteadOfRetryingTask() {
    assertThatThrownBy(() -> run("test.greeting", "{\"name\":{\"nested\":1}}"))
        .isInstanceOf(WorkflowFailedException.class)
        .cause()
        .isInstanceOfSatisfying(
            ApplicationFailure.class,
            f -> assertThat(f.getType()).isEqualTo(ProcessEventWorkflowImpl.INVALID_PAYLOAD));
  }

  @Test
  void rejectsDuplicateFactoriesForOneType() {
    assertThatThrownBy(() -> HandlerRegistry.of(List.of(new GreetingFactory(), new GreetingFactory())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("test.greeting");
  }
}
