package com.linklens.notificationservice.config;

import com.linkcontext.contracts.EventRoutingKey;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    public static final String LINK_EVENTS_EXCHANGE="link.events";

    public static final String SUMMARY_QUEUE=
            "notification.summary-generated";

    public static final String EXTRACTION_FAILED_QUEUE=
            "notification.extraction-failed";

    public static final String SUMMARIZATION_FAILED_QUEUE=
            "notification.summarization-failed";

    public static final String PRIVACY_STATUS_QUEUE=
            "notification.privacy-request-status";

    public static final String PRIVACY_REMINDER_QUEUE=
            "notification.privacy-request-reminder";

    @Bean
    public TopicExchange linkEventsExchange() {
        return new TopicExchange(LINK_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue summaryGeneratedQueue(){
        return QueueBuilder.durable(SUMMARY_QUEUE).build();
    }

    @Bean
    public Queue extractionFailedQueue(){
        return QueueBuilder.durable(EXTRACTION_FAILED_QUEUE).build();
    }

    @Bean
    public Queue summaryFailedQueue(){
        return QueueBuilder.durable(SUMMARIZATION_FAILED_QUEUE).build();
    }

    @Bean
    public Queue privacyStatusQueue(){
        return QueueBuilder.durable(PRIVACY_STATUS_QUEUE).build();
    }

    @Bean
    public Queue privacyReminderQueue(){
        return QueueBuilder.durable(PRIVACY_REMINDER_QUEUE).build();
    }

    @Bean
    public Binding summaryGeneratedBinding(){
        return BindingBuilder
                .bind(summaryGeneratedQueue())
                .to(linkEventsExchange())
                .with(EventRoutingKey.SUMMARY_GENERATED);
    }

    @Bean
    public Binding extractionFailedBinding(){
        return BindingBuilder.bind(extractionFailedQueue())
                .to(linkEventsExchange())
                .with(EventRoutingKey.EXTRACTION_FAILED);
    }

    @Bean
    public Binding summaryFailedBinding(){
        return BindingBuilder.bind(summaryFailedQueue())
                .to(linkEventsExchange())
                .with(EventRoutingKey.SUMMARIZATION_FAILED);
    }

    @Bean
    public Binding privacyStatusBinding(){
        return BindingBuilder.bind(privacyStatusQueue())
                .to(linkEventsExchange())
                .with(EventRoutingKey.PRIVACY_REQUEST_STATUS_CHANGED);
    }

    @Bean
    public Binding privacyReminderBinding(){
        return BindingBuilder.bind(privacyReminderQueue())
                .to(linkEventsExchange())
                .with(EventRoutingKey.PRIVACY_REQUEST_REMINDER);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
