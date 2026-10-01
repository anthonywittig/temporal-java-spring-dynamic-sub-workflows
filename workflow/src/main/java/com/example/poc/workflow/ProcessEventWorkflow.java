package com.example.poc.workflow;

import com.example.poc.api.EventEnvelope;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * The single workflow type for all events. Signals and queries are not declared here: each
 * handler declares its own and the workflow registers the handler as a listener at runtime.
 */
@WorkflowInterface
public interface ProcessEventWorkflow {

  @WorkflowMethod(name = "ProcessEventWorkflow")
  String process(EventEnvelope envelope);
}
