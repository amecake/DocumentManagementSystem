package at.fhtw.swen3.paperless.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

// only one kind of message: send it straight to the OCR queue (default exchange, queue name as routing key)
@Component
@RabbitPublication(exchange = RabbitConfig.DEFAULT_EXCHANGE, routingKey = RabbitConfig.OCR_QUEUE)
public class DocumentUploadedPublisher extends RabbitEventPublisher<DocumentUploadedEvent>
        implements DocumentEventPublisher {

    public DocumentUploadedPublisher(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }
}
