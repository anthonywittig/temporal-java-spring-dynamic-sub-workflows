package com.example.poc.consumer;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConfig {

  @Bean
  NewTopic eventsTopic(ConsumerProperties properties) {
    return new NewTopic(properties.topic(), 3, (short) 1);
  }

  @Bean
  NewTopic deadLetterTopic(ConsumerProperties properties) {
    return new NewTopic(properties.deadLetterTopic(), 1, (short) 1);
  }

  /**
   * Retries a failed record forever with backoff (capped at 30s) without acking it. The partition
   * stalls while Temporal is unavailable, which is the point: nothing is skipped.
   */
  @Bean
  DefaultErrorHandler errorHandler() {
    ExponentialBackOff backOff = new ExponentialBackOff(500, 2.0);
    backOff.setMaxInterval(30_000);
    DefaultErrorHandler handler = new DefaultErrorHandler(backOff);
    // Don't commit offsets for a record that's still being retried.
    handler.setCommitRecovered(false);
    return handler;
  }
}
