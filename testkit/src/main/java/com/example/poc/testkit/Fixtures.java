package com.example.poc.testkit;

import com.example.poc.api.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Sample Kafka messages and captured workflow histories, shared by tests and the demo. */
public final class Fixtures {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private Fixtures() {}

  /** Raw JSON of {@code events/<name>.json}, exactly as it would appear on the topic. */
  public static String eventJson(String name) {
    return resource("events/" + name + ".json").trim();
  }

  public static EventEnvelope event(String name) {
    try {
      return MAPPER.readValue(eventJson(name), EventEnvelope.class);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** JSON history of {@code histories/<name>.json}, as written by {@code temporal workflow show -o json}. */
  public static String historyJson(String name) {
    return resource("histories/" + name + ".json");
  }

  private static String resource(String path) {
    try (InputStream in = Fixtures.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalArgumentException("No test fixture " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
