package at.fhtw.swen3.paperless.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OcrJobDispatcher {

    private final OcrJobPublisher publisher;

    public OcrJobDispatcher(OcrJobPublisher publisher) {
        this.publisher = publisher;
    }

    // runs after commit, otherwise the worker could get an id that isn't saved yet
    @TransactionalEventListener
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        publisher.publish(new OcrJobMessage(event.documentId(), event.filename()));
    }
}
