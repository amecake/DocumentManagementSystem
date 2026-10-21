package at.fhtw.swen3.paperless.worker.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class DocumentUploadedListenerTest {

    @Test
    void handleUpload_writesLogEntry(CapturedOutput output) {
        new DocumentUploadedListener().handleUpload(new DocumentUploadedEvent(1L, "Invoice", "invoice.pdf"));

        assertThat(output).contains("OCR worker received document id=1, title='Invoice', file='invoice.pdf'");
    }
}
