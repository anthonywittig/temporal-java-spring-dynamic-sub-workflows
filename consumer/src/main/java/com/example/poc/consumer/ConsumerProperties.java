package com.example.poc.consumer;

import com.example.poc.api.EventTypes;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param topic topic to consume event envelopes from
 * @param deadLetterTopic where malformed or unroutable messages go
 * @param eventTypes types this consumer routes; defaults to {@link EventTypes#ALL}. Narrow it to
 *     roll a new type out gradually.
 */
@ConfigurationProperties("poc.consumer")
public record ConsumerProperties(String topic, String deadLetterTopic, Set<String> eventTypes) {

  public ConsumerProperties {
    if (eventTypes == null || eventTypes.isEmpty()) {
      eventTypes = EventTypes.ALL;
    }
  }
}
