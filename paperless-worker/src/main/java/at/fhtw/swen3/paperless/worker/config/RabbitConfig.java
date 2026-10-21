package at.fhtw.swen3.paperless.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String DOCUMENT_EVENTS_EXCHANGE = "document.events";
    public static final String DOCUMENT_UPLOADED_ROUTING_KEY = "document.uploaded";
    public static final String OCR_QUEUE = "paperless.ocr";

    @Bean
    public TopicExchange documentEventsExchange() {
        return new TopicExchange(DOCUMENT_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue ocrQueue() {
        return QueueBuilder.durable(OCR_QUEUE).build();
    }

    @Bean
    public Binding ocrBinding(Queue ocrQueue, TopicExchange documentEventsExchange) {
        return BindingBuilder.bind(ocrQueue)
                .to(documentEventsExchange)
                .with(DOCUMENT_UPLOADED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
