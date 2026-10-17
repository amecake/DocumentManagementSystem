package at.fhtw.swen3.paperless.messaging;

import at.fhtw.swen3.paperless.service.DocumentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OcrResultListener {

    private final DocumentService documentService;

    public OcrResultListener(DocumentService documentService) {
        this.documentService = documentService;
    }

    @RabbitListener(queues = "${paperless.queues.ocr-result}")
    public void onResult(OcrResultMessage result) {
        documentService.applyOcrResult(result);
    }
}
