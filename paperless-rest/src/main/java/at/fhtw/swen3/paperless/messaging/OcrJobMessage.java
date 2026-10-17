package at.fhtw.swen3.paperless.messaging;

public record OcrJobMessage(Long documentId, String filename) {
}
