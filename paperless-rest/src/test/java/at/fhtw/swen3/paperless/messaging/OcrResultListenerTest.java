package at.fhtw.swen3.paperless.messaging;

import at.fhtw.swen3.paperless.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OcrResultListenerTest {

    @Mock
    private DocumentService documentService;

    @Test
    void onResult_passesResultToService() {
        var result = new OcrResultMessage(1L, true, "text", null);

        new OcrResultListener(documentService).onResult(result);

        verify(documentService).applyOcrResult(result);
    }
}
