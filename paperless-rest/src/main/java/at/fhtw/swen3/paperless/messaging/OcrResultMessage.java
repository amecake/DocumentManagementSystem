package at.fhtw.swen3.paperless.messaging;

public record OcrResultMessage(Long documentId, boolean success, String text, String error) {
}
