package com.linklens.linkservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class RabbitMQConfig {

    public static final String LINK_EVENTS_EXCHANGE= "link.events";

    public static final String EXTRACTION_QUEUE="extraction.link-submitted";

    public static final String LINK_SUBMITTED_ROUTING_KEY="link.submitted.v1";

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
            Queue extractionQueue,
            TopicExchange linkEventsExchange){
        return BindingBuilder
                .bind(extractionQueue)
                .to(linkEventsExchange)
                .with(LINK_SUBMITTED_ROUTING_KEY);
    }
}
