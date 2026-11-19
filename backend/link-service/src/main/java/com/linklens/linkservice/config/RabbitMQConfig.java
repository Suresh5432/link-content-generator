package com.linklens.linkservice.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableScheduling
public class RabbitMQConfig {

    public static final String LINK_EVENTS_EXCHANGE=
            "link.events";

    public static final String EXTRACTION_QUEUE=
            "extraction.link-submitted";

    public static final String LINK_SUBMITTED_ROUTING_KEY=
            "link.submitted.v1";

    public static final String SUMMARY_GENERATED_QUEUE =
            "link.summary-generated";

    public static final String SUMMARY_GENERATED_ROUTING_KEY =
            "summary.generated.v1";

    public static final String SUMMARY_RETRY_QUEUE=
            "link.summary-generated.retry";

    public static final String SUMMARY_DLQ=
            "link.summary-generated.dlq";

    public static final int MAX_RETRY_ATTEMPTS=3;

    public static final int RETRY_DELAY_MS=10000;

    public static final String EXTRACTION_FAILED_QUEUE=
            "link.extraction-failed";

    public static final String SUMMARIZATION_FAILED_QUEUE=
            "link.summarization-failed";

    public static final String EXTRACTION_FAILED_ROUTING_KEY=
            "extraction.failed.v1";

    public static final String SUMMARIZATION_FAILED_ROUTING_KEY=
            "summarization.failed.v1";

    public static final String FAILURE_RETRY_EXCHANGE =
            "link.failure.retry";

    public static final String EXTRACTION_FAILURE_RETRY_QUEUE =
            "link.extraction-failed.retry";

    public static final String SUMMARIZATION_FAILURE_RETRY_QUEUE =
            "link.summarization-failed.retry";

    public static final String FAILURE_DLQ_EXCHANGE =
            "link.failure.dlq";

    public static final String EXTRACTION_FAILURE_DLQ =
            "link.extraction-failed.dlq";

    public static final String SUMMARIZATION_FAILURE_DLQ =
            "link.summarization-failed.dlq";

    public static final int FAILURE_RETRY_DELAY_MS = 10000;

    public static final int MAX_FAILURE_RETRIES = 3;


    @Bean
    public TopicExchange linkEventsExchange() {
        return new TopicExchange(LINK_EVENTS_EXCHANGE,true,false);
    }

    @Bean
    public Queue extractionQueue() {
        return new Queue(EXTRACTION_QUEUE, true);
    }

    @Bean
    public Binding linkSubmittedBinding(
            @Qualifier("extractionQueue")
            Queue extractionQueue,
            @Qualifier("linkEventsExchange")
            TopicExchange linkEventsExchange){

        return BindingBuilder
                .bind(extractionQueue)
                .to(linkEventsExchange)
                .with(LINK_SUBMITTED_ROUTING_KEY);
    }

    @Bean
    public Queue summaryGeneratedQueue() {
        return new Queue(SUMMARY_GENERATED_QUEUE, true);
    }

    @Bean
    public Binding summaryGeneratedBinding(
            @Qualifier("summaryGeneratedQueue")
            Queue summaryGeneratedQueue,
            @Qualifier("linkEventsExchange")
            TopicExchange linkEventsExchange) {

        return BindingBuilder
                .bind(summaryGeneratedQueue)
                .to(linkEventsExchange)
                .with(SUMMARY_GENERATED_ROUTING_KEY);
    }

    @Bean
    public Queue summaryRetryQueue() {

        Map<String, Object> arguments=new HashMap<>();

        arguments.put("x-message-ttl",RETRY_DELAY_MS);
        arguments.put("x-dead-letter-exchange",LINK_EVENTS_EXCHANGE);
        arguments.put("x-dead-letter-routing-key",SUMMARY_GENERATED_ROUTING_KEY);

        return QueueBuilder
                .durable(SUMMARY_RETRY_QUEUE)
                .withArguments(arguments)
                .build();
    }

    @Bean
    public Queue summaryDeadLetterQueue(){

        return QueueBuilder
                .durable(SUMMARY_DLQ)
                .build();
    }

    @Bean
    public Queue extractionFailedQueue(){
        return new Queue(EXTRACTION_FAILED_QUEUE, true);
    }

    @Bean
    public Queue summarizationFailedQueue(){
        return new Queue(SUMMARIZATION_FAILED_QUEUE, true);
    }

    @Bean
    public Binding extractionFailedBiding(
            @Qualifier("extractionFailedQueue")
            Queue queue,
            @Qualifier("linkEventsExchange")
            TopicExchange exchange){

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(EXTRACTION_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding summarizationFailedBinding(
            @Qualifier("summarizationFailedQueue")
            Queue queue,
            @Qualifier("linkEventsExchange")
            TopicExchange exchange){

        return  BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(SUMMARIZATION_FAILED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange failureRetryExchange() {
        return new DirectExchange(FAILURE_RETRY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange failureDlqExchange() {
        return new DirectExchange(FAILURE_DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue extractionFailureRetryQueue() {
        return QueueBuilder.durable(EXTRACTION_FAILURE_RETRY_QUEUE)
                .withArgument("x-message-ttl", FAILURE_RETRY_DELAY_MS)
                .withArgument("x-dead-letter-exchange", LINK_EVENTS_EXCHANGE)
                .withArgument(
                        "x-dead-letter-routing-key",
                        EXTRACTION_FAILED_ROUTING_KEY
                )
                .build();
    }

    @Bean
    public Queue summarizationFailureRetryQueue() {
        return QueueBuilder.durable(SUMMARIZATION_FAILURE_RETRY_QUEUE)
                .withArgument("x-message-ttl", FAILURE_RETRY_DELAY_MS)
                .withArgument("x-dead-letter-exchange", LINK_EVENTS_EXCHANGE)
                .withArgument(
                        "x-dead-letter-routing-key",
                        SUMMARIZATION_FAILED_ROUTING_KEY
                )
                .build();
    }

    @Bean
    public Binding extractionFailureRetryBinding() {
        return BindingBuilder
                .bind(extractionFailureRetryQueue())
                .to(failureRetryExchange())
                .with(EXTRACTION_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding summarizationFailureRetryBinding() {
        return BindingBuilder
                .bind(summarizationFailureRetryQueue())
                .to(failureRetryExchange())
                .with(SUMMARIZATION_FAILED_ROUTING_KEY);
    }

    @Bean
    public Queue extractionFailureDlq() {
        return new Queue(EXTRACTION_FAILURE_DLQ, true);
    }

    @Bean
    public Queue summarizationFailureDlq() {
        return new Queue(SUMMARIZATION_FAILURE_DLQ, true);
    }

    @Bean
    public Binding extractionFailureDlqBinding() {
        return BindingBuilder
                .bind(extractionFailureDlq())
                .to(failureDlqExchange())
                .with(EXTRACTION_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding summarizationFailureDlqBinding() {
        return BindingBuilder
                .bind(summarizationFailureDlq())
                .to(failureDlqExchange())
                .with(SUMMARIZATION_FAILED_ROUTING_KEY);
    }
}
