package com.example.poc.consumer;

import com.example.poc.api.EventEnvelope;
import com.example.poc.api.PocConstants;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Starts one {@code ProcessEventWorkflow} per event, by name, without handler classes. */
@Component
public class EventDispatcher {

  public enum Result {
    STARTED,
    DUPLICATE
  }

  private static final Logger log = LoggerFactory.getLogger(EventDispatcher.class);

  private static final SearchAttributeKey<String> EVENT_TYPE =
      SearchAttributeKey.forKeyword(PocConstants.EVENT_TYPE_SEARCH_ATTRIBUTE);

  private final WorkflowClient client;

  public EventDispatcher(WorkflowClient client) {
    this.client = client;
  }

  /**
   * @return {@link Result#DUPLICATE} if a workflow already exists for this event, running or
   *     closed (within the namespace retention period)
   * @throws io.temporal.client.WorkflowServiceException or another runtime exception if Temporal
   *     can't be reached; the caller must not ack
   */
  public Result dispatch(EventEnvelope envelope) {
    WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setWorkflowId(envelope.workflowId())
            .setTaskQueue(PocConstants.TASK_QUEUE)
            // The default (ALLOW_DUPLICATE) would start a second run after the first completes.
            .setWorkflowIdReusePolicy(
                WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
            .setTypedSearchAttributes(
                SearchAttributes.newBuilder().set(EVENT_TYPE, envelope.type()).build())
            .build();
    try {
      client.newUntypedWorkflowStub(PocConstants.WORKFLOW_TYPE, options).start(envelope);
      log.info("Started {}", envelope.workflowId());
      return Result.STARTED;
    } catch (WorkflowExecutionAlreadyStarted e) {
      log.info("Duplicate event, workflow {} already exists", envelope.workflowId());
      return Result.DUPLICATE;
    }
  }
}
