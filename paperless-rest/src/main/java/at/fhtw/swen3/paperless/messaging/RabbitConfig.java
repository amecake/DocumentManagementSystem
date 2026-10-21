package at.fhtw.swen3.paperless.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String DOCUMENT_EVENTS_EXCHANGE = "document.events";
    public static final String DOCUMENT_UPLOADED_ROUTING_KEY = "document.uploaded";

    @Bean
    public TopicExchange documentEventsExchange() {
        return new TopicExchange(DOCUMENT_EVENTS_EXCHANGE);
    }

    // messages are sent as JSON
    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
