package at.fhtw.swen3.paperless.worker.messaging;

import at.fhtw.swen3.paperless.worker.ocr.OcrException;
import at.fhtw.swen3.paperless.worker.ocr.OcrProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrJobListenerTest {

    @Mock
    private OcrProcessor processor;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OcrJobListener listener;

    @BeforeEach
    void setUp() {
        listener = new OcrJobListener(processor, rabbitTemplate, "ocr.result.queue");
    }

    @Test
    void onJob_success_sendsCompletedResult() throws Exception {
        var job = new OcrJobMessage(1L, "a.pdf");
        when(processor.extractText(job)).thenReturn("text of a");

        listener.onJob(job);

        verify(rabbitTemplate).convertAndSend("ocr.result.queue", OcrResultMessage.ok(1L, "text of a"));
    }

    @Test
    void onJob_processingFails_sendsFailedResult() throws Exception {
        var job = new OcrJobMessage(2L, "broken.pdf");
        when(processor.extractText(job)).thenThrow(new OcrException("unreadable"));

        listener.onJob(job);

        verify(rabbitTemplate).convertAndSend("ocr.result.queue", OcrResultMessage.failed(2L, "unreadable"));
    }
}
