package at.fhtw.swen3.paperless.worker.ocr;

import at.fhtw.swen3.paperless.worker.messaging.OcrJobMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

// TODO replace with real OCR (Tesseract) once the PDF files are stored
@Component
public class SimulatedOcrProcessor implements OcrProcessor {

    private static final Logger log = LoggerFactory.getLogger(SimulatedOcrProcessor.class);

    private final Duration delay;

    public SimulatedOcrProcessor(@Value("${paperless.worker.simulated-delay}") Duration delay) {
        this.delay = delay;
    }

    @Override
    public String extractText(OcrJobMessage job) throws OcrException {
        if (job.documentId() == null || !StringUtils.hasText(job.filename())) {
            throw new OcrException("Job is missing documentId or filename");
        }
        log.debug("Simulating OCR for document {} ({} ms)", job.documentId(), delay.toMillis());
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OcrException("Interrupted while processing document " + job.documentId(), e);
        }
        return "Simulated OCR text for " + job.filename();
    }
}
