package com.example.poc.consumer;

import com.example.poc.api.EventTypes;
import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fails startup if the consumer is configured to route a type that has no handler. The worker
 * checks that its registry equals {@link EventTypes#ALL}, so checking against that set here means
 * the consumer never starts a workflow that's guaranteed to fail with UnknownEventType.
 */
@Component
public class EventTypeCheck {

  private static final Logger log = LoggerFactory.getLogger(EventTypeCheck.class);

  private final ConsumerProperties properties;

  public EventTypeCheck(ConsumerProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  void check() {
    Set<String> unknown = new HashSet<>(properties.eventTypes());
    unknown.removeAll(EventTypes.ALL);
    if (!unknown.isEmpty()) {
      throw new IllegalStateException(
          "poc.consumer.event-types contains types with no registered handler: " + unknown);
    }
    log.info("Routing event types {}", properties.eventTypes());
  }
}
