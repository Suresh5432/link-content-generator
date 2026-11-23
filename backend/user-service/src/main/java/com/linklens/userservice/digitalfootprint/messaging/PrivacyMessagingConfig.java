package com.linklens.userservice.digitalfootprint.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrivacyMessagingConfig {

    @Bean
    public TopicExchange linkEventsExchange(@Value("${digital-footprint.events.exchange:link.events}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }
}
