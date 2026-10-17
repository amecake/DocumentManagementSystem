package at.fhtw.swen3.paperless.worker.messaging;

public record OcrResultMessage(Long documentId, boolean success, String text, String error) {

    public static OcrResultMessage ok(Long documentId, String text) {
        return new OcrResultMessage(documentId, true, text, null);
    }

    public static OcrResultMessage failed(Long documentId, String error) {
        return new OcrResultMessage(documentId, false, null, error);
    }
}
