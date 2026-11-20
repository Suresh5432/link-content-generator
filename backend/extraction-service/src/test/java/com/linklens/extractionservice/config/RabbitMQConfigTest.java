package com.linklens.extractionservice.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RabbitMQConfigTest {
 @Test void retryQueueHasFiniteDelayAndReturnsToSubmissionQueue() {
  var queue = new RabbitMQConfig().retryQueue();
  assertTrue(queue.isDurable());
  assertEquals(10_000, queue.getArguments().get("x-message-ttl"));
  assertEquals(RabbitMQConfig.LINK_EXCHANGE, queue.getArguments().get("x-dead-letter-exchange"));
  assertEquals(RabbitMQConfig.LINK_SUBMITTED_ROUTING_KEY, queue.getArguments().get("x-dead-letter-routing-key"));
 }
 @Test void retryRoutingAndDeadLetterNamesAreDistinct() {
  assertNotEquals(RabbitMQConfig.RETRY_ROUTING_KEY, RabbitMQConfig.DLQ_ROUTING_KEY);
  assertNotEquals(RabbitMQConfig.RETRY_QUEUE, RabbitMQConfig.DLQ_QUEUE);
 }
}
