package com.example.poc.worker;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.poc.api.EventTypes;
import com.example.poc.workflow.HandlerRegistry;
import org.junit.jupiter.api.Test;

/**
 * Catches a handler added without updating {@link EventTypes#ALL} (the consumer won't route it),
 * or a type in ALL with no handler (its workflows would fail with UnknownEventType).
 */
class HandlerRegistryConsistencyTest {

  @Test
  void registryMatchesExpectedEventTypes() {
    assertThat(HandlerRegistry.defaultRegistry().eventTypes())
        .containsExactlyInAnyOrderElementsOf(EventTypes.ALL);
  }
}
