package com.example.poc.api;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The message format on the Kafka topic and the single input to {@code ProcessEventWorkflow}.
 *
 * @param type event type, for example {@code order.placed}; selects the handler
 * @param id producer-assigned event ID, unique per type; used to derive the workflow ID
 * @param payload type-specific body, converted to the handler's payload record inside the workflow
 */
public record EventEnvelope(String type, String id, JsonNode payload) {

  /** Workflow ID for this event. Redelivery of the same event maps to the same ID. */
  public String workflowId() {
    return type + "-" + id;
  }
}
