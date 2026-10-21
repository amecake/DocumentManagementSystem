package at.fhtw.swen3.paperless.worker.messaging;

import at.fhtw.swen3.paperless.worker.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** "Empty" OCR worker: only logs the uploaded document, the OCR itself comes in sprint 4. */
@Component
public class DocumentUploadedListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadedListener.class);

    @RabbitListener(queues = RabbitConfig.OCR_QUEUE)
    public void handleUpload(DocumentUploadedEvent event) {
        log.info("OCR worker received document id={}, title='{}', file='{}'",
                event.documentId(), event.title(), event.filename());
    }
}
