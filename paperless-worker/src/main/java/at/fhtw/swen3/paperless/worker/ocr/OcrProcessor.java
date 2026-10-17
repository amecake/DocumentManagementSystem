package at.fhtw.swen3.paperless.worker.ocr;

import at.fhtw.swen3.paperless.worker.messaging.OcrJobMessage;

public interface OcrProcessor {

    String extractText(OcrJobMessage job) throws OcrException;
}
