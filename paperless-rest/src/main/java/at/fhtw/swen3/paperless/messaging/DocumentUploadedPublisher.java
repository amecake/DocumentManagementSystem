package at.fhtw.swen3.paperless.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RabbitPublication(exchange = RabbitConfig.DOCUMENT_EVENTS_EXCHANGE,
        routingKey = RabbitConfig.DOCUMENT_UPLOADED_ROUTING_KEY)
public class DocumentUploadedPublisher extends RabbitEventPublisher<DocumentUploadedEvent> {

    public DocumentUploadedPublisher(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }
}
