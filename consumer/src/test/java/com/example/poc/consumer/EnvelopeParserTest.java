package com.example.poc.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.poc.api.EventEnvelope;
import com.example.poc.api.EventTypes;
import com.example.poc.testkit.Fixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EnvelopeParserTest {

  private final EnvelopeParser parser =
      new EnvelopeParser(new ObjectMapper(), new ConsumerProperties("events", "events.DLT", null));

  @Test
  void parsesValidEnvelope() {
    EventEnvelope envelope = parser.parse(Fixtures.eventJson("order.placed"));

    assertThat(envelope.type()).isEqualTo("order.placed");
    assertThat(envelope.workflowId()).isEqualTo("order.placed-o-2001");
    assertThat(envelope.payload().get("totalCents").asLong()).isEqualTo(4599);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not json",
        "null",
        "{\"id\":\"1\",\"payload\":{}}",
        "{\"type\":\"order.placed\",\"payload\":{}}",
        "{\"type\":\"order.placed\",\"id\":\" \",\"payload\":{}}",
        "{\"type\":\"order.placed\",\"id\":\"1\"}",
        "{\"type\":\"order.placed\",\"id\":\"1\",\"payload\":[1]}",
        "{\"type\":\"no.such.type\",\"id\":\"1\",\"payload\":{}}",
      })
  void rejectsMalformedOrUnroutable(String message) {
    assertThatThrownBy(() -> parser.parse(message)).isInstanceOf(InvalidEventException.class);
  }

  @Test
  void rejectsTypesExcludedByConfig() {
    EnvelopeParser narrowed =
        new EnvelopeParser(
            new ObjectMapper(),
            new ConsumerProperties("events", "events.DLT", Set.of(EventTypes.CUSTOMER_REGISTERED)));

    assertThatThrownBy(() -> narrowed.parse(Fixtures.eventJson("order.placed")))
        .isInstanceOf(InvalidEventException.class)
        .hasMessageContaining("Unsupported event type");
  }
}
