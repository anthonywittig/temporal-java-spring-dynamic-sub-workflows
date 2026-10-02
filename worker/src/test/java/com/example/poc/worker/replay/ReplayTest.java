package com.example.poc.worker.replay;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.poc.api.EventTypes;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

/**
 * Replays histories captured from real runs against the current code. Because every event type
 * shares one workflow class, this suite is the guard against a change to one handler (or to the
 * workflow itself) breaking in-flight executions of any type.
 */
class ReplayTest {

  @ParameterizedTest
  @FieldSource("com.example.poc.worker.replay.ReplayTestSupport#HISTORIES")
  void replaysCapturedHistory(String name) throws Exception {
    ReplayTestSupport.replay(name);
  }

  @Test
  void everyEventTypeHasAtLeastOneCapturedHistory() {
    assertThat(
            ReplayTestSupport.HISTORIES.stream()
                .map(name -> name.substring(0, name.indexOf('-')))
                .collect(Collectors.toSet()))
        .containsAll(EventTypes.ALL);
  }
}
