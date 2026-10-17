package at.fhtw.swen3.paperless.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitOcrJobPublisher implements OcrJobPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitOcrJobPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String queue;

    public RabbitOcrJobPublisher(RabbitTemplate rabbitTemplate, @Value("${paperless.queues.ocr}") String queue) {
        this.rabbitTemplate = rabbitTemplate;
        this.queue = queue;
    }

    @Override
    public boolean publish(OcrJobMessage job) {
        try {
            rabbitTemplate.convertAndSend(queue, job);
            log.info("OCR job for document {} sent to queue '{}'", job.documentId(), queue);
            return true;
        } catch (AmqpException e) {
            // upload should still work if rabbit is down, the scheduler sends it later
            log.warn("Could not send OCR job for document {}: {}", job.documentId(), e.getMessage());
            return false;
        }
    }
}
