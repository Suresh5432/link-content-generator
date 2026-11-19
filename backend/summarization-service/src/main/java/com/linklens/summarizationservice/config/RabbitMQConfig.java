package com.linklens.summarizationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXTRACTION_EXCHANGE=
            "extraction.events";

    public static final String CONTENT_EXTRACTED_QUEUE=
            "summarization.content-extracted";

    public static final String CONTENT_EXTRACTED_ROUTING_KEY=
            "content.extracted.v1";

    public static final String RETRY_EXCHANGE=
            "summarization.retry";

    public static final String RETRY_QUEUE=
            "summarization.content-extracted.retry";

    public static final String RETRY_ROUTING_KEY=
            "content-extracted.retry";

    public static final String DLQ_EXCHANGE=
            "summarization.dlq";

    public static final String DLQ_QUEUE=
            "summarization.content-extracted.dlq";

    public static final String DLQ_ROUTING_KEY=
            "content-extracted.dlq";

    @Bean
    public TopicExchange extractionExchange(){
        return new TopicExchange(EXTRACTION_EXCHANGE, true, false);
    }

    @Bean
    public Queue contentExtarctedQueue(){
        return QueueBuilder.durable(CONTENT_EXTRACTED_QUEUE).build();
    }

    @Bean
    public Binding contentExtractedBinding(){
        return BindingBuilder
                .bind(contentExtarctedQueue())
                .to(extractionExchange())
                .with(CONTENT_EXTRACTED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange retryExchange(){
        return new TopicExchange(RETRY_EXCHANGE, true, false);
    }

    @Bean
    public Queue retryQueue(){
        return QueueBuilder.durable(RETRY_QUEUE)
                .withArgument("x-message-ttl",10_000)
                .withArgument("x-dead-letter-exchange",EXTRACTION_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", CONTENT_EXTRACTED_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding retryBinding(){
        return BindingBuilder
                .bind(retryQueue())
                .to(retryExchange())
                .with(RETRY_ROUTING_KEY);
    }

    @Bean
    public TopicExchange dlqExchange(){
        return new TopicExchange(DLQ_EXCHANGE,true,false);
    }

    @Bean
    public Queue deadLetterQueue(){
        return new Queue(DLQ_QUEUE, true);
    }

    @Bean
    public Binding deadLetterBinding(){
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(dlqExchange())
                .with(DLQ_ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){

        RabbitTemplate template=new RabbitTemplate(connectionFactory);

        template.setMandatory(true);
        template.setObservationEnabled(true);
        return template;
    }

}
