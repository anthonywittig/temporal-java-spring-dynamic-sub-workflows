package com.example.poc.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import com.example.poc.api.PocConstants;
import com.example.poc.testkit.Fixtures;
import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.client.WorkflowClient;
import io.temporal.testing.TestWorkflowEnvironment;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** Kafka to Temporal start, with an embedded broker and the in-memory Temporal test server. */
@SpringBootTest(
    properties = {
      "spring.temporal.test-server.enabled=true",
      "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    })
@EmbeddedKafka(partitions = 1, topics = {"events", "events.DLT"})
class ConsumerKafkaIntegrationTest {

  @Autowired TestWorkflowEnvironment env;
  @Autowired WorkflowClient client;
  @Autowired KafkaTemplate<String, String> kafka;
  @Autowired EmbeddedKafkaBroker broker;
  @MockitoSpyBean EventDispatcher dispatcher;

  @Test
  void startsOnceRetriesWithoutAckingAndDeadLettersMalformed() throws Exception {
    env.registerSearchAttribute(
        PocConstants.EVENT_TYPE_SEARCH_ATTRIBUTE, IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);

    List<Object> outcomes = new CopyOnWriteArrayList<>();
    // First attempt fails as if Temporal were down, then behave normally.
    doThrow(new RuntimeException("Temporal unavailable"))
        .doAnswer(
            invocation -> {
              Object result = invocation.callRealMethod();
              outcomes.add(result);
              return result;
            })
        .when(dispatcher)
        .dispatch(any());

    String order = Fixtures.eventJson("order.placed");
    kafka.send("events", "o-2001", order).get();
    kafka.send("events", "o-2001", order).get(); // redelivery from the producer side
    kafka.send("events", "bad", Fixtures.eventJson("malformed")).get();

    // The failed attempt wasn't acked, so the record was redelivered and then started exactly once.
    await()
        .atMost(Duration.ofSeconds(30))
        .until(() -> outcomes.size() == 2);
    assertThat(outcomes)
        .containsExactly(EventDispatcher.Result.STARTED, EventDispatcher.Result.DUPLICATE);
    assertThat(client.newUntypedWorkflowStub("order.placed-o-2001").describe().getWorkflowType())
        .isEqualTo(PocConstants.WORKFLOW_TYPE);

    try (Consumer<String, String> dlt = dltConsumer()) {
      broker.consumeFromAnEmbeddedTopic(dlt, "events.DLT");
      ConsumerRecord<String, String> dead =
          KafkaTestUtils.getSingleRecord(dlt, "events.DLT", Duration.ofSeconds(30));
      assertThat(dead.key()).isEqualTo("bad");
      assertThat(dead.value()).isEqualTo(Fixtures.eventJson("malformed"));
      assertThat(
              new String(
                  dead.headers().lastHeader(EventListener.ERROR_HEADER).value(),
                  StandardCharsets.UTF_8))
          .contains("Missing 'id'");
    }
  }

  private Consumer<String, String> dltConsumer() {
    Map<String, Object> props = KafkaTestUtils.consumerProps("dlt-reader", "true", broker);
    return new DefaultKafkaConsumerFactory<>(
            props, new StringDeserializer(), new StringDeserializer())
        .createConsumer();
  }
}
