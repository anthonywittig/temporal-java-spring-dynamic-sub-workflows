package com.example.poc.consumer;

import com.example.poc.api.EventEnvelope;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Acks a message only once it's durably handled: a workflow exists for it, or it's on the
 * dead-letter topic. Any other failure is thrown so the container's error handler redelivers it.
 */
@Component
public class EventListener {

  public static final String ERROR_HEADER = "poc-error";

  private static final Logger log = LoggerFactory.getLogger(EventListener.class);

  private final EnvelopeParser parser;
  private final EventDispatcher dispatcher;
  private final KafkaTemplate<String, String> kafka;
  private final ConsumerProperties properties;

  public EventListener(
      EnvelopeParser parser,
      EventDispatcher dispatcher,
      KafkaTemplate<String, String> kafka,
      ConsumerProperties properties) {
    this.parser = parser;
    this.dispatcher = dispatcher;
    this.kafka = kafka;
    this.properties = properties;
  }

  @KafkaListener(topics = "${poc.consumer.topic}")
  public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack)
      throws ExecutionException, InterruptedException, TimeoutException {
    EventEnvelope envelope;
    try {
      envelope = parser.parse(record.value());
    } catch (InvalidEventException e) {
      log.warn(
          "Dead-lettering {}-{}@{}: {}",
          record.topic(),
          record.partition(),
          record.offset(),
          e.getMessage());
      ProducerRecord<String, String> dead =
          new ProducerRecord<>(properties.deadLetterTopic(), record.key(), record.value());
      dead.headers().add(ERROR_HEADER, e.getMessage().getBytes(StandardCharsets.UTF_8));
      // Wait for the DLT write before acking, or the message could be lost.
      kafka.send(dead).get(10, TimeUnit.SECONDS);
      ack.acknowledge();
      return;
    }

    // Temporal failures propagate: not acked, retried by the error handler.
    dispatcher.dispatch(envelope);
    ack.acknowledge();
  }
}
