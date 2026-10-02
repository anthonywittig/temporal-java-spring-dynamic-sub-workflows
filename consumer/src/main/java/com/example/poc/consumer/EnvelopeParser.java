package com.example.poc.consumer;

import com.example.poc.api.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Validates the envelope only. The payload is opaque here: it's checked against the handler's
 * payload type inside the workflow, which keeps handler classes off the consumer's classpath.
 */
@Component
public class EnvelopeParser {

  private final ObjectMapper mapper;
  private final ConsumerProperties properties;

  public EnvelopeParser(ObjectMapper mapper, ConsumerProperties properties) {
    this.mapper = mapper;
    this.properties = properties;
  }

  public EventEnvelope parse(String message) {
    EventEnvelope envelope;
    try {
      envelope = mapper.readValue(message, EventEnvelope.class);
    } catch (JsonProcessingException e) {
      throw new InvalidEventException("Not a valid event envelope: " + e.getOriginalMessage(), e);
    }
    if (envelope == null) {
      throw new InvalidEventException("Empty message");
    }
    if (isBlank(envelope.type())) {
      throw new InvalidEventException("Missing 'type'");
    }
    if (isBlank(envelope.id())) {
      throw new InvalidEventException("Missing 'id'");
    }
    if (envelope.payload() == null || !envelope.payload().isObject()) {
      throw new InvalidEventException("'payload' must be a JSON object");
    }
    if (!properties.eventTypes().contains(envelope.type())) {
      throw new InvalidEventException("Unsupported event type '" + envelope.type() + "'");
    }
    return envelope;
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
