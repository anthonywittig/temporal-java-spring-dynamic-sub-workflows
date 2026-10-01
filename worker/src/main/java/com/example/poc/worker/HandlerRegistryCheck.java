package com.example.poc.worker;

import com.example.poc.api.EventTypes;
import com.example.poc.workflow.HandlerRegistry;
import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Refuses to start a worker whose handler set differs from the expected event types. Every worker
 * polling the task queue must run the same handler set, or a workflow could land on a worker that
 * can't process it.
 */
@Component
public class HandlerRegistryCheck {

  private static final Logger log = LoggerFactory.getLogger(HandlerRegistryCheck.class);

  @PostConstruct
  void check() {
    Set<String> registered = HandlerRegistry.defaultRegistry().eventTypes();
    if (!registered.equals(EventTypes.ALL)) {
      Set<String> missing = new HashSet<>(EventTypes.ALL);
      missing.removeAll(registered);
      Set<String> unexpected = new HashSet<>(registered);
      unexpected.removeAll(EventTypes.ALL);
      throw new IllegalStateException(
          "Handler registry doesn't match EventTypes.ALL. Missing: %s, unexpected: %s"
              .formatted(missing, unexpected));
    }
    log.info("Registered handlers for event types {}", registered);
  }
}
