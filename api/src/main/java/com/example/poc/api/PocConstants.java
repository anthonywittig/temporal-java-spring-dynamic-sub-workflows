package com.example.poc.api;

public final class PocConstants {

  /** Workflow type name. The consumer starts workflows by this name with an untyped stub. */
  public static final String WORKFLOW_TYPE = "ProcessEventWorkflow";

  public static final String TASK_QUEUE = "process-events";

  /** Keyword search attribute holding the event type. */
  public static final String EVENT_TYPE_SEARCH_ATTRIBUTE = "EventType";

  private PocConstants() {}
}
