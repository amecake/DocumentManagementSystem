package at.fhtw.swen3.paperless.worker.messaging;

import at.fhtw.swen3.paperless.worker.ocr.OcrException;
import at.fhtw.swen3.paperless.worker.ocr.OcrProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OcrJobListener {

    private static final Logger log = LoggerFactory.getLogger(OcrJobListener.class);

    private final OcrProcessor processor;
    private final RabbitTemplate rabbitTemplate;
    private final String resultQueue;

    public OcrJobListener(OcrProcessor processor, RabbitTemplate rabbitTemplate,
                          @Value("${paperless.queues.ocr-result}") String resultQueue) {
        this.processor = processor;
        this.rabbitTemplate = rabbitTemplate;
        this.resultQueue = resultQueue;
    }

    @RabbitListener(queues = "${paperless.queues.ocr}")
    public void onJob(OcrJobMessage job) {
        log.info("Received OCR job for document {} ({})", job.documentId(), job.filename());
        OcrResultMessage result;
        try {
            result = OcrResultMessage.ok(job.documentId(), processor.extractText(job));
            log.info("OCR finished for document {}", job.documentId());
        } catch (OcrException e) {
            log.warn("OCR failed for document {}: {}", job.documentId(), e.getMessage());
            result = OcrResultMessage.failed(job.documentId(), e.getMessage());
        }
        rabbitTemplate.convertAndSend(resultQueue, result);
    }
}
