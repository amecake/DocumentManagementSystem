package at.fhtw.swen3.paperless.messaging;

import at.fhtw.swen3.paperless.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrRetrySchedulerTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private OcrJobPublisher publisher;

    @Test
    void requeueStuckDocuments_republishesEachStuckJob() {
        var a = new OcrJobMessage(1L, "a.pdf");
        var b = new OcrJobMessage(2L, "b.pdf");
        when(documentService.markStuckForRetry(Duration.ofMinutes(2))).thenReturn(List.of(a, b));

        new OcrRetryScheduler(documentService, publisher, Duration.ofMinutes(2)).requeueStuckDocuments();

        verify(publisher).publish(a);
        verify(publisher).publish(b);
    }

    @Test
    void requeueStuckDocuments_nothingStuck_publishesNothing() {
        when(documentService.markStuckForRetry(any())).thenReturn(List.of());

        new OcrRetryScheduler(documentService, publisher, Duration.ofMinutes(2)).requeueStuckDocuments();

        verifyNoInteractions(publisher);
    }
}
