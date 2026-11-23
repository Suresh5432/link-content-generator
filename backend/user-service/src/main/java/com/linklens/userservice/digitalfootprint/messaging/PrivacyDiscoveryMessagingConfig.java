package com.linklens.userservice.digitalfootprint.messaging;

import com.linkcontext.contracts.EventRoutingKey;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class PrivacyDiscoveryMessagingConfig {

    public static final String EXTRACTION_EXCHANGE = "extraction.events";
    public static final String PRIVACY_DISCOVERY_QUEUE = "footprint.privacy-discovery";

    @Bean
    public TopicExchange extractionEventsExchange() {
        return new TopicExchange(EXTRACTION_EXCHANGE, true, false);
    }

    @Bean
    public Queue privacyDiscoveryQueue() {
        return QueueBuilder.durable(PRIVACY_DISCOVERY_QUEUE).build();
    }

    @Bean
    public Binding privacyDiscoveryBinding() {
        return BindingBuilder.bind(privacyDiscoveryQueue())
                .to(extractionEventsExchange())
                .with(EventRoutingKey.PRIVACY_DISCOVERY_DISCOVERED);
    }

    @Bean
    public JacksonJsonMessageConverter footprintMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
