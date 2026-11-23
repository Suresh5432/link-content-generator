package com.linklens.extractionservice.messaging;

import com.linklens.extractionservice.config.RabbitMQConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExtractionRetryPublisherRabbitMQIntegrationTest {

    private static final String USERNAME = "integration";
    private static final String PASSWORD = "integration-secret";

    @Container
    static final GenericContainer<?> RABBIT = new GenericContainer<>(
            DockerImageName.parse("rabbitmq:4.1-management"))
            .withEnv("RABBITMQ_DEFAULT_USER", USERNAME)
            .withEnv("RABBITMQ_DEFAULT_PASS", PASSWORD)
            .withExposedPorts(5672)
            .withStartupTimeout(Duration.ofSeconds(60));

    private static CachingConnectionFactory connectionFactory;
    private static RabbitTemplate rabbitTemplate;
    private static RabbitAdmin rabbitAdmin;
    private static ExtractionRetryPublisher publisher;
    private static final RabbitMQConfig TOPOLOGY = new RabbitMQConfig();

    @org.junit.jupiter.api.BeforeAll
    static void setUpBrokerClient() {
        connectionFactory = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getMappedPort(5672));
        connectionFactory.setUsername(USERNAME);
        connectionFactory.setPassword(PASSWORD);
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);

        rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(returned -> { });
        rabbitAdmin = new RabbitAdmin(connectionFactory);
        publisher = new ExtractionRetryPublisher(rabbitTemplate);
    }

    @BeforeEach
    void declareTopologyAndClearQueues() {
        rabbitAdmin.declareExchange(TOPOLOGY.linkExchange());
        rabbitAdmin.declareExchange(TOPOLOGY.retryExchange());
        rabbitAdmin.declareExchange(TOPOLOGY.dlqExchange());
        rabbitAdmin.declareQueue(TOPOLOGY.extractionQueue());
        rabbitAdmin.declareQueue(TOPOLOGY.retryQueue());
        rabbitAdmin.declareQueue(TOPOLOGY.deadLetterQueue());
        rabbitAdmin.declareBinding(TOPOLOGY.extractionBinding());
        rabbitAdmin.declareBinding(TOPOLOGY.retryBinding());
        rabbitAdmin.declareBinding(TOPOLOGY.deadLetterBinding());
        rabbitAdmin.purgeQueue(RabbitMQConfig.EXTRACTION_QUEUE);
        rabbitAdmin.purgeQueue(RabbitMQConfig.RETRY_QUEUE);
        rabbitAdmin.purgeQueue(RabbitMQConfig.DLQ_QUEUE);
    }

    @AfterAll
    static void closeClient() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    @Order(1)
    void confirmedRetryIsRoutedWithIncrementedAttemptCount() throws Exception {
        publisher.retry(originalMessage(0));

        Message forwarded = rabbitTemplate.receive(RabbitMQConfig.RETRY_QUEUE, 2_000);

        assertNotNull(forwarded);
        assertEquals(1, forwarded.getMessageProperties().getHeaders().get("x-retry-count"));
        assertEquals("event-1", forwarded.getMessageProperties().getMessageId());
    }

    @Test
    @Order(2)
    void exhaustedRetryIsConfirmedIntoDeadLetterQueue() throws Exception {
        publisher.retry(originalMessage(3));

        Message forwarded = rabbitTemplate.receive(RabbitMQConfig.DLQ_QUEUE, 2_000);

        assertNotNull(forwarded);
        assertEquals("Maximum retry attempts exceeded",
                forwarded.getMessageProperties().getHeaders().get("x-dead-letter-reason"));
    }

    @Test
    @Order(3)
    void mandatoryUnroutableRetryFailsInsteadOfAppearingSuccessful() {
        Binding binding = TOPOLOGY.retryBinding();
        rabbitAdmin.removeBinding(binding);

        assertThrows(Exception.class, () -> publisher.retry(originalMessage(0)));
    }

    @Test
    @Order(4)
    void brokerInterruptionCausesPublishToFailWithoutAcknowledgementClaim() {
        RABBIT.stop();

        assertThrows(Exception.class, () -> publisher.retry(originalMessage(0)));
    }

    private Message originalMessage(int retryCount) {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId("event-1");
        properties.setHeader("x-retry-count", retryCount);
        properties.setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
        return new Message("payload".getBytes(StandardCharsets.UTF_8), properties);
    }
}
