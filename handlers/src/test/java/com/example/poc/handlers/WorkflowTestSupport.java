package com.example.poc.handlers;

import com.example.poc.api.EventEnvelope;
import com.example.poc.api.PocConstants;
import com.example.poc.workflow.ProcessEventWorkflow;
import com.example.poc.workflow.ProcessEventWorkflowImpl;
import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

/** Runs the real workflow and handler registry against the in-memory test server. */
public class WorkflowTestSupport implements AutoCloseable {

  public final TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance();
  public final Worker worker = env.newWorker(PocConstants.TASK_QUEUE);
  public final WorkflowClient client = env.getWorkflowClient();

  public WorkflowTestSupport(Object... activityImplementations) {
    env.registerSearchAttribute(
        PocConstants.EVENT_TYPE_SEARCH_ATTRIBUTE, IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
    worker.registerWorkflowImplementationTypes(ProcessEventWorkflowImpl.class);
    worker.registerActivitiesImplementations(activityImplementations);
    env.start();
  }

  public static WorkflowOptions options(EventEnvelope envelope) {
    return WorkflowOptions.newBuilder()
        .setWorkflowId(envelope.workflowId())
        .setTaskQueue(PocConstants.TASK_QUEUE)
        .build();
  }

  /** Runs the workflow to completion and returns its result. */
  public String run(EventEnvelope envelope) {
    return client.newWorkflowStub(ProcessEventWorkflow.class, options(envelope)).process(envelope);
  }

  /** Starts the workflow and returns an untyped stub, as the consumer would. */
  public WorkflowStub start(EventEnvelope envelope) {
    WorkflowStub stub =
        client.newUntypedWorkflowStub(PocConstants.WORKFLOW_TYPE, options(envelope));
    stub.start(envelope);
    return stub;
  }

  @Override
  public void close() {
    env.close();
  }
}
