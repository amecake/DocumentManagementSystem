package at.fhtw.swen3.paperless.messaging;

import at.fhtw.swen3.paperless.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class OcrRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(OcrRetryScheduler.class);

    private final DocumentService documentService;
    private final OcrJobPublisher publisher;
    private final Duration retryAfter;

    public OcrRetryScheduler(DocumentService documentService, OcrJobPublisher publisher,
                             @Value("${paperless.ocr.retry-after}") Duration retryAfter) {
        this.documentService = documentService;
        this.publisher = publisher;
        this.retryAfter = retryAfter;
    }

    // resend jobs that got lost (broker was down, worker crashed, ...)
    @Scheduled(fixedDelayString = "${paperless.ocr.retry-interval}")
    public void requeueStuckDocuments() {
        List<OcrJobMessage> jobs = documentService.markStuckForRetry(retryAfter);
        if (jobs.isEmpty()) {
            return;
        }
        log.info("Re-sending {} OCR job(s) that were not processed within {}", jobs.size(), retryAfter);
        jobs.forEach(publisher::publish);
    }
}
