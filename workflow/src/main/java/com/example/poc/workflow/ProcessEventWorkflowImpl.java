package com.example.poc.workflow;

import com.example.poc.api.EventEnvelope;
import com.example.poc.api.EventHandler;
import com.example.poc.api.PocConstants;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.common.SearchAttributeKey;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

@WorkflowImpl(taskQueues = PocConstants.TASK_QUEUE)
public class ProcessEventWorkflowImpl implements ProcessEventWorkflow {

  public static final SearchAttributeKey<String> EVENT_TYPE =
      SearchAttributeKey.forKeyword(PocConstants.EVENT_TYPE_SEARCH_ATTRIBUTE);

  public static final String INVALID_PAYLOAD = "InvalidPayload";

  private static final Logger log = Workflow.getLogger(ProcessEventWorkflowImpl.class);

  // Pure in-memory conversion, so it's safe in workflow code. Unknown fields are ignored so
  // producers can add fields without breaking running workers.
  private static final ObjectMapper MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final HandlerRegistry registry;

  public ProcessEventWorkflowImpl() {
    this(HandlerRegistry.defaultRegistry());
  }

  /** For tests that need a non-default handler set, via {@code registerWorkflowImplementationFactory}. */
  public ProcessEventWorkflowImpl(HandlerRegistry registry) {
    this.registry = registry;
  }

  @Override
  public String process(EventEnvelope envelope) {
    // The consumer sets EventType at start. Upsert only if a different client started us without it.
    if (Workflow.getTypedSearchAttributes().get(EVENT_TYPE) == null) {
      Workflow.upsertTypedSearchAttributes(EVENT_TYPE.valueSet(envelope.type()));
    }

    EventHandler<?> handler = registry.forType(envelope.type());
    // Lets handlers own their signals and queries. Signals received before this point are buffered
    // by the SDK and delivered now.
    Workflow.registerListener(handler);

    log.info("Processing {} {}", envelope.type(), envelope.id());
    return invoke(handler, envelope);
  }

  private static <T> String invoke(EventHandler<T> handler, EventEnvelope envelope) {
    T payload;
    try {
      payload = MAPPER.treeToValue(envelope.payload(), handler.payloadType());
    } catch (Exception e) {
      // A plain exception here would fail the workflow task and retry forever. A bad payload
      // won't fix itself, so fail the workflow instead.
      throw ApplicationFailure.newNonRetryableFailureWithCause(
          "Payload for " + envelope.type() + " is not a valid " + handler.payloadType().getSimpleName(),
          INVALID_PAYLOAD,
          e);
    }
    return handler.handle(payload);
  }
}
