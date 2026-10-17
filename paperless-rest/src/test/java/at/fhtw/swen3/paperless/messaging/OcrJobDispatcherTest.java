package at.fhtw.swen3.paperless.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OcrJobDispatcherTest {

    @Mock
    private OcrJobPublisher publisher;

    @Test
    void onDocumentUploaded_publishesOcrJob() {
        new OcrJobDispatcher(publisher).onDocumentUploaded(new DocumentUploadedEvent(5L, "scan.pdf"));

        verify(publisher).publish(new OcrJobMessage(5L, "scan.pdf"));
    }
}
