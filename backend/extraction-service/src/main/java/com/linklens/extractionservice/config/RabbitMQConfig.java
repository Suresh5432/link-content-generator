package com.linklens.extractionservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String LINK_EXCHANGE="link.events";

    public static final String EXTRACTION_QUEUE="extraction.link-submitted";

    public static final String LINK_SUBMITTED_ROUTING_KEY="link.submitted.v1";

    public static final String EXTRACTION_EXCHANGE="extarction.events";

    public static final String CONTENT_EXTRACTED_ROUTING_KEY="content.extracted.v1";

    @Bean
    public TopicExchange linkExchange() {
        return new TopicExchange(LINK_EXCHANGE, true, false);
    }

    @Bean
    public Queue extractionQueue() {
        return new Queue(EXTRACTION_QUEUE, true);
    }

    @Bean
    public Binding extractionBinding(){
        return BindingBuilder
                .bind(extractionQueue())
                .to(linkExchange())
                .with(LINK_SUBMITTED_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter(){
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public ConnectionFactory connectionFactory(){
        return new CachingConnectionFactory();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter jsonMessageConverter){
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        return template;
    }

}
