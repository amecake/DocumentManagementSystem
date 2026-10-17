package at.fhtw.swen3.paperless.worker.messaging;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue ocrQueue(@Value("${paperless.queues.ocr}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    public Queue ocrResultQueue(@Value("${paperless.queues.ocr-result}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
