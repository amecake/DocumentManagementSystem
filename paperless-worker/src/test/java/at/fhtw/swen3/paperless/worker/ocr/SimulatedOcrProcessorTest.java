package at.fhtw.swen3.paperless.worker.ocr;

import at.fhtw.swen3.paperless.worker.messaging.OcrJobMessage;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatedOcrProcessorTest {

    private final SimulatedOcrProcessor processor = new SimulatedOcrProcessor(Duration.ZERO);

    @Test
    void extractText_returnsTextForFile() throws Exception {
        assertThat(processor.extractText(new OcrJobMessage(1L, "invoice.pdf")))
                .isEqualTo("Simulated OCR text for invoice.pdf");
    }

    @Test
    void extractText_missingFilename_throws() {
        assertThatThrownBy(() -> processor.extractText(new OcrJobMessage(1L, " ")))
                .isInstanceOf(OcrException.class);
    }

    @Test
    void extractText_missingId_throws() {
        assertThatThrownBy(() -> processor.extractText(new OcrJobMessage(null, "a.pdf")))
                .isInstanceOf(OcrException.class);
    }
}
