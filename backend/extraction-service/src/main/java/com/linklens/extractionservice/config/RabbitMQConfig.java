package com.linklens.extractionservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String LINK_EXCHANGE="link.events";

    public static final String EXTRACTION_QUEUE="extraction.link-submitted";

    public static final String LINK_SUBMITTED_ROUTING_KEY="link.submitted.v1";

    public static final String EXTRACTION_EXCHANGE="extraction.events";

    public static final String CONTENT_EXTRACTED_ROUTING_KEY="content.extracted.v1";

    public static final String RETRY_EXCHANGE="extraction.retry";

    public static final String RETRY_QUEUE="extraction.link-submitted.retry";

    public static final String RETRY_ROUTING_KEY="link-extraction.retry";

    public static final String DLQ_EXCHANGE="extraction.dlq";

    public static final String DLQ_QUEUE="extraction.link-submitted.dlq";

    public static final String DLQ_ROUTING_KEY="link-submitted.dlq";

    @Bean
    public DirectExchange retryExchange(){
        return new DirectExchange(RETRY_EXCHANGE, true, false);
    }

    @Bean
    public Queue retryQueue(){
        return QueueBuilder.durable(RETRY_QUEUE)
                .withArgument("x-message-ttl", 10_000)
                .withArgument("x-dead-letter-exchange", LINK_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", LINK_SUBMITTED_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding retryBinding(){
        return BindingBuilder.bind(retryQueue())
                .to(retryExchange())
                .with(RETRY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange dlqExchange(){
        return new DirectExchange(DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue(){
        return new Queue(DLQ_QUEUE, true);
    }

    @Bean
    public Binding deadLetterBinding(){
        return BindingBuilder.bind(deadLetterQueue())
                .to(dlqExchange())
                .with(DLQ_ROUTING_KEY);
    }

    @Bean
    public TopicExchange linkExchange() {
        return new TopicExchange(LINK_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange extractionExchange() {
        return new TopicExchange(EXTRACTION_EXCHANGE, true, false);
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
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter jsonMessageConverter){
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        return template;
    }

}
